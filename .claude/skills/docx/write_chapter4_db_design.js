const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, ImageRun,
        AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, PageBreak } = require('docx');
const fs = require('fs');

// A4页面参数 (DXA单位)
const PAGE_WIDTH = 11906;
const PAGE_HEIGHT = 16838;
const MARGIN = 1418;

// 字体大小
const FONT_SIZE_BODY = 24;
const FONT_SIZE_H1 = 30;
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

function h1(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_1,
        spacing: { before: 240, after: 240, line: LINE_SPACING, lineRule: "auto" },
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H1, bold: true })]
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

function body(text) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "auto" },
        indent: { firstLine: FIRST_LINE_INDENT },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY })]
    });
}

function spacer() {
    return new Paragraph({ spacing: { line: LINE_SPACING } });
}

function figCaption(text) {
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 60, after: 200, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_FIG })]
    });
}

function createImagePara(imagePath) {
    try {
        if (fs.existsSync(imagePath)) {
            const imageBuffer = fs.readFileSync(imagePath);
            return new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 120, after: 60 },
                children: [new ImageRun({
                    data: imageBuffer,
                    transformation: { width: 550, height: 350 },
                    type: "png"
                })]
            });
        }
    } catch (e) {}
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 120, after: 60 },
        children: [new TextRun({ text: `[图片: ${imagePath}]`, font: "宋体", size: FONT_SIZE_BODY, italics: true })]
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

