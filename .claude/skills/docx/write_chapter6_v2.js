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

function h4(text) {
    return new Paragraph({
        spacing: { before: 80, after: 80, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY, bold: true })]
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

function pageBreak() {
    return new Paragraph({ children: [new PageBreak()] });
}

function figCaption(text) {
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 60, after: 200, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_FIG })]
    });
}

function tableCaption(num, name) {
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 200, after: 100, line: 240, lineRule: "exact" },
        children: [new TextRun({ text: `表${num} ${name}`, font: "黑体", size: FONT_SIZE_FIG })]
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
    return new Paragraph({ children: [new TextRun({ text: `[图片: ${imagePath}]`, font: "宋体", size: FONT_SIZE_BODY, italics: true })] });
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

// ============ 第6章 系统实现 ============
const chapter6 = [
    h1("第6章 系统实现"),

    body("本章详细介绍系统的具体实现过程。首先说明系统的开发环境与工具配置，然后重点阐述文档上传处理模块和RAG智能问答模块的实现细节，最后展示系统的主要界面实现效果。"),

    spacer(),
    h2("6.1 系统开发环境与工具"),

    h3("6.1.1 硬件与软件环境"),

    body("系统开发及运行所使用的硬件和软件环境如下表所示。开发工作站采用普通个人计算机即可满足开发调试需求，服务器部署时可根据用户规模选择更高配置。"),

    spacer(),
    tableCaption("6.1", "系统开发与运行环境"),
    createThreeLineTable(
        ["类别", "名称/版本", "说明"],
        [
            ["开发工作站", "Intel Core i7 / 16GB / 512GB SSD", "日常开发调试"],
            ["操作系统", "Windows 11 / Ubuntu 22.04", "开发与部署环境"],
            ["Java运行时", "JDK 17", "Spring Boot后端服务"],
            ["Python运行时", "Python 3.11", "AI微服务"],
            ["Node.js", "v18+", "前端开发构建"],
            ["IDE", "IntelliJ IDEA / PyCharm / VSCode", "开发工具"]
        ],
        [2000, 3500, 3570]
    ),

    spacer(),
    h3("6.1.2 核心技术栈"),

    body("系统采用前后端分离架构，后端基于Spring Boot框架实现业务逻辑，Python微服务负责多模态embedding生成，前端基于Vue.js构建用户交互界面。核心技术栈如下表所示。"),

    spacer(),
    tableCaption("6.2", "核心技术栈"),
    createThreeLineTable(
        ["层次", "技术选型", "版本/说明"],
        [
            ["后端框架", "Spring Boot + Spring AI", "3.4.1 / AI应用框架"],
            ["前端框架", "Vue3 + Vite + Element Plus", "3.x / 组件库"],
            ["Python服务", "FastAPI + PyTorch", "AI推理服务"],
            ["关系数据库", "MySQL", "8.0 / 业务数据存储"],
            ["向量数据库", "PostgreSQL + PgVector", "16.x / 语义向量存储"],
            ["缓存服务", "Redis", "7.0+ / 会话缓存与计数缓冲"],
            ["容器化", "Docker + Docker Compose", "24.0 / 环境编排"]
        ],
        [2000, 3500, 3570]
    ),

    spacer(),
    h3("6.1.3 开发环境搭建"),

    body("系统采用Docker容器化部署数据库和中间件服务，开发环境搭建主要包含以下步骤："),

    body("（1）Docker环境配置。首先安装Docker Desktop for Windows，然后通过docker-compose.yml文件定义所有服务依赖，包括MySQL数据库、PostgreSQL向量数据库和Redis缓存服务。PgVector扩展在PostgreSQL容器启动时自动安装配置，无需手动处理。"),

    body("（2）MySQL配置。建立knowledge_management数据库，配置字符集为utf8mb4以支持中文存储。创建专用数据库用户，配置合理的连接池参数。"),

    body("（3）PostgreSQL/PgVector配置。建立vector_db数据库，启用pgvector扩展后即可支持向量存储和相似度检索。配置HNSW索引参数以优化检索性能。"),

    body("（4）Redis配置。用于会话缓存和浏览计数缓冲，设置合理的过期时间和内存限制。"),

    body("（5）后端服务配置。在application.yml中配置数据库连接信息、Redis连接、JWT密钥等。Python服务地址配置为Docker Compose定义的服务名。"),

    spacer(),
    h3("6.1.4 项目组织结构"),

    body("系统采用分层架构设计，后端项目结构如下表所示。"),

    spacer(),
    tableCaption("6.3", "后端项目结构"),
    createThreeLineTable(
        ["包名", "职责", "主要类"],
        [
            ["controller", "处理HTTP请求", "AdminController、ChatController等"],
            ["service", "业务逻辑处理", "RagService、TopicService等"],
            ["repository", "数据访问层", "JPA Repository接口"],
            ["model", "实体类定义", "User、Document、Topic等"],
            ["config", "配置类", "SecurityConfig、RedisConfig等"],
            ["client", "外部服务调用", "PythonEmbeddingClient等"],
            ["security", "安全认证", "JwtTokenProvider、JwtAuthenticationFilter"]
        ],
        [2000, 3000, 4354]
    ),

    spacer(),
    h2("6.2 核心功能模块实现"),

    h3("6.2.1 文档上传与处理模块"),

    body("文档上传与处理模块负责接收用户上传的多模态文件，并完成内容提取和向量生成存储。该模块的核心类包括DocumentController、DocumentService和PythonEmbeddingClient。"),

    body("模块功能概述：用户通过前端页面上传文件后，后端首先保存文件到指定存储位置，然后调用Python服务进行模态检测。系统根据文件类型选择不同的处理流程：文本类文件通过Apache Tika解析，图像类通过CLIP模型生成向量，音视频类通过Whisper转写后处理。最终处理结果存储到MySQL和PgVector数据库中。"),

    spacer(),
    h4("（1）关键类设计"),

    body("DocumentController接收文件上传请求，调用DocumentService处理核心逻辑。PythonEmbeddingClient负责与Python服务通信，生成文本或图像的向量表示。VectorStoreService封装向量存储操作，将向量写入PgVector并建立HNSW索引。"),

    spacer(),
    h4("（2）核心流程实现"),

    body("文档上传处理的核心流程如图6.1所示。系统接收到上传文件后，首先根据文件扩展名和Magic Bytes判断文件模态类型。对于文本类文件，系统调用Apache Tika解析器提取正文内容，按固定长度分块后调用embedding接口生成向量。对于图像文件，系统调用CLIP模型生成512维向量，通过VectorAligner降维到384维后存储。音视频文件则通过Whisper模型转写为文本，再进入文本处理流程。"),

    spacer(),
    createImagePara("E:/答辩/论文/图5.2_文档上传处理时序图.png"),
    figCaption("图6.1 文档上传处理时序图"),

    spacer(),
    h4("（3）实现难点与解决方案"),

    body("多模态文件处理的主要难点在于不同类型文件的解析和向量生成速度差异较大。系统采用异步处理策略，文件上传后立即返回处理中的状态，前端通过轮询或WebSocket获取处理进度。向量降维通过预计算的投影矩阵实现，避免了实时矩阵运算的性能开销。"),

    spacer(),
    h3("6.2.2 RAG智能问答模块"),

    body("RAG智能问答模块是系统的核心功能入口，实现基于检索增强生成的问答能力。该模块的核心类是RagService，配合VectorStoreService进行向量检索，通过LLMService调用大语言模型生成回答。"),

    body("模块功能概述：用户提交问题后，系统首先通过混合检索策略从向量数据库中召回相关文档片段，然后将问题与上下文组装成Prompt发送给大模型。大模型生成的回答包含引用来源，用户可以点击跳转到原始文档。系统支持多轮对话，通过sessionId关联会话历史。"),

    spacer(),
    h4("（1）关键类设计"),

    body("RagService是RAG流程的核心编排类，负责协调检索、上下文构建和生成三个阶段。HybridSearchService实现混合检索，结合向量相似度和关键词匹配两种方法。LLMService封装与大模型服务的通信，处理重试和异常情况。ChatController维护会话历史，支持多轮对话。"),

    spacer(),
    h4("（2）核心流程实现"),

    body("RAG问答的核心流程如图6.2所示。用户问题首先进行预处理（分词、去停用词），然后并行执行向量检索和关键词检索两路操作。结果合并去重后，按综合评分排序。上下文构建阶段将相关文档片段组装，并按用户等级限制总长度（普通用户6000字符，高级用户9000字符）。最后将Prompt发送给Qwen-Plus大模型生成回答。"),

    spacer(),
    createImagePara("E:/答辩/论文/图5.4_RAG问答时序图.png"),
    figCaption("图6.2 RAG问答时序图"),

    spacer(),
    h4("（3）实现难点与解决方案"),

    body("RAG问答的主要难点在于检索质量和响应速度的平衡。系统采用混合检索策略弥补单一向量检索的不足，关键词检索可以召回向量相似但文字表达差异大的文档。向量检索使用HNSW索引加速，检索时间控制在200ms以内。大模型调用实现3次重试机制，处理瞬时网络错误。"),

    spacer(),
    h2("6.3 用户界面实现"),

    h3("6.3.1 前台功能界面"),

    body("前台功能界面面向普通用户，提供文档管理、知识检索和智能问答等服务。用户注册登录后可上传和管理个人文档，通过搜索或话题浏览发现内容，并使用AI助手进行知识问答。前台主要界面如图6.3至图6.5所示。"),

    spacer(),
    createImagePara("E:/答辩/论文/图6.1_用户注册登录界面.png"),
    figCaption("图6.3 用户注册登录界面"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.2_文档管理界面.png"),
    figCaption("图6.4 文档管理界面"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.6_智能问答界面.png"),
    figCaption("图6.5 智能问答界面"),

    spacer(),
    h3("6.3.2 后台管理界面"),

    body("后台管理界面面向系统管理员，提供用户管理、内容管理和数据统计等功能。管理员可以查看和管理系统用户，调整用户权限，审核和删除违规文档，监控系统的运行状况。后台主要界面如图6.6和图6.7所示。"),

    spacer(),
    createImagePara("E:/答辩/论文/图6.7_用户管理界面.png"),
    figCaption("图6.6 用户管理界面"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.11_数据统计界面.png"),
    figCaption("图6.7 数据统计界面"),

    spacer(),
    h2("6.4 本章小结"),

    body("本章详细介绍了系统的具体实现过程。首先说明了系统的开发环境配置，包括硬件软件环境、核心技术栈、Docker环境搭建步骤和项目组织结构。然后重点阐述了文档上传处理模块和RAG智能问答模块的实现细节，包括关键类设计、核心流程和实现难点。最后展示了系统前台用户界面和管理后台界面的实现效果。本章内容为系统测试和部署奠定了基础。"),
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
        children: chapter6
    }]
});

Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/第6章_系统实现_v2.docx", buffer);
    console.log("已生成：E:/答辩/论文/第6章_系统实现_v2.docx");
}).catch(err => {
    console.error(err);
});