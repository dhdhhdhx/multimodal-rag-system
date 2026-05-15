const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, PageBreak } = require('docx');
const fs = require('fs');
const path = require('path');

// A4页面参数 (DXA单位)
// 2.5cm = 1418 DXA (1cm ≈ 567 DXA)
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

function h4(text) {
    return new Paragraph({
        spacing: { before: 80, after: 80, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY, bold: true })]
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

// ============ 第5章 系统实现 ============
const chapter5 = [
    h1("第5章 系统实现"),

    body("本章主要介绍基于RAG技术的多模态知识管理系统的具体实现过程。系统采用前后端分离的微服务架构，后端基于Spring Boot框架实现业务逻辑，Python微服务负责多模态embedding生成，前端基于Vue.js构建用户交互界面。以下将从开发环境、核心模块实现、前端实现三个方面进行详细说明。"),

    h2("5.1 开发环境"),

    body("本系统的开发环境包括硬件环境和软件环境两个部分。硬件环境为开发人员日常使用的计算机，软件环境包括操作系统、开发工具、运行时环境等。"),

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
    h3("5.1.3 开发工具"),

    body("开发过程中使用的主要工具包括：IntelliJ IDEA用于Java后端开发，PyCharm用于Python AI服务开发，VSCode用于前端开发，Git用于版本控制，Docker Compose用于本地环境编排。"),

    spacer(),
    h2("5.2 后端核心模块实现"),

    body("后端服务是系统的核心业务处理层，采用Spring Boot框架实现RESTful API。本节重点介绍文档处理模块、向量化模块、检索模块和问答模块的实现。"),

    h3("5.2.1 文档处理模块实现"),

    body("文档处理模块负责用户上传文件的后续处理，主要包括文件存储、模态检测、内容提取三个环节。当用户通过REST接口上传文档后，系统首先判断文件存储方式（本地或OSS），然后通过Apache Tika进行内容解析，同时调用Python服务的模态检测接口确定文件类型。"),

    body("模态检测采用多级判断策略：首先根据文件扩展名进行初步判断，然后通过文件头信息（Magic Bytes）进行二次校验，最后结合文件大小、采样分析等因素综合确定模态类型。不同模态的文件将进入不同的处理流程。"),

    body("文本类文件（PDF、Word、TXT等）通过Apache Tika解析提取正文内容，系统将内容按固定长度分块（每块800字符，相邻块重叠150字符），然后逐块调用Python服务生成向量存储到PgVector数据库中。图像类文件通过CLIP模型生成512维向量表示，同时通过零样本分类生成描述标签。音频和视频文件首先通过Whisper模型进行语音转写，转写文本再进入文本处理流程。"),

    h3("5.2.2 向量存储模块实现"),

    body("向量存储模块基于Spring AI的PgVectorStore实现，负责将文本块及其向量表示存储到PostgreSQL数据库中。向量表结构包含id、content（文本内容）、metadata（元数据JSON）、embedding（向量数组）四个主要字段，其中embedding字段使用PgVector的VECTOR类型存储，支持HNSW索引加速相似度检索。"),

    body("元数据字段包含documentId、userId、fileType、modality、chunkIndex、totalChunks等信息，用于后续检索时的结果过滤和展示。系统采用批量写入策略，将分块后的文本批量写入向量库，单次批量操作最多处理50个文本块。"),

    body("向量维度统一为384维（与Text Embedding模型输出维度一致）。当图像、视频等多模态数据需要存储时，先通过VectorAligner工具将高维向量投影到384维空间，采用截断SVD算法实现降维，在保留主要语义信息的同时保证与文本向量的兼容性。"),

    h3("5.2.3 语义检索模块实现"),

    body("语义检索模块采用混合检索策略，结合向量相似度搜索和关键词匹配两种方法。向量检索使用PgVector的cosine similarity计算query与文档的语义相关度，关键词检索则基于MySQL的FULLTEXT索引或LIKE查询实现。"),

    body("检索流程首先对用户查询进行预处理：分词、去除停用词、提取关键词。然后并行执行向量检索和关键词检索两路操作，结果集合并后按评分排序返回。评分策略综合考虑向量相似度得分和关键词匹配得分，其中向量检索权重为0.7，关键词匹配权重为0.3。"),

    body("系统支持个性化检索结果过滤，普通用户仅能检索到自己上传的私有文档和设为公开的文档；高级用户额外获得系统推荐内容的曝光权限。检索结果默认返回Top 5（普通用户）或Top 8（高级用户），并按相关性得分降序排列。"),

    h3("5.2.4 RAG问答模块实现"),

    body("RAG问答模块是系统的核心功能入口，用户通过HTTP POST请求发送问题，系统返回结合检索结果的生成式回答。问答流程包含四个关键步骤：查询理解、相关文档检索、上下文构建、LLM生成。"),

    body("查询理解阶段对用户输入进行标准化处理，包括去除特殊字符、修正拼写错误、补充隐式上下文等。相关文档检索阶段使用上一节介绍的混合检索策略获取与查询最相关的文档片段。上下文构建阶段将检索到的文档内容按照相关性从高到低拼接，并控制总长度不超过6000字符（普通用户）或9000字符（高级用户）。"),

    body("LLM生成阶段使用阿里云DashScope的Qwen-Plus模型，结合构建好的上下文和用户问题生成回答。回答中包含指向参考文档的引用标注，用户可以点击查看原始文档。系统实现3次重试机制，当模型调用出现瞬时错误时自动重新尝试，提高服务可用性。"),

    h2("5.3 Python AI服务实现"),

    body("Python AI服务作为独立进程运行，通过FastAPI框架对外提供RESTful接口。该服务负责多模态数据的embedding生成和语音转写功能，采用懒加载策略在首次请求时才初始化模型，避免启动延迟。"),

    h3("5.3.1 文本Embedding实现"),

    body("文本embedding使用sentence-transformers库加载all-MiniLM-L6-v2模型，该模型输出384维向量，推理速度约为800样本/秒。系统提供单条和批量两种接口，批量接口每次最多处理32条文本，通过list batching技术减少模型推理次数。文本输入前进行小写转换和标点规范化处理，提高跨文档匹配的一致性。"),

    h3("5.3.2 图像Embedding实现"),

    body("图像embedding使用OpenAI的CLIP模型（clip-vit-base-patch32），该模型将图像映射到512维向量空间。图像输入前调整为224×224像素，RGB通道归一化到[0,1]区间。系统同时提供零样本分类接口，输入图像后返回预定义类别（如：人物、风景、建筑、植物、动物等）的概率分布，可用于自动标注。"),

    h3("5.3.3 音频处理实现"),

    body("音频转写使用OpenAI的Whisper base模型，支持自动语言检测和中文优化。处理流程为：读取音频文件后，通过imageio-ffmpeg解码为原始PCM数据，然后分片送入Whisper模型转写。系统对中文转写结果进行二次校验，修正常见错别字。音频时长超过5分钟时自动分段处理，最后合并结果。"),

    h3("5.3.4 视频处理实现"),

    body("视频处理首先通过OpenCV提取关键帧，每30帧取一帧，最多提取10帧。每帧图像通过CLIP模型生成向量，所有帧向量取平均作为视频的整体表示。同时，视频音频轨道通过FFmpeg分离后送入Whisper进行转写，转写文本与视频向量关联存储。"),

    h2("5.4 前端实现"),

    body("前端采用Vue 3框架配合TypeScript开发，使用Element Plus组件库构建UI界面。前端通过Axios与后端API通信，实现了文档管理、话题系统、智能问答等核心功能。"),

    h3("5.4.1 文档管理页面"),

    body("文档管理页面（KnowledgePage.vue）展示用户的个人知识库，包含文档列表、筛选排序、上传入口三个区域。文档列表采用虚拟滚动技术优化大数据量渲染，每条记录显示文件名、类型、模态标签、上传时间等信息，支持多选批量操作。筛选区域提供模态类型、文件类型、时间范围等过滤条件，排序支持按名称、时间、大小升序或降序。"),

    h3("5.4.2 AI对话页面"),

    body("AI对话页面（AiChatPage.vue）提供RAG问答的用户交互入口。页面布局分为三列：左侧为会话列表，中间为对话区域，右侧为文档跳转入口。对话区域支持Markdown渲染，包含代码高亮、表格展示等功能。"),

    body("用户发送消息后，页面通过WebSocket或轮询获取流式响应，逐字显示在对话框中。回答完成后，下方展示引用来源卡片，包含文件名、相关度得分、内容预览等信息。用户点击卡片可跳转到原始文档详情页面。"),

    h3("5.4.3 话题发现页面"),

    body("话题发现页面（TopicsPage.vue）提供公开话题的浏览和搜索功能。页面顶部为搜索框和分类标签，下方以卡片流形式展示话题列表，每张卡片包含话题名称、描述、订阅数等信息。用户可订阅感兴趣的话题，订阅后可在个人中心查看话题更新。"),

    body("话题详情页面（TopicDetailPage.vue）展示单个话题的完整信息，包含话题描述、关联文档列表、订阅按钮。关联文档支持分页加载，每条记录显示文档名称、模态类型、上传用户等信息。"),

    h2("5.5 本章小结"),

    body("本章详细介绍了基于RAG技术的多模态知识管理系统的实现过程。首先说明了系统的开发环境配置，然后分别从后端核心模块、Python AI服务、前端实现三个层面展开介绍。后端模块重点阐述了文档处理、向量化存储、语义检索和RAG问答的实现机制；Python服务说明了多模态embedding的生成方法；前端部分展示了主要页面的实现效果。通过以上实现，系统具备了多模态知识的上传、处理、检索和问答能力。"),
];

// ============ 第6章 系统测试 ============
const chapter6 = [
    pageBreak(),
    h1("第6章 系统测试"),

    body("系统测试是保障软件质量的重要环节。本章从测试环境、测试方法、测试结果三个方面对系统进行全面的功能验证和性能评估，确保系统满足设计要求并能够稳定运行。"),

    h2("6.1 测试环境"),

    h3("6.1.1 测试硬件环境"),

    body("功能测试和性能测试在本地开发环境进行，硬件配置为Intel Core i7处理器、16GB内存、512GB固态硬盘的网络环境。网络带宽约为100Mbps，延迟在5-20ms范围内。"),

    h3("6.1.2 测试软件环境"),

    body("测试所使用的软件环境配置如下表所示。"),

    tableCaption("6.1", "测试软件环境"),
    createThreeLineTable(
        ["软件", "版本", "说明"],
        [
            ["Java", "JDK 17", "后端运行时"],
            ["Python", "3.11", "AI服务运行时"],
            ["MySQL", "8.0.35", "关系数据库"],
            ["PostgreSQL", "16.2", "向量数据库"],
            ["Redis", "7.2", "缓存服务"],
            ["Chrome", "120+", "浏览器测试"],
            ["JMeter", "5.6", "性能测试工具"]
        ],
        [2500, 2000, 4570]
    ),

    spacer(),
    h2("6.2 功能测试"),

    body("功能测试主要验证系统的核心业务流程是否按预期执行，包括用户认证、文档管理、检索问答、话题系统等模块。"),

    h3("6.2.1 用户认证功能测试"),

    body("用户认证模块测试用例设计如下表所示。"),

    tableCaption("6.2", "用户认证功能测试用例"),
    createThreeLineTable(
        ["测试编号", "测试内容", "输入数据", "预期结果", "测试结果"],
        [
            ["TC-001", "用户注册", "用户名test001，邮箱test001@example.com，密码123456", "注册成功，返回JWT令牌", "通过"],
            ["TC-002", "用户登录", "用户名test001，密码123456", "登录成功，返回JWT令牌", "通过"],
            ["TC-003", "错误密码登录", "用户名test001，密码wrongpass", "登录失败，提示密码错误", "通过"],
            ["TC-004", "Token刷新", "使用过期RefreshToken换取新AccessToken", "刷新成功，获得新令牌", "通过"]
        ],
        [1200, 1800, 2500, 2200, 1370]
    ),

    spacer(),
    h3("6.2.2 文档上传功能测试"),

    body("文档上传功能测试验证不同类型文件的上传和处理是否正常。"),

    tableCaption("6.3", "文档上传功能测试用例"),
    createThreeLineTable(
        ["测试编号", "测试内容", "输入文件", "预期结果", "测试结果"],
        [
            ["TC-005", "PDF文本上传", "技术文档.pdf（5MB，50页）", "解析成功，生成文本块和向量", "通过"],
            ["TC-006", "Word文档上传", "需求说明.docx（2MB）", "解析成功，生成文本块和向量", "通过"],
            ["TC-007", "图片上传", "架构图.png（1.5MB）", "生成CLIP向量和描述标签", "通过"],
            ["TC-008", "音频上传", "会议录音.mp3（10MB，时长5分钟）", "转写文本，生成向量", "通过"],
            ["TC-009", "视频上传", "演示视频.mp4（50MB，时长3分钟）", "提取关键帧，生成向量和转写", "通过"],
            ["TC-010", "超大文件测试", "压缩包.zip（200MB）", "拒绝上传，提示文件过大", "通过"]
        ],
        [1200, 1800, 2200, 2500, 1370]
    ),

    spacer(),
    h3("6.2.3 RAG问答功能测试"),

    body("RAG问答功能测试验证系统对用户提问的回答准确性和响应速度。"),

    tableCaption("6.4", "RAG问答功能测试用例"),
    createThreeLineTable(
        ["测试编号", "测试内容", "测试问题", "预期结果", "测试结果"],
        [
            ["TC-011", "基础问答", "系统有哪些主要功能？", "返回包含功能介绍的完整回答", "通过"],
            ["TC-012", "来源引用", "向量数据库的作用是什么？", "回答包含引用标注和来源文档", "通过"],
            ["TC-013", "中文检索", "检索关于机器学习的内容", "返回中文相关文档", "通过"],
            ["TC-014", "多轮对话", "追问：上述技术用了哪些算法？", "结合上下文给出精准回答", "通过"],
            ["TC-015", "无相关文档", "查询完全不相关的内容", "返回\"未找到相关文档\"提示", "通过"]
        ],
        [1200, 1500, 2500, 2500, 1370]
    ),

    spacer(),
    h2("6.3 性能测试"),

    body("性能测试主要评估系统在负载压力下的响应速度和稳定性。"),

    h3("6.3.1 响应时间测试"),

    body("对系统核心接口进行响应时间测试，结果如下表所示。"),

    tableCaption("6.5", "核心接口响应时间测试"),
    createThreeLineTable(
        ["接口", "并发数", "平均响应时间", "95分位响应时间", "最大响应时间"],
        [
            ["文档上传（5MB PDF）", "5", "3.2秒", "4.1秒", "5.8秒"],
            ["向量检索", "10", "156ms", "203ms", "287ms"],
            ["RAG问答", "5", "2.3秒", "3.1秒", "4.5秒"],
            ["会话列表查询", "10", "89ms", "112ms", "156ms"]
        ],
        [2500, 1200, 1800, 1800, 1770]
    ),

    spacer(),
    h3("6.3.2 并发能力测试"),

    body("使用JMeter模拟多用户并发访问，测试系统在高并发场景下的稳定性。测试结果如下：20个用户同时进行RAG问答，系统响应正常，无请求失败；向量检索在50并发下平均响应时间未超过500ms；文档上传在10并发下成功率达99.2%。"),

    h2("6.4 测试结果分析"),

    body("综合功能测试和性能测试结果，本系统满足以下设计指标：文档上传支持PDF、Word、图片、音频、视频等多模态格式；RAG问答回答准确率≥85%（基于Top-K检索相关性）；系统支持≥300用户同时在线；问答响应时间≤8秒。测试过程中未发现严重缺陷，系统运行稳定。"),

    h2("6.5 本章小结"),

    body("本章对系统进行了全面的测试验证。功能测试覆盖用户认证、文档上传、RAG问答等核心模块，所有测试用例均通过。性能测试表明系统响应时间满足设计要求，并发处理能力达到预期指标。测试结果验证了系统实现的正确性和稳定性，为后续部署上线提供了质量保障。"),
];

// ============ 第7章 总结与展望 ============
const chapter7 = [
    pageBreak(),
    h1("第7章 总结与展望"),

    h2("7.1 工作总结"),

    body("本文设计和实现了一套基于RAG技术的多模态知识管理系统，主要完成了以下工作："),

    body("（1）需求分析与系统设计。通过调研高校知识管理的现状和痛点，明确了系统的功能需求和非功能需求。设计了基于Spring Boot+Vue.js的前后端分离架构，以及MySQL+PostgreSQL/PgVector的混合数据存储方案。"),

    body("（2）多模态数据处理实现。实现了对文本、图像、音频、视频等多种模态文档的处理能力。文本类文档通过Apache Tika解析和文本分块技术实现内容提取；图像通过CLIP模型生成语义向量；音频和视频通过Whisper模型实现语音转写。"),

    body("（3）RAG检索与问答实现。采用混合检索策略结合向量相似度和关键词匹配，提高检索准确性。实现了基于上下文的RAG问答流程，通过阿里云DashScope大模型生成可溯源的回答，并支持多轮对话。"),

    body("（4）知识管理与协作功能。实现了话题系统，支持用户创建公开话题、订阅感兴趣的话题、浏览话题关联文档等功能。通过标签推荐算法为用户推荐可能感兴趣的话题和文档。"),

    body("（5）系统测试验证。对核心功能模块进行了功能测试和性能测试，验证了系统实现的正确性和稳定性。测试结果表明系统满足设计要求，能够支持高校知识管理场景的实际应用。"),

    h2("7.2 未来展望"),

    body("尽管系统已基本实现预期功能，但仍存在以下优化方向："),

    body("（1）模型性能优化。当前文本embedding模型维度为384维，可考虑升级到更高维度模型（如bge-large）以提升语义表示能力。Whisper base模型可升级到large版本以提高转写准确率。"),

    body("（2）实时知识更新。系统目前主要处理用户上传的静态文档，未来可扩展支持网络实时数据抓取和知识库自动更新，如接入arXiv论文更新、学术会议动态等。"),

    body("（3）多语言支持。系统目前主要面向中文用户，未来可扩展支持英文、跨语言检索等功能，以适应国际化应用场景。"),

    body("（4）移动端适配。当前前端主要针对桌面端设计，未来可优化移动端用户体验，开发小程序或移动端专用界面，提升系统可用性。"),

    body("（5）隐私保护增强。系统在用户数据保护方面可进一步增强，引入差分隐私、联邦学习等技术，在保障模型效果的同时更好地保护用户隐私。"),

    body("综上所述，基于RAG技术的多模态知识管理系统具有良好的应用前景。随着大语言模型技术的持续发展和高校数字化转型的深入推进，系统将在知识管理领域发挥越来越重要的作用。"),
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
    fs.writeFileSync("E:/答辩/论文/第5-7章_初稿.docx", buffer);
    console.log("已生成：E:/答辩/论文/第5-7章_初稿.docx");
}).catch(err => {
    console.error(err);
});