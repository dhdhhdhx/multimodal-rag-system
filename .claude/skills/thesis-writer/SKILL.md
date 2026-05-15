---
name: thesis-writer
description: |
  毕业论文（设计）论文章节写作技能。当用户要求撰写毕业论文章节、生成论文Word文档、按照论文撰写规范排版、或要求将论文内容写入.docx文件时触发此技能。

  典型场景：
  - "帮我写第X章"
  - "将内容写入docx文件"
  - "按照论文规范生成Word文档"
  - "继续撰写论文"
  - "生成毕业论文"

  此技能封装了从内容撰写到Word文档生成的全流程，包括：
  - 中文学术论文内容撰写
  - 论文格式规范（中国高校本硕博论文格式）
  - docx.js库生成Word文档
  - 常见错误与解决方案
---

# 毕业论文写作技能

## 工作流程概述

撰写毕业论文并生成Word文档的标准流程：

```
1. 明确论文规范 → 2. 查阅参考文献 → 3. 撰写章节内容 → 4. 控制引用比例 → 5. 生成docx文档 → 6. 校验输出
```

**关键约束**：
- 引用比例不得超过 **7%**
- 引用前必须先读取原文PDF确认内容准确性
- 优先使用转述而非直接引用

---

## 第一步：明确论文格式规范

在开始撰写前，必须了解目标院校的论文撰写规范。典型规范包含：

### 页面设置（A4纸）
- 纸张大小：11906 × 16838 DXA（A4）
- 页边距：上2.5cm、下2cm、内侧2.5cm、外侧2cm、装订线0.5cm
- 页码：摘要目录用罗马数字，正文起阿拉伯数字（-1-格式）
- 装订：左侧装订

### 字体字号
| 位置 | 中文 | 英文/数字 | 大小 |
|------|------|----------|------|
| 正文 | 宋体 | Times New Roman | 小四(14pt)/28半磅 |
| 一级标题 | 黑体 | Arial | 小三(15pt)/30半磅 |
| 二级标题 | 黑体 | Arial | 四号(14pt)/28半磅 |
| 三级标题 | 黑体 | Arial | 小四(14pt)/28半磅 |
| 四级标题 | 宋体 | Times New Roman | 小四(14pt)/28半磅 |

### 行距与缩进
- 正文：1.5倍行距
- 段落：首行缩进2字符（约567 DXA，1字符≈567 DXA @ 小四14pt）
- 一级标题：段前间距1行
- 章节标题前：空一行

### 标题层级格式
```
一级标题：第X章 XXXX（小三号黑体居中，段前1行）
二级标题：X.X XXXX（四号黑体顶格）
三级标题：X.X.X XXXX（小四号黑体顶格）
四级标题：（1）（2）...（小四号宋体，缩进）
```

### 图表规范
- **图名**：五号黑体，在图**下方**居中，单倍行距，段后1行（先放图片，再放图名）
- **表题**：五号黑体，在表上方居中，单倍行距，段后1行
- **表格**：采用三线表（上下1.5磅，中间0.75磅，无竖线）
- **三线表边框代码关键点**：
  - 顶线/底线：sz="12"（1.5磅）
  - 中间分割线：sz="6"（0.75磅）
  - 表级别tblBorders必须设为none，否则会覆盖单元格边框
  - `borders: tableBorders`（tableBorders所有方向为none）
- **表内文字**：五号宋体/Times New Roman，单倍行距
- 图表编号：按章节顺序（如图1.1、表2.3）

### 页眉页脚
- **页眉**：奇数页"南京工业职业技术大学毕业论文（设计）"，偶数页"论文题目"；宋体五号，居中
- **页脚**：-页码-格式，宋体五号，居中
- 封面、诚信承诺书：无页码
- 摘要、目录：小写罗马数字
- 正文（第一章起）：阿拉伯数字

### 代码实现注意
插入图片和图名时，必须先放图片，再放图名：
```javascript
// 正确顺序：图片在前，图名在后
createImagePara("图片路径.png"),  // 图片
figCaption("图1.1 图片名称"),      // 图名在下方

// 错误顺序：图名在上，图片在下（不符合论文格式）
figCaption("图1.1 图片名称"),      // 错误：图名在上方
createImagePara("图片路径.png"),  // 错误：图片在下方
```

---

## 第二步：撰写章节内容

### 内容结构建议

论文正文通常包含7-8章：

