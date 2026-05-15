"""
生成第五章类图（PlantUML格式，然后用graphviz渲染）
图5.5 文档管理类图
图5.6 话题系统类图
图5.7 RAG问答类图
"""
from PIL import Image, ImageDraw, ImageFont
import os

# 设置路径
BASE_DIR = "E:/答辩/论文"
OUT_DIR = BASE_DIR

def create_simple_class_diagram(title, classes, output_path):
    """
    创建一个简单的类图
    classes: list of (class_name, methods) tuples
    """
    # 类图尺寸
    box_width = 180
    box_height = 80
    box_spacing = 40
    margin = 60

    # 计算画布大小
    total_width = len(classes) * (box_width + box_spacing) - box_spacing + 2 * margin
    total_height = 200 + 2 * margin

    img = Image.new('RGB', (total_width, total_height), 'white')
    draw = ImageDraw.Draw(img)

    # 尝试加载字体
    try:
        font_title = ImageFont.truetype("simhei.ttf", 16)
        font_class = ImageFont.truetype("simhei.ttf", 12)
        font_method = ImageFont.truetype("simsun.ttc", 10)
    except:
        font_title = ImageFont.load_default()
        font_class = ImageFont.load_default()
        font_method = ImageFont.load_default()

    # 画标题
    title_x = total_width // 2 - 100
    draw.text((title_x, 20), title, fill='black', font=font_title)

    # 画每个类
    for i, (class_name, methods) in enumerate(classes):
        x = margin + i * (box_width + box_spacing)
        y = 60

        # 画类框
        draw.rectangle([x, y, x + box_width, y + box_height], outline='black', width=2)

        # 类名（加粗）
        class_text_y = y + 10
        draw.text((x + 10, class_text_y), class_name, fill='black', font=font_class)

        # 分隔线
        draw.line([x, y + 30, x + box_width, y + 30], fill='black', width=1)

        # 方法列表
        method_text_y = y + 38
        for j, method in enumerate(methods):
            draw.text((x + 10, method_text_y + j * 15), method, fill='black', font=font_method)

    img.save(output_path)
    print(f"已生成: {output_path}")

# 图5.5 文档管理类图
classes_5_5 = [
    ("MultimodalDocument", ["+id: Long", "+title: String", "+content: String", "+type: DocumentType"]),
    ("DocumentController", ["+upload()", "+download()", "+delete()"]),
    ("DocumentService", ["+processUpload()", "+parseDocument()", "+storeMetadata()"]),
    ("PythonEmbeddingClient", ["+getEmbedding()", "+transcribe()"])
]
create_simple_class_diagram("图5.5 文档管理类图", classes_5_5, os.path.join(OUT_DIR, "图5.5_文档管理类图.png"))

# 图5.6 话题系统类图
classes_5_6 = [
    ("Topic", ["+id: Long", "+name: String", "+parentId: Long", "+isPublic: Boolean"]),
    ("TopicController", ["+create()", "+subscribe()", "+recommend()"]),
    ("TopicService", ["+getTree()", "+matchInterests()", "+subscribe()"]),
    ("TopicSubscription", ["+userId: Long", "+topicId: Long", "+subscribeTime: Date"])
]
create_simple_class_diagram("图5.6 话题系统类图", classes_5_6, os.path.join(OUT_DIR, "图5.6_话题系统类图.png"))

# 图5.7 RAG问答类图
classes_5_7 = [
    ("RagService", ["+retrieve()", "+generate()", "+hybridSearch()"]),
    ("VectorStoreService", ["+ similaritySearch()", "+addVectors()"]),
    ("LLMService", ["+chat()", "+summarize()"]),
    ("ChatController", ["+ask()", "+getHistory()"])
]
create_simple_class_diagram("图5.7 RAG问答类图", classes_5_7, os.path.join(OUT_DIR, "图5.7_RAG问答类图.png"))

print("\n所有类图生成完成！")