// ============ 第4章 数据库设计 ============
const chapter4 = [
    h1("第4章 数据库设计"),

    h2("4.1 数据库设计概述"),

    h3("4.1.1 设计原则与命名规范"),

    body("本系统采用关系型数据库与向量数据库混合存储架构。良好的数据库设计能够确保数据的完整性、一致性与可扩展性，支撑起上层业务逻辑的高效运转，是系统长期稳定运行的重要基础。本系统在设计过程中综合考量了以下方面：首先是整体ER建模，明确系统核心实体（用户、文档、话题、问答会话等）及其关联关系；其次是根据业务功能划分数据表模块，包括用户权限模块、文档管理模块、话题系统模块、问答笔记模块等；最后针对核心数据表制定详细的字段设计规范，明确字段类型、主键约束、外键关联与索引策略，为后续编码实现提供清晰蓝图。"),

    body("数据库命名遵循以下规范：表名与字段名均采用有明确语义的英文命名，以字母开头避免与保留字冲突；主键统一采用自增Long型整型id；外键字段以_id后缀命名以示区分；时间字段统一使用created_at和updated_at记录创建与修改时间。"),

    spacer(),
    h3("4.1.2 全局E-R图"),

    body("系统的核心实体包括用户（User）、多模态文档（MultimodalDocument）、话题（Topic）和问答会话（ChatSession）四大类。用户与文档之间为一对多关系，即一个用户可以上传并管理多份文档；用户与话题之间存在多对多关系，用户可以订阅多个感兴趣的话题，话题也可以被多个用户订阅；文档与话题之间同样为多对多关系，一份文档可归属多个话题，一个话题可包含多份文档；用户与问答会话为一对多关系，一个用户可以发起多个独立的问答会话，每个会话内部又包含多条问答消息。如图4-1所示。"),

    spacer(),
    createImagePara("E:/答辩/论文/图4.1_全局E-R图.png"),
    figCaption("图4-1 全局E-R图"),

    spacer(),
    h2("4.2 数据表模块划分与功能说明"),

    h3("4.2.1 用户与权限模块"),

    body("用户与权限模块负责管理系统用户的基本信息、角色定义与操作权限，是整个系统安全机制的基础。该模块包含以下数据表："),

    body("· 用户信息表（User）——存储系统用户的基本信息，包括用户名、邮箱、加密密码、角色类型及账户状态。"),
    body("· 角色定义表（Role）——定义系统内的角色类型，包括普通用户、高级用户和管理员三种角色。"),
    body("· 权限定义表（Permission）——列举系统内所有可被控制的操作权限，角色与权限之间通过多对多关联实现灵活的权限分配。"),

    body("各表关系如图4-2所示。"),

    spacer(),
    createImagePara("E:/答辩/论文/图4.2_用户与权限E-R图.png"),
    figCaption("图4-2 用户与权限E-R图"),

    spacer(),
    h3("4.2.2 文档管理模块"),

    body("文档管理模块负责存储和管理用户上传的多模态文档元数据，以及文档的访问行为记录。该模块包含以下数据表："),

    body("· 多模态文档表（MultimodalDocument）——存储文档的元数据信息，包括标题、存储路径、模态类型、文本内容、标签及浏览量等核心字段，是RAG检索流程的数据主体。"),
    body("· 文档访问日志表（DocumentAccessLog）——记录用户对文档的访问行为，包括访问者ID、访问时间等信息，为浏览量统计和用户行为分析提供数据支撑。"),

    spacer(),
    h3("4.2.3 话题系统模块"),

    body("话题系统模块支撑起系统的知识组织与社交发现功能。该模块包含以下数据表："),

    body("· 话题表（Topic）——存储用户创建的知识话题信息，包括话题名称、描述、公开状态及订阅人数等。"),
    body("· 话题-文档关联表（TopicDocument）——建立话题与多模态文档之间的多对多关系，支持知识的交叉组织与多维度检索。"),
    body("· 用户话题订阅表（TopicSubscription）——记录用户对话题的订阅关系，订阅后用户可在个人中心查看话题动态。"),

    body("各表关系如图4-3所示。"),

    spacer(),
    createImagePara("E:/答辩/论文/图4.3_话题系统E-R图.png"),
    figCaption("图4-3 话题系统E-R图"),

    spacer(),
    h3("4.2.4 问答与笔记模块"),

    body("问答与笔记模块是系统RAG智能问答功能的核心支撑，同时提供知识笔记的沉淀能力。该模块包含以下数据表："),

    body("· 问答会话表（ChatSession）——以会话为单位组织多轮对话，用户可以为每次问答会话命名以便后续查阅。"),
    body("· 问答消息记录表（ChatMessage）——存储对话中的每一条消息，包括用户提问、检索来源文档及LLM生成回答，是RAG问答流程的完整日志。"),
    body("· 知识笔记表（KnowledgeNote）——支持用户对文档或话题撰写笔记，实现知识的二次沉淀与个性化整理。"),
    body("· 查询日志表（QueryLog）——记录用户的检索行为与检索结果反馈，为后续检索排序优化提供数据基础。"),

    body("各表关系如图4-4所示。"),

    spacer(),
    createImagePara("E:/答辩/论文/图4.4_问答与笔记E-R图.png"),
    figCaption("图4-4 问答与笔记E-R图"),

    spacer(),
    h3("4.2.5 辅助功能模块"),

    body("辅助功能模块包含内容标注功能，支持用户对文档内容或问答答案进行高亮和批注，实现重点信息的标记与二次查阅。"),

    body("· 内容标注表（ContentAnnotation）——存储用户的标注行为记录，包括标注类型（高亮或批注）、标注目标及标注内容。"),

    spacer(),
    h2("4.3 核心数据表详细设计"),

    body("本节选取系统中最为核心的5张数据表进行详细设计说明，涵盖多模态文档、用户、话题、问答消息和知识笔记等关键业务实体。"),

    spacer(),
    h3("4.3.1 多模态文档表（MultimodalDocument）详细设计"),

    body("多模态文档表是系统的核心数据表之一，用于存储用户上传的所有类型文档的元数据信息，包括文档标题、存储路径、模态类型、提取的文本内容、标签分类及浏览量统计等关键字段，是RAG检索流程的数据主体，如表4-1所示。"),

    spacer(),
    createThreeLineTable(
        ["字段名", "数据类型", "是否主键", "是否为空", "说明"],
        [
            ["id", "BIGINT", "是", "否", "主键，自增"],
            ["title", "VARCHAR(255)", "否", "否", "文档标题"],
            ["file_path", "VARCHAR(512)", "否", "否", "文件存储路径（阿里云OSS）"],
            ["file_size", "BIGINT", "否", "是", "文件大小（字节）"],
            ["modality", "VARCHAR(20)", "否", "否", "模态类型：TEXT/IMAGE/AUDIO/VIDEO"],
            ["content_text", "TEXT", "否", "是", "提取的文本内容"],
            ["tags", "VARCHAR(500)", "否", "是", "标签，逗号分隔"],
            ["owner_id", "BIGINT", "否", "否", "所有者用户ID"],
            ["view_count", "INT", "否", "否", "浏览次数，默认0"],
            ["status", "VARCHAR(20)", "否", "否", "状态：PROCESSING/SUCCESS/FAILED"],
            ["created_at", "TIMESTAMP", "否", "否", "创建时间"],
            ["updated_at", "TIMESTAMP", "否", "否", "更新时间"]
        ],
        [1800, 2200, 1200, 1200, 3670]
    ),
    spacer(),
    figCaption("表4-1 多模态文档表（MultimodalDocument）详细设计"),

    spacer(),
    h3("4.3.2 用户表（User）详细设计"),

    body("用户表是系统用户体系的基础表，存储用户的注册信息和账户状态，用于登录认证和个性化服务提供，如表4-2所示。"),

    spacer(),
    createThreeLineTable(
        ["字段名", "数据类型", "是否主键", "是否为空", "说明"],
        [
            ["id", "BIGINT", "是", "否", "主键，自增"],
            ["username", "VARCHAR(50)", "否", "否", "用户名，唯一"],
            ["email", "VARCHAR(100)", "否", "否", "邮箱，唯一"],
            ["password_hash", "VARCHAR(255)", "否", "否", "BCrypt加密密码"],
            ["role", "VARCHAR(20)", "否", "否", "角色：USER/PREMIUM/ADMIN"],
            ["status", "VARCHAR(20)", "否", "否", "账户状态：ACTIVE/DISABLED"],
            ["interest_tags", "VARCHAR(500)", "否", "是", "兴趣标签，用于推荐"],
            ["created_at", "TIMESTAMP", "否", "否", "注册时间"],
            ["updated_at", "TIMESTAMP", "否", "否", "更新时间"]
        ],
        [1800, 2200, 1200, 1200, 3670]
    ),
    spacer(),
    figCaption("表4-2 用户表（User）详细设计"),

    spacer(),
    h3("4.3.3 话题表（Topic）详细设计"),

    body("话题表用于存储用户创建的知识话题信息，是话题系统功能的核心数据表，如表4-3所示。"),

    spacer(),
    createThreeLineTable(
        ["字段名", "数据类型", "是否主键", "是否为空", "说明"],
        [
            ["id", "BIGINT", "是", "否", "主键，自增"],
            ["name", "VARCHAR(100)", "否", "否", "话题名称"],
            ["description", "TEXT", "否", "是", "话题描述"],
            ["is_public", "TINYINT(1)", "否", "否", "是否公开：0-私有，1-公开"],
            ["owner_id", "BIGINT", "否", "否", "创建者用户ID"],
            ["subscriber_count", "INT", "否", "否", "订阅人数，默认0"],
            ["created_at", "TIMESTAMP", "否", "否", "创建时间"],
            ["updated_at", "TIMESTAMP", "否", "否", "更新时间"]
        ],
        [1800, 2200, 1200, 1200, 3670]
    ),
    spacer(),
    figCaption("表4-3 话题表（Topic）详细设计"),

    spacer(),
    h3("4.3.4 问答消息记录表（ChatMessage）详细设计"),

    body("问答消息记录表是RAG问答流程的完整日志，存储用户每次提问、系统检索来源文档及LLM生成回答，是问答功能的核心数据表，如表4-4所示。"),

    spacer(),
    createThreeLineTable(
        ["字段名", "数据类型", "是否主键", "是否为空", "说明"],
        [
            ["id", "BIGINT", "是", "否", "主键，自增"],
            ["session_id", "BIGINT", "否", "否", "所属会话ID"],
            ["sender_type", "VARCHAR(20)", "否", "否", "发送者类型：USER/ASSISTANT"],
            ["query_text", "TEXT", "否", "是", "用户提问内容"],
            ["answer_text", "TEXT", "否", "是", "LLM生成回答"],
            ["source_documents", "TEXT", "否", "是", "检索来源文档JSON"],
            ["created_at", "TIMESTAMP", "否", "否", "创建时间"]
        ],
        [1800, 2200, 1200, 1200, 3670]
    ),
    spacer(),
    figCaption("表4-4 问答消息记录表（ChatMessage）详细设计"),

    spacer(),
    h3("4.3.5 知识笔记表（KnowledgeNote）详细设计"),

    body("知识笔记表用于存储用户对文档或话题的笔记记录，实现知识的二次沉淀与个性化整理，是用户知识积累功能的核心数据表，如表4-5所示。"),

    spacer(),
    createThreeLineTable(
        ["字段名", "数据类型", "是否主键", "是否为空", "说明"],
        [
            ["id", "BIGINT", "是", "否", "主键，自增"],
            ["user_id", "BIGINT", "否", "否", "笔记所属用户ID"],
            ["document_id", "BIGINT", "否", "是", "关联文档ID"],
            ["topic_id", "BIGINT", "否", "是", "关联话题ID"],
            ["title", "VARCHAR(200)", "否", "否", "笔记标题"],
            ["content", "TEXT", "否", "否", "笔记正文内容"],
            ["created_at", "TIMESTAMP", "否", "否", "创建时间"],
            ["updated_at", "TIMESTAMP", "否", "否", "更新时间"]
        ],
        [1800, 2200, 1200, 1200, 3670]
    ),
    spacer(),
    figCaption("表4-5 知识笔记表（KnowledgeNote）详细设计"),
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
        children: chapter4
    }]
});

Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/第4章_数据库设计_v3.docx", buffer);
    console.log("已生成：E:/答辩/论文/第4章_数据库设计_v3.docx");
}).catch(err => {
    console.error(err);
});
