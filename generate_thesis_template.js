/**
 * 生成符合南京工业职业技术大学论文规范的Word模板
 */
const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, Header, Footer, PageBreak } = require('docx');
const fs = require('fs');

// A4纸尺寸 (单位: DXA, 1cm = 567 DXA)
const PAGE_WIDTH = 11906;      // A4宽度
const PAGE_HEIGHT = 16838;    // A4高度

// 页边距
const TOP_MARGIN = 1418;       // 上2.5cm
const BOTTOM_MARGIN = 1134;    // 下2cm
const INNER_MARGIN = 1418;     // 内侧(装订侧)2.5cm
const OUTER_MARGIN = 1134;     // 外侧2cm
const GUTTER = 284;            // 装订线0.5cm

// 字体大小 (半磅, 小四=28半磅=14pt, 小三=30半磅=15pt, 四号=28半磅=14pt)
const FONT_SIZE_BODY = 28;     // 小四
const FONT_SIZE_H1 = 30;       // 小三
const FONT_SIZE_H2 = 28;       // 四号
const FONT_SIZE_H3 = 24;       // 小四
const FONT_SIZE_FIGURE = 20;   // 五号

// 行距 (1.5倍 = 360)
const LINE_SPACING = 360;

// 首行缩进 2字符 (约567 DXA @ 小四14pt)
const FIRST_LINE_INDENT = 567;

// 段落间距
const PARAGRAPH_SPACING_1 = 240;  // 1行间距

// 创建正文段落
function createBodyParagraph(text) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "auto" },
        indent: { firstLine: FIRST_LINE_INDENT },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY, fontCs: "Times New Roman" })]
    });
}

// 创建一级标题 (第X章)
function createH1(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_1,
        spacing: { before: 240, after: 240, line: LINE_SPACING, lineRule: "auto" },
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H1, bold: true })]
    });
}

// 创建二级标题 (X.X)
function createH2(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_2,
        spacing: { before: 180, after: 180, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H2, bold: true })]
    });
}

// 创建三级标题 (X.X.X)
function createH3(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_3,
        spacing: { before: 120, after: 120, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H3, bold: true })]
    });
}

// 创建四级标题 ((1)(2)...)
function createH4(text) {
    return new Paragraph({
        spacing: { before: 120, after: 120, line: LINE_SPACING, lineRule: "auto" },
        indent: { left: 567 },  // 缩进
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY, bold: true })]
    });
}

// 创建图题 (在图下方, 五号黑体, 居中, 单倍行距)
function figCaption(text) {
    return new Paragraph({
        spacing: { line: 240, lineRule: "exact", after: 200 },  // 单倍行距, 段后1行
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_FIGURE })]
    });
}

// 创建表题 (在表上方, 五号黑体, 居中)
function tableTitle(text) {
    return new Paragraph({
        spacing: { line: 240, lineRule: "exact", after: 200 },
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_FIGURE })]
    });
}

// 创建三线表
function createThreeLineTable(headers, rows, widths) {
    const border1_5 = { style: BorderStyle.SINGLE, size: 18, color: "000000" };  // 1.5磅
    const border0_75 = { style: BorderStyle.SINGLE, size: 8, color: "000000" };  // 0.75磅
    const noBorder = { style: BorderStyle.NIL };

    const borders = {
        top: border1_5, bottom: border1_5,
        left: noBorder, right: noBorder,
        insideHorizontal: noBorder, insideVertical: noBorder
    };

    const headerBorders = {
        top: border1_5, bottom: border0_75,
        left: noBorder, right: noBorder,
        insideHorizontal: noBorder, insideVertical: noBorder
    };

    const headerCells = headers.map((h, i) =>
        new TableCell({
            borders: headerBorders,
            width: { size: widths[i], type: WidthType.DXA },
            margins: { top: 60, bottom: 60, left: 120, right: 120 },
            verticalAlign: VerticalAlign.CENTER,
            children: [new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { line: 240, lineRule: "exact" },
                children: [new TextRun({ text: h, font: "黑体", size: FONT_SIZE_FIGURE })]
            })]
        })
    );

    const dataRows = rows.map(row =>
        new TableRow({
            children: row.map((cell, i) =>
                new TableCell({
                    borders: {
                        top: noBorder, bottom: noBorder,
                        left: noBorder, right: noBorder,
                        insideHorizontal: noBorder, insideVertical: noBorder
                    },
                    width: { size: widths[i], type: WidthType.DXA },
                    margins: { top: 40, bottom: 40, left: 120, right: 120 },
                    children: [new Paragraph({
                        spacing: { line: 240, lineRule: "exact" },
                        children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_FIGURE })]
                    })]
                })
            )
        })
    );

    return new Table({
        width: { size: 9354, type: WidthType.DXA },
        columnWidths: widths,
        rows: [new TableRow({ children: headerCells }), ...dataRows]
    });
}

