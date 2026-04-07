#!/usr/bin/env python3
"""
多模态RAG系统测试数据生成器
生成190+测试文件用于系统验证
"""

import os
import random
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import wave
import struct
import math

# 配置
OUTPUT_DIR = Path("../test_data")
TEXT_COUNT = 100
CODE_COUNT = 50
IMAGE_COUNT = 20
AUDIO_COUNT = 10
VIDEO_COUNT = 10  # 视频生成需要ffmpeg，这里生成占位符

# 创建输出目录
(OUTPUT_DIR / "texts").mkdir(parents=True, exist_ok=True)
(OUTPUT_DIR / "codes").mkdir(parents=True, exist_ok=True)
(OUTPUT_DIR / "images").mkdir(parents=True, exist_ok=True)
(OUTPUT_DIR / "audios").mkdir(parents=True, exist_ok=True)
(OUTPUT_DIR / "videos").mkdir(parents=True, exist_ok=True)

print("🚀 开始生成测试数据...")

# ==================== 1. 生成文本文件 (100个) ====================
print(f"\n📝 生成 {TEXT_COUNT} 个文本文件...")

text_templates = [
    "人工智能技术在教育领域的应用研究",
    "大数据分析方法及实践",
    "云计算架构设计与优化",
    "机器学习算法原理详解",
    "深度学习框架比较分析",
    "自然语言处理技术综述",
    "计算机视觉应用案例",
    "区块链技术原理与应用",
    "物联网系统设计方案",
    "网络安全防护策略"
]

for i in range(TEXT_COUNT):
    topic = random.choice(text_templates)
    content = f"""# {topic} - 第{i+1}篇

## 摘要
本文探讨了{topic}的核心概念、技术原理及实际应用场景。通过系统的分析和案例研究，为相关领域的研究者和实践者提供参考。

## 引言
随着信息技术的快速发展，{topic}已成为当前研究的热点。本文将从理论和实践两个层面进行深入探讨。

## 核心内容

### 1. 基本概念
{topic}是指...（此处省略详细内容{i}）

### 2. 技术原理
核心技术包括：
- 技术点A：数据采集与预处理
- 技术点B：模型训练与优化
- 技术点C：系统部署与维护

### 3. 应用场景
实际应用包括：
1. 教育培训领域
2. 企业管理系统
3. 科研数据分析

## 案例分析
某高校采用该技术后，教学效率提升了35%，学生满意度提高了42%。

## 结论
{topic}具有广阔的应用前景和重要的研究价值。

---
文档编号: DOC-{i+1:04d}
生成时间: 2026年1月
关键词: AI, 大数据, 技术应用
"""
    
    filepath = OUTPUT_DIR / "texts" / f"document_{i+1:04d}.txt"
    filepath.write_text(content, encoding='utf-8')

print(f"✅ 已生成 {TEXT_COUNT} 个文本文件")

# ==================== 2. 生成代码文件 (50个) ====================
print(f"\n💻 生成 {CODE_COUNT} 个代码文件...")

code_templates = {
    "python": """# Python示例代码 - {}
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split

class DataProcessor:
    def __init__(self, data_path):
        self.data = pd.read_csv(data_path)
    
    def preprocess(self):
        # 数据预处理
        self.data = self.data.dropna()
        return self.data
    
    def train_model(self):
        X = self.data.drop('target', axis=1)
        y = self.data['target']
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)
        return X_train, X_test, y_train, y_test

if __name__ == '__main__':
    processor = DataProcessor('data.csv')
    processor.preprocess()
    print("数据处理完成")
""",
    "java": """// Java示例代码 - {}
package com.example.demo;

import java.util.*;
import java.util.stream.Collectors;

public class DataAnalyzer {{
    private List<Integer> data;
    
    public DataAnalyzer(List<Integer> data) {{
        this.data = data;
    }}
    
    public double calculateAverage() {{
        return data.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
    }}
    
    public int findMax() {{
        return data.stream()
            .max(Integer::compareTo)
            .orElse(0);
    }}
    
    public static void main(String[] args) {{
        List<Integer> dataset = Arrays.asList(1, 2, 3, 4, 5);
        DataAnalyzer analyzer = new DataAnalyzer(dataset);
        System.out.println("Average: " + analyzer.calculateAverage());
        System.out.println("Max: " + analyzer.findMax());
    }}
}}
""",
    "javascript": """// JavaScript示例代码 - {}
class APIClient {{
    constructor(baseURL) {{
        this.baseURL = baseURL;
        this.headers = {{
            'Content-Type': 'application/json'
        }};
    }}
    
    async fetchData(endpoint) {{
        try {{
            const response = await fetch(`${{this.baseURL}}/${{endpoint}}`, {{
                headers: this.headers
            }});
            return await response.json();
        }} catch (error) {{
            console.error('API请求失败:', error);
            throw error;
        }}
    }}
    
    async postData(endpoint, data) {{
        const response = await fetch(`${{this.baseURL}}/${{endpoint}}`, {{
            method: 'POST',
            headers: this.headers,
            body: JSON.stringify(data)
        }});
        return await response.json();
    }}
}}

export default APIClient;
"""
}

