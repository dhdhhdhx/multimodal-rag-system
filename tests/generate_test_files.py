"""
生成测试文件 - 图片、音频、视频占位文件
"""
import os
from PIL import Image, ImageDraw, ImageFont

BASE_DIR = os.path.dirname(__file__)

def create_test_images():
    """生成测试图片"""
    test_images = [
        ("test_image_cat.jpg", "猫", (255, 200, 150)),
        ("test_image_landscape.jpg", "风景", (150, 200, 255)),
        ("test_image_document.jpg", "文档", (240, 240, 240)),
        ("test_image_chart.jpg", "图表", (200, 220, 180)),
    ]

    for filename, text, color in test_images:
        img = Image.new('RGB', (800, 600), color)
        draw = ImageDraw.Draw(img)

        # 绘制文字
        try:
            font = ImageFont.truetype("arial.ttf", 60)
        except:
            font = ImageFont.load_default()

        text_bbox = draw.textbbox((0, 0), text, font=font)
        text_width = text_bbox[2] - text_bbox[0]
        text_height = text_bbox[3] - text_bbox[1]

        position = ((800 - text_width) // 2, (600 - text_height) // 2)
        draw.text(position, text, fill=(0, 0, 0), font=font)

        # 保存
        filepath = os.path.join(BASE_DIR, filename)
        img.save(filepath, quality=85)
        print(f"✓ 生成测试图片: {filepath}")

if __name__ == "__main__":
    print("开始生成测试文件...")
    create_test_images()
    print("\n✅ 测试文件生成完成！")
    print(f"文件位置: {BASE_DIR}")
