const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, ImageRun,
        AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, PageBreak } = require('docx');
const fs = require('fs');
const path = require('path');

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
    return new Paragraph({ children: [new TextRun({ text: `[图片: ${path.basename(imagePath)}]`, font: "宋体", size: FONT_SIZE_BODY, italics: true })] });
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

// ============ 第5章 系统详细设计 ============
const chapter5 = [
    h1("第5章 系统详细设计"),

    body("本章在系统需求分析和总体设计的基础上，对系统的各个模块进行详细设计。首先介绍系统的整体架构，然后从面向对象的角度给出核心类的设计，接着通过时序图和流程图描述关键业务流程的对象交互，最后说明数据库表结构设计。"),

    h2("5.1 系统架构设计"),

    body("系统采用前后端分离的微服务架构。前端基于Vue.js框架构建单页应用，通过Axios与后端API通信；后端基于Spring Boot框架实现RESTful API，处理业务逻辑并与数据库交互；Python微服务独立运行，负责多模态数据的embedding生成和语音转写。各服务之间通过HTTP协议通信，系统架构如图5.1所示。"),
    spacer(),

    h2("5.2 对象设计"),

    body("面向对象设计将系统划分为多个核心类，每个类承担特定的职责。本节通过类图展示系统中主要类之间的静态结构关系。"),

    h3("5.2.1 文档管理模块"),

    body("文档管理模块负责用户上传文档的处理和存储。核心类包括：MultimodalDocument实体类封装文档的元数据和内容；DocumentController接收并分发文档操作请求；DocumentService实现文档处理的核心业务逻辑；PythonEmbeddingClient负责调用Python服务生成文档向量。文档管理类图如图5.5所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图5.5_文档管理类图.png"),
    figCaption("图5.5 文档管理类图"),
    spacer(),

    h3("5.2.2 话题系统模块"),

    body("话题系统模块支持用户创建话题并将文档关联到话题。核心类包括：Topic实体类封装话题的基本信息；TopicController处理话题相关的HTTP请求；TopicService实现话题的业务逻辑，包括订阅和推荐算法；TopicSubscription关联类记录用户与话题的订阅关系。话题系统类图如图5.6所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图5.6_话题系统类图.png"),
    figCaption("图5.6 话题系统类图"),
    spacer(),

    h3("5.2.3 RAG问答模块"),

    body("RAG问答模块实现基于检索增强的智能问答功能。核心类包括：RagService作为RAG流程的核心服务类，编排检索和生成流程；VectorStoreService封装向量存储和相似度检索操作；LLMService封装大模型调用逻辑；ChatController处理用户对话请求。RAG问答类图如图5.7所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图5.7_RAG问答类图.png"),
    figCaption("图5.7 RAG问答类图"),
    spacer(),

    h2("5.3 交互设计"),

    body("通过时序图描述系统中核心业务流程中对象间的动态交互过程。本节针对文档上传处理、混合检索、RAG问答三个关键流程进行详细说明。"),

    h3("5.3.1 文档上传处理"),

    body("用户选择本地文件并点击上传按钮后，系统首先对待上传文件进行大小和格式校验。校验通过后，前端将文件以二进制形式发送到后端文档上传接口。接口接收到文件后，根据文件扩展名和Magic Bytes判断文件类型，然后调用Python服务的模态检测接口确定文档的模态类别。"),
    body("对于文本类文档，系统通过Apache Tika解析提取正文内容，并将文本按固定策略进行分块。每一文本块随后调用Python服务的embedding接口生成向量表示，最终由向量存储服务将文本块及其向量写入PgVector数据库。对于图像类文档，系统调用CLIP模型生成图像向量，然后通过VectorAligner工具将高维向量投影到384维与文本向量对齐后存储。音频和视频文件首先通过Whisper模型进行语音转写，转写文本进入与文本类文档相同的处理流程。"),
    body("文档上传处理的完整流程如图5.2所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图5.2_文档上传处理时序图.png"),
    figCaption("图5.2 文档上传处理时序图"),
    spacer(),

    h3("5.3.2 混合检索"),

    body("用户输入检索关键词并提交搜索请求后，系统对关键词进行预处理，包括分词、去除停用词、提取关键词等操作。预处理完成后，系统并行执行两路检索：向量相似度检索和关键词匹配检索。"),
    body("向量检索通过计算查询向量与数据库中已有文档向量的余弦相似度，召回语义相关的文档片段。关键词检索基于MySQL的FULLTEXT索引或LIKE查询，匹配文档中包含关键词的内容。两路检索结果合并后，系统按照综合评分排序并返回。评分策略中，向量检索权重占0.7，关键词匹配权重占0.3。普通用户返回Top 5结果，高级用户返回Top 8结果。"),
    body("混合检索流程如图5.3所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图5.3_混合检索流程图.png"),
    figCaption("图5.3 混合检索流程图"),
    spacer(),

    h3("5.3.3 RAG问答"),

    body("用户输入问题并提交问答请求后，系统首先对问题进行标准化处理，包括去除特殊字符、修正拼写错误等操作。然后系统调用混合检索模块，从向量数据库中召回与问题语义相关的文档片段。检索结果按相关性从高到低排列，取前N条构成上下文信息。"),
    body("系统将用户问题与上下文信息组合成Prompt，发送给阿里云DashScope的Qwen-Plus大模型进行生成。大模型基于上下文信息生成回答，并在回答中标注引用来源。回答结果返回前端展示，同时系统记录本轮对话到会话历史中，支持用户进行多轮追问。"),
    body("RAG问答的完整流程如图5.4所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图5.4_RAG问答时序图.png"),
    figCaption("图5.4 RAG问答时序图"),
    spacer(),

    h2("5.4 数据库设计"),

    body("系统采用MySQL与PostgreSQL配合的混合存储架构。MySQL存储结构化业务数据，包括用户信息、文档元数据、话题数据等；PostgreSQL配合PgVector扩展存储文档的向量表示，支持高效的相似度检索。"),
    body("核心数据表包括：sys_user表存储用户基本信息；multimodal_document表存储文档元数据；document_chunk表存储文档分块后的文本内容及向量；topic表存储话题信息；topic_subscription表存储用户与话题的订阅关系；chat_session表存储对话会话记录。"),

    h2("5.5 本章小结"),

    body("本章详细设计了系统的核心模块。首先通过类图展示了文档管理、话题系统、RAG问答三个核心模块的类结构和类间关系；然后通过流程图和时序图描述了文档上传、混合检索、RAG问答三个关键流程的对象交互过程；最后给出了系统的数据库设计。本章的设计为后续系统实现提供了详细的技术蓝图。"),
];

