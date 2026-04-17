const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, PageBreak } = require('docx');
const fs = require('fs');

const PAGE_WIDTH = 11906;
const PAGE_HEIGHT = 16838;
const TOP_MARGIN = 1418;
const BOTTOM_MARGIN = 1134;
const INNER_MARGIN = 1134;
const OUTER_MARGIN = 1134;
const GUTTER = 284;
const CONTENT_WIDTH = PAGE_WIDTH - INNER_MARGIN - OUTER_MARGIN - GUTTER;

const FONT_SIZE_BODY = 24;
const FONT_SIZE_H1 = 30;
const FONT_SIZE_H2 = 28;
const FONT_SIZE_H3 = 24;
const LINE_SPACING = 360;
// 首行缩进2字符：在12pt字体下，1个中文字符≈567 DXA，2字符≈567 DXA
const FIRST_LINE_INDENT = 567;

const border = { style: BorderStyle.SINGLE, size: 1, color: "000000" };
const borders = { top: border, bottom: border, left: border, right: border };

function createBodyParagraph(text) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "auto" },
        indent: { firstLine: FIRST_LINE_INDENT },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY })]
    });
}

function createH1(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_1,
        spacing: { before: 240, after: 240, line: LINE_SPACING, lineRule: "auto" },
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H1, bold: true })]
    });
}

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

function createTable(headers, rows, widths) {
    const headerCells = headers.map((h, i) =>
        new TableCell({
            borders,
            width: { size: widths[i], type: WidthType.DXA },
            shading: { fill: "D9D9D9", type: ShadingType.CLEAR },
            margins: { top: 80, bottom: 80, left: 120, right: 120 },
            verticalAlign: VerticalAlign.CENTER,
            children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: h, font: "黑体", size: FONT_SIZE_BODY, bold: true })] })]
        })
    );
    const dataRows = rows.map(row =>
        new TableRow({ children: row.map((cell, i) =>
            new TableCell({
                borders,
                width: { size: widths[i], type: WidthType.DXA },
                margins: { top: 80, bottom: 80, left: 120, right: 120 },
                children: [new Paragraph({ children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_BODY })] })]
            })
        )})
    );
    return new Table({ width: { size: CONTENT_WIDTH, type: WidthType.DXA }, columnWidths: widths, rows: [new TableRow({ children: headerCells }), ...dataRows] });
}

function spacer() {
    return new Paragraph({ spacing: { line: LINE_SPACING } });
}

// ==================== 正文内容 ====================

