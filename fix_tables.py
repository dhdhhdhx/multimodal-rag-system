#!/usr/bin/env python3
"""修复文档中所有表格的三线表边框格式 - 清洁版"""

import re
import os
import subprocess

def fix_single_table(table_xml):
    """修复单个表格"""

    # 1. 将表级别的tblBorders全部改为none
    table_xml = re.sub(
        r'<w:tblBorders>.*?</w:tblBorders>',
        '''<w:tblBorders>
          <w:top w:val="none"/>
          <w:left w:val="none"/>
          <w:bottom w:val="none"/>
          <w:right w:val="none"/>
          <w:insideH w:val="none"/>
          <w:insideV w:val="none"/>
        </w:tblBorders>''',
        table_xml,
        flags=re.DOTALL
    )

    # 删除tblPrEx（如果有的话）
    table_xml = re.sub(r'<w:tblPrEx>.*?</w:tblPrEx>', '', table_xml, flags=re.DOTALL)

    # 2. 解析并处理每一行
    rows = list(re.finditer(r'<w:tr\b.*?</w:tr>', table_xml, re.DOTALL))
    if not rows:
        return table_xml

    # 处理每一行的单元格
    new_rows = []
    for row_idx, row_match in enumerate(rows):
        row_xml = row_match.group(0)
        is_first = (row_idx == 0)
        is_last = (row_idx == len(rows) - 1)

        def replace_cell_borders(cell_match):
            cell = cell_match.group(0)
            if is_first:
                # 首行: top=1.5磅(12), bottom=0.75磅(6)
                cell = re.sub(r'<w:tcBorders>.*?</w:tcBorders>',
                    '''<w:tcBorders>
                      <w:top w:val="single" w:color="000000" w:sz="12"/>
                      <w:left w:val="none"/>
                      <w:bottom w:val="single" w:color="000000" w:sz="6"/>
                      <w:right w:val="none"/>
                    </w:tcBorders>''',
                    cell, flags=re.DOTALL)
            elif is_last:
                # 末行: top=none, bottom=1.5磅(12)
                cell = re.sub(r'<w:tcBorders>.*?</w:tcBorders>',
                    '''<w:tcBorders>
                      <w:top w:val="none"/>
                      <w:left w:val="none"/>
                      <w:bottom w:val="single" w:color="000000" w:sz="12"/>
                      <w:right w:val="none"/>
                    </w:tcBorders>''',
                    cell, flags=re.DOTALL)
            else:
                # 中间行: 全部无边框
                cell = re.sub(r'<w:tcBorders>.*?</w:tcBorders>',
                    '''<w:tcBorders>
                      <w:top w:val="none"/>
                      <w:left w:val="none"/>
                      <w:bottom w:val="none"/>
                      <w:right w:val="none"/>
                    </w:tcBorders>''',
                    cell, flags=re.DOTALL)
            return cell

        row_xml = re.sub(r'<w:tc>.*?</w:tc>', replace_cell_borders, row_xml, flags=re.DOTALL)
        new_rows.append(row_xml)

    # 重新组装表格
    rows_str = ''.join(new_rows)
    table_xml = re.sub(r'<w:tr\b.*?</w:tr>.*?</w:tbl>',
                       rows_str + '</w:tbl>',
                       table_xml, flags=re.DOTALL)

    return table_xml

def main():
    unpacked_dir = "unpacked_draft"
    original_docx = "E:/答辩/论文/初稿.docx"
    output_docx = "E:/答辩/论文/初稿_三线表修正版.docx"

    doc_path = os.path.join(unpacked_dir, "word", "document.xml")
    with open(doc_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 找出所有表格并逐个处理
    tables = list(re.finditer(r'<w:tbl>.*?</w:tbl>', content, re.DOTALL))
    print(f"找到 {len(tables)} 个表格")

    new_content = content
    offset = 0
    for table_match in tables:
        start = table_match.start() + offset
        end = table_match.end() + offset
        original_table = content[table_match.start():table_match.end()]
        fixed_table = fix_single_table(original_table)
        new_content = content[:start] + fixed_table + content[end:]
        content = new_content
        offset += len(fixed_table) - len(original_table)

    with open(doc_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    # 重新打包
    result = subprocess.run(
        ['python', 'scripts/office/pack.py', unpacked_dir, output_docx,
         '--original', original_docx, '--validate', 'false'],
        cwd=".claude/skills/docx",
        capture_output=True, text=True
    )
    print(result.stdout)
    if result.stderr:
        print(result.stderr)
    print(f"输出文件: {output_docx}")

if __name__ == "__main__":
    main()