```
第1章 绪论
  1.1 研究背景
  1.2 国内外研究现状
  1.3 研究意义
  1.4 论文结构

第2章 相关技术与理论基础
  2.1 核心技术1
  2.2 核心技术2
  2.3 ...

第3章 系统需求分析
  3.1 需求概述
  3.2 功能需求
  3.3 非功能需求
  3.4 用例分析

第4章 系统设计
  4.1 总体架构
  4.2 技术选型
  4.3 功能模块设计
  4.4 数据库设计
  4.5 接口设计

第5章 系统实现
  5.1 开发环境
  5.2 核心模块实现
  5.3 代码示例

第6章 系统测试
  6.1 测试环境
  6.2 功能测试
  6.3 性能测试
  6.4 测试结果分析

第7章 总结与展望
  7.1 工作总结
  7.2 未来展望
```

### 写作注意事项

1. **语言规范**：使用学术书面语，避免口语化表达
2. **术语统一**：全文同一术语表示同一概念
3. **引用规范**：正确引用文献，遵循GB/T 7714-2015
4. **字数要求**：本科论文≥12000字，专科≥8000字
5. **图表要求**：每章至少2-3张图表，图表需有自明性

---

## 第三步：生成Word文档

### 环境准备

1. **Node.js环境**（必须）
   ```bash
   node -v  # 确认Node.js已安装
   ```

2. **安装docx包**（全局）
   ```bash
   npm install -g docx
   ```

3. **确认全局模块路径**
   ```bash
   npm root -g
   # 输出类似：D:\java\nvm\v24.8.0\node_modules
   ```

### 生成文档流程

**方法一：新建文档（推荐）**

当文档不存在或可以接受覆盖时，直接用docx.js生成：

