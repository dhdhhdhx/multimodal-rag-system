const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, PageBreak, ImageRun } = require('docx');
const fs = require('fs');
const path = require('path');

// A4页面参数 (DXA单位)
const PAGE_WIDTH = 11906;
const PAGE_HEIGHT = 16838;
const MARGIN = 1418;
const CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2; // 9070

// 字体大小
const FONT_SIZE_BODY = 24;  // 小四
const FONT_SIZE_H1 = 30;    // 小三
const FONT_SIZE_H2 = 28;    // 四号
const FONT_SIZE_H3 = 24;    // 小四
const FONT_SIZE_FIG = 21;   // 五号

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

function bodyNoIndent(text) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY })]
    });
}

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

function spacer() {
    return new Paragraph({ spacing: { line: LINE_SPACING } });
}

function pageBreak() {
    return new Paragraph({ children: [new PageBreak()] });
}

function tableCaption(num, name) {
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 200, after: 100, line: 240, lineRule: "exact" },
        children: [new TextRun({ text: `表${num} ${name}`, font: "黑体", size: FONT_SIZE_FIG })]
    });
}

function figCaption(text) {
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 60, after: 200, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_FIG })]
    });
}

function createImagePara(imagePath, width) {
    const absPath = path.resolve(imagePath);
    if (!fs.existsSync(absPath)) {
        return new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { before: 200, after: 100 },
            children: [new TextRun({ text: `【图片缺失: ${imagePath}】`, font: "宋体", size: FONT_SIZE_BODY, color: "FF0000" })]
        });
    }
    const imageBuffer = fs.readFileSync(absPath);
    // 获取图片尺寸
    let height = 300;
    if (absPath.endsWith('.png') && imageBuffer.length > 24) {
        try {
            const w = imageBuffer.readUInt32BE(16);
            const h = imageBuffer.readUInt32BE(20);
            if (w && h) {
                height = Math.round(width * h / w);
            }
        } catch (e) {}
    }
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 120, after: 60, line: LINE_SPACING, lineRule: "auto" },
        children: [
            new ImageRun({
                type: "png",
                data: imageBuffer,
                transformation: { width, height },
                altText: { title: "图", description: "图", name: "图" }
            })
        ]
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