// 创建文档
const doc = new Document({
    styles: {
        default: {
            document: {
                run: { font: "宋体", size: FONT_SIZE_BODY }
            }
        },
        paragraphStyles: [
            {
                id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
                run: { font: "黑体", size: FONT_SIZE_H1, bold: true },
                paragraph: {
                    spacing: { before: 240, after: 240 },
                    alignment: AlignmentType.CENTER,
                    outlineLevel: 0
                }
            },
            {
                id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
                run: { font: "黑体", size: FONT_SIZE_H2, bold: true },
                paragraph: {
                    spacing: { before: 180, after: 180 },
                    outlineLevel: 1
                }
            },
            {
                id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
                run: { font: "黑体", size: FONT_SIZE_H3, bold: true },
                paragraph: {
                    spacing: { before: 120, after: 120 },
                    outlineLevel: 2
                }
            },
        ]
    },
    sections: [{
        properties: {
            page: {
                size: { width: PAGE_WIDTH, height: PAGE_HEIGHT },
                margin: {
                    top: TOP_MARGIN,
                    bottom: BOTTOM_MARGIN,
                    left: INNER_MARGIN,
                    right: OUTER_MARGIN,
                    gutter: GUTTER
                }
            }
        },
        headers: {
            default: new Header({
                children: [new Paragraph({
                    alignment: AlignmentType.CENTER,
                    children: [new TextRun({
                        text: "南京工业职业技术大学毕业论文（设计）",
                        font: "宋体",
                        size: 20  // 五号
                    })]
                })]
            })
        },
        footers: {
            default: new Footer({
                children: [new Paragraph({
                    alignment: AlignmentType.CENTER,
                    children: [
                        new TextRun({ text: "-", font: "宋体", size: 20 }),
                        new TextRun({ children: [PageNumber.CURRENT], font: "宋体", size: 20 }),
                        new TextRun({ text: "-", font: "宋体", size: 20 })
                    ]
                })]
            })
        },
        children: [
            // 演示一级标题
            createH1("第1章 绪论"),
            createBodyParagraph("本章主要介绍研究背景与意义..."),
            createH2("1.1 研究背景"),
            createBodyParagraph("随着信息技术的快速发展..."),

            // 演示二级标题
            createH2("1.2 国内外研究现状"),
            createH3("1.2.1 RAG技术研究进展"),
            createBodyParagraph("检索增强生成（Retrieval-Augmented Generation, RAG）..."),

            // 演示四级标题
            createH4("（1）技术发展历程"),
            createBodyParagraph("第一阶段是简单检索阶段..."),

            // 空行
            new Paragraph({ children: [new TextRun("")] }),

            // 演示三线表
            tableTitle("表1.1 技术对比表"),
            createThreeLineTable(
                ["指标", "技术A", "技术B"],
                [
                    ["准确率", "95%", "92%"],
                    ["响应速度", "200ms", "300ms"]
                ],
                [3118, 3118, 3118]
            ),

            // 空行
            new Paragraph({ children: [new TextRun("")] }),

            // 演示图题
            figCaption("图1.1 系统架构图"),
        ]
    }]
});

// 生成文件
Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/论文格式规范模板.docx", buffer);
    console.log("论文格式规范模板已生成: E:/答辩/论文/论文格式规范模板.docx");
    console.log("\n格式说明:");
    console.log("- A4纸张, 页边距: 上2.5cm, 下2cm, 内侧2.5cm, 外侧2cm, 装订线0.5cm");
    console.log("- 正文: 宋体小四, 1.5倍行距, 首行缩进2字符");
    console.log("- 一级标题: 黑体小三, 居中, 段前1行");
    console.log("- 二级标题: 黑体四号, 左对齐");
    console.log("- 三级标题: 黑体小四号, 左对齐");
    console.log("- 四级标题: (1)(2)..., 宋体小四, 缩进");
    console.log("- 图题: 黑体五号, 居中, 单倍行距, 段后1行");
    console.log("- 表题: 黑体五号, 居中, 单倍行距, 段后1行");
    console.log("- 三线表: 上下1.5磅, 中间0.75磅, 无竖线");
    console.log("- 页眉: 居中, 宋体五号");
    console.log("- 页脚: -页码-, 宋体五号");
});