```javascript
const { Document, Packer, Paragraph, TextRun, HeadingLevel,
        AlignmentType, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, PageBreak } = require('docx');
const fs = require('fs');

// A4纸：11906 × 16838 DXA
const PAGE_WIDTH = 11906;
const PAGE_HEIGHT = 16838;

// 页边距：上2.5cm=1418, 下2cm=1134, 内侧2cm=1134, 外侧2cm=1134, 装订线0.5cm=284
const TOP_MARGIN = 1418;
const BOTTOM_MARGIN = 1134;
const INNER_MARGIN = 1134;
const OUTER_MARGIN = 1134;
const GUTTER = 284;

// 正文字体大小（半磅）：小四=24
const FONT_SIZE_BODY = 24;
// 标题字体大小
const FONT_SIZE_H1 = 30;  // 小三
const FONT_SIZE_H2 = 28;  // 四号
const FONT_SIZE_H3 = 24;  // 小四

// 行距：1.5倍 = 360
const LINE_SPACING = 360;
// 首行缩进：2字符 ≈ 567 DXA（1字符≈567 DXA @ 12pt）
const FIRST_LINE_INDENT = 567;

// 创建正文段落
function createBodyParagraph(text) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "auto" },
        indent: { firstLine: FIRST_LINE_INDENT },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY })]
    });
}

// 创建一级标题（居中）
function createH1(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_1,
        spacing: { before: 240, after: 240, line: LINE_SPACING, lineRule: "auto" },
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H1, bold: true })]
    });
}

// 创建二/三级标题（顶格）
function createH2(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_2,
        spacing: { before: 180, after: 180, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H2, bold: true })]
    });
}

function createH3(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_3,
        spacing: { before: 120, after: 120, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H3, bold: true })]
    });
}

// 创建三线表（上下1.5磅，中间0.75磅，无竖线）
// Word sz单位是1/8磅，所以1.5磅=12, 0.75磅=6
function createThreeLineTable(headers, rows, widths) {
    const BORDER_1_5 = { style: BorderStyle.SINGLE, size: 12, color: "000000" };  // 1.5磅
    const BORDER_0_75 = { style: BorderStyle.SINGLE, size: 6, color: "000000" };   // 0.75磅
    const BORDER_NONE = { style: BorderStyle.NONE };

    // 表头：顶1.5磅，底0.75磅
    const headerBorders = {
        top: BORDER_1_5, bottom: BORDER_0_75,
        left: BORDER_NONE, right: BORDER_NONE,
        insideHorizontal: BORDER_NONE, insideVertical: BORDER_NONE
    };
    // 数据行：无边框
    const dataBorders = {
        top: BORDER_NONE, bottom: BORDER_NONE,
        left: BORDER_NONE, right: BORDER_NONE,
        insideHorizontal: BORDER_NONE, insideVertical: BORDER_NONE
    };
    // 末行：底1.5磅
    const lastRowBorders = {
        top: BORDER_NONE, bottom: BORDER_1_5,
        left: BORDER_NONE, right: BORDER_NONE
    };
    // 表级别边框：无（由单元格控制）
    const tableBorders = {
        top: BORDER_NONE, bottom: BORDER_NONE,
        left: BORDER_NONE, right: BORDER_NONE,
        insideHorizontal: BORDER_NONE, insideVertical: BORDER_NONE
    };

    // 计算总宽度
    const totalWidth = widths.reduce((a, b) => a + b, 0);

    // 表头行
    const headerCells = headers.map((h, i) =>
        new TableCell({
            borders: headerBorders,
            width: { size: widths[i], type: WidthType.DXA },
            margins: { top: 60, bottom: 60, left: 100, right: 100 },
            verticalAlign: VerticalAlign.CENTER,
            children: [new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { line: 240, lineRule: "exact" },
                children: [new TextRun({ text: h, font: "黑体", size: FONT_SIZE_FIG || 21, bold: true })]
            })]
        })
    );

    // 数据行（中间行）
    const dataRows = rows.slice(0, -1).map(row =>
        new TableRow({
            children: row.map((cell, i) =>
                new TableCell({
                    borders: dataBorders,
                    width: { size: widths[i], type: WidthType.DXA },
                    margins: { top: 40, bottom: 40, left: 100, right: 100 },
                    children: [new Paragraph({
                        alignment: AlignmentType.LEFT,
                        spacing: { line: 240, lineRule: "exact" },
                        children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_FIG || 21 })]
                    })]
                })
            )
        })
    );

    // 末行
    const lastRowCells = rows[rows.length - 1].map((cell, i) =>
        new TableCell({
            borders: lastRowBorders,
            width: { size: widths[i], type: WidthType.DXA },
            margins: { top: 40, bottom: 40, left: 100, right: 100 },
            children: [new Paragraph({
                alignment: AlignmentType.LEFT,
                spacing: { line: 240, lineRule: "exact" },
                children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_FIG || 21 })]
            })]
        })
    );

    return new Table({
        width: { size: totalWidth, type: WidthType.DXA },
        columnWidths: widths,
        borders: tableBorders,  // 关键：表级别边框设为none，防止覆盖单元格边框
        rows: [new TableRow({ children: headerCells }), ...dataRows, new TableRow({ children: lastRowCells })]
    });
}

// 创建普通表格（全部边框，用于非三线表场景）
function createTable(headers, rows, widths) {
    const border = { style: BorderStyle.SINGLE, size: 1, color: "000000" };
    const borders = { top: border, bottom: border, left: border, right: border };

    const headerCells = headers.map((h, i) =>
        new TableCell({
            borders,
            width: { size: widths[i], type: WidthType.DXA },
            shading: { fill: "D9D9D9", type: ShadingType.CLEAR },
            margins: { top: 80, bottom: 80, left: 120, right: 120 },
            verticalAlign: VerticalAlign.CENTER,
            children: [new Paragraph({
                alignment: AlignmentType.CENTER,
                children: [new TextRun({ text: h, font: "黑体", size: FONT_SIZE_BODY, bold: true })]
            })]
        })
    );

    const dataRows = rows.map(row =>
        new TableRow({
            children: row.map((cell, i) =>
                new TableCell({
                    borders,
                    width: { size: widths[i], type: WidthType.DXA },
                    margins: { top: 80, bottom: 80, left: 120, right: 120 },
                    children: [new Paragraph({
                        children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_BODY })]
                    })]
                })
            )
        })
    );

    return new Table({
        width: { size: 9354, type: WidthType.DXA },  // 内容宽度
        columnWidths: widths,
        rows: [new TableRow({ children: headerCells }), ...dataRows]
    });
}

// 创建文档
const doc = new Document({
    styles: {
        default: { document: { run: { font: "宋体", size: FONT_SIZE_BODY } } },
        paragraphStyles: [
            { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
              run: { font: "黑体", size: FONT_SIZE_H1, bold: true },
              paragraph: { spacing: { before: 240, after: 240 }, alignment: AlignmentType.CENTER, outlineLevel: 0 } },
            { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
              run: { font: "黑体", size: FONT_SIZE_H2, bold: true },
              paragraph: { spacing: { before: 180, after: 180 }, outlineLevel: 1 } },
            { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
              run: { font: "黑体", size: FONT_SIZE_H3, bold: true },
              paragraph: { spacing: { before: 120, after: 120 }, outlineLevel: 2 } },
        ]
    },
    sections: [{
        properties: {
            page: {
                size: { width: PAGE_WIDTH, height: PAGE_HEIGHT },
                margin: { top: TOP_MARGIN, bottom: BOTTOM_MARGIN, left: INNER_MARGIN, right: OUTER_MARGIN, gutter: GUTTER }
            }
        },
        children: [
            // 在这里添加章节内容
        ]
    }]
});

// 生成文件（重要：Windows路径用双反斜杠或正斜杠）
Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/章节名.docx", buffer);
    console.log("文档生成成功！");
});
```

