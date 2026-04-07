import os
import torch
import torch.nn.functional as F
from random import randint
from utils.loss_utils import l1_loss, ssim
from gaussian_renderer import render
import sys
from scene import Scene, GaussianModel
from utils.general_utils import safe_state
import uuid
from tqdm import tqdm
from argparse import ArgumentParser, Namespace
from arguments import ModelParams, PipelineParams, OptimizationParams

try:
    from torch.utils.tensorboard import SummaryWriter
    TENSORBOARD_FOUND = True
except ImportError:
    TENSORBOARD_FOUND = False

def prepare_output_and_logger(args):    
    if not args.model_path:
        unique_str = str(uuid.uuid4())
        args.model_path = os.path.join("./output/", unique_str[0:10])
    os.makedirs(args.model_path, exist_ok = True)
    with open(os.path.join(args.model_path, "cfg_args"), 'w') as cfg_log_f:
        cfg_log_f.write(str(Namespace(**vars(args))))
    tb_writer = None
    if TENSORBOARD_FOUND:
        tb_writer = SummaryWriter(args.model_path)
    return tb_writer

def training(dataset, opt, pipe, testing_iterations, saving_iterations, checkpoint_iterations, checkpoint, debug_from):
    first_iter = 0
    tb_writer = prepare_output_and_logger(dataset)
    
    # 初始化模型与场景
    gaussians = GaussianModel(dataset.sh_degree)
    scene = Scene(dataset, gaussians)
    gaussians.training_setup(opt)
   
    if checkpoint:
        (model_params, first_iter) = torch.load(checkpoint)
        gaussians.restore(model_params, opt)

    bg_color = [1, 1, 1] if dataset.white_background else [0, 0, 0]
    background = torch.tensor(bg_color, dtype=torch.float32, device="cuda")

    viewpoint_stack = None
    ema_loss_for_log = 0.0
    ema_semantic_loss_for_log = 0.0

    progress_bar = tqdm(range(first_iter, opt.iterations), desc="Training progress")
    
    for iteration in range(first_iter + 1, opt.iterations + 1):
        gaussians.update_learning_rate(iteration)

        if iteration % 1000 == 0:
            gaussians.oneupSHdegree()

        if not viewpoint_stack:
            viewpoint_stack = scene.getTrainCameras().copy()
        viewpoint_cam = viewpoint_stack.pop(randint(0, len(viewpoint_stack) - 1))

        # 渲染输出
        render_pkg = render(viewpoint_cam, gaussians, pipe, background)
        image = render_pkg["render"]
        viewspace_point_tensor = render_pkg["viewspace_points"]
        visibility_filter = render_pkg["visibility_filter"]
        radii = render_pkg["radii"]
        render_semantic = render_pkg["render_semantic"] 

        # --- 核心修改：处理 Alpha 通道并合成背景 ---
        gt_full = viewpoint_cam.original_image.cuda()
        if gt_full.shape[0] == 4:
            gt_rgb = gt_full[:3, :, :]
            gt_alpha = gt_full[3:4, :, :]
            # 合成公式：Color = RGB * Alpha + BG * (1 - Alpha)
            bg_color_tensor = background.view(3, 1, 1)
            gt_image = gt_rgb * gt_alpha + bg_color_tensor * (1.0 - gt_alpha)
        else:
            gt_image = gt_full

        # RGB 损失
        Ll1 = l1_loss(image, gt_image)
        loss_ssim = 1.0 - ssim(image, gt_image)
        rgb_loss = (1.0 - opt.lambda_dssim) * Ll1 + opt.lambda_dssim * loss_ssim
        
        # 语义蒸馏损失
        semantic_loss =0
        if hasattr(viewpoint_cam, 'sam_features') and viewpoint_cam.sam_features is not None:
            gt_semantic = viewpoint_cam.sam_features.cuda()
            semantic_loss = F.mse_loss(render_semantic, gt_semantic)

        # 总损失回传
        lambda_semantic = getattr(opt, "lambda_semantic", 1.0)
        loss = rgb_loss + lambda_semantic * semantic_loss 
        loss.backward()
        if iteration % 100 == 0:
              print(f"XYZ grad mean: {gaussians._xyz.grad.abs().mean().item()}")

        with torch.no_grad():
            ema_loss_for_log = 0.4 * loss.item() + 0.6 * ema_loss_for_log
            ema_semantic_loss_for_log = 0.4 * semantic_loss.item() + 0.6 * ema_semantic_loss_for_log

            if iteration % 10 == 0:
                progress_bar.set_postfix({"Loss": f"{ema_loss_for_log:.4f}", "Pts": f"{gaussians.get_xyz.shape[0]}"})
                progress_bar.update(10)

            # --- 致密化逻辑：解决正方体问题的关键 ---
            if iteration < opt.densify_until_iter:
                gaussians.max_radii2D[visibility_filter] = torch.max(gaussians.max_radii2D[visibility_filter], radii[visibility_filter])
                gaussians.add_densification_stats(viewspace_point_tensor, visibility_filter)

                if iteration > opt.densify_from_iter and iteration % opt.densification_interval == 0:
                    size_threshold = 20 if iteration > opt.opacity_reset_interval else None
                    gaussians.densify_and_prune(opt.densify_grad_threshold, 0.005, scene.cameras_extent, size_threshold, radii)
                
                if iteration % opt.opacity_reset_interval == 0:
                    gaussians.reset_opacity()

            if iteration < opt.iterations:
                gaussians.optimizer.step()
                gaussians.optimizer.zero_grad(set_to_none = True)

            if iteration in saving_iterations:
                scene.save(iteration)

    print("\nTraining complete.")

if __name__ == "__main__":
    parser = ArgumentParser(description="Training script parameters")
    lp = ModelParams(parser)
    op = OptimizationParams(parser)
    pp = PipelineParams(parser)
    
    # 移除之前冲突的这一行：
    # parser.add_argument("--source_path", type=str, ...) 

    parser.add_argument("--test_iterations", nargs="+", type=int, default=[7000, 30000])
    parser.add_argument("--save_iterations", nargs="+", type=int, default=[7000, 30000])
    parser.add_argument("--quiet", action="store_true")
    # 如果你想保留默认路径，可以在这里检查并在参数为空时手动赋值，或者在命令行输入
    
    args = parser.parse_args(sys.argv[1:])
    args.save_iterations.append(args.iterations)
    
    # [修正方法]：如果命令行没传 source_path，则手动指定默认值
    if not args.source_path:
        args.source_path = "/home/apulis-dev/code/zhan/dataset/nerf_synthetic/lego"
    
    safe_state(args.quiet)
    training(lp.extract(args), op.extract(args), pp.extract(args), args.test_iterations, args.save_iterations, [], None, -1)