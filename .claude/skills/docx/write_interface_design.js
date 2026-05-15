const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, PageBreak } = require('docx');
const fs = require('fs');

// A4页面参数 (DXA单位)
const PAGE_WIDTH = 11906;
const PAGE_HEIGHT = 16838;
const MARGIN = 1418;

// 字体大小
const FONT_SIZE_BODY = 24;
const FONT_SIZE_H2 = 28;
const FONT_SIZE_H3 = 24;
const FONT_SIZE_FIG = 21;

// 行距
const LINE_SPACING = 360;
const FIRST_LINE_INDENT = 567;

// 三线表边框
const BORDER_1_5 = { style: BorderStyle.SINGLE, size: 12, color: "000000" };
const BORDER_0_75 = { style: BorderStyle.SINGLE, size: 6, color: "000000" };
const BORDER_NONE = { style: BorderStyle.NONE };

const headerBorders = { top: BORDER_1_5, bottom: BORDER_0_75, left: BORDER_NONE, right: BORDER_NONE, insideHorizontal: BORDER_NONE, insideVertical: BORDER_NONE };
const dataBorders = { top: BORDER_NONE, bottom: BORDER_NONE, left: BORDER_NONE, right: BORDER_NONE, insideHorizontal: BORDER_NONE, insideVertical: BORDER_NONE };
const lastRowBorders = { top: BORDER_NONE, bottom: BORDER_1_5, left: BORDER_NONE, right: BORDER_NONE };
const tableBorders = { top: BORDER_NONE, bottom: BORDER_NONE, left: BORDER_NONE, right: BORDER_NONE, insideHorizontal: BORDER_NONE, insideVertical: BORDER_NONE };

// ============ 辅助函数 ============

function body(text) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "auto" },
        indent: { firstLine: FIRST_LINE_INDENT },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY })]
    });
}

function h2(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_2,
        spacing: { before: 180, after: 180, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H2, bold: true })]
    });
}

function h3(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_3,
        spacing: { before: 120, after: 120, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H3, bold: true })]
    });
}

function spacer() {
    return new Paragraph({ spacing: { line: LINE_SPACING } });
}

function tableCaption(num, name) {
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 200, after: 100, line: 240, lineRule: "exact" },
        children: [new TextRun({ text: `表${num} ${name}`, font: "黑体", size: FONT_SIZE_FIG })]
    });
}

function createThreeLineTable(headers, rows, widths) {
    const totalWidth = widths.reduce((a, b) => a + b, 0);

    const headerCells = headers.map((h, i) =>
        new TableCell({
            borders: headerBorders,
            width: { size: widths[i], type: WidthType.DXA },
            margins: { top: 60, bottom: 60, left: 100, right: 100 },
            verticalAlign: VerticalAlign.CENTER,
            children: [new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { line: 240, lineRule: "exact" },
                children: [new TextRun({ text: h, font: "黑体", size: FONT_SIZE_FIG, bold: true })]
            })]
        })
    );

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
                        children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_FIG })]
                    })]
                })
            )
        })
    );

    const lastRowCells = rows[rows.length - 1].map((cell, i) =>
        new TableCell({
            borders: lastRowBorders,
            width: { size: widths[i], type: WidthType.DXA },
            margins: { top: 40, bottom: 40, left: 100, right: 100 },
            children: [new Paragraph({
                alignment: AlignmentType.LEFT,
                spacing: { line: 240, lineRule: "exact" },
                children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_FIG })]
            })]
        })
    );

    return new Table({
        width: { size: totalWidth, type: WidthType.DXA },
        columnWidths: widths,
        borders: tableBorders,
        rows: [new TableRow({ children: headerCells }), ...dataRows, new TableRow({ children: lastRowCells })]
    });
}

function createCodeBlock(code) {
    const lines = code.split('\n');
    return lines.map(line =>
        new Paragraph({
            spacing: { line: 240, lineRule: "exact" },
            indent: { left: 567 },
            children: [new TextRun({ text: line, font: "Consolas", size: 20 })]
        })
    );
}