**运行脚本**：
```bash
# Windows环境需要设置NODE_PATH
NODE_PATH="D:/java/nvm/v24.8.0/node_modules" node create_thesis.js
```

### 关键参数速查表

| 参数 | 值 | 说明 |
|------|-----|------|
| `PAGE_WIDTH` | 11906 | A4宽度DXA |
| `PAGE_HEIGHT` | 16838 | A4高度DXA |
| `TOP_MARGIN` | 1418 | 上2.5cm |
| `BOTTOM_MARGIN` | 1134 | 下2cm |
| `INNER_MARGIN` | 1418 | 内侧(装订侧)2.5cm |
| `OUTER_MARGIN` | 1134 | 外侧2cm |
| `GUTTER` | 284 | 装订线0.5cm |
| `FONT_SIZE_BODY` | 28 | 小四=14pt |
| `FONT_SIZE_H1` | 30 | 小三=15pt |
| `FONT_SIZE_H2` | 28 | 四号=14pt |
| `LINE_SPACING` | 360 | 1.5倍行距 |
| `FIRST_LINE_INDENT` | 567 | 首行缩进2字符（1字符≈567 DXA @ 14pt）|

---

## 第四步：追加内容到现有文档

### 方法：重建而非编辑

**重要原则**：追加内容到现有docx文件的最佳方式是重建整个文档，而不是解包编辑XML。

原因：
1. 解包XML需要`defusedxml` Python模块（`pip install defusedxml`）
2. 解包后需要修改多个XML文件（document.xml、styles.xml等）
3. 重新打包后容易出现格式不一致
4. docx.js重建更可靠，且可增量构建内容后一次性输出

### 追加内容的工作流

```
1. 读取目标文档（获取已有章节内容）
2. 在内存中追加新章节
3. 生成新的完整文档
4. 保存为新文件名（或确认原文件未在使用）
```

### 文件锁定问题

**问题**：EBUSY: resource busy or locked

**原因**：目标文件正在被Word或其他程序打开

**解决**：
1. 关闭所有打开该文件的程序
2. 使用新文件名保存（如`第2-4章完成版.docx`）
3. 避免覆盖正在编辑的文档

---

## 常见错误与解决方案

### 错误1：Cannot find module 'docx'

**原因**：Node.js找不到全局安装的docx模块

**解决**：
```bash
# 方法1：设置NODE_PATH环境变量
NODE_PATH=$(npm root -g) node create_thesis.js

# Windows示例：
NODE_PATH="D:/java/nvm/v24.8.0/node_modules" node create_thesis.js

# 方法2：在项目目录本地安装
npm install docx
```

### 错误2：EBUSY: resource busy or locked

**原因**：目标文件被占用

**解决**：
1. 关闭占用文件的程序（Word、浏览器等）
2. 使用新文件名输出
3. 确认没有其他进程在访问该文件

### 错误3：路径语法错误

**问题**：Windows路径中的反斜杠导致语法错误

```javascript
// 错误示例
fs.writeFileSync("E:\答辩\论文\文件.docx", buffer);
// SyntaxError

// 正确写法
fs.writeFileSync("E:/答辩/论文/文件.docx", buffer);
// 或
fs.writeFileSync("E:\\答辩\\论文\\文件.docx", buffer);
```

### 错误4：defusedxml模块未找到

**问题**：运行unpack.py时报错

```bash
ModuleNotFoundError: No module named 'defusedxml'
```

**解决**：
```bash
pip install defusedxml
```

### 错误5：文档格式错乱

**常见原因**：
1. 字体未正确设置（中文要用宋体/黑体，不是Arial）
2. 行距/缩进未设置
3. 表格宽度计算错误

**检查清单**：
- [ ] 正文字体：`font: "宋体"`
- [ ] 标题字体：`font: "黑体"`
- [ ] 行距：`spacing: { line: 360 }`
- [ ] 首行缩进：`indent: { firstLine: 567 }`
- [ ] 表格宽度：所有列宽之和等于 CONTENT_WIDTH

### 错误6：页边距设置不生效

**原因**：docx-js的margin单位理解错误

**正确理解**：
- DXA单位：1440 DXA = 1英寸 = 2.54cm
- 换算：1cm ≈ 567 DXA

| 边距 | DXA值 |
|------|-------|
| 1cm | 567 |
| 2cm | 1134 |
| 2.5cm | 1418 |

