"""
生成第六章新增占位符图片（标签管理、话题管理）
"""
from PIL import Image, ImageDraw, ImageFont
import os

OUT_DIR = "E:/答辩/论文"

def create_placeholder_image(title, output_path):
    width, height = 600, 400
    img = Image.new('RGB', (width, height), 'white')
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, width-1, height-1], outline='gray', width=2)
    margin = 30
    draw.rectangle([margin, margin, width-margin, height-margin], outline='lightgray', width=1)
    try:
        font = ImageFont.truetype("simhei.ttf", 20)
    except:
        font = ImageFont.load_default()
    text = title
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    x = (width - text_width) // 2
    y = (height - text_height) // 2
    draw.rectangle([x-20, y-10, x+text_width+20, y+text_height+10], fill='white')
    draw.text((x, y), text, fill='gray', font=font)
    img.save(output_path)
    print(f"已生成: {output_path}")

# 新增占位符图片
images = [
    ("图6.9_标签管理界面", "图6.9 标签管理界面"),
    ("图6.10_话题管理界面", "图6.10 话题管理界面"),
    ("图6.11_数据统计界面", "图6.11 数据统计界面"),
]

for filename, title in images:
    create_placeholder_image(title, os.path.join(OUT_DIR, f"{filename}.png"))

print("\n新增占位符图片生成完成！")