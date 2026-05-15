/**
 * 生成符合南京工业职业技术大学规范的三线表模板
 */
const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        AlignmentType, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, Header, Footer } = require('docx');
const fs = require('fs');

// A4纸尺寸
const PAGE_WIDTH = 11906;
const PAGE_HEIGHT = 16838;

// 页边距
const TOP_MARGIN = 1418;
const BOTTOM_MARGIN = 1134;
const INNER_MARGIN = 1418;
const OUTER_MARGIN = 1134;
const GUTTER = 284;
const CONTENT_WIDTH = PAGE_WIDTH - INNER_MARGIN - OUTER_MARGIN - GUTTER; // 9354

// 字体大小
const FONT_SIZE_TITLE = 21;  // 五号
const LINE_SPACING_SINGLE = 240;  // 单倍行距

// 三线表边框定义
const border1_5 = { style: BorderStyle.SINGLE, size: 12, color: "000000" };  // 1.5磅 = 12个1/8磅
const border0_75 = { style: BorderStyle.SINGLE, size: 6, color: "000000" };  // 0.75磅 = 6个1/8磅
const noBorder = { style: BorderStyle.NONE };

// 表头边框：顶1.5磅，底0.75磅
const headerBorders = {
    top: border1_5, bottom: border0_75,
    left: noBorder, right: noBorder,
    insideHorizontal: noBorder, insideVertical: noBorder
};

// 数据行边框：无
const dataBorders = {
    top: noBorder, bottom: noBorder,
    left: noBorder, right: noBorder,
    insideHorizontal: noBorder, insideVertical: noBorder
};

// 末行边框：底1.5磅
const lastRowBorders = {
    top: noBorder, bottom: border1_5,
    left: noBorder, right: noBorder
};

// 表级别边框：全部无边框，由单元格控制
const tableBorders = {
    top: noBorder,
    bottom: noBorder,
    left: noBorder,
    right: noBorder,
    insideHorizontal: noBorder,
    insideVertical: noBorder
};

// 创建表题（表上方，黑体五号，居中，单倍行距，段后1行）
function createTableTitle(text) {
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 200, line: LINE_SPACING_SINGLE, lineRule: "exact" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_TITLE })]
    });
}

// 创建三线表
// headers: 表头数组
// rows: 数据行数组
// widths: 每列宽度数组
function createThreeLineTable(headers, rows, widths) {
    const headerCells = headers.map((h, i) =>
        new TableCell({
            borders: headerBorders,
            width: { size: widths[i], type: WidthType.DXA },
            margins: { top: 60, bottom: 60, left: 120, right: 120 },
            verticalAlign: VerticalAlign.CENTER,
            children: [new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { line: LINE_SPACING_SINGLE, lineRule: "exact" },
                children: [new TextRun({ text: h, font: "黑体", size: FONT_SIZE_TITLE, bold: true })]
            })]
        })
    );

    const dataRows = rows.slice(0, -1).map(row =>
        new TableRow({
            children: row.map((cell, i) =>
                new TableCell({
                    borders: dataBorders,
                    width: { size: widths[i], type: WidthType.DXA },
                    margins: { top: 40, bottom: 40, left: 120, right: 120 },
                    children: [new Paragraph({
                        alignment: AlignmentType.LEFT,
                        spacing: { line: LINE_SPACING_SINGLE, lineRule: "exact" },
                        children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_TITLE })]
                    })]
                })
            )
        })
    );

    // 末行数据
    const lastRow = rows[rows.length - 1];
    const lastRowCells = lastRow.map((cell, i) =>
        new TableCell({
            borders: lastRowBorders,
            width: { size: widths[i], type: WidthType.DXA },
            margins: { top: 40, bottom: 40, left: 120, right: 120 },
            children: [new Paragraph({
                alignment: AlignmentType.LEFT,
                spacing: { line: LINE_SPACING_SINGLE, lineRule: "exact" },
                children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_TITLE })]
            })]
        })
    );
    const lastRowElement = new TableRow({ children: lastRowCells });

    return new Table({
        width: { size: CONTENT_WIDTH, type: WidthType.DXA },
        columnWidths: widths,
        borders: tableBorders,
        rows: [new TableRow({ children: headerCells }), ...dataRows, lastRowElement]
    });
}