---

## 内容撰写模板

### 摘要模板

```
摘要（居中，黑体小三号，1.5倍行距）

[研究目的] 本文针对...[研究问题]...

[方法] 采用...技术/方法...

[结果] 实验结果表明...

[结论] 本系统实现了...，为...提供了...

关键词：关键词1、关键词2、关键词3（黑体小三号，关键词之间用顿号或逗号分隔）
```

### 章节小结模板

```
X.X 本章小结

本章介绍了/分析了/设计了...。通过...，有效解决了...问题。...技术为...奠定了基础。上述工作为后续章节的...提供了支撑。
```

### 用例描述模板

```
用例编号：UC-XXX
用例名称：[功能名称]
参与者：[用户角色]
前置条件：[系统状态]
基本流程：
1. [步骤1]
2. [步骤2]
...
N. [成功响应]
备选流程：
- [异常1] → [处理方式]
- [异常2] → [处理方式]
```

---

## 输出文件命名建议

```
格式：第X-Y章-章节名.docx
示例：
  第2章-相关技术与理论基础.docx
  第2-3章-理论基础与需求分析.docx
  第2-4章-系统设计完成版.docx
```

---

## 快速参考命令

```bash
# 1. 检查Node.js
node -v

# 2. 安装docx（全局）
npm install -g docx

# 3. 查找全局模块路径
npm root -g

# 4. 运行生成脚本
NODE_PATH="$(npm root -g)" node create_thesis.js

# 5. 安装Python依赖（解包docx用）
pip install defusedxml

# 6. 解包docx（可选，了解内部结构）
python .claude/skills/docx/scripts/office/unpack.py input.docx unpacked/
```

---

## 参考文献调用规范（重要）

### 参考文献目录

用户提供的参考文献存放在：`E:\答辩\参考文献pdf`

**使用规则**：
1. 撰写论文时，如需引用某个概念、技术或数据，**必须先调用该PDF文件确认原文内容**
2. 引用格式遵循GB/T 7714-2015标准
3. 参考文献只引用**实际参考过的**，避免虚假引用

### 查重率控制（重要）

**核心原则**：全文引用比例不得超过 **7%**

**计算方式**：
```
引用率 = (直接引用字数 + 间接引用转述字数) / 论文字数总数 × 100%
```

**控制策略**：
1. **多用转述，少用直引**：用自己的话描述技术原理，降低直接引用
2. **精简引用来源**：每个技术点引用1-2篇代表性文献即可，不要重复堆砌
3. **控制引用长度**：单次引用不超过2-3句话
4. **增加原创内容**：系统实现细节、测试数据、性能分析等内容属于原创，不计入引用

**警告**：引用率过高会导致查重不通过，请务必控制！

### 正确引用示例

```markdown
// 正确写法（转述为主）
Spring AI是Spring生态下的AI应用框架，它通过统一的抽象接口简化了大模型集成的工程复杂度[1]。

// 错误写法（引用过长）
检索增强生成（RAG）是一种将信息检索与大语言模型生成能力相融合的技术架构[1][2][3][4][5]。
```

### 文献检索流程

当需要引用某个概念时：
1. 先用Glob或Bash列出 `E:\答辩\参考文献pdf\` 下的PDF文件
2. 找到相关文献后用Read工具读取PDF内容
3. 确认引用内容的准确性
4. 在论文正文中用`[序号]`格式标注
5. 在参考文献列表中补充完整条目

---

## 项目特定上下文

如果论文是关于多模态RAG知识管理系统的，可以参考以下项目信息：

**项目路径**：`E:\ai-framework\multimodal-rag-system`

**技术栈**：
- 后端：Spring Boot 3.4.1 + Spring AI 1.0.0-M4 + Java 17
- 前端：Vue3 + Vite + Element Plus
- 向量库：PGVector（PostgreSQL扩展）
- AI服务：Python FastAPI（CLIP、Whisper、Sentence-Transformers）
- 大模型：阿里云Dashscope（Qwen-Plus）

**关键文件**：
- 配置：`backend/src/main/resources/application.yml`
- 核心服务：`RagService.java`、`KnowledgeService.java`、`VectorStoreService.java`
- 实体类：`backend/src/main/java/com/multimodal/rag/model/`

**开题报告参考**：`E:\答辩\开题报告\开题报告-答辩修改版.docx`

**论文模板参考**：`E:\答辩\模板\附件5：南京工业职业技术大学本科毕业论文（设计）撰写规范.docx`

---

如需继续撰写下一章，请告诉我要写第几章，我会按照当前文档的内容结构继续生成。