extensions = {'python': '.py', 'java': '.java', 'javascript': '.js'}

for i in range(CODE_COUNT):
    lang = random.choice(list(code_templates.keys()))
    code = code_templates[lang].format(f"模块{i+1}")
    ext = extensions[lang]
    
    filepath = OUTPUT_DIR / "codes" / f"code_{i+1:03d}_{lang}{ext}"
    filepath.write_text(code, encoding='utf-8')

print(f"✅ 已生成 {CODE_COUNT} 个代码文件")

# ==================== 3. 生成图片文件 (20个) ====================
print(f"\n🖼️  生成 {IMAGE_COUNT} 个图片文件...")

for i in range(IMAGE_COUNT):
    # 创建随机颜色的图片
    width, height = 800, 600
    color = (random.randint(50, 255), random.randint(50, 255), random.randint(50, 255))
    img = Image.new('RGB', (width, height), color=color)
    
    # 添加文字
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arial.ttf", 40)
    except:
        font = ImageFont.load_default()
    
    text = f"测试图片 #{i+1}\nMultimodal RAG System\nTest Image"
    draw.text((width//2 - 150, height//2 - 50), text, fill='white', font=font)
    
    # 添加一些图形
    for _ in range(5):
        x1, y1 = random.randint(0, width), random.randint(0, height)
        x2, y2 = random.randint(0, width), random.randint(0, height)
        # 确保坐标顺序正确(左上角到右下角)
        x1, x2 = min(x1, x2), max(x1, x2)
        y1, y2 = min(y1, y2), max(y1, y2)
        shape_color = (random.randint(0, 255), random.randint(0, 255), random.randint(0, 255))
        draw.rectangle([x1, y1, x2, y2], outline=shape_color, width=3)
    
    filepath = OUTPUT_DIR / "images" / f"image_{i+1:03d}.png"
    img.save(filepath)

print(f"✅ 已生成 {IMAGE_COUNT} 个图片文件")

# ==================== 4. 生成音频文件 (10个) ====================
print(f"\n🎵 生成 {AUDIO_COUNT} 个音频文件...")

def generate_tone(frequency, duration, sample_rate=44100):
    """生成指定频率和时长的音调"""
    num_samples = int(sample_rate * duration)
    samples = []
    for i in range(num_samples):
        value = int(32767 * math.sin(2 * math.pi * frequency * i / sample_rate))
        samples.append(value)
    return samples

for i in range(AUDIO_COUNT):
    filepath = OUTPUT_DIR / "audios" / f"audio_{i+1:03d}.wav"
    
    # 生成不同频率的音调（模拟语音）
    frequencies = [440, 523, 587, 659]  # A, C, D, E notes
    duration = 0.5  # 每个音调0.5秒
    sample_rate = 44100
    
    all_samples = []
    for freq in frequencies:
        all_samples.extend(generate_tone(freq, duration, sample_rate))
    
    # 写入WAV文件
    with wave.open(str(filepath), 'w') as wav_file:
        wav_file.setnchannels(1)  # 单声道
        wav_file.setsampwidth(2)  # 16-bit
        wav_file.setframerate(sample_rate)
        
        for sample in all_samples:
            wav_file.writeframes(struct.pack('<h', sample))

print(f"✅ 已生成 {AUDIO_COUNT} 个音频文件")

# ==================== 5. 生成视频占位符 (10个) ====================
print(f"\n🎬 生成 {VIDEO_COUNT} 个视频占位符...")

video_info = """# 视频文件说明 - {}

视频标题: 多模态RAG系统演示视频 #{}
时长: 30秒
分辨率: 1920x1080
格式: MP4

内容描述:
本视频演示了多模态RAG系统的核心功能，包括：
1. 文档上传功能
2. 跨模态语义检索
3. 智能问答交互
4. 结果可视化展示

注意：实际生成视频需要安装ffmpeg工具。
本文件为视频元数据描述。

如需生成真实视频，请运行:
ffmpeg -f lavfi -i testsrc=duration=30:size=1920x1080:rate=30 -pix_fmt yuv420p video_{:03d}.mp4
"""

for i in range(VIDEO_COUNT):
    filepath = OUTPUT_DIR / "videos" / f"video_{i+1:03d}_metadata.txt"
    filepath.write_text(video_info.format(i+1, i+1, i+1), encoding='utf-8')

print(f"✅ 已生成 {VIDEO_COUNT} 个视频占位符文件")

# ==================== 汇总统计 ====================
print("\n" + "="*60)
print("📊 测试数据生成完成！")
print("="*60)
print(f"文本文件: {TEXT_COUNT} 个")
print(f"代码文件: {CODE_COUNT} 个")
print(f"图片文件: {IMAGE_COUNT} 个")
print(f"音频文件: {AUDIO_COUNT} 个")
print(f"视频文件: {VIDEO_COUNT} 个（元数据）")
print(f"总计: {TEXT_COUNT + CODE_COUNT + IMAGE_COUNT + AUDIO_COUNT + VIDEO_COUNT} 个文件")
print(f"\n输出目录: {OUTPUT_DIR.absolute()}")
print("="*60)