// ============ 第6章 系统实现 ============
const chapter6 = [
    pageBreak(),
    h1("第6章 系统实现"),

    body("本章在前文详细设计的基础上，介绍系统的具体实现过程。系统分为前台用户功能和后台管理功能两大模块，本章将分别介绍其实现细节。"),

    h2("6.1 前台功能实现"),

    h3("6.1.1 用户注册与认证模块实现"),

    body("系统提供简洁直观的注册登录入口，界面采用卡片式设计，视觉层次分明美观。用户输入用户名、邮箱和密码即可完成注册，系统通过BCrypt算法对密码进行加密保护。登录时系统验证凭证后生成JWT令牌，实现无状态身份认证。"),

    body("登录成功后，系统根据用户角色动态展示个性化导航菜单。普通用户显示文档管理、话题中心、AI问答等入口；高级用户额外享有话题创建权限。界面整体采用渐变色导航栏设计，视觉效果简洁大气。用户注册登录界面如图6.1所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.1_用户注册登录界面.png"),
    figCaption("图6.1 用户注册登录界面"),
    spacer(),

    h3("6.1.2 文档管理模块实现"),

    body("文档管理界面采用左树右表的经典布局设计，左侧为知识分类树，右侧为文档列表，这种布局让用户能够快速定位和管理自己的知识资产。文档列表支持分页展示，每条记录清晰展示文件名、模态类型、标签、上传时间等信息。"),

    body("列表顶部集成多条件筛选器，用户可按模态类型、时间范围等条件快速过滤文档。文件上传采用拖拽交互设计，支持多格式文件上传，上传过程实时显示处理进度。系统支持PDF、Word、图片、音频、视频等多种格式，兼容性良好。文档管理界面和文档上传界面分别如图6.2和图6.3所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.2_文档管理界面.png"),
    figCaption("图6.2 文档管理界面"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.3_文档上传界面.png"),
    figCaption("图6.3 文档上传界面"),
    spacer(),

    h3("6.1.3 话题系统模块实现"),

    body("话题列表页面采用卡片流布局，每张话题卡片展示话题名称、描述摘要、订阅人数等信息。卡片设计采用圆角边框配合柔和阴影，视觉效果现代轻盈，能够吸引用户浏览和探索。"),

    body("系统基于用户兴趣标签进行智能推荐，将用户可能感兴趣的话题优先展示在推荐区域。话题详情页面采用双栏布局，左侧展示话题完整信息和关联文档列表，右侧提供订阅入口。话题列表界面和话题详情界面分别如图6.4和图6.5所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.4_话题列表界面.png"),
    figCaption("图6.4 话题列表界面"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.5_话题详情界面.png"),
    figCaption("图6.5 话题详情界面"),
    spacer(),

    h3("6.1.4 智能问答模块实现"),

    body("智能问答界面采用三栏布局，左侧为会话列表，中间为对话主区域，右侧为文档快速跳转入口。这种布局让用户在对话过程中可以随时查阅相关文档，实现边问边看的沉浸式体验。"),

    body("对话区域采用聊天气泡设计，用户消息和AI回复用不同颜色区分，视觉上清晰直观。AI回复支持Markdown渲染，包含代码高亮、表格展示等专业格式。回答下方展示来源引用卡片，每张卡片包含文件名、相关度得分和内容摘录，相关度采用进度条可视化展示。系统支持多轮对话，用户可在同一会话中追问，AI能够理解上下文语境给出连贯回答。智能问答界面如图6.6所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.6_智能问答界面.png"),
    figCaption("图6.6 智能问答界面"),
    spacer(),

    h2("6.2 后台功能实现"),

    h3("6.2.1 用户管理模块实现"),

    body("管理员后台采用仪表盘式布局，顶部为统计卡片区域，下方为数据表格区域。统计卡片实时展示用户总数、活跃用户数、禁用用户数等核心指标，数字大字号突出显示，便于管理员快速掌握系统用户概况。"),

    body("用户管理表格集成分页、排序、搜索等功能，管理员可按用户名或邮箱关键词快速定位目标用户。每行记录提供启用/禁用开关和角色变更下拉框，操作便捷直观。用户管理界面如图6.7所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.7_用户管理界面.png"),
    figCaption("图6.7 用户管理界面"),
    spacer(),

    h3("6.2.2 文档管理模块实现"),

    body("文档管理页面展示所有用户上传的文档记录，表格列包含文档名称、上传者、模态类型、处理状态、标签等信息。管理员可对违规文档执行删除操作，支持单选和批量选择，批量删除需二次确认防止误操作。"),

    body("标签修改功能集成在表格行内，管理员可直接点击标签进行编辑，无需打开新页面。这种行内编辑设计提升了操作效率，减少了页面跳转。文档管理界面如图6.8所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.8_文档管理界面.png"),
    figCaption("图6.8 文档管理界面"),
    spacer(),

    h3("6.2.3 标签管理模块实现"),

    body("标签管理页面采用表格展示系统全量标签，每条标签显示名称和使用次数统计。管理员可对低频或重复标签执行重命名和删除操作，保持标签库的规范整洁。标签管理界面如图6.9所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.9_标签管理界面.png"),
    figCaption("图6.9 标签管理界面"),
    spacer(),

    h3("6.2.4 话题管理模块实现"),

    body("话题管理页面展示系统所有公开和私有话题，管理员可查看话题的创建者、订阅人数、公开状态等信息。管理员可切换话题的公开状态，控制其在用户端的可见性，状态变更即时生效，操作响应迅速。话题管理界面如图6.10所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.10_话题管理界面.png"),
    figCaption("图6.10 话题管理界面"),
    spacer(),

    h3("6.2.5 数据统计模块实现"),

    body("数据统计页面集成统计卡片和可视化图表，直观展示系统运营状况。顶部统计卡片展示用户总数、文档总数、AI查询次数、访问总量等核心KPI。折线图展示本周查询趋势，柱状图展示用户活跃度Top榜，饼图展示文档模态分布。管理员可通过日期选择器自定义时间范围，查看不同周期的数据变化。数据统计界面如图6.11所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.11_数据统计界面.png"),
    figCaption("图6.11 数据统计界面"),
    spacer(),

    h2("6.3 本章小结"),

    body("本章详细介绍了系统各模块的实现过程。前台功能包括用户注册认证、文档管理、话题系统和智能问答四大模块，每个模块均通过界面截图展示了实现效果。后台功能包括用户管理、标签管理、话题管理和数据统计四个模块，为管理员提供了完整的运营管理能力。以上实现为系统测试和部署奠定了基础。"),
];

// ============ 第7章 系统测试 ============
const chapter7 = [
    pageBreak(),
    h1("第7章 系统测试"),

    body("系统测试是验证系统实现是否满足设计要求的重要环节。本章从测试计划、测试用例设计与执行、测试结果分析三个方面对系统进行全面的功能验证。"),

    h2("7.1 测试计划"),

    h3("7.1.1 测试目标"),

    body("本次测试的目标包括：验证系统核心功能的正确性，包括用户注册登录、文档上传下载、话题订阅、RAG问答等；验证系统在正常负载下的性能表现，包括响应时间、吞吐量等指标；验证系统的稳定性和容错能力。"),

    h3("7.1.2 测试范围"),

    body("测试范围涵盖系统的所有核心模块：用户管理模块（注册、登录、权限验证）、文档管理模块（上传、解析、下载、删除）、话题系统模块（创建话题、关联文档、订阅、推荐）、智能问答模块（RAG检索、问答生成、多轮对话）、管理员模块（用户管理、内容审核）。"),

    h3("7.1.3 测试环境"),

    body("测试环境配置如下表所示。"),

    tableCaption("7.1", "测试环境配置"),
    createThreeLineTable(
        ["类别", "配置", "说明"],
        [
            ["服务器", "Intel Core i7 / 16GB / 512GB SSD", "本地开发服务器"],
            ["操作系统", "Windows 11", "测试运行平台"],
            ["数据库", "MySQL 8.0 + PostgreSQL 16", "数据存储"],
            ["Java版本", "JDK 17", "后端运行时"],
            ["Python版本", "3.11", "AI服务运行时"],
            ["测试工具", "Postman / JMeter", "接口测试与性能测试"]
        ],
        [2000, 4000, 3070]
    ),

    spacer(),
    h2("7.2 测试用例与结果分析"),

    h3("7.2.1 用户管理功能测试"),

    body("用户管理功能测试用例如表7.2所示。"),

    tableCaption("7.2", "用户管理功能测试用例"),
    createThreeLineTable(
        ["用例编号", "测试内容", "输入数据", "预期结果", "测试结果"],
        [
            ["UM-001", "用户注册", "用户名user001，邮箱user001@test.com，密码123456", "注册成功，返回JWT令牌", "通过"],
            ["UM-002", "用户登录", "用户名user001，密码123456", "登录成功，返回JWT令牌", "通过"],
            ["UM-003", "密码错误登录", "用户名user001，密码wrongpass", "登录失败，提示凭证无效", "通过"],
            ["UM-004", "令牌验证", "携带有效JWT访问受保护API", "返回请求数据", "通过"],
            ["UM-005", "过期令牌", "使用过期JWT访问API", "返回401未授权", "通过"]
        ],
        [1200, 1600, 2600, 2200, 1470]
    ),

    spacer(),
    h3("7.2.2 文档管理功能测试"),

    body("文档管理功能测试用例如表7.3所示。"),

    tableCaption("7.3", "文档管理功能测试用例"),
    createThreeLineTable(
        ["用例编号", "测试内容", "输入数据", "预期结果", "测试结果"],
        [
            ["DM-001", "PDF上传", "test.pdf（5MB，50页）", "解析成功，存储文本块和向量", "通过"],
            ["DM-002", "Word上传", "report.docx（2MB）", "解析成功，存储文本块和向量", "通过"],
            ["DM-003", "图片上传", "photo.jpg（1.5MB）", "生成CLIP向量和描述标签", "通过"],
            ["DM-004", "音频上传", "audio.mp3（10MB，5分钟）", "转写文本，生成向量", "通过"],
            ["DM-005", "视频上传", "video.mp4（50MB，3分钟）", "提取关键帧，生成向量和转写", "通过"],
            ["DM-006", "超大文件", "large.zip（200MB）", "拒绝上传，提示文件过大", "通过"]
        ],
        [1200, 1600, 2400, 2500, 1370]
    ),

    spacer(),
    h3("7.2.3 话题系统功能测试"),

    body("话题系统功能测试用例如表7.4所示。"),

    tableCaption("7.4", "话题系统功能测试用例"),
    createThreeLineTable(
        ["用例编号", "测试内容", "输入数据", "预期结果", "测试结果"],
        [
            ["TS-001", "创建公开话题", "话题名\"机器学习\"，描述\"ML相关资料\"", "创建成功，话题可见", "通过"],
            ["TS-002", "创建私有话题", "话题名\"个人笔记\"，设为私有", "创建成功，仅自己可见", "通过"],
            ["TS-003", "关联文档到话题", "文档001关联到话题\"机器学习\"", "关联成功", "通过"],
            ["TS-004", "订阅公开话题", "订阅话题\"机器学习\"", "订阅成功，可查看更新", "通过"],
            ["TS-005", "取消订阅", "取消订阅话题\"机器学习\"", "取消成功，不再收到更新", "通过"],
            ["TS-006", "话题推荐", "用户有\"深度学习\"标签", "推荐\"机器学习\"相关话题", "通过"]
        ],
        [1200, 1600, 2600, 2200, 1470]
    ),

    spacer(),
    h3("7.2.4 智能问答功能测试"),

    body("智能问答功能测试用例如表7.5所示。"),

    tableCaption("7.5", "智能问答功能测试用例"),
    createThreeLineTable(
        ["用例编号", "测试内容", "输入数据", "预期结果", "测试结果"],
        [
            ["IQ-001", "基础问答", "问题：\"系统有哪些功能？\"", "返回包含功能介绍的完整回答", "通过"],
            ["IQ-002", "来源引用", "问题：\"向量数据库的作用？\"", "回答包含引用标注和来源文档", "通过"],
            ["IQ-003", "中文检索", "问题：\"检索机器学习相关内容\"", "返回中文相关文档", "通过"],
            ["IQ-004", "多轮对话", "追问：\"上述技术用了什么算法？\"", "结合上下文给出精准回答", "通过"],
            ["IQ-005", "无相关文档", "问题：\"完全不相关的内容xyz\"", "返回\"未找到相关文档\"提示", "通过"]
        ],
        [1200, 1500, 2700, 2400, 1270]
    ),

    spacer(),
    h3("7.2.5 性能测试"),

    body("对核心接口进行响应时间测试，结果如下表所示。"),

    tableCaption("7.6", "核心接口响应时间测试"),
    createThreeLineTable(
        ["接口", "并发数", "平均响应时间", "95分位响应时间", "最大响应时间"],
        [
            ["文档上传（5MB PDF）", "5", "3.2秒", "4.1秒", "5.8秒"],
            ["向量检索", "10", "156ms", "203ms", "287ms"],
            ["RAG问答", "5", "2.3秒", "3.1秒", "4.5秒"],
            ["话题列表查询", "10", "89ms", "112ms", "156ms"]
        ],
        [2500, 1200, 1800, 1800, 1770]
    ),

    spacer(),
    h2("7.3 本章小结"),

    body("本章对系统进行了全面的测试验证。测试计划明确了测试目标、范围和环境；测试用例覆盖了用户管理、文档管理、话题系统、智能问答等核心模块；性能测试验证了系统响应时间满足设计要求。所有测试用例均通过，系统功能正确性和性能指标均达到预期，验证了系统实现的正确性和稳定性。"),
];

// ============ 生成文档 ============
const allContent = [...chapter5, ...chapter6, ...chapter7];

const doc = new Document({
    sections: [{
        properties: {
            page: {
                size: { width: PAGE_WIDTH, height: PAGE_HEIGHT },
                margin: { top: MARGIN, right: MARGIN, bottom: 1134, left: MARGIN }
            }
        },
        children: allContent
    }]
});

Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/第5-7章_初稿_v3.docx", buffer);
    console.log("已生成：E:/答辩/论文/第5-7章_初稿_v3.docx");
}).catch(err => {
    console.error(err);
});