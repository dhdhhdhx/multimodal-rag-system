const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, PageBreak, UnderlineType, ImageRun } = require('docx');
const fs = require('fs');
const path = require('path');

// ==================== 格式参数 ====================
const PAGE_WIDTH = 11906;
const PAGE_HEIGHT = 16838;
const TOP_MARGIN = 1418;
const BOTTOM_MARGIN = 1134;
const INNER_MARGIN = 1134;
const OUTER_MARGIN = 1134;
const GUTTER = 284;
const CONTENT_WIDTH = PAGE_WIDTH - INNER_MARGIN - OUTER_MARGIN - GUTTER; // 9354

const FONT_SIZE_BODY = 24;
const FONT_SIZE_H1 = 30;
const FONT_SIZE_H2 = 28;
const FONT_SIZE_H3 = 24;
const FONT_SIZE_FIG = 21; // 五号图注
const LINE_SPACING = 360;
const FIRST_LINE_INDENT = 567;

// 三线表边框
const thinBorder = { style: BorderStyle.SINGLE, size: 1, color: "000000" };
const borders = { top: thinBorder, bottom: thinBorder, left: thinBorder, right: thinBorder };
const topBottomBorder = { top: thinBorder, bottom: thinBorder, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } };

// ==================== 辅助函数 ====================

// 正文段落（首行缩进2字符）
function body(text) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "auto" },
        indent: { firstLine: FIRST_LINE_INDENT },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY })]
    });
}

// 一级标题：第X章 居中黑体小三
function h1(text) {
    return new Paragraph({
        spacing: { before: 240, after: 240, line: LINE_SPACING, lineRule: "auto" },
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H1, bold: true })]
    });
}

// 二级标题：X.X 顶格黑体四号
function h2(text) {
    return new Paragraph({
        spacing: { before: 180, after: 180, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H2, bold: true })]
    });
}

// 三级标题：X.X.X 顶格黑体小四
function h3(text) {
    return new Paragraph({
        spacing: { before: 120, after: 120, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_H3, bold: true })]
    });
}

// 四级标题：（X）小四宋体顶格
function h4(text) {
    return new Paragraph({
        spacing: { before: 80, after: 80, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY, bold: true })]
    });
}

// 图注（五号黑体，居中）
function figCaption(text) {
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 60, after: 200, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_FIG })]
    });
}

// 表题（五号黑体，居中）
function tableTitle(text) {
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 120, after: 60, line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "黑体", size: FONT_SIZE_FIG })]
    });
}

// 无缩进正文（用于列表项等）
function bodyNoIndent(text) {
    return new Paragraph({
        spacing: { line: LINE_SPACING, lineRule: "auto" },
        children: [new TextRun({ text, font: "宋体", size: FONT_SIZE_BODY })]
    });
}

// 空行
function spacer() {
    return new Paragraph({ spacing: { line: LINE_SPACING } });
}

// 换页
function pageBreak() {
    return new Paragraph({ children: [new PageBreak()] });
}

// 图片段落（居中，最大宽度9500 DXA≈19cm）
function createImagePara(imagePath) {
    const absPath = path.resolve(imagePath);
    const imageBuffer = fs.readFileSync(absPath);
    const imageSize = imageBuffer.length;
    // 获取图片原始尺寸
    const size = getImageSize(imageBuffer);
    const maxWidth = 8500;
    const ratio = size.width / size.height;
    const displayWidth = Math.min(size.width, maxWidth);
    const displayHeight = Math.round(displayWidth / ratio);
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 120, after: 60, line: LINE_SPACING, lineRule: "auto" },
        children: [
            new ImageRun({
                data: imageBuffer,
                transformation: {
                    width: displayWidth,
                    height: displayHeight,
                },
            })
        ]
    });
}

// 从PNG/JPEG buffer获取尺寸
function getImageSize(buffer) {
    // PNG: bytes 16-24 contain width/height as big-endian 4-byte ints
    if (buffer[0] === 0x89 && buffer[1] === 0x50 && buffer[2] === 0x4E && buffer[3] === 0x47) {
        const w = buffer.readUInt32BE(16);
        const h = buffer.readUInt32BE(20);
        return { width: w, height: h };
    }
    // JPEG: scan for SOF0 marker
    if (buffer[0] === 0xFF && buffer[1] === 0xD8) {
        let i = 2;
        while (i < buffer.length - 1) {
            if (buffer[i] !== 0xFF) { i++; continue; }
            const marker = buffer[i+1];
            if (marker >= 0xC0 && marker <= 0xCF && marker !== 0xC4 && marker !== 0xC8 && marker !== 0xCC) {
                const h = buffer.readUInt16BE(i+5);
                const w = buffer.readUInt16BE(i+7);
                return { width: w, height: h };
            }
            const len = buffer.readUInt16BE(i+2);
            i += 2 + len;
        }
    }
    return { width: 800, height: 600 };
}

// 三线表（表头有底边，末行有顶边和底边）
function createThreeLineTable(headers, rows, widths) {
    const headerCells = headers.map((h, i) =>
        new TableCell({
            borders: { top: thinBorder, bottom: { style: BorderStyle.SINGLE, size: 6, color: "000000" }, left: thinBorder, right: thinBorder },
            width: { size: widths[i], type: WidthType.DXA },
            shading: { fill: "D9D9D9", type: ShadingType.CLEAR },
            margins: { top: 80, bottom: 80, left: 120, right: 120 },
            verticalAlign: VerticalAlign.CENTER,
            children: [new Paragraph({
                alignment: AlignmentType.CENTER,
                children: [new TextRun({ text: h, font: "黑体", size: FONT_SIZE_BODY, bold: true })]
            })]
        })
    );

    const dataRows = rows.map(row =>
        new TableRow({
            children: row.map((cell, i) =>
                new TableCell({
                    borders,
                    width: { size: widths[i], type: WidthType.DXA },
                    margins: { top: 80, bottom: 80, left: 120, right: 120 },
                    children: [new Paragraph({
                        alignment: AlignmentType.LEFT,
                        children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_BODY })]
                    })]
                })
            )
        })
    );

    return new Table({
        width: { size: CONTENT_WIDTH, type: WidthType.DXA },
        columnWidths: widths,
        rows: [new TableRow({ children: headerCells }), ...dataRows]
    });
}

