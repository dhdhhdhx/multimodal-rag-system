"""
按章节拆分PDF脚本
使用PyMuPDF读取PDF目录结构，按一级章节拆分成独立PDF文件
"""
import fitz
import os
import re

# ============== 配置 ==============
INPUT_PDF = r"E:\软考\知识点\03-跨越软件设计师考试精讲精练.pdf"
OUTPUT_DIR = r"E:\软考\知识点"
# ==================================

def sanitize_filename(title):
    """清理文件名中的非法字符"""
    # 替换非法字符
    title = re.sub(r'[<>:"/\\|?*]', '_', title)
    # 移除多余空格
    title = re.sub(r'\s+', ' ', title)
    # 限制长度
    if len(title) > 80:
        title = title[:80]
    return title.strip()

def split_pdf_by_chapters(input_path, output_dir):
    """按章节拆分PDF"""
    if not os.path.exists(input_path):
        print(f"错误: 文件不存在 {input_path}")
        return

    os.makedirs(output_dir, exist_ok=True)

    doc = fitz.open(input_path)
    total_pages = len(doc)

    print(f"PDF总页数: {total_pages}")
    print("=" * 50)

    # 获取目录
    toc = doc.get_toc()
    if not toc:
        print("错误: PDF没有目录结构")
        return

    # 找出所有一级章节（以'第X章'开头的）
    chapters = []
    for item in toc:
        level = item[0]
        title = item[1]
        page = item[2]
        if level == 1 and '第' in title and '章' in title:
            chapters.append({'title': title, 'page': page})

    print(f"识别到 {len(chapters)} 个一级章节\n")

    # 为每个章节创建PDF
    for i, chapter in enumerate(chapters):
        chapter_title = sanitize_filename(chapter['title'])
        start_page = chapter['page']

        # 结束页 = 下一章节的起始页 - 1
        if i < len(chapters) - 1:
            end_page = chapters[i + 1]['page'] - 1
        else:
            end_page = total_pages - 1

        # 文件名
        output_name = f"第{i+1:02d}章_{chapter_title}.pdf"
        output_path = os.path.join(output_dir, output_name)

        # 创建新PDF
        writer = fitz.open()
        for page_num in range(start_page - 1, end_page):  # fitz页码从0开始
            writer.insert_pdf(doc, from_page=page_num, to_page=page_num)

        # 保存
        writer.save(output_path)
        writer.close()

        page_count = end_page - start_page + 1
        print(f"[{i+1}/{len(chapters)}] {chapter_title}")
        print(f"      页码: P{start_page}-P{end_page+1} ({page_count}页)")
        print(f"      -> {output_name}\n")

    doc.close()

    print("=" * 50)
    print(f"拆分完成! 共 {len(chapters)} 个章节文件")
    print(f"保存目录: {output_dir}")

if __name__ == "__main__":
    split_pdf_by_chapters(INPUT_PDF, OUTPUT_DIR)