// 创建正文段落（首行缩进2字符）
function createBodyParagraph(text) {
    return new Paragraph({
        spacing: { line: 360, lineRule: "auto" },
        indent: { firstLine: 567 },
        children: [new TextRun({ text, font: "宋体", size: 28 })]
    });
}

// 创建一级标题
function createH1(text) {
    return new Paragraph({
        heading: 1,
        spacing: { before: 240, after: 240, line: 360, lineRule: "auto" },
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text, font: "黑体", size: 30, bold: true })]
    });
}

// 空行
function spacer() {
    return new Paragraph({ spacing: { line: 360 } });
}

// 生成文档
const doc = new Document({
    styles: {
        default: { document: { run: { font: "宋体", size: 28 } } },
        paragraphStyles: [
            { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
              run: { font: "黑体", size: 30, bold: true },
              paragraph: { spacing: { before: 240, after: 240, line: 360, lineRule: "auto" }, alignment: AlignmentType.CENTER, outlineLevel: 0 } },
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
            // 示例1：角色权限表（2列）
            createH1("表3.1 角色权限对照表示例"),
            spacer(),
            createTableTitle("表3.1 角色权限对照表"),
            createThreeLineTable(
                ["角色", "权限描述"],
                [
                    ["普通会员", "上传与管理个人知识文档、进行语义检索与RAG问答、查看公开知识库"],
                    ["高级会员", "享受更高的检索配额与问答频率、优先使用知识推荐功能"],
                    ["管理员", "用户管理与权限配置、系统运营数据分析、全局知识库监督与审核"],
                ],
                [2000, 7354]
            ),

            spacer(),
            spacer(),

            // 示例2：技术对比表（5列）
            createTableTitle("表2.1 向量数据库对比表"),
            createThreeLineTable(
                ["数据库", "类型", "索引算法", "优点", "适用场景"],
                [
                    ["Milvus", "分布式向量数据库", "HNSW/IVF/PQ", "性能强，扩展性好", "大规模生产环境"],
                    ["PGVector", "PostgreSQL扩展", "HNSW/IVFFlat", "SQL兼容，运维简单", "中小规模，混合检索"],
                    ["Chroma", "嵌入式向量数据库", "HNSW", "轻量易用", "快速原型开发"],
                    ["Weaviate", "分布式向量搜索引擎", "HNSW", "混合检索强", "多模态检索"],
                ],
                [1200, 1600, 1400, 2600, 2554]
            ),

            spacer(),
            spacer(),

            // 示例3：性能指标表
            createTableTitle("表3.2 系统性能需求指标"),
            createThreeLineTable(
                ["指标", "要求"],
                [
                    ["系统并发用户数", "≥300用户同时在线"],
                    ["页面响应时间", "≤3秒（检索、文档列表等页面操作）"],
                    ["智能问答生成时间", "≤8秒（首字输出延迟）"],
                    ["向量检索召回率", "Top-K相关性文档召回准确率≥85%"],
                ],
                [4677, 4677]
            ),

            spacer(),
            spacer(),

            // 示例4：字段说明表（4列）
            createTableTitle("表4.1 users（用户表）"),
            createThreeLineTable(
                ["字段名", "数据类型", "约束", "说明"],
                [
                    ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "用户唯一标识"],
                    ["username", "VARCHAR(50)", "UNIQUE, NOT NULL", "用户名"],
                    ["email", "VARCHAR(100)", "UNIQUE, NOT NULL", "邮箱"],
                    ["password_hash", "VARCHAR(255)", "NOT NULL", "密码哈希（BCrypt）"],
                    ["is_active", "TINYINT(1)", "DEFAULT 1", "是否激活"],
                ],
                [2000, 2400, 2700, 2254]
            ),
        ]
    }]
});

// 生成文件
Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/三线表模板-修正版.docx", buffer);
    console.log("三线表模板已生成: E:/答辩/论文/三线表模板-修正版.docx");
    console.log("\n格式说明:");
    console.log("- 表题：黑体五号，表上方居中，单倍行距，段后1行");
    console.log("- 表头：黑体五号加粗，水平居中，垂直居中");
    console.log("- 表头边框：顶1.5磅，底0.75磅，无竖线");
    console.log("- 数据行：无内部横线和竖线");
    console.log("- 末行边框：底1.5磅");
    console.log("- 表内文字：宋体五号，单倍行距");
});