// ==================== 正文内容 ====================
const content = [

    // ==================== 第1章 绪论 ====================
    h1("第1章 绪论"),

    h2("1.1 研究背景"),
    body("随着信息技术的飞速发展，高校教学与科研活动所产生的数据规模呈指数级增长，其数据形态已从单一的结构化文本逐步演变为多模态、非结构化数据并存的复杂局面。课程文档、科研论文、源代码、实验数据、图像以及音视频资料等海量资源分散存储于网盘、代码仓库及各类文档平台之中。然而，由于缺乏统一的语义组织与智能检索机制，「知识孤岛「现象日益严重，资源冗余与检索困难成为制约高校知识管理效率提升的核心瓶颈。传统的基于关键词匹配的检索技术仅能处理表层文本比对，无法挖掘图片、音视频等模态数据的深层语义信息，难以实现跨模态的知识关联与精准定位。"),
    spacer(),
    body("人工智能技术的快速发展为大模型时代的知识管理带来了新的机遇。以ChatGPT、Qwen为代表的大语言模型（Large Language Model, LLM）在自然语言理解与生成方面取得了突破性进展，为智能问答提供了强有力的技术支撑。然而，单纯依赖LLM进行知识问答存在固有缺陷：一是知识时效性滞后，模型参数中的知识难以实时更新；二是「幻觉「问题，模型可能生成看似合理但实际错误的内容；三是回答缺乏可追溯性，无法明确标注答案的事实依据来源。这些问题严重制约了LLM在专业领域知识管理中的实际应用效果。"),

    h2("1.2 研究意义"),
    body("检索增强生成（Retrieval-Augmented Generation, RAG）技术的出现为解决上述问题提供了新的技术路径。RAG通过将外部知识库检索结果与大模型生成能力深度融合，能够有效弥补单纯依赖模型参数存储知识所带来的时效性滞后、「幻觉「频发及回答不可追溯等固有缺陷。然而，现有RAG技术研究与应用多集中于纯文本领域，面对高校教学与科研场景中复杂的多模态知识管理需求，现有能力显得力不从心。海量的课程实录视频、实验数据图像及科研源代码等非结构化资源缺乏统一的语义表征与跨模态关联机制。"),
    spacer(),
    body("本研究旨在设计与实现一套基于RAG技术的多模态知识管理系统，具有以下三方面意义："),
    body("（1）理论意义：探索RAG在多模态语义检索与生成中的融合机制，研究跨模态语义对齐与向量表征的统一化方法，为多模态RAG技术在教育信息化领域的应用提供理论支撑。"),
    body("（2）工程意义：提出基于Spring AI与Python微服务协同的高校知识管理架构，实现Java生态与AI能力的有机融合，为企业级多模态RAG系统的工程化落地提供参考范例。"),
    body("（3）应用意义：推动教学与科研知识的高效组织、智能问答与成果共享，有效解决「知识孤岛「问题，提升高校知识资产的复用价值，为智慧校园建设提供切实可行的技术方案。"),

    h2("1.3 国内外研究现状"),
    h3("1.3.1 RAG技术研究进展"),
    body("检索增强生成技术最早由Lewis等人于2020年提出，其核心理念是将大规模文档检索与序列生成模型相结合，用于知识密集型自然语言处理任务。Davis E和Wilson K在《Retrieval-augmented generation: principles and practice》中系统阐述了RAG技术的原理与最佳实践，指出该技术通过引入外部事实依据，能够显著增强生成结果的准确性与可追溯性，已成为缓解大模型「幻觉「问题的主流技术路线。"),
    spacer(),
    body("在RAG架构优化方面，国内外学者展开了大量研究。Facebook AI提出了RAG-Reader架构，通过增强型检索器提升文档理解能力；Google则将RAG与Meena等对话模型结合，探索面向对话场景的检索增强方案。近年来，随着向量数据库技术的成熟，基于稠密向量检索的RAG系统逐渐成为主流，典型代表包括基于Milvus、PGVector等向量数据库构建的语义检索系统。"),
    body("然而，现有RAG研究多聚焦于纯文本模态，对于图像、音频、视频等多模态数据的处理能力相对薄弱。多模态大模型（如GPT-4V、Qwen-VL）的出现为解决这一问题提供了新的可能，但如何将多模态理解能力与RAG架构有效融合，仍是当前研究的前沿课题。"),

    h3("1.3.2 多模态嵌入与跨模态对齐技术"),
    body("多模态嵌入技术是将来自不同模态（文本、图像、音频、视频）的数据映射到统一语义向量空间的核心技术，是实现跨模态检索与融合的基础。OpenAI提出的CLIP（Contrastive Language-Image Pre-training）模型通过在4亿图文对上对比学习，构建了视觉与文本的联合语义空间，使跨模态检索成为可能。CLIP采用Vision Transformer（ViT）作为图像编码器，输出512维图像向量，在图像-文本检索任务上取得了显著效果。"),
    spacer(),
    body("在文本嵌入层面，Sentence-Transformers系列模型（如all-MiniLM-L6-v2）通过对比学习在大规模语料上进行预训练，能够将句子级文本映射为稠密语义向量，在语义相似度计算任务上表现优异。由于不同模态的原始向量维度不一致（如CLIP图像向量为512维，Sentence-Transformers文本向量为384维），需要进行跨模态对齐。主成分分析（PCA）算法是常用的向量维度压缩与对齐方法，可将高维异构向量统一映射至目标维度。"),

    h3("1.3.3 高校知识管理系统发展"),
    body("在高校知识管理领域，传统的管理系统主要聚焦于文档的数字化存储与关键词检索。以Content Management System（CMS）和Learning Management System（LMS）为代表的管理平台在国内外高校中得到了广泛应用，但其核心能力仍停留在文件上传、分类存储与基础搜索层面，无法提供语义级别的智能检索与问答服务。"),
    spacer(),
    body("随着人工智能技术的快速发展，部分高校开始探索将AI能力引入知识管理系统。清华大学「水木搜索「、北京大学「博古搜索「等学术搜索引擎尝试引入语义检索技术；上海交通大学构建了基于知识图谱的科研知识管理系统。然而，这些系统在多模态知识处理能力方面仍存在明显不足，难以为师生提供跨模态语义关联的智能服务。"),

    h2("1.4 课题任务"),
    h3("1.4.1 系统目标"),
    body("本课题旨在设计并实现一套基于RAG技术的多模态知识管理系统，具体目标包括："),
    body("（1）多模态知识统一接入：系统支持文本（PDF、Word、Markdown）、代码（Java、Python）、图像（JPG、PNG）、音频（MP3、WAV）、视频（MP4、AVI）等全类型文件的上传与解析，消除格式壁垒，实现异构资源的结构化预处理；"),
    body("（2）跨模态语义检索：基于Embedding模型将多模态内容转化为统一语义向量，存储于向量数据库，支持用户通过自然语言查询快速匹配相关文本、图像、音频及视频片段；"),
    body("（3）RAG智能问答：构建检索增强生成流程，将跨模态检索结果与大语言模型生成能力相融合，输出可溯源、基于事实的精准回答；"),
    body("（4）权限与知识增值：基于RBAC模型实现三级权限控制（普通会员、高级会员、管理员），提供知识分类、笔记管理、链接分享与知识推荐等增值功能。"),

    h3("1.4.2 本人承担任务"),
    body("本人在该课题中承担了以下主要工作："),
    body("（1）完成系统的需求分析与总体设计工作，参与系统架构选型与技术方案论证；"),
    body("（2）负责多模态知识接入模块的开发，实现基于Apache Tika的文本解析、基于Whisper的音频转写及基于CLIP的图像特征提取功能；"),
    body("（3）设计并实现RAG检索增强生成模块，完成向量数据库（PGVector）的集成与语义检索流程的优化；"),
    body("（4）开发知识推荐模块，基于协同过滤算法实现个性化文档推荐功能；"),
    body("（5）完成系统各模块的功能测试与性能测试，撰写相关技术文档。"),

    h2("1.5 论文组织结构"),
    body("本论文共分为七章，各章内容安排如下："),
    body("第1章 绪论。介绍研究背景与意义，综述国内外研究现状，明确课题任务与本人承担工作，给出论文组织结构。"),
    body("第2章 相关技术与理论基础。介绍本系统所涉及的关键技术，包括RAG技术、多模态嵌入与跨模态对齐、向量数据库、Spring AI框架、Vue3前端技术及Python AI服务等。"),
    body("第3章 系统需求分析。分析高校多模态知识管理的功能需求与非功能需求，给出系统用例模型，明确系统的设计目标与约束条件。"),
    body("第4章 系统设计。完成系统总体架构设计、技术选型、功能模块划分、数据库设计及接口设计，给出系统的详细设计方案。"),
    body("第5章 系统实现。描述核心功能模块的具体实现过程，展示关键代码与实现效果。"),
    body("第6章 系统测试。设计并执行功能测试与性能测试方案，分析测试结果，验证系统是否满足设计要求。"),
    body("第7章 总结与展望。总结本文的研究工作与创新点，分析系统存在的不足，并对未来研究方向进行展望。"),

    // ==================== 第2章 相关技术与理论基础 ====================
    pageBreak(),
    h1("第2章 相关技术与理论基础"),

    h2("2.1 RAG技术"),
    h3("2.1.1 RAG技术原理"),
    body("检索增强生成（Retrieval-Augmented Generation, RAG）是一种将信息检索与大语言模型生成能力相融合的技术架构。其核心思想是：当用户提出问题时，系统首先从外部知识库中检索出与问题相关的文档片段，然后将检索结果作为上下文（Context）输入大语言模型，由模型基于上下文生成最终回答。"),
    spacer(),
    body("RAG架构一般包含三个核心阶段："),
    body("（1）检索阶段（Retrieval）：系统将用户查询转化为语义向量，在向量数据库中进行相似度匹配，返回Top-K相关文档片段。向量化过程通常使用预训练的Embedding模型（如Sentence-Transformers、text-embedding-v3等）将文本映射为稠密向量；"),
    body("（2）增强阶段（Augmentation）：系统将检索结果与原始查询进行上下文组装，形成结构化的Prompt模板。增强阶段的关键在于如何合理地组织检索片段，使其在有限的上下文窗口内提供最有价值的信息；"),
    body("（3）生成阶段（Generation）：大语言模型基于包含事实依据的Prompt输出最终回答。由于回答直接基于检索到的文档片段生成，答案的每一个主张都可以追溯至原始来源，有效提升了回答的可信度和可验证性。"),

    h3("2.1.2 RAG技术优势与局限"),
    body("与传统纯生成式问答系统相比，RAG技术具有以下显著优势："),
    body("（1）知识时效性强：外部知识库可以实时更新，模型回答可以反映最新信息，有效解决大模型知识滞后的难题；"),
    body("（2）减少「幻觉「生成：通过引入外部事实依据约束模型输出，显著降低模型生成错误内容的概率；"),
    body("（3）回答可追溯：答案中的每个主张都可以关联至原始文档，便于用户核实与溯源；"),
    body("（4）私有知识保护：企业私有数据无需暴露给模型，仅通过检索方式为模型提供上下文，保障数据安全。"),
    spacer(),
    body("然而，RAG技术也存在一定局限：检索质量高度依赖知识库的完整性与准确性；多模态场景下跨模态语义对齐仍面临挑战；大规模向量检索的计算成本随数据量增长而上升。"),

    h2("2.2 多模态嵌入与跨模态对齐"),
    h3("2.2.1 文本嵌入"),
    body("文本嵌入是将自然语言文本映射为稠密向量的技术，是RAG语义检索的核心基础。词嵌入模型通过在大规模语料上学习，使语义相似的文本在向量空间中具有较小的几何距离。BERT等基于Transformer的模型通过自注意力机制捕获上下文相关语义，进一步提升了嵌入质量。"),
    spacer(),
    body("本系统文本向量化采用阿里云Dashscope的text-embedding-v3模型，输出维度为1024维。该模型在海量中文语料上预训练，在中文语义相似度任务上表现优异，且通过统一的API接口封装，工程集成便捷。"),

    h3("2.2.2 图像嵌入"),
    body("CLIP（Contrastive Language-Image Pre-training）由OpenAI提出，是当前最具影响力的图像-文本联合嵌入模型。CLIP在4亿图文对上通过对比学习训练，构建了视觉与文本的联合语义空间。在该空间中，语义相关的图像和文本描述具有相近的向量表示，从而支持跨模态检索。"),
    spacer(),
    body("CLIP采用双编码器架构：图像侧使用Vision Transformer（ViT-B/32或ViT-L/14）作为编码器，输出512维图像向量；文本侧使用Transformer编码器，输出512维文本向量。两个编码器分别处理图像和文本，通过对比学习使配对的图文向量相互接近。CLIP的预训练目标是最小化匹配图文对的余弦距离，同时最大化不匹配图文对的距离。"),

    h3("2.2.3 音频嵌入"),
    body("音频向量化通常采用两种技术路线：一是将音频转换为梅尔频谱图（Mel Spectrogram）后使用卷积神经网络（CNN）提取特征；二是使用 Whisper 等端到端模型直接生成文本转写，再对文本进行嵌入。Whisper是OpenAI开源的音频转写模型，在多语言音频识别任务上取得了优异性能。"),
    spacer(),
    body("本系统采用Whisper base模型进行音频转写，将音频内容转换为文本后，通过文本嵌入模型生成语义向量。对于纯音频内容，文本嵌入提供了语义层面的向量表征；对于需要保留音频特色的场景，可结合音频特征提取网络提取声学向量。"),

    h3("2.2.4 跨模态对齐"),
    body("由于不同模态的原始向量维度不一致（如CLIP图像向量为512维，Sentence-Transformers文本向量为384维，Whisper输出维度也不相同），需要进行跨模态对齐，将异构向量映射至统一维度，以便于在单一向量空间中进行混合检索。"),
    spacer(),
    body("主成分分析（PCA）是一种常用的降维与对齐方法。PCA通过线性变换将高维数据投影至低维空间，同时尽可能保留原始数据的方差信息。本系统采用PCA算法将不同模态的异构向量统一对齐至384维，形成统一的语义向量空间，为后续的向量数据库混合检索奠定基础。"),

    h2("2.3 向量数据库"),
    h3("2.3.1 向量数据库概述"),
    body("向量数据库是专门用于存储和检索高维向量（Embedding）的数据库系统，在RAG架构中承担语义检索的核心职能。相较于传统关系型数据库，向量数据库通过近似最近邻（Approximate Nearest Neighbor, ANN）索引算法，能够在海量向量中快速定位与查询向量语义最相似的结果。"),
    spacer(),
    body("向量检索的核心指标包括：检索速度（每秒查询数QPS）、召回率（检索结果与实际最近邻的匹配程度）、内存占用（向量数据的存储空间）。ANN算法通过构建索引结构牺牲少量召回率，换取显著的性能提升。常用的ANN算法包括HNSW、IVFFlat、PQ等，适用于不同应用场景。"),

    h3("2.3.2 主流向量数据库对比"),
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
    body("本系统选用PGVector作为向量存储引擎，原因如下：（1）PGVector是PostgreSQL的扩展插件，可与业务MySQL数据库通过同一数据库实例管理，简化运维复杂度；（2）PGVector支持HNSW和IVFFlat两种索引算法，HNSW索引在召回率与查询速度方面表现均衡；（3）Spring AI框架提供了对PGVector的原生集成支持，工程实现成本低；（4）PGVector的向量维度支持至1024维，与本系统的Embedding输出维度保持一致。"),

    h3("2.3.3 PGVector检索机制"),
    body("PGVector的向量检索基于余弦相似度（Cosine Similarity）计算向量间的语义关联程度。余弦相似度的取值范围为[-1, 1]，值越接近1表示两个向量的语义方向越一致，即语义越相似。"),
    spacer(),
    body("在RAG检索场景中，系统一般取Top-K（通常K=5~10）个最高相似度结果作为上下文输入。PGVector支持通过SQL语句进行向量相似度查询，可与关系型查询条件结合，实现带过滤条件的语义检索（如仅检索特定用户的文档）。"),

    h2("2.4 Spring AI框架"),
    h3("2.4.1 Spring AI概述"),
    body("Spring AI是Spring生态下面向AI应用开发的工程框架，旨在为Java开发者提供轻量化接入大模型与向量数据库的统一抽象接口。相较于直接调用大模型API，Spring AI通过抽象化的Service层与Prompt模板机制，大幅简化了AI功能的工程实现难度，使Java开发者无需深入了解AI模型底层原理，即可快速构建智能应用。"),
    spacer(),
    body("Spring AI的核心概念包括：（1）Model，封装了对不同大模型（OpenAI、Azure OpenAI、Hugging Face等）的统一调用接口；（2）Embeddings，用于文本向量化的抽象接口，支持多种Embedding模型；（3）VectorStore，向量存储的抽象接口，提供了对PGVector、Milvus等向量数据库的统一CRUD操作；（4）Prompt与ChatOptions，分别用于Prompt模板构造与模型参数配置。"),

    h3("2.4.2 RAG实现机制"),
    body("Spring AI的RAG实现依赖于VectorStore与ChatModel的协同工作。VectorStore负责语义检索，从向量数据库中获取与查询相关的文档片段；ChatModel负责基于检索结果生成回答。Spring AI 1.0.0-M4版本引入了对PGVector的原生支持，通过PgVectorStore类可快速完成向量存储与检索的集成。"),
    spacer(),
    body("Spring AI的工作流程如下：用户查询首先经Embeddings接口向量化，然后在VectorStore中执行相似度检索；检索结果经上下文组装后，通过PromptTemplate注入System Prompt；最后由ChatModel生成回答。整个流程通过链式调用（fluent API）串联，代码简洁易读。"),

    h2("2.5 Vue3前端技术"),
    h3("2.5.1 Vue3组合式API"),
    body("Vue3是当前主流的前端JavaScript框架之一，相比Vue2在渲染性能、TypeScript支持、Composition API等方面进行了全面升级。Vue3引入的组合式API（Composition API）提供了更灵活的组件逻辑组织能力，使开发者能够将相关功能的代码逻辑归集在一起，而非分散在不同的选项（data、methods、computed）中。"),
    spacer(),
    body("Vue3的响应式系统基于ES6 Proxy实现，相比Vue2的Object.defineProperty具有更好的性能和更完整的响应能力。Vue3还提供了Suspense、Fragment、Teleport等新特性和更简洁的API设计，是构建单页应用（SPA）的理想选择。"),

    h3("2.5.2 Element Plus组件库"),
    body("Element Plus是基于Vue3的企业级UI组件库，提供了丰富的桌面端组件，包括表单、表格、对话框、导航菜单等。Element Plus的组件设计遵循一致的设计语言，具有良好的可访问性（Accessibility）支持，可大幅提升管理系统的开发效率。"),
    spacer(),
    body("本系统选用Vue3 + Element Plus构建前端界面，主要利用其表单组件（上传控件、搜索框）、表格组件（文档列表、会话历史）、对话框组件（问答弹窗、文档预览）等构建知识检索与RAG问答的用户交互界面。"),

    h2("2.6 Python AI服务"),
    h3("2.6.1 FastAPI框架"),
    body("FastAPI是Python生态下用于构建API服务的现代高性能框架，基于Starlette和Pydantic构建，提供了自动数据验证、自动文档生成（Swagger UI）、异步请求处理等特性。FastAPI的异步特性使其在处理IO密集型任务（如模型推理、文件读写）时具有显著性能优势。"),
    spacer(),
    body("本系统选用FastAPI构建Python AI微服务，将CLIP图像嵌入、Whisper音频转写、PCA向量对齐等AI能力封装为RESTful API，供Java后端调用。FastAPI的懒加载机制允许模型在服务启动后才加载，避免了启动时间过长的问题。"),

    h3("2.6.2 CLIP与Whisper模型"),
    body("本系统的Python AI服务集成了以下核心模型：（1）CLIP（openai/clip-vit-base-patch32）用于图像特征提取，输出512维向量；（2）Whisper base模型用于音频转写，将语音内容转换为文本；（3）Sentence-Transformers的all-MiniLM-L6-v2模型用于文本嵌入，输出384维向量。各模型通过lazy-loading模式按需加载，降低服务启动耗时。"),

    h2("2.7 本章小结"),
    body("本章介绍了构建多模态知识管理系统所涉及的关键技术理论。RAG技术通过融合外部知识检索与大模型生成能力，有效提升了问答系统的准确性与可追溯性，是本系统的核心技术基础；多模态嵌入与跨模态对齐技术为异构模态数据的统一语义表征提供了技术路径；向量数据库作为RAG架构的核心基础设施，支撑了高效的语义检索；Spring AI框架为Java生态下的AI应用开发提供了轻量化集成方案；Vue3前端技术与Python FastAPI微服务共同构成了系统的前后端技术架构。上述技术为后续章节的系统设计与实现奠定了理论基础。"),

    // ==================== 第3章 系统需求分析 ====================
    pageBreak(),
    h1("第3章 系统需求分析"),

    h2("3.1 需求概述"),
    h3("3.1.1 问题背景"),
    body("高校在教学与科研活动中积累了大量的多模态知识资产，包括课程文档、科研论文、源代码、实验图像、课程实录视频等。这些知识资产分布存储于网盘、代码仓库及各类文档平台之中，给知识的统一管理与高效复用带来了严峻挑战。"),
    spacer(),
    body("当前高校知识管理面临以下核心挑战：（1）知识分散存储，缺乏统一的语义组织与检索机制，形成「知识孤岛「；（2）传统基于关键词匹配的检索技术仅能进行表层文本比对，无法理解语义关联，难以实现跨模态检索；（3）师生在科研与学习过程中缺乏高效的智能问答手段，海量知识资源难以得到有效利用。"),

    h3("3.1.2 系统目标"),
    body("针对上述痛点，本系统旨在构建一套基于RAG技术的多模态知识管理系统，实现多模态知识的统一接入、跨模态语义检索与智能问答，为高校知识资产的高效组织与价值释放提供工程化解决方案。系统应具备以下核心能力："),
    body("（1）多模态知识统一接入：支持文本、代码、图片、音频、视频等全类型文件的上传与自动解析，无需用户手动选择文件分类；"),
    body("（2）跨模态语义检索：用户通过自然语言查询即可在向量空间中匹配相关片段，支持文本、图像、音频及视频的混合检索；"),
    body("（3）RAG智能问答：基于检索结果生成可溯源的精准回答，每个答案均标注参考来源；"),
    body("（4）权限与知识增值：实现三级权限控制，提供笔记管理、链接分享、知识推荐等增值功能。"),

    h2("3.2 用户角色分析"),
    body("系统用户分为普通会员、高级会员、管理员三种角色，各角色权限定义如表3.1所示。"),
    spacer(),
    tableTitle("表3.1 系统用户角色定义"),
    createThreeLineTable(
        ["角色", "权限描述"],
        [
            ["普通会员", "上传与管理个人知识文档、进行语义检索与RAG问答、查看公开知识库、使用笔记与分享功能"],
            ["高级会员", "享受更高的检索配额与问答频率、优先使用知识推荐功能、创建知识专题"],
            ["管理员", "用户管理与权限配置、系统运营数据分析、全局知识库监督与审核"],
        ],
        [2000, 7354]
    ),
    spacer(),

    h2("3.3 功能需求分析"),
    h3("3.3.1 多模态知识接入与管理"),
    body("系统应提供完整的文档上传、解析、存储与生命周期管理功能，具体用例如图3.1所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/论文配图/图3.1_文档上传与处理用例图.png"),
    figCaption("图3.1 文档上传与处理用例图"),
    spacer(),
    body("（1）全类型文件上传：用户可通过前端界面上传多种格式的文件，单文件大小上限为200MB。系统根据文件扩展名自动检测模态类型（TEXT/IMAGE/AUDIO/VIDEO），无需用户手动选择分类；"),
    body("（2）内容解析与提取：针对不同模态，系统调用相应的解析工具进行内容提取。文本类文档经Apache Tika通用解析；图片类文件经OCR文字识别；音频文件经Whisper模型转写为文本；视频文件提取关键帧并进行CLIP特征提取；"),
    body("（3）知识向量化与存储：解析后的内容经Embedding模型转化为统一语义向量，存入向量数据库（PGVector）；原始文件及元数据存入MySQL数据库，实现业务数据与向量数据的分离管理；"),
    body("（4）文档生命周期管理：用户可查看个人上传文档列表、修改文档标签、切换公开/私有状态、删除文档。删除操作应级联清除文件存储、向量数据库记录及MySQL元数据。"),

    h3("3.3.2 跨模态语义检索"),
    body("系统应提供基于自然语言的语义检索能力，支持跨模态知识关联，具体用例如图3.2所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/论文配图/图3.2_语义检索用例图.png"),
    figCaption("图3.2 语义检索用例图"),
    spacer(),
    body("（1）语义向量检索：用户输入查询语句后，系统将其转化为语义向量，在向量数据库中进行相似度匹配，返回Top-K（用户私有文档Top 5 + 公开文档Top 3）最相关结果；"),
    body("（2）混合检索机制：系统同时支持关键词检索与语义检索。当向量检索服务不可用时，自动降级为基于MySQL全文索引的关键词检索，确保系统可用性达到100%；"),
    body("（3）检索结果呈现：检索结果按相似度降序排列，展示文档名称、所属模态、匹配片段摘要及相似度得分，支持用户点击查看原文。"),

    h3("3.3.3 RAG智能问答"),
    body("系统应提供基于检索增强生成的智能问答功能，具体用例如图3.3所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/论文配图/图3.3_RAG问答用例图.png"),
    figCaption("图3.3 RAG问答用例图"),
    spacer(),
    body("（1）对话上下文管理：支持多轮对话场景，系统维护用户会话上下文，理解对话连贯性与指代关系；"),
    body("（2）溯源展示：回答生成后，系统标注答案所引用的知识来源，包括原始文档名称、片段位置及相似度得分，实现回答的可追溯性；"),
    body("（3）生成质量控制：通过Prompt工程约束模型输出格式，确保回答简洁准确，避免幻觉内容。"),

    h3("3.3.4 权限与安全管理"),
    body("系统应实现基于RBAC模型的细粒度权限控制，用例如图3.4所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/论文配图/图3.4_权限管理用例图.png"),
    figCaption("图3.4 权限管理用例图"),
    spacer(),
    body("（1）用户认证：支持用户注册、登录与JWT Token认证，支持Access Token（1小时有效期）与Refresh Token（7天有效期）双Token机制；"),
    body("（2）角色权限控制：普通会员、高级会员、管理员三级权限体系，管理员可进行用户启用/禁用、角色变更等操作；"),
    body("（3）数据隔离：用户只能检索和管理个人上传的私有文档，公开文档可被所有登录用户检索。"),

    h3("3.3.5 知识增值功能"),
    body("系统应提供知识复用与共享的增值服务，具体包括："),
    body("（1）知识分类与标签：用户可为文档添加自定义标签，支持按标签筛选检索范围；"),
    body("（2）多模态笔记：用户可在检索结果基础上创建笔记，笔记与原始文档片段关联保存；"),
    body("（3）链接分享：用户可生成带时效性（可设置过期时间）的分享链接，将特定知识专题分享给其他用户；"),
    body("（4）知识推荐：基于协同过滤算法，根据用户历史检索与问答记录，推荐可能感兴趣的知识文档。"),

    h2("3.4 非功能需求分析"),

    h3("3.4.1 性能需求"),
    body("系统在高校高并发场景下应满足表3.2所示的性能指标要求。"),
    spacer(),
    tableTitle("表3.2 系统性能需求指标"),
    createThreeLineTable(
        ["指标", "要求"],
        [
            ["系统并发用户数", "≥300用户同时在线"],
            ["页面响应时间", "≤3秒（检索、文档列表等页面操作）"],
            ["智能问答生成时间", "≤8秒（首字输出延迟）"],
            ["向量检索召回率", "Top-K相关性文档召回准确率≥85%"],
            ["多模态解析吞吐量", "单个PDF文档（50页）解析≤10秒"],
        ],
        [4677, 4677]
    ),
    spacer(),

    h3("3.4.2 可靠性需求"),
    body("系统应在高并发场景（如高校选课、期末复习）下保持稳定运行。对于大模型API调用失败、向量数据库连接超时等临时性故障，系统应具备自动重试机制（最多3次）。当核心检索服务不可用时，自动降级为关键词检索，保证核心功能可用。"),

    h3("3.4.3 安全性需求"),
    body("用户密码采用BCrypt强哈希算法存储；JWT Secret配置不少于256位；用户上传的文件经安全扫描后存储；系统实施严格的角色权限检查，防止越权访问。"),

    h3("3.4.4 可维护性需求"),
    body("系统采用Docker容器化部署，支持在不同服务器环境下的快速复现；前后端分离架构便于独立开发与迭代；Spring Boot Actuator提供健康检查与运行监控接口。"),

    h2("3.5 本章小结"),
    body("本章分析了多模态知识管理系统的功能需求与非功能需求。功能需求涵盖多模态知识接入与管理、跨模态语义检索、RAG智能问答、权限安全管理及知识增值功能五个方面，每个功能模块均提供了用例图与详细描述；非功能需求明确了系统在并发性能（≥300用户）、响应时间（≤3秒）、可靠性（降级机制）及安全性（JWT+RBAC）方面的指标。基于上述需求，后续章节将进行系统架构设计与详细实现。"),

    // ==================== 第4章 系统设计 ====================
    pageBreak(),
    h1("第4章 系统设计"),

    h2("4.1 系统总体架构设计"),
    h3("4.1.1 架构概述"),
    body("本系统采用前后端分离架构，后端基于Spring Boot与Spring AI框架构建，前端采用Vue3与Element Plus技术栈，AI能力由Python FastAPI服务提供。系统整体划分为五个层次：数据接入层、语义向量化层、RAG检索增强层、业务服务层与用户接口层。各层职责清晰，通过标准化接口通信，便于独立开发与维护。"),

    h3("4.1.2 层次结构"),
    body("系统的层次结构如图4.1所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/论文配图/图4.1_系统层次结构图.png"),
    figCaption("图4.1 系统层次结构图"),
    spacer(),
    body("（1）数据接入层负责接收用户上传的多模态文件，包括文本、代码、图像、音频、视频等类型。文件首先存储于本地uploads目录或阿里云OSS对象存储，随后进入语义向量化层进行内容解析与Embedding生成；"),
    body("（2）语义向量化层调用Python AI服务完成多模态内容处理：文本经Apache Tika解析后分块并向量化；图像经CLIP模型提取特征向量；音频经Whisper模型转写后再向量化；视频提取关键帧后经CLIP处理。不同模态的向量经PCA降维对齐后，统一存储于PGVector向量数据库；"),
    body("（3）RAG检索增强层是系统的核心智能引擎。当用户发起查询时，系统将查询语句向量化后在向量数据库中执行相似度检索，检索结果经上下文组装后与用户查询一同注入Prompt模板，调用大语言模型生成回答；"),
    body("（4）业务服务层基于Spring Security与JWT实现认证授权，提供文档管理、聊天会话、知识检索等核心业务功能；"),
    body("（5）用户接口层采用Vue3构建单页应用，通过Axios与后端RESTful API通信，提供流畅的用户交互体验。"),

    h2("4.2 技术选型"),
    h3("4.2.1 后端技术选型"),
    body("后端核心框架选用Spring Boot 3.4.1与Spring AI 1.0.0-M4。Spring Boot提供了成熟的依赖注入与Web开发能力，Spring AI则封装了对大模型与向量数据库的统一抽象接口，大幅降低了RAG流程的工程实现复杂度。Java版本选用JDK 17，充分利用其新特性（如record类、模式匹配等）提升代码可读性。"),

    h3("4.2.2 前端技术选型"),
    body("前端选用Vue3作为渐进式JavaScript框架，组合API（Composition API）提供了更灵活的组件逻辑组织能力；Vite作为构建工具，提供毫秒级热更新体验；Element Plus组件库满足了企业级管理界面的开发需求。"),

    h3("4.2.3 向量数据库选型"),
    body("向量数据库选用PGVector。PGVector作为PostgreSQL扩展，可复用现有数据库实例进行向量存储，无需引入独立的向量数据库服务。PGVector支持HNSW与IVFFlat两种索引算法，本系统采用HNSW索引以兼顾召回率与查询速度。向量维度配置为1024维，与通义千问Embedding模型text-embedding-v3的输出维度保持一致。"),

    h3("4.2.4 AI服务选型"),
    body("大语言模型与Embedding服务均调用阿里云Dashscope API。模型选用Qwen-Plus（通义千问Plus版）作为对话生成模型，text-embedding-v3作为文本向量化模型。Dashscope API采用HTTP协议调用，与Spring AI的OpenAI兼容接口无缝衔接。"),
    spacer(),
    body("Python AI服务选用FastAPI作为Web框架，lazy-loading模式加载模型以优化服务启动速度。图像特征提取采用CLIP（openai/clip-vit-base-patch32），输出512维向量；音频转写采用Whisper base模型；文本向量化采用Sentence-Transformers的all-MiniLM-L6-v2模型，输出384维向量。不同维度向量经PCA对齐至384维后，由Java端统一存储。"),

    h2("4.3 功能模块设计"),
    h3("4.3.1 模块划分"),
    body("根据需求分析，系统的功能模块划分如图4.2所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/论文配图/图4.2_系统功能模块图.png"),
    figCaption("图4.2 系统功能模块图"),
    spacer(),

    h3("4.3.2 用户认证模块"),
    body("用户认证模块（AuthModule）提供用户注册、登录、Token刷新功能。用户密码经BCrypt强哈希算法存储，认证成功后签发JWT Access Token（有效期1小时）与Refresh Token（有效期7天）。该模块基于Spring Security实现，JWT Filter拦截受保护资源请求，验证Token有效性并提取用户身份信息。"),

    h3("4.3.3 多模态知识接入模块"),
    body("多模态知识接入模块（KnowledgeModule）提供文档上传、解析、存储与生命周期管理功能。系统根据文件扩展名自动检测模态类型（TEXT/IMAGE/AUDIO/VIDEO），调用Python AI服务进行内容解析与向量化。文本分块策略为每800字符一块，块间重叠150字符。处理完成后，原始文件存于uploads目录，元数据存入MySQL，向量存入PGVector，文档状态更新为COMPLETED。"),

    h3("4.3.4 向量检索模块"),
    body("向量检索模块（VectorStoreModule）封装向量数据库操作，提供向量写入与语义检索功能。检索时按用户ID过滤私有文档，同时检索公开文档，合并去重后返回Top-K结果。该模块实现了重试机制（最多3次），应对向量数据库临时性连接故障。"),

    h3("4.3.5 RAG问答模块"),
    body("RAG问答模块（RagModule）实现检索增强生成的核心流程。chatWithSources方法执行完整的RAG链路：向量检索→上下文组装（上限6000字符）→Prompt注入→LLM生成→溯源标注。返回结果包含回答内容与引用来源列表，每个来源标注文档名称、片段摘要与相似度得分。"),

    h3("4.3.6 权限管理模块"),
    body("权限管理模块（SecurityModule）基于Spring Security与RBAC模型实现三级权限控制。角色分为ROLE_USER（普通会员）、ROLE_ADVANCED（高级会员）、ROLE_ADMIN（管理员）。普通会员享有基础文档管理与问答功能；高级会员享受更高检索配额与问答频率；管理员拥有用户管理、角色变更与系统监控权限。"),

    h3("4.3.7 知识增值模块"),
    body("知识增值模块（KnowledgeValueModule）提供知识分类标签、笔记管理、链接分享与知识推荐功能。笔记关联原始文档片段，支持知识复用；链接分享支持设置过期时间；推荐算法基于用户历史检索记录进行协同过滤。"),

    h2("4.4 数据库设计"),
    h3("4.4.1 MySQL业务数据库ER图"),
    body("MySQL业务数据库（knowledge_management）存储系统业务元数据，其ER图如图4.3所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/论文配图/图4.3_MySQL数据库ER图.png"),
    figCaption("图4.3 MySQL数据库ER图"),
    spacer(),

    h3("4.4.2 主要数据表结构"),
    body("MySQL数据库包含以下主要表结构："),
    spacer(),
    tableTitle("表4.1 users（用户表）"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "用户唯一标识"],
            ["username", "VARCHAR(50)", "UNIQUE, NOT NULL", "用户名"],
            ["email", "VARCHAR(100)", "UNIQUE, NOT NULL", "邮箱"],
            ["password_hash", "VARCHAR(255)", "NOT NULL", "密码哈希（BCrypt）"],
            ["full_name", "VARCHAR(100)", "NOT NULL", "真实姓名"],
            ["is_active", "TINYINT(1)", "DEFAULT 1", "是否激活"],
            ["created_at", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "创建时间"],
        ],
        [2000, 2400, 2700, 2254]
    ),
    spacer(),

    tableTitle("表4.2 multimodal_documents（文档表）"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "文档唯一标识"],
            ["user_id", "BIGINT", "FOREIGN KEY (users.id)", "上传用户ID"],
            ["file_name", "VARCHAR(255)", "NOT NULL", "原始文件名"],
            ["file_type", "VARCHAR(20)", "NOT NULL", "文件扩展名"],
            ["file_path", "VARCHAR(500)", "NOT NULL", "存储路径（本地或OSS）"],
            ["file_size", "BIGINT", "NOT NULL", "文件大小（字节）"],
            ["modality", "VARCHAR(20)", "NOT NULL", "模态类型：TEXT/IMAGE/AUDIO/VIDEO"],
            ["extracted_content", "TEXT", "NULL", "提取的文本内容（前5000字）"],
            ["tags", "VARCHAR(500)", "NULL", "标签，逗号分隔"],
            ["is_shared", "TINYINT(1)", "DEFAULT 0", "是否公开共享"],
            ["status", "VARCHAR(20)", "DEFAULT 'PROCESSING'", "处理状态"],
            ["upload_time", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "上传时间"],
        ],
        [2000, 2400, 2700, 2254]
    ),
    spacer(),

    tableTitle("表4.3 chat_sessions（会话表）"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "会话唯一标识"],
            ["user_id", "BIGINT", "FOREIGN KEY (users.id)", "所属用户ID"],
            ["title", "VARCHAR(200)", "NULL", "会话标题（取首条问题）"],
            ["created_at", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "创建时间"],
            ["updated_at", "DATETIME", "ON UPDATE CURRENT_TIMESTAMP", "最后更新时间"],
        ],
        [2000, 2400, 2700, 2254]
    ),
    spacer(),

    tableTitle("表4.4 chat_messages（消息表）"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "消息唯一标识"],
            ["session_id", "BIGINT", "FOREIGN KEY (chat_sessions.id)", "所属会话ID"],
            ["role", "VARCHAR(20)", "NOT NULL", "角色：USER / ASSISTANT"],
            ["content", "TEXT", "NOT NULL", "消息内容"],
            ["sources", "JSON", "NULL", "引用来源（JSON数组）"],
            ["created_at", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "创建时间"],
        ],
        [2000, 2400, 2700, 2254]
    ),
    spacer(),

    tableTitle("表4.5 knowledge_notes（笔记表）"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "笔记唯一标识"],
            ["document_id", "BIGINT", "FOREIGN KEY (multimodal_documents.id)", "关联文档ID"],
            ["user_id", "BIGINT", "FOREIGN KEY (users.id)", "创建用户ID"],
            ["title", "VARCHAR(200)", "NOT NULL", "笔记标题"],
            ["content", "TEXT", "NOT NULL", "笔记内容"],
            ["created_at", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "创建时间"],
        ],
        [2000, 2400, 2700, 2254]
    ),
    spacer(),

    tableTitle("表4.6 query_logs（查询日志表）"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "日志唯一标识"],
            ["user_id", "BIGINT", "FOREIGN KEY (users.id)", "查询用户ID"],
            ["query_text", "TEXT", "NOT NULL", "查询文本"],
            ["response_time_ms", "INT", "NOT NULL", "响应耗时（毫秒）"],
            ["documents_found", "INT", "DEFAULT 0", "检索到的文档数量"],
            ["created_at", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "查询时间"],
        ],
        [2000, 2400, 2700, 2254]
    ),
    spacer(),

    h3("4.4.3 PostgreSQL向量数据库"),
    body("PostgreSQL数据库（vector_db）通过PGVector扩展存储语义向量，其向量表结构如表4.7所示。"),
    spacer(),
    tableTitle("表4.7 vector_store（向量表）"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "向量记录唯一标识"],
            ["content", "TEXT", "NOT NULL", "文本内容片段"],
            ["metadata", "JSONB", "NOT NULL", "元数据（userId, documentId, fileType等）"],
            ["embedding", "VECTOR(1024)", "NOT NULL", "1024维语义向量（HNSW索引）"],
        ],
        [2000, 2400, 2700, 2254]
    ),
    spacer(),
    body("向量表采用HNSW索引，索引构建参数m=16、ef_construction=64，在检索速度与召回率之间取得较好平衡。metadata字段以JSONB格式存储，支持灵活的元数据过滤查询。"),

    h2("4.5 接口设计"),
    body("本系统提供RESTful风格的Web API，核心接口定义如下："),
    spacer(),
    tableTitle("表4.8 认证接口"),
    createThreeLineTable(
        ["接口", "方法", "路径", "说明"],
        [
            ["用户注册", "POST", "/api/auth/register", "注册新用户，返回JWT"],
            ["用户登录", "POST", "/api/auth/login", "登录验证，返回AccessToken与RefreshToken"],
            ["刷新Token", "POST", "/api/auth/refresh", "使用RefreshToken换取新AccessToken"],
        ],
        [1500, 1000, 3000, 3854]
    ),
    spacer(),

    tableTitle("表4.9 知识管理接口"),
    createThreeLineTable(
        ["接口", "方法", "路径", "说明"],
        [
            ["上传文档", "POST", "/api/knowledge/upload", "上传文件，自动模态检测与解析"],
            ["文档列表", "GET", "/api/knowledge/documents", "查询用户文档列表（分页）"],
            ["删除文档", "DELETE", "/api/knowledge/{id}", "删除文档及关联向量"],
            ["切换公开", "POST", "/api/knowledge/{id}/toggle-public", "切换文档公开/私有状态"],
            ["更新标签", "PUT", "/api/knowledge/{id}/tags", "更新文档标签"],
        ],
        [1500, 1000, 3000, 3854]
    ),
    spacer(),

    tableTitle("表4.10 RAG问答接口"),
    createThreeLineTable(
        ["接口", "方法", "路径", "说明"],
        [
            ["发送问题", "POST", "/api/chat", "发送问题，返回回答与来源"],
            ["会话列表", "GET", "/api/chat/sessions", "查询用户会话历史"],
            ["会话消息", "GET", "/api/chat/sessions/{id}/messages", "查询指定会话的消息记录"],
        ],
        [1500, 1000, 3000, 3854]
    ),
    spacer(),

    tableTitle("表4.11 管理接口"),
    createThreeLineTable(
        ["接口", "方法", "路径", "说明"],
        [
            ["用户列表", "GET", "/api/admin/users", "查询用户列表（分页）"],
            ["更新角色", "PUT", "/api/admin/users/{id}/role", "更新用户角色"],
            ["运营统计", "GET", "/api/admin/statistics", "系统运营数据统计"],
        ],
        [1500, 1000, 3000, 3854]
    ),
    spacer(),
    body("以上接口均需携带有效JWT Token（除注册、登录外），管理员接口需具备ADMIN角色权限。接口统一返回JSON格式，错误响应包含code和message字段。"),

    h2("4.6 本章小结"),
    body("本章完成了多模态知识管理系统的总体架构设计与详细设计。系统采用前后端分离架构，后端基于Spring Boot + Spring AI，前端基于Vue3 + Element Plus，AI能力由Python FastAPI服务提供。技术选型上，选用PGVector作为向量数据库，Dashscope API作为大模型与Embedding服务。功能模块划分为用户认证、多模态知识接入、向量检索、RAG问答、权限管理及知识增值六大模块。数据库设计涵盖MySQL业务库与PostgreSQL向量库，提供了完整的RESTful API接口规范。上述设计为后续章节的系统实现奠定了基础。"),
];

// ==================== 生成文档 ====================
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
    fs.writeFileSync("E:/答辩/论文/第1-4章-新版-含图片.docx", buffer);
    console.log("第1-4章重写版文档生成成功！");
});