// ============ 第5章内容 ============
const chapter5Content = [
    h1("第5章 系统实现"),

    body("本章详细介绍基于RAG技术的多模态知识管理系统的具体实现过程。系统采用前后端分离的微服务架构，后端基于Spring Boot框架实现业务逻辑，Python微服务负责多模态embedding生成，前端基于Vue.js构建用户交互界面。以下将从开发环境、系统架构、核心模块实现三个方面进行详细说明。"),

    h2("5.1 开发环境"),

    body("本系统的开发环境包括硬件环境和软件环境两部分。硬件环境为开发人员日常使用的计算机，软件环境包括操作系统、开发工具、运行时环境等。"),

    h3("5.1.1 硬件环境"),

    body("开发工作站配置Intel Core i7处理器、16GB内存、512GB固态硬盘，网络环境为校园网高速接入。这种配置能够满足系统开发、调试以及本地测试的需求。"),

    h3("5.1.2 软件环境"),

    body("系统开发和运行所依赖的软件环境如下表所示。"),

    tableCaption("5.1", "开发与运行环境"),
    createThreeLineTable(
        ["类别", "软件/版本", "说明"],
        [
            ["操作系统", "Windows 11 / Ubuntu 22.04", "开发与部署环境"],
            ["Java运行时", "JDK 17", "Spring Boot后端服务"],
            ["Python运行时", "Python 3.11", "AI微服务"],
            ["Node.js", "v18+", "前端开发构建"],
            ["MySQL", "8.0", "关系型数据库"],
            ["PostgreSQL", "16 + PgVector", "向量数据库"],
            ["Redis", "7.0+", "缓存服务"],
            ["Docker", "24.0", "容器化部署"]
        ],
        [2000, 3000, 4070]
    ),

    spacer(),
    h2("5.2 系统架构"),

    body("系统采用前后端分离的微服务架构，主要包含三个服务层：Vue.js构建的前端用户界面、Spring Boot实现的后端REST API服务、以及Python FastAPI实现的AI推理服务。数据层使用MySQL存储结构化元数据，PostgreSQL+PgVector存储向量嵌入，Redis提供缓存支持。"),

    // 系统架构图 - 检查文件是否存在
    (() => {
        const imgPath = "E:/答辩/论文/论文配图/系统层次结构图.png";
        if (fs.existsSync(imgPath)) {
            return [
                createImagePara(imgPath, 650),
                figCaption("图5.1 系统层次结构图"),
            ];
        }
        return [
            new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 200, after: 100 },
                children: [new TextRun({ text: "【图5.1 系统层次结构图】", font: "宋体", size: FONT_SIZE_BODY, color: "FF0000" })]
            }),
            figCaption("图5.1 系统层次结构图"),
        ];
    })(),

    spacer(),
    h2("5.3 后端核心模块实现"),

    body("后端服务是系统的核心业务处理层，采用Spring Boot框架实现RESTful API。本节重点介绍文档上传与解析模块、向量化存储模块、语义检索模块和RAG问答模块的实现。"),

    h3("5.3.1 文档上传与解析模块"),

    body("文档处理模块负责用户上传文件的后续处理，主要包括文件存储、模态检测、内容提取三个环节。当用户通过REST接口上传文档后，系统首先判断文件存储方式（本地或OSS），然后通过Apache Tika进行内容解析，同时调用Python服务的模态检测接口确定文件类型。"),

    body("模态检测采用多级判断策略：首先根据文件扩展名进行初步判断，然后通过文件头信息（Magic Bytes）进行二次校验，最后结合文件大小、采样分析等因素综合确定模态类型。不同模态的文件将进入不同的处理流程。"),

    // 文档上传时序图
    (() => {
        const imgPath = "E:/答辩/论文/图5.2_文档上传处理时序图.png";
        if (fs.existsSync(imgPath)) {
            return [
                createImagePara(imgPath, 700),
                figCaption("图5.2 文档上传处理时序图"),
            ];
        }
        return [
            new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 200, after: 100 },
                children: [new TextRun({ text: "【图5.2 文档上传处理时序图】", font: "宋体", size: FONT_SIZE_BODY, color: "FF0000" })]
            }),
            figCaption("图5.2 文档上传处理时序图"),
        ];
    })(),

    spacer(),
    body("文本类文件（PDF、Word、TXT等）通过Apache Tika解析提取正文内容，系统将内容按固定长度分块（每块800字符，相邻块重叠150字符），然后逐块调用Python服务生成向量存储到PgVector数据库中。图像类文件通过CLIP模型生成512维向量表示，同时通过零样本分类生成描述标签。音频和视频文件首先通过Whisper模型进行语音转写，转写文本再进入文本处理流程。"),

    h3("5.3.2 向量存储模块"),

    body("向量存储模块基于Spring AI的PgVectorStore实现，负责将文本块及其向量表示存储到PostgreSQL数据库中。向量表结构包含id、content（文本内容）、metadata（元数据JSON）、embedding（向量数组）四个主要字段，其中embedding字段使用PgVector的VECTOR类型存储，支持HNSW索引加速相似度检索。"),

    body("元数据字段包含documentId、userId、fileType、modality、chunkIndex、totalChunks等信息，用于后续检索时的结果过滤和展示。系统采用批量写入策略，将分块后的文本批量写入向量库，单次批量操作最多处理50个文本块。"),

    body("向量维度统一为384维（与Text Embedding模型输出维度一致）。当图像、视频等多模态数据需要存储时，先通过VectorAligner工具将高维向量投影到384维空间，采用截断SVD算法实现降维，在保留主要语义信息的同时保证与文本向量的兼容性。"),

    h3("5.3.3 语义检索模块"),

    body("语义检索模块采用混合检索策略，结合向量相似度搜索和关键词匹配两种方法。向量检索使用PgVector的cosine similarity计算query与文档的语义相关度，关键词检索则基于MySQL的FULLTEXT索引或LIKE查询实现。"),

    // 混合检索流程图
    (() => {
        const imgPath = "E:/答辩/论文/图5.3_混合检索流程图.png";
        if (fs.existsSync(imgPath)) {
            return [
                createImagePara(imgPath, 550),
                figCaption("图5.3 混合检索流程图"),
            ];
        }
        return [
            new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 200, after: 100 },
                children: [new TextRun({ text: "【图5.3 混合检索流程图】", font: "宋体", size: FONT_SIZE_BODY, color: "FF0000" })]
            }),
            figCaption("图5.3 混合检索流程图"),
        ];
    })(),

    spacer(),
    body("检索流程首先对用户查询进行预处理：分词、去除停用词、提取关键词。然后并行执行向量检索和关键词检索两路操作，结果集合并后按评分排序返回。评分策略综合考虑向量相似度得分和关键词匹配得分，其中向量检索权重为0.7，关键词匹配权重为0.3。"),

    body("系统支持个性化检索结果过滤，普通用户仅能检索到自己上传的私有文档和设为公开的文档；高级用户额外获得系统推荐内容的曝光权限。检索结果默认返回Top 5（普通用户）或Top 8（高级用户），并按相关性得分降序排列。"),

    h3("5.3.4 RAG问答模块"),

    body("RAG问答模块是系统的核心功能入口，用户通过HTTP POST请求发送问题，系统返回结合检索结果的生成式回答。问答流程包含四个关键步骤：查询理解、相关文档检索、上下文构建、LLM生成。"),

    // RAG问答时序图
    (() => {
        const imgPath = "E:/答辩/论文/图5.4_RAG问答时序图.png";
        if (fs.existsSync(imgPath)) {
            return [
                createImagePara(imgPath, 700),
                figCaption("图5.4 RAG问答时序图"),
            ];
        }
        return [
            new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 200, after: 100 },
                children: [new TextRun({ text: "【图5.4 RAG问答时序图】", font: "宋体", size: FONT_SIZE_BODY, color: "FF0000" })]
            }),
            figCaption("图5.4 RAG问答时序图"),
        ];
    })(),

    spacer(),
    body("查询理解阶段对用户输入进行标准化处理，包括去除特殊字符、修正拼写错误、补充隐式上下文等。相关文档检索阶段使用上一节介绍的混合检索策略获取与查询最相关的文档片段。上下文构建阶段将检索到的文档内容按照相关性从高到低拼接，并控制总长度不超过6000字符（普通用户）或9000字符（高级用户）。"),

    body("LLM生成阶段使用阿里云DashScope的Qwen-Plus模型，结合构建好的上下文和用户问题生成回答。回答中包含指向参考文档的引用标注，用户可以点击查看原始文档。系统实现3次重试机制，当模型调用出现瞬态错误时自动重新尝试，提高服务可用性。"),

    h2("5.4 Python AI服务实现"),

    body("Python AI服务作为独立进程运行，通过FastAPI框架对外提供RESTful接口。该服务负责多模态数据的embedding生成和语音转写功能，采用懒加载策略在首次请求时才初始化模型，避免启动延迟。"),

    h3("5.4.1 服务架构"),

    body("Python服务采用FastAPI异步框架构建，提供以下核心接口：文本embedding接口（/embed/text）、图像embedding接口（/embed/image）、视频embedding接口（/embed/video）、音频转写接口（/transcribe）。服务启动时仅加载FastAPI框架，各模型（sentence-transformers、CLIP、Whisper）在收到首次请求时才进行加载，后续请求直接使用已加载的模型实例。"),

    h3("5.4.2 文本Embedding实现"),

    body("文本embedding使用sentence-transformers库加载all-MiniLM-L6-v2模型，该模型输出384维向量，推理速度约为800样本/秒。系统提供单条和批量两种接口，批量接口每次最多处理32条文本，通过list batching技术减少模型推理次数。文本输入前进行小写转换和标点规范化处理，提高跨文档匹配的一致性。"),

    h3("5.4.3 图像Embedding实现"),

    body("图像embedding使用OpenAI的CLIP模型（clip-vit-base-patch32），该模型将图像映射到512维向量空间。图像输入前调整为224×224像素，RGB通道归一化到[0,1]区间。系统同时提供零样本分类接口，输入图像后返回预定义类别（如：人物、风景、建筑、植物、动物等）的概率分布，可用于自动标注。"),

    h3("5.4.4 音频处理实现"),

    body("音频转写使用OpenAI的Whisper base模型，支持自动语言检测和中文优化。处理流程为：读取音频文件后，通过imageio-ffmpeg解码为原始PCM数据，然后分片送入Whisper模型转写。系统对中文转写结果进行二次校验，修正常见错别字。音频时长超过5分钟时自动分段处理，最后合并结果。"),

    h3("5.4.5 视频处理实现"),

    body("视频处理首先通过OpenCV提取关键帧，每30帧取一帧，最多提取10帧。每帧图像通过CLIP模型生成向量，所有帧向量取平均作为视频的整体表示。同时，视频音频轨道通过FFmpeg分离后送入Whisper进行转写，转写文本与视频向量关联存储。"),

    h3("5.4.6 向量维度对齐"),

    body("由于文本模型输出384维、CLIP图像模型输出512维，需要统一维度才能存储到同一向量空间。系统采用截断SVD算法将高维向量投影到384维，在保留主要语义信息的同时保证与文本向量的兼容性。投影矩阵通过主成分分析学习得到，存储在服务端复用。"),

    h2("5.5 前端实现"),

    body("前端采用Vue 3框架配合TypeScript开发，使用Element Plus组件库构建UI界面。前端通过Axios与后端API通信，实现了文档管理、话题系统、智能问答等核心功能。"),

    h3("5.5.1 文档管理页面"),

    body("文档管理页面展示用户的个人知识库，包含文档列表、筛选排序、上传入口三个区域。文档列表采用虚拟滚动技术优化大数据量渲染，每条记录显示文件名、类型、模态标签、上传时间等信息，支持多选批量操作。"),

    // 【图5.5 文档管理页面截图】
    new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 200, after: 100 },
        children: [new TextRun({ text: "【图5.5 文档管理页面截图】", font: "宋体", size: FONT_SIZE_BODY, color: "FF0000" })]
    }),
    figCaption("图5.5 文档管理页面截图"),

    spacer(),
    h3("5.5.2 AI对话页面"),

    body("AI对话页面提供RAG问答的用户交互入口。页面布局分为三列：左侧为会话列表，中间为对话区域，右侧为文档跳转入口。对话区域支持Markdown渲染，包含代码高亮、表格展示等功能。"),

    body("用户发送消息后，页面通过轮询获取流式响应，逐字显示在对话框中。回答完成后，下方展示引用来源卡片，包含文件名、相关度得分、内容预览等信息。用户点击卡片可跳转到原始文档详情页面。"),

    // 【图5.6 AI对话页面截图】
    new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 200, after: 100 },
        children: [new TextRun({ text: "【图5.6 AI对话页面截图】", font: "宋体", size: FONT_SIZE_BODY, color: "FF0000" })]
    }),
    figCaption("图5.6 AI对话页面截图"),

    spacer(),
    h3("5.5.3 话题发现页面"),

    body("话题发现页面提供公开话题的浏览和搜索功能。页面顶部为搜索框和分类标签，下方以卡片流形式展示话题列表，每张卡片包含话题名称、描述、订阅数等信息。用户可订阅感兴趣的话题，订阅后可在个人中心查看话题更新。"),

    // 【图5.7 话题发现页面截图】
    new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 200, after: 100 },
        children: [new TextRun({ text: "【图5.7 话题发现页面截图】", font: "宋体", size: FONT_SIZE_BODY, color: "FF0000" })]
    }),
    figCaption("图5.7 话题发现页面截图"),

    spacer(),
    h2("5.6 本章小结"),

    body("本章详细介绍了基于RAG技术的多模态知识管理系统的实现过程。首先说明了系统的开发环境配置和整体架构，然后重点阐述了文档上传与解析、语义检索和RAG问答三个核心模块的实现机制，接着介绍了Python AI服务的架构和各个模型的使用方法，最后展示了前端主要页面的实现效果。通过以上实现，系统具备了多模态知识的上传、处理、检索和问答能力。"),
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
        children: chapter5Content
    }]
});

Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/第5章_系统实现.docx", buffer);
    console.log("已生成：E:/答辩/论文/第5章_系统实现.docx");
}).catch(err => {
    console.error(err);
});