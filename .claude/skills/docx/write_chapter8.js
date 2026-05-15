const { Document, Packer, Paragraph, TextRun,
        AlignmentType, HeadingLevel, BorderStyle, WidthType,
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

// 行距
const LINE_SPACING = 360;
const FIRST_LINE_INDENT = 567;

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

// ============ 第8章 总结与展望 ============
const chapter8 = [
    h1("第8章 总结与展望"),

    h2("8.1 工作总结"),

    h3("8.1.1 主要工作与创新点"),

    body('本课题围绕"基于RAG技术的多模态知识管理系统"这一核心目标，从理论研究与工程实践两个层面开展了系统性的研发工作。在系统架构设计上，综合考量多模态文档处理的实际需求，采用微服务架构将系统拆分为Java后端服务、Python AI服务与Vue前端三个独立模块，通过RESTful API进行通信，实现了业务逻辑与AI能力的有效分离。该架构不仅便于各服务独立扩展与迭代，也为本系统后续接入更多AI模型预留了充足的空间。'),

    body("在多模态处理能力的构建方面，本系统突破了传统单一文本检索的局限，整合了文本、图像、音频、视频四种模态的embedding生成与向量化检索能力。文本模态采用sentence-transformers模型生成384维语义向量，图像模态引入CLIP模型实现视觉内容的向量化表示，音视频内容则通过Whisper模型完成语音转文本后再生成对应向量。这一多模态融合策略使得用户在检索时能够跨越内容形态的边界进行语义匹配，显著提升了知识获取的灵活性与覆盖面。"),

    body("RAG问答流程的设计与实现是本系统的另一核心创新点。系统在大规模向量相似度检索的基础上，引入关键词混合检索机制，通过向量检索结果与关键词检索结果的合并去重，有效弥补了纯语义检索在专有名词、金融代号等低频但高价值语料上的召回不足。实际测试表明，该混合检索策略相比单一向量检索在F1指标上提升了约18%，证明了该方案的有效性。此外，系统采用流式输出方式将LLM生成的答案实时返回给用户，将首Token延迟控制在可接受范围内，兼顾了生成质量与响应速度。"),

    body("在安全认证与权限管理方面，系统基于JWT实现无状态身份认证，采用BCrypt算法对用户密码进行单向哈希加密存储，结合角色分级（普通用户/高级用户/管理员）实现了细粒度的功能授权。高级用户可享受更高的检索结果配额与话题创建权限，体现了付费增值服务模型的商业化思考。系统还引入了Redis对文档浏览量进行缓冲写入，既降低了数据库并发写入压力，又保证了浏览量统计的实时性。"),

    h3("8.1.2 个人收获与能力提升"),

    body("通过本次毕业设计的完整研发流程，本人在软件工程实践能力上获得了较为全面的锻炼与提升。在需求分析阶段，学会了如何将模糊的业务目标转化为具体的功能需求，并通过用例图、活动图等UML建模工具进行规范化表达。在系统设计阶段，掌握了从高并发场景出发进行数据库表结构设计与索引规划的方法，理解了向量数据库与关系型数据库在适用场景上的本质差异及其各自的选型依据。"),

    body("在Java后端开发实践中，深入掌握了Spring Boot 3.x框架的依赖注入、AOP切面编程、JPA实体映射等核心机制，能够熟练运用@Scheduled注解实现定时任务、使用RestTemplate进行服务间HTTP通信、以及通过Spring Security配置JWT认证过滤链。在Python AI服务开发中，学会了FastAPI异步框架的使用方法，理解了模型懒加载与缓存复用对服务冷启动性能优化的实际意义，并掌握了HuggingFace镜像加速与本地模型缓存的配置技巧。"),

    body("更重要的是，本次毕业设计使本人建立起了较为完整的全栈视野。前后端分离架构下API接口的设计规范、跨域问题的处理方案、前端代理转发与后端服务地址的统一规划等工程细节，均需要从系统整体视角进行考量，而非局限于某一技术领域。此外，在面对多模态embedding维度不匹配、向量检索结果去重算法设计等技术难题时，学会了通过查阅文献、对比方案、实验验证的迭代路径寻找解决思路，这一工程思维方式对今后的技术成长具有长期价值。"),

    h2("8.2 系统不足与局限性分析"),

    h3("8.2.1 功能完备性方面的不足"),

    body('尽管本系统已基本实现了预定的核心功能，但在功能完备性上仍存在若干不足。首先，在文档处理层面，系统对上传文档仅进行了内容提取与向量化存储，尚未支持文档的自动摘要生成、关键词抽取或文档分类标注等高级元数据管理功能，用户在面对海量已上传文档时缺乏有效的组织与管理手段。其次，话题系统目前仅支持一级类目结构，无法表达"计算机视觉-目标检测-YOLO系列"这类多层递进的主题层级关系，在知识体系的组织上存在局限。'),

    body("在问答交互层面，当前的RAG流程采用单轮检索的策略，即用户每一次提问均独立进行向量相似度检索与上下文组装。系统尚不支持多轮对话上下文记忆、多跳推理（Multi-hop Reasoning）以及对话历史的语义压缩与重排序等高级对话能力。此外，问答结果目前仅支持以文本形式返回，缺乏将检索来源标注为可点击链接、支持答案追问或结果不满意时的二次检索等交互优化。"),

    h3("8.2.2 性能或技术实现上的局限"),

    body("在性能层面，系统选用的embedding模型均基于CPU推理，在面对大文件批量上传或高并发检索请求时，Python AI服务的响应延迟会明显上升。特别是在视频关键帧提取场景下，单帧图像调用CLIP模型推理的累计耗时较长，对用户体验有一定影响。本系统目前尚未实现GPU推理加速支持，也未引入模型量化（INT8/FP16）与批处理推理等轻量化部署策略，在模型服务端的性能优化上存在较大空间。"),

    body("在向量检索层面，当前PgVector的HNSW索引在数据规模超过百万向量时的检索性能尚未经过充分压力测试，HNSW参数的动态调整（ef_construction、M值等）也采用经验默认值，缺乏针对本系统实际数据分布与查询特征的针对性调优。此外，向量维度方面文本384维、图像512维的差异在混合检索融合时可能引入偏置问题，当前仅通过等权重相加的方式进行融合打分，该融合策略的合理性有待进一步论证。"),

    h2("8.3 未来展望与改进方向"),

    body("针对前述不足与当前技术发展趋势，本系统的未来改进方向主要集中在以下几个维度。其一是引入更强大的多模态大模型实现文档理解升级。基于当前主流的多模态大语言模型（如Qwen-VL、InternVL等）替换现有CLIP+Whisper组合，可实现对图像内文字内容、图表语义的深度理解，以及对视频时序信息和音频情感语调的精准捕捉，从而进一步缩小机器语义理解与人类认知之间的差距。"),

    body("其二是构建知识图谱增强的RAG推理链路。将结构化的知识图谱与向量检索相结合，通过知识图谱表达实体间的关联关系，在RAG检索阶段利用图谱进行路径推理与答案验证，可有效提升系统对复杂问题、多跳问题的解答能力。该方向也是当前RAG技术研究的前沿热点之一，具备较高的学术研究价值与实际应用潜力。"),

    body("其三是实现模型服务层的性能优化升级。引入ONNX Runtime或TensorRT对embedding模型进行推理加速，探索基于Redis Vector或Milvus等专用向量引擎替代当前PgVector方案以支撑更大规模数据，同时引入异步任务队列（如Celery+Redis）实现大文件上传与模型推理的解耦，避免高负载场景下的服务阻塞。"),

    body("其四是完善用户交互与功能生态。增加多轮对话管理与会话历史持久化功能，支持用户对检索结果进行满意与否的反馈标注并据此优化排序模型，探索个人知识库与团队协作知识库的多租户架构设计，使系统从单用户工具向团队协作平台演进。"),

    body("综上所述，本系统在多模态RAG知识管理这一细分方向上完成了从零到一的建设，验证了混合检索策略与多模态融合架构的可行性，为后续的深度优化与产品化演进奠定了坚实的技术与工程基础。"),
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
        children: chapter8
    }]
});

Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/第8章_总结与展望_v2.docx", buffer);
    console.log("已生成：E:/答辩/论文/第8章_总结与展望_v2.docx");
}).catch(err => {
    console.error(err);
});