// ============ 4.5 接口设计 ============
const content = [
    h2("4.5 接口设计"),

    body("系统提供丰富的RESTful API接口供前端和第三方应用调用，所有接口遵循统一的HTTP/HTTPS协议和数据格式规范。系统共开放71个接口，按功能划分为认证接口、知识管理接口、RAG问答接口、话题系统接口、管理接口、推荐接口、统计接口、笔记接口和标注接口九大类。接口采用无状态设计，通信数据格式统一为JSON，身份认证采用JWT令牌机制，管理员接口需验证ADMIN角色权限。以下为各模块接口的详细设计说明。"),

    spacer(),
    h2("4.5.1 认证接口详细设计"),

    h3("（1）接口概述"),
    body("请求方式：POST"),
    body("请求路径：/api/auth/register"),

    spacer(),
    h3("（2）接口功能介绍"),
    body("用户注册接口用于新用户创建账户。系统接收用户提交的用户名、邮箱和密码信息，对密码进行BCrypt加密后存储到数据库，并返回JWT令牌供后续请求使用。注册时系统会验证用户名和邮箱的唯一性，避免重复注册。"),

    spacer(),
    h3("（3）请求参数说明"),
    body("注册接口请求参数包括用户名、邮箱和密码三个字段，其中用户名和邮箱需保证全局唯一。具体参数说明如下表所示："),
    spacer(),
    tableCaption("4.8", "认证接口请求参数说明"),
    createThreeLineTable(
        ["参数名", "类型", "位置", "必填", "说明"],
        [
            ["username", "String", "Body", "是", "用户名，4-20字符，唯一"],
            ["email", "String", "Body", "是", "邮箱地址，唯一"],
            ["password", "String", "Body", "是", "密码，6-20字符"],
            ["fullName", "String", "Body", "否", "真实姓名"]
        ],
        [1500, 1200, 1200, 1200, 3954]
    ),

    spacer(),
    h3("（4）返回参数说明"),
    body("成功响应返回用户基本信息及JWT令牌，失败响应返回错误码和错误信息。具体返回参数说明如下表所示："),
    spacer(),
    tableCaption("4.9", "认证接口返回参数说明"),
    createThreeLineTable(
        ["参数名", "类型", "说明"],
        [
            ["code", "Integer", "状态码，200表示成功"],
            ["message", "String", "操作结果描述"],
            ["data", "Object", "返回数据，包含token和userInfo"],
            ["data.token", "String", "JWT访问令牌"],
            ["data.userId", "Long", "用户ID"],
            ["data.username", "String", "用户名"]
        ],
        [2000, 2000, 5070]
    ),
    spacer(),
    body("成功返回示例："),
    ...createCodeBlock(`{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "testuser"
  }
}`),
    spacer(),
    body("失败返回示例："),
    ...createCodeBlock(`{
  "code": 400,
  "message": "用户名已存在",
  "data": null
}`),

    spacer(),
    h2("4.5.2 文档上传接口详细设计"),

    h3("（1）接口概述"),
    body("请求方式：POST"),
    body("请求路径：/api/knowledge/upload"),

    spacer(),
    h3("（2）接口功能介绍"),
    body("文档上传接口提供多模态文档的上传与自动处理功能。用户通过该接口上传文件后，系统自动进行文件类型检测、内容解析、文本分块及向量生成，并将处理结果存储到数据库中。该接口支持PDF、Word、TXT、图片、音频、视频等多种格式，文件大小限制为200MB。"),

    spacer(),
    h3("（3）请求参数说明"),
    body("上传接口请求参数包括文件数据和认证令牌，其中文件为必填项，认证令牌用于验证用户身份。具体参数说明如下表所示："),
    spacer(),
    tableCaption("4.10", "文档上传接口请求参数说明"),
    createThreeLineTable(
        ["参数名", "类型", "位置", "必填", "说明"],
        [
            ["file", "File", "Body", "是", "上传的文件，支持PDF、Word、TXT、图片、音频、视频"],
            ["Authorization", "String", "Header", "是", "JWT令牌，格式：Bearer {token}"]
        ],
        [1500, 1200, 1200, 1200, 3954]
    ),

    spacer(),
    h3("（4）返回参数说明"),
    body("成功响应返回文档基本信息及处理状态，失败响应返回错误码和错误信息。具体返回参数说明如下表所示："),
    spacer(),
    tableCaption("4.11", "文档上传接口返回参数说明"),
    createThreeLineTable(
        ["参数名", "类型", "说明"],
        [
            ["code", "Integer", "状态码，200表示成功"],
            ["message", "String", "操作结果描述"],
            ["data", "Object", "返回数据"],
            ["data.id", "Long", "文档ID"],
            ["data.fileName", "String", "原始文件名"],
            ["data.modality", "String", "模态类型：TEXT/IMAGE/AUDIO/VIDEO"],
            ["data.status", "String", "处理状态：PROCESSING/COMPLETED/FAILED"],
            ["data.uploadTime", "String", "上传时间"]
        ],
        [2000, 2000, 5070]
    ),
    spacer(),
    body("成功返回示例："),
    ...createCodeBlock(`{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": 123,
    "fileName": "技术文档.pdf",
    "modality": "TEXT",
    "status": "PROCESSING",
    "uploadTime": "2024-01-15 10:30:00"
  }
}`),
    spacer(),
    body("失败返回示例："),
    ...createCodeBlock(`{
  "code": 400,
  "message": "文件大小超出限制（最大200MB）",
  "data": null
}`),

    spacer(),
    h2("4.5.3 RAG问答接口详细设计"),

    h3("（1）接口概述"),
    body("请求方式：POST"),
    body("请求路径：/api/chat"),

    spacer(),
    h3("（2）接口功能介绍"),
    body("RAG问答接口提供基于检索增强生成的智能问答功能。用户提交问题后，系统通过混合检索策略从向量数据库中召回相关文档片段，并将问题与上下文组合发送给大模型生成回答，返回结果包含回答内容及引用来源列表。该接口支持多轮对话，通过sessionId关联会话历史。"),

    spacer(),
    h3("（3）请求参数说明"),
    body("问答接口请求参数包括问题内容、会话ID和认证令牌，其中问题内容和认证令牌为必填项。具体参数说明如下表所示："),
    spacer(),
    tableCaption("4.12", "RAG问答接口请求参数说明"),
    createThreeLineTable(
        ["参数名", "类型", "位置", "必填", "说明"],
        [
            ["query", "String", "Body", "是", "用户问题文本，最大长度500字符"],
            ["sessionId", "Long", "Body", "否", "会话ID，不传则创建新会话"],
            ["Authorization", "String", "Header", "是", "JWT令牌，格式：Bearer {token}"]
        ],
        [1500, 1200, 1200, 1200, 3954]
    ),

    spacer(),
    h3("（4）返回参数说明"),
    body("成功响应返回AI生成的回答及引用来源列表，失败响应返回错误信息。具体返回参数说明如下表所示："),
    spacer(),
    tableCaption("4.13", "RAG问答接口返回参数说明"),
    createThreeLineTable(
        ["参数名", "类型", "说明"],
        [
            ["code", "Integer", "状态码，200表示成功"],
            ["message", "String", "操作结果描述"],
            ["data", "Object", "返回数据"],
            ["data.answer", "String", "AI生成的回答内容"],
            ["data.sources", "Array", "引用来源列表"],
            ["data.sources[].docId", "Long", "文档ID"],
            ["data.sources[].fileName", "String", "文件名"],
            ["data.sources[].excerpt", "String", "相关片段摘录"],
            ["data.sources[].score", "Float", "相似度得分（0-1）"],
            ["data.sessionId", "Long", "会话ID"]
        ],
        [2000, 2000, 5070]
    ),
    spacer(),
    body("成功返回示例："),
    ...createCodeBlock(`{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "根据检索到的文档，RAG是一种将信息检索与大语言模型生成能力相融合的技术架构...",
    "sources": [
      {
        "docId": 45,
        "fileName": "RAG技术综述.pdf",
        "excerpt": "检索增强生成（RAG）是一种将信息检索与大语言模型...",
        "score": 0.87
      }
    ],
    "sessionId": 12
  }
}`),
    spacer(),
    body("失败返回示例："),
    ...createCodeBlock(`{
  "code": 500,
  "message": "模型服务暂时不可用，请稍后重试",
  "data": null
}`),
];

// ============ 生成文档 ============
const doc = new Document({
    sections: [{
        properties: {
            page: {
                size: { width: PAGE_WIDTH, height: PAGE_HEIGHT },
                margin: { top: MARGIN, right: MARGIN, bottom: 1134, left: MARGIN }
            }
        },
        children: content
    }]
});

Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/接口设计详细_v3.docx", buffer);
    console.log("已生成：E:/答辩/论文/接口设计详细_v3.docx");
}).catch(err => {
    console.error(err);
});