const content = [
    // ========== 第1章 绪论 ==========
    createH1("第1章 绪论"),

    createH2("1.1 研究背景与意义"),
    createBodyParagraph(`随着高校教学与科研活动的不断深化，知识资产的规模呈指数级增长，其形态已从传统的结构化数据演变为非结构化、多模态数据并存的状态。大量的教学文档、课程资源、科研论文、源代码、实验数据、图片与音视频资料分散存储于网盘、代码仓库及各类文档平台中。由于缺乏统一的语义组织与管理机制，这种"知识孤岛"现象导致了严重的资源冗余与检索困难。传统的基于关键词匹配的检索技术仅能处理表层文本比对，无法挖掘图片、音视频等模态数据的深层语义，难以实现跨模态的知识关联，导致知识检索效率低下、复用率不足，已成为制约高校信息化建设与知识资产价值释放的主要瓶颈。`),
    createBodyParagraph("本研究的意义主要体现在三个方面："),
    createBodyParagraph("（1）理论上：探索RAG在多模态语义检索与生成中的融合机制；"),
    createBodyParagraph("（2）工程上：提出基于Spring AI的高校知识管理架构，实现Java生态下的智能知识系统；"),
    createBodyParagraph("（3）应用上：推动教学与科研知识的高效组织、智能问答与成果共享，为智慧校园建设提供参考模型。"),

    createH2("1.2 国内外研究现状"),
    createBodyParagraph(`在人工智能技术领域，大语言模型（LLM）在自然语言理解与生成方面取得了突破性进展，为智能问答提供了新的可能。然而，单纯依赖LLM进行问答存在知识时效性滞后、"幻觉"频发以及回答内容不可追溯等固有缺陷。Davis E和Wilson K在《Retrieval-augmented generation: principles and practice》一书中指出，检索增强生成（RAG）技术通过将外部知识库检索结果与大模型生成能力深度融合，能够有效缓解模型因知识时效性滞后产生的局限性及"幻觉"风险，通过引入外部事实依据，显著增强生成结果的准确性与可追溯性。尽管如此，现有的RAG技术研究与应用多集中于纯文本领域，尚难以有效适配高校教学与科研场景中复杂的多模态知识管理需求。在实际的高校业务中，海量的课程实录视频、实验数据图像及科研源代码等非结构化资源，缺乏统一的语义表征与跨模态关联机制，面对这一现状，如何实现高效的图像文字识别（OCR）、音视频内容转写以及异构模态间的细粒度语义对齐，成为构建高校智能化知识管理系统的关键技术瓶颈，严重制约了多模态RAG技术在教育信息化领域的实际落地效果。`),
    createBodyParagraph("在工程实现层面，Java生态下的Spring AI框架为解决上述问题提供了新的路径。Wang X等（2024）在研究中提出，Spring AI能够实现大模型与Java应用的轻量化集成，通过统一的抽象接口简化了多模态数据处理与向量化的工程难度，为构建企业级多模态RAG系统奠定了坚实基础。在此背景下，本课题拟设计并实现一套基于RAG技术的多模态知识管理系统，旨在解决多模态知识的统一接入、语义检索与智能问答问题，探索Java技术栈在AI原生应用中的工程化价值。"),

    createH2("1.3 本文主要工作"),
    createBodyParagraph("本文围绕高校教学与科研场景下多模态知识管理效率低、跨模态检索难及智能问答能力不足等核心问题，设计与实现一套基于检索增强生成（RAG）技术的多模态知识管理系统。具体工作内容如下："),
    createH3("1.3.1 系统架构设计与技术选型"),
    createBodyParagraph("采用前后端分离架构，后端基于Spring Boot与Spring AI框架，前端选用Vue3 + Vite + Element Plus技术栈。设计系统总体架构，划分为数据接入层、语义向量化层、RAG检索增强层、业务逻辑层与用户接口层。完成MySQL数据库与向量数据库（PGVector/Milvus）的表结构设计，建立多模态数据元数据与语义向量的映射关系。"),
    createH3("1.3.2 多模态知识接入与预处理模块开发"),
    createBodyParagraph("构建支持文本（Word/PDF/Markdown）、代码（Java/Python）、图片（JPG/PNG）、音频（MP3/WAV）、视频（MP4/AVI）及压缩包等多种格式的统一知识接入能力。集成Apache Tika实现通用文档解析，利用OCR工具实现图像文字识别，借助FFmpeg与Whisper组件完成音视频内容转写，形成结构化的多模态数据预处理流程，为后续向量化提供高质量的语义基础。"),
    createH3("1.3.3 跨模态语义检索与RAG问答模块实现"),
    createBodyParagraph("基于Spring AI框架集成Embedding模型，将预处理后的多模态内容转化为统一语义向量，并存储至向量数据库。实现跨模态语义相似度检索模块，支持用户通过自然语言查询在向量空间中匹配Top-K相关多模态片段。构建检索增强生成流程，将检索结果作为上下文输入大语言模型，生成基于事实的精准回答，并实现回答内容与原始文件片段的溯源展示。"),
    createH3("1.3.4 权限管理与知识增值功能开发"),
    createBodyParagraph("基于Spring Security + JWT + RBAC模型，设计普通会员、高级会员与管理员三级权限体系，实现细粒度的访问控制。开发知识分类管理、多模态笔记生产、带时效性的链接分享以及基于协同过滤的知识推荐等增值功能，提升知识在高校教学与科研场景中的复用价值。"),
    createH3("1.3.5 系统测试与性能优化"),
    createBodyParagraph("开展全维度功能测试与性能测试，模拟高校高并发场景（≥300用户），重点优化多模态解析效率与向量检索速度，确保页面响应时间≤3秒，智能问答生成时间≤8秒。实施系统Docker容器化部署，验证系统在不同环境下的可复现性与易维护性，形成完整的系统交付与运行方案。"),

    createH2("1.4 论文组织结构"),
    createBodyParagraph("本论文的组织结构如下："),
    createBodyParagraph("第一章 阐述研究背景与现状，综述相关领域进展，明确本文研究内容与创新点。"),
    createBodyParagraph("第二章 介绍RAG、多模态嵌入与对齐、向量数据库及前后端开发框架等关键技术。"),
    createBodyParagraph("第三章 分析系统功能与非功能需求，设计总体架构、数据库及权限模型。"),
    createBodyParagraph("第四章 详述多模态接入、语义对齐、跨模态检索及RAG问答等核心模块的实现。"),
    createBodyParagraph("第五章 设计测试方案，验证系统性能与检索效果，分析优化结果。"),
    createBodyParagraph("第六章 总结全文工作，分析不足，并对未来研究方向进行展望。"),

    // ========== 第2章 相关技术与理论基础 ==========
    new Paragraph({ children: [new PageBreak()] }),
    createH1("第2章 相关技术与理论基础"),

    createH2("2.1 检索增强生成技术"),
    createBodyParagraph("检索增强生成（Retrieval-Augmented Generation, RAG）是一种将信息检索与大语言模型（Large Language Model, LLM）生成能力相融合的技术架构。RAG通过从外部知识库中检索相关文档，并将检索结果作为上下文输入LLM，有效弥补了单纯依赖模型参数存储知识所带来的时效性滞后、幻觉频发及回答不可追溯等固有缺陷。"),
    createBodyParagraph("RAG架构一般包含检索（Retrieval）、增强（Augmentation）和生成（Generation）三个核心阶段。在检索阶段，系统将用户查询转化为语义向量，在向量数据库中进行相似度匹配，返回Top-K相关文档片段；在增强阶段，系统将检索结果与原始查询进行上下文组装，形成结构化的Prompt输入；在生成阶段，LLM基于包含事实依据的Prompt输出最终回答。Davis E和Wilson K在《Retrieval-augmented generation: principles and practice》中指出，RAG通过引入外部事实依据，能够显著增强生成结果的准确性与可追溯性，已成为缓解大模型幻觉问题的主流技术路线。"),
    createBodyParagraph("传统RAG多聚焦于纯文本领域，其核心流程可概括为：将文本语料分块（Chunking）后经Embedding模型转化为稠密向量，存储于向量数据库；用户查询同样经Embedding向量化后，在向量空间中进行语义相似度检索；最终将检索结果作为上下文注入Prompt，驱动LLM生成答案。"),

    createH2("2.2 多模态嵌入与跨模态对齐"),
    createBodyParagraph("多模态嵌入（Multimodal Embedding）是将来自不同模态（文本、图像、音频、视频）的数据映射到统一语义向量空间的技术，是实现跨模态检索与融合的核心基础。跨模态对齐（Cross-modal Alignment）则关注不同模态表征之间的语义关联与一致性建模。"),
    createBodyParagraph("在文本嵌入层面，Sentence-Transformers系列模型（如all-MiniLM-L6-v2）通过对比学习在大规模语料上进行预训练，能够将句子级文本映射为稠密语义向量，在语义相似度计算任务上表现优异。在图像嵌入层面，CLIP（Contrastive Language-Image Pre-training）模型由OpenAI提出，通过在4亿图文对上对比学习，构建了视觉与文本的联合语义空间，使图生文、文生图以及跨模态检索成为可能。CLIP采用ViT（Vision Transformer）架构作为图像编码器，输出512维图像向量。"),
    createBodyParagraph("由于不同模态的原始向量维度不一致（如CLIP图像向量为512维，Sentence-Transformers文本向量为384维），需要进行跨模态对齐。本系统采用主成分分析（PCA）算法进行向量维度压缩与对齐，将高维异构向量统一映射至384维语义空间，为后续向量数据库的混合检索奠定基础。"),

    createH2("2.3 向量数据库"),
    createBodyParagraph("向量数据库是专门用于存储和检索高维向量（Embedding）的数据库系统，在RAG架构中承担语义检索的核心职能。相较于传统关系型数据库，向量数据库通过近似最近邻（Approximate Nearest Neighbor, ANN）索引算法，能够在海量向量中快速定位与查询向量语义最相似的结果。"),
    createBodyParagraph("当前主流的向量数据库包括Milvus、PGVector、Chroma和Weaviate等。本系统选用PGVector作为向量存储引擎，原因如下：（1）PGVector是PostgreSQL的扩展插件，可与业务 MySQL数据库通过同一数据库实例管理，简化运维复杂度；（2）PGVector支持HNSW（Hierarchical Navigable Small World）和IVFFlat两种索引算法，HNSW索引在召回率与查询速度方面表现均衡；（3）Spring AI框架提供了对PGVector的原生集成支持，工程实现成本低。"),
    createBodyParagraph("PGVector的向量检索基于余弦相似度（Cosine Similarity）计算向量间的语义关联程度，相似度取值范围为[-1, 1]，值越接近1表示语义越相似。在RAG检索场景中，系统一般取Top-K（通常K=5~10）个最高相似度结果作为上下文输入。"),

    createH2("2.4 Spring AI框架"),
    createBodyParagraph("Spring AI是Spring生态下面向AI应用开发的工程框架，旨在为Java开发者提供轻量化接入大模型与向量数据库的统一抽象接口。相较于直接调用大模型API，Spring AI通过抽象化的Service层与Prompt模板机制，大幅简化了AI功能的工程实现难度。"),
    createBodyParagraph("Spring AI的核心概念包括：（1）Model，封装了对不同大模型（OpenAI、Azure OpenAI、Hugging Face等）的统一调用接口；（2）Embeddings，用于文本向量化的抽象接口，支持多种Embedding模型；（3）VectorStore，向量存储的抽象接口，提供了对PGVector、Milvus等向量数据库的统一CRUD操作；（4）Prompt与ChatOptions，分别用于Prompt模板构造与模型参数配置。"),
    createBodyParagraph("Spring AI的RAG实现依赖于VectorStore与ChatModel的协同工作：VectorStore负责语义检索，ChatModel负责基于检索结果生成回答。Spring AI 1.0.0-M4版本引入了对PGVector的原生支持，通过PgVectorStore类可快速完成向量存储与检索的集成。然而，该版本对自定义Embedding的支持存在一定限制，在后续版本中有望得到进一步完善。"),

    createH2("2.5 本章小结"),
    createBodyParagraph("本章介绍了构建多模态知识管理系统所涉及的关键技术理论。RAG技术通过融合外部知识检索与大模型生成能力，有效提升了问答系统的准确性与可追溯性；多模态嵌入与跨模态对齐技术为异构模态数据的统一语义表征提供了技术路径；向量数据库作为RAG架构的核心基础设施，支撑了高效语义检索；Spring AI框架则为Java生态下的AI应用开发提供了轻量化集成方案。上述技术为后续章节的系统设计与实现奠定了理论基础。"),

    // ========== 第3章 系统需求分析 ==========
    new Paragraph({ children: [new PageBreak()] }),
    createH1("第3章 系统需求分析"),

    createH2("3.1 系统需求概述"),
    createBodyParagraph("高校在教学与科研活动中积累了大量的多模态知识资产，包括课程文档、科研论文、源代码、实验图像、课程实录视频等。当前高校知识管理面临以下核心挑战：（1）知识分散存储于网盘、代码仓库及各类文档平台，缺乏统一的语义组织与检索机制，形成知识孤岛；（2）传统基于关键词匹配的检索技术仅能进行表层文本比对，无法实现跨模态语义关联；（3）师生在科研与学习过程中缺乏高效的智能问答手段，知识复用率低。"),
    createBodyParagraph("针对上述痛点，本系统旨在构建一套基于RAG技术的多模态知识管理系统，实现多模态知识的统一接入、跨模态语义检索与智能问答，为高校知识资产的高效组织与价值释放提供工程化解决方案。"),

    createH2("3.1.1 系统目标"),
    createBodyParagraph("本系统的核心目标包括：（1）多模态知识统一接入：系统支持文本（PDF、Word、Markdown）、代码（Java、Python）、图像（JPG、PNG）、音频（MP3，WAV）、视频（MP4、AVI）等全类型文件的上传与解析，消除格式壁垒，实现异构资源的结构化预处理；（2）跨模态语义检索：基于Embedding模型将多模态内容转化为统一语义向量，存储于向量数据库，支持用户通过自然语言查询快速匹配相关文本、图像、音频及视频片段；（3）RAG智能问答：构建检索增强生成流程，将跨模态检索结果与大语言模型生成能力相融合，输出可溯源、基于事实的精准回答；（4）权限与知识增值：基于RBAC模型实现三级权限控制（普通会员、高级会员、管理员），提供知识分类、笔记生产、链接分享与协同推荐等增值功能。"),

    createH2("3.1.2 系统用户角色"),
    createBodyParagraph("本系统的用户角色分为以下三类："),
    createTable(["角色", "权限描述"], [
        ["普通会员", "上传与管理个人知识文档、进行语义检索与RAG问答、查看公开知识库、使用笔记与分享功能"],
        ["高级会员", "享受更高的检索配额与问答频率、优先使用知识推荐功能、创建知识专题"],
        ["管理员", "用户管理与权限配置、系统运营数据分析、全局知识库监督与审核"]
    ], [2000, 7354]),
    spacer(),

    createH2("3.2 功能需求分析"),
    createH2("3.2.1 多模态知识接入与管理"),
    createBodyParagraph("系统应提供完整的文档上传、解析、存储与生命周期管理功能，具体包括：（1）全类型文件上传：用户可通过前端界面上传多种格式的文件，单文件大小上限为200MB。系统根据文件扩展名自动检测模态类型（TEXT/IMAGE/AUDIO/VIDEO），无需用户手动选择分类；（2）内容解析与提取：针对不同模态，系统调用相应的解析工具进行内容提取。文本类文档经Apache Tika通用解析；图片类文件经OCR文字识别；音频文件经Whisper模型转写为文本；视频文件提取关键帧并进行CLIP特征提取；（3）知识向量化与存储：解析后的内容经Embedding模型转化为统一语义向量，存入向量数据库（PGVector）；原始文件及元数据存入MySQL数据库，实现业务数据与向量数据的分离管理；（4）文档生命周期管理：用户可查看个人上传文档列表、修改文档标签、切换公开/私有状态、删除文档。删除操作应级联清除文件存储、向量数据库记录及MySQL元数据。"),

    createH2("3.2.2 跨模态语义检索"),
    createBodyParagraph("系统应提供基于自然语言的语义检索能力，支持跨模态知识关联，具体包括：（1）语义向量检索：用户输入查询语句后，系统将其转化为语义向量，在向量数据库中进行相似度匹配，返回Top-K（用户私有文档Top 5 + 公开文档Top 3）最相关结果；（2）混合检索机制：系统同时支持关键词检索与语义检索。当向量检索服务不可用时，自动降级为基于MySQL全文索引的关键词检索，确保系统可用性达到100%；（3）检索结果呈现：检索结果按相似度降序排列，展示文档名称、所属模态、匹配片段摘要及相似度得分，支持用户点击查看原文。"),

    createH2("3.2.3 RAG智能问答"),
    createBodyParagraph("系统应提供基于检索增强生成的智能问答功能，具体包括：（1）对话上下文管理：支持多轮对话场景，系统维护用户会话上下文，理解对话连贯性与指代关系；（2）溯源展示：回答生成后，系统标注答案所引用的知识来源，包括原始文档名称、片段位置及相似度得分，实现回答的可追溯性；（3）生成质量控制：通过Prompt工程约束模型输出格式，确保回答简洁准确，避免幻觉内容。"),

    createH2("3.2.4 权限与安全管理"),
    createBodyParagraph("系统应实现基于RBAC模型的细粒度权限控制，具体包括：（1）用户认证：支持用户注册、登录与JWT token认证，支持Access Token（1小时有效期）与Refresh Token（7天有效期）双token机制；（2）角色权限控制：普通会员、高级会员、管理员三级权限体系，管理员可进行用户启用/禁用、角色变更等操作；（3）数据隔离：用户只能检索和管理个人上传的私有文档，公开文档可被所有登录用户检索。"),

    createH2("3.2.5 知识增值功能"),
    createBodyParagraph("系统应提供知识复用与共享的增值服务，具体包括：（1）知识分类与标签：用户可为文档添加自定义标签，支持按标签筛选检索范围；（2）多模态笔记：用户可在检索结果基础上创建笔记，笔记与原始文档片段关联保存；（3）链接分享：用户可生成带时效性（可设置过期时间）的分享链接，将特定知识专题分享给其他用户；（4）知识推荐：基于协同过滤算法，根据用户历史检索与问答记录，推荐可能感兴趣的知识文档。"),

    createH2("3.3 非功能需求分析"),
    createH2("3.3.1 性能需求"),
    createTable(["指标", "要求"], [
        ["系统并发用户数", "≥300用户同时在线"],
        ["页面响应时间", "≤3秒（检索、文档列表等页面操作）"],
        ["智能问答生成时间", "≤8秒（首字输出延迟）"],
        ["向量检索召回", "Top-K相关性文档召回准确率≥85%"],
        ["多模态解析吞吐量", "单个PDF文档（50页）解析≤10秒"]
    ], [4677, 4677]),
    spacer(),

    createH2("3.3.2 可靠性需求"),
    createBodyParagraph("系统应在高并发场景（如高校选课、期末复习）下保持稳定运行。对于大模型API调用失败、向量数据库连接超时等临时性故障，系统应具备自动重试机制（最多3次）。当核心检索服务不可用时，自动降级为关键词检索，保证核心功能可用。"),

    createH2("3.3.3 安全性需求"),
    createBodyParagraph("用户密码采用BCrypt强哈希算法存储；JWT Secret配置不少于256位；用户上传的文件经安全扫描后存储；系统实施严格的角色权限检查，防止越权访问。"),

    createH2("3.3.4 可维护性需求"),
    createBodyParagraph("系统采用Docker容器化部署，支持在不同服务器环境下的快速复现；前后端分离架构便于独立开发与迭代；Spring Boot Actuator提供健康检查与运行监控接口。"),

    createH2("3.4 用例分析"),
    createH2("3.4.1 用户注册与登录"),
    createBodyParagraph("用例编号：UC-001。用例名称：用户注册与登录。参与者：普通访客。前置条件：用户访问系统首页。基本流程：1. 访客点击注册，输入用户名、密码、确认密码、真实姓名；2. 系统验证两次密码一致、用户名唯一性；3. 系统创建用户账号，发送欢迎提示；4. 用户使用注册账号登录，系统验证用户名密码，签发JWT Token；5. 登录成功，跳转至知识管理主界面。备选流程：用户名已存在 → 提示该用户名已被注册，请重新输入；密码强度不足 → 提示密码需包含字母与数字，至少8位。"),

    createH2("3.4.2 文档上传与处理"),
    createBodyParagraph("用例编号：UC-002。用例名称：文档上传与处理。参与者：普通会员、高级会员。前置条件：用户已登录。基本流程：1. 用户点击上传按钮，选择本地文件（≤200MB）；2. 系统检测文件类型，调用相应解析服务；3. 文本：Apache Tika解析 → 分块（800字符/块，150字符重叠）→ Embedding向量化；4. 图像：OCR识别 → CLIP向量化 + 标签提取；5. 音频：Whisper转写 → 文本分块 → Embedding向量化；6. 视频：关键帧提取 → CLIP向量化 + 标签提取；7. 向量存入PGVector，元数据存入MySQL；8. 系统返回上传成功提示，展示文档卡片。"),

    createH2("3.4.3 语义检索与RAG问答"),
    createBodyParagraph("用例编号：UC-003。用例名称：语义检索与RAG问答。参与者：普通会员、高级会员。前置条件：用户已登录，系统已有可检索文档。基本流程：1. 用户在搜索框输入自然语言查询，如查找关于Spring AI框架使用的文档；2. 系统将查询向量化，在向量数据库中执行Top-8检索（用户私有5条+公开3条）；3. 检索结果按相似度降序，取最高相似度结果构建上下文（上限6000字符）；4. 将用户查询与上下文注入Prompt，调用大语言模型生成回答；5. 系统展示回答内容，并列出引用的知识来源（文档名、片段、相似度）；6. 用户可点击来源链接查看原始文档。"),

    createH2("3.5 本章小结"),
    createBodyParagraph("本章分析了多模态知识管理系统的功能需求与非功能需求。功能需求涵盖多模态知识接入与管理、跨模态语义检索、RAG智能问答、权限安全管理及知识增值功能五个方面；非功能需求明确了系统在并发性能（≥300用户）、响应时间（≤3秒）、可靠性（降级机制）及安全性（JWT+RBAC）方面的指标。基于上述需求，后续章节将进行系统架构设计与详细实现。"),

    // ========== 第4章 系统设计 ==========
    new Paragraph({ children: [new PageBreak()] }),
    createH1("第4章 系统设计"),

    createH2("4.1 系统总体架构设计"),
    createBodyParagraph("本系统采用前后端分离架构，后端基于Spring Boot与Spring AI框架构建，前端采用Vue3与Element Plus技术栈，AI能力由Python FastAPI服务提供。系统整体划分为五个层次：数据接入层、语义向量化层、RAG检索增强层、业务服务层与用户接口层。"),
    createBodyParagraph("数据接入层负责接收用户上传的多模态文件，包括文本（PDF、Word、Markdown）、代码（Java、Python）、图像（JPG、PNG）、音频（MP3，WAV）、视频（MP4、AVI）等类型。文件首先存储于本地uploads目录或阿里云OSS对象存储，随后进入语义向量化层进行内容解析与Embedding生成。语义向量化层调用Python AI服务完成多模态内容处理：文本经Apache Tika解析后分块并向量化；图像经CLIP模型提取特征向量并生成标签；音频经Whisper模型转写为文本后再向量化；视频提取关键帧后经CLIP处理。不同模态的向量经PCA降维对齐后，统一存储于PGVector向量数据库。"),
    createBodyParagraph("RAG检索增强层是系统的核心智能引擎。当用户发起查询时，系统将查询语句向量化后，在向量数据库中执行相似度检索，返回Top-8相关结果（用户私有文档Top 5 + 公开文档Top 3）。检索结果经上下文组装后，与用户查询一同注入Prompt模板，调用大语言模型（通义千问Qwen-Plus）生成回答。业务服务层基于Spring Security与JWT实现认证授权，提供文档管理、聊天会话，知识检索等核心业务功能。用户接口层采用Vue3构建单页应用，通过Axios与后端RESTful API通信，提供流畅的用户交互体验。"),

    createH2("4.2 技术选型"),
    createBodyParagraph("本系统的技术选型综合考虑了功能需求，性能指标及工程实现成本，各层关键技术选型如下："),
    createBodyParagraph("后端核心框架选用Spring Boot 3.4.1与Spring AI 1.0.0-M4。Spring Boot提供了成熟的依赖注入与Web开发能力，Spring AI则封装了对大模型与向量数据库的统一抽象接口，大幅降低了RAG流程的工程实现复杂度。前端选用Vue3作为渐进式JavaScript框架，组合API（Composition API）提供了更灵活的组件逻辑组织能力；Vite作为构建工具，提供毫秒级热更新体验；Element Plus组件库则满足了企业级管理界面的开发需求。"),
    createBodyParagraph("向量数据库选用PGVector。PGVector作为PostgreSQL扩展，可复用现有数据库实例进行向量存储，无需引入独立的向量数据库服务。PGVector支持HNSW与IVFFlat两种索引算法，本系统采用HNSW索引以兼顾召回率与查询速度。向量维度配置为1024维，与通义千问Embedding模型text-embedding-v3的输出维度保持一致。"),
    createBodyParagraph("大语言模型与Embedding服务均调用阿里云Dashscope API。模型选用Qwen-Plus（通义千问Plus版）作为对话生成模型，text-embedding-v3作为文本向量化模型。Dashscope API采用HTTP协议调用，与Spring AI的OpenAI兼容接口无缝衔接。"),
    createBodyParagraph("Python AI服务选用FastAPI作为Web框架，lazy-loading模式加载模型以优化服务启动速度。图像特征提取采用CLIP（openai/clip-vit-base-patch32），输出512维向量；音频转写采用Whisper base模型；文本向量化采用Sentence-Transformers的all-MiniLM-L6-v2模型，输出384维向量。不同维度向量经PCA对齐至384维后，由Java端统一存储。"),

    createH2("4.3 功能模块设计"),
    createBodyParagraph("根据需求分析，本系统的功能模块划分如下："),
    createBodyParagraph("用户认证模块（AuthModule）：提供用户注册、登录、Token刷新功能。用户密码经BCrypt强哈希算法存储，认证成功后签发JWT Access Token（有效期1小时）与Refresh Token（有效期7天）。该模块基于Spring Security实现，JWT Filter拦截受保护资源请求，验证Token有效性并提取用户身份信息。"),
    createBodyParagraph("多模态知识接入模块（KnowledgeModule）：提供文档上传、解析、存储与生命周期管理功能。系统根据文件扩展名自动检测模态类型（TEXT/IMAGE/AUDIO/VIDEO），调用Python AI服务进行内容解析与向量化。文本分块策略为每800字符一块，块间重叠150字符。处理完成后，原始文件存于uploads目录，元数据存入MySQL，向量存入PGVector，文档状态更新为COMPLETED。"),
    createBodyParagraph("向量检索模块（VectorStoreModule）：封装向量数据库操作，提供向量写入与语义检索功能。检索时按用户ID过滤私有文档，同时检索公开文档，合并去重后返回Top-K结果。该模块实现了重试机制（最多3次），应对向量数据库临时性连接故障。"),
    createBodyParagraph("RAG问答模块（RagModule）：实现检索增强生成的核心流程。chatWithSources方法执行完整的RAG链路：向量检索→上下文组装（上限6000字符）→Prompt注入→LLM生成→溯源标注。返回结果包含回答内容与引用来源列表，每个来源标注文档名称、片段摘要与相似度得分。"),
    createBodyParagraph("权限管理模块（SecurityModule）：基于Spring Security与RBAC模型实现三级权限控制。角色分为ROLE_USER（普通会员）、ROLE_ADVANCED（高级会员）、ROLE_ADMIN（管理员）。普通会员享有基础文档管理与问答功能；高级会员享受更高检索配额与问答频率；管理员拥有用户管理、角色变更与系统监控权限。"),
    createBodyParagraph("知识增值模块（KnowledgeValueModule）：提供知识分类标签、笔记管理、链接分享与知识推荐功能。笔记关联原始文档片段，支持知识复用；链接分享支持设置过期时间；推荐算法基于用户历史检索记录进行协同过滤。"),

    createH2("4.4 数据库设计"),
    createH2("4.4.1 MySQL业务数据库"),
    createBodyParagraph("MySQL数据库（knowledge_management）存储系统业务元数据，主要表结构如下："),
    createBodyParagraph("users表：存储用户账户信息，包括用户名、密码哈希、真实姓名、邮箱、是否激活、创建时间等字段。角色信息通过user_roles中间表关联。"),
    createBodyParagraph("multimodal_documents表：存储文档元数据，包括文件名、文件类型、文件路径、文件大小、提取内容、标签、浏览次数、公开状态、上传时间等。user_id外键关联users表，标识文档所有者。"),
    createBodyParagraph("chat_sessions表：存储用户会话信息，包括会话标题、创建时间、更新时间。每个会话归属于特定用户。"),
    createBodyParagraph("chat_messages表：存储会话消息，包括角色（USER/ASSISTANT）、消息内容、引用来源（JSON格式）、创建时间。session_id外键关联chat_sessions表。"),
    createBodyParagraph("query_logs表：存储查询日志，包括用户ID、查询文本、响应耗时、检索文档数量、创建时间。用于系统运营分析与性能监控。"),
    createBodyParagraph("document_access_logs表：存储文档访问日志，包括文档ID、用户ID、访问类型（VIEW/DOWNLOAD/DELETE）、访问时间。用于知识版权追踪与使用分析。"),

    createH2("4.4.2 PostgreSQL向量数据库"),
    createBodyParagraph("PostgreSQL数据库（vector_db）通过PGVector扩展存储语义向量。向量表vector_store包含以下字段：id（主键）、content（文本内容片段）、metadata（JSONB格式元数据，含userId、documentId、fileType等）、embedding（1024维向量）。采用HNSW索引以支持高效相似度检索，索引构建参数m=16、ef_construction=64。"),

    createH2("4.5 接口设计"),
    createBodyParagraph("本系统提供RESTful风格的Web API，核心接口如下："),
    createBodyParagraph("认证接口（/api/auth/**）：POST /api/auth/register（用户注册）、POST /api/auth/login（用户登录，返回JWT）、POST /api/auth/refresh（刷新Token）。"),
    createBodyParagraph("知识管理接口（/api/knowledge/**）：POST /api/knowledge/upload（上传文档）、GET /api/knowledge/documents（查询用户文档列表）、DELETE /api/knowledge/{id}（删除文档）、POST /api/knowledge/{id}/toggle-public（切换公开状态）、PUT /api/knowledge/{id}/tags（更新标签）。"),
    createBodyParagraph("RAG问答接口（/api/chat/**）：POST /api/chat（发送问题，返回回答与来源）、GET /api/chat/sessions（查询会话列表）、GET /api/chat/sessions/{id}/messages（查询会话消息历史）。"),
    createBodyParagraph("管理接口（/api/admin/**）：GET /api/admin/users（查询用户列表）、PUT /api/admin/users/{id}/role（更新用户角色）、GET /api/admin/statistics（系统运营统计）。该组接口需管理员权限。"),

    createH2("4.6 本章小结"),
    createBodyParagraph("本章完成了多模态知识管理系统的总体架构设计与详细设计。系统采用前后端分离架构，后端基于Spring Boot + Spring AI，前端基于Vue3 + Element Plus，AI能力由Python FastAPI服务提供。技术选型上，选用PGVector作为向量数据库，Dashscope API作为大模型与Embedding服务。功能模块划分为用户认证、多模态知识接入、向量检索、RAG问答、权限管理及知识增值六大模块。数据库设计涵盖MySQL业务库与PostgreSQL向量库，提供了完整的RESTful API接口规范。上述设计为后续章节的系统实现奠定了基础。"),
];

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
        children: content
    }]
});

Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/初写-规范格式版-修正缩进.docx", buffer);
    console.log("首行缩进已修正为2字符（567 DXA），规范格式版文档生成成功！");
});
