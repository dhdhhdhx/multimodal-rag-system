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

    body("本章在前文详细设计的基础上，介绍系统的具体实现过程。首先说明系统的开发环境与工具配置，然后重点阐述前台用户功能和后台管理功能的实现细节。"),

    spacer(),
    h2("6.1 系统开发环境与工具"),

    h3("6.1.1 硬件与软件环境"),

    body("系统开发及运行所使用的硬件和软件环境如下表所示。开发工作站采用普通个人计算机即可满足开发调试需求，服务器部署时可根据用户规模选择更高配置。"),

    spacer(),
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
    body("在开发工具与平台的选择上，综合考虑了技术成熟度、社区生态、性能表现及团队实际条件等因素。Intel Core i7处理器与16GB内存的组合能够保障本地开发调试的流畅性，在运行IDE、数据库容器、前端构建服务等多项任务时依然能够保持稳定响应。操作系统层面，Windows 11作为日常开发环境提供了良好的兼容性，Ubuntu 22.04则用于服务器部署，其稳定性和Docker生态更为完善。JDK 17是Spring Boot 3.x推荐的LTS版本，在生产环境中经过广泛验证；Python 3.11在AI模型推理场景下拥有丰富的库生态支持，FastAPI的异步特性也适合高并发AI服务场景。Node.js v18+则确保了Vue 3和Vite构建工具的完整功能支持。IDE选择方面，IntelliJ IDEA对Java生态的深度支持、PyCharm对Python开发的优化、以及VSCode对前端和配置文件的高效编辑能力，形成了互补的工具链组合。"),

    spacer(),
    h3("6.1.2 核心技术栈"),

    body("系统采用前后端分离架构，后端基于Spring Boot框架实现业务逻辑，Python微服务负责多模态embedding生成，前端基于Vue.js构建用户交互界面。核心技术栈如下表所示。"),

    spacer(),
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
    h2("6.2 前台功能实现"),

    h3("6.2.1 用户注册与认证模块实现"),

    body("系统提供简洁直观的注册登录入口，界面采用卡片式设计，视觉层次分明美观。用户输入用户名、邮箱和密码即可完成注册，系统通过BCrypt算法对密码进行加密保护。登录时系统验证凭证后生成JWT令牌，实现无状态身份认证。"),

    body("登录成功后，系统根据用户角色动态展示个性化导航菜单。普通用户显示文档管理、话题中心、AI问答等入口；高级用户额外享有话题创建权限。界面整体采用渐变色导航栏设计，视觉效果简洁大气。用户注册登录界面如图6.1所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.1_用户注册登录界面.png"),
    figCaption("图6.1 用户注册登录界面"),
    spacer(),

    h3("6.2.2 文档管理模块实现"),

    body("文档管理界面采用左树右表的经典布局设计，左侧为知识分类树，右侧为文档列表，这种布局让用户能够快速定位和管理自己的知识资产。文档列表支持分页展示，每条记录清晰展示文件名、模态类型、标签、上传时间等信息。"),

    body("列表顶部集成多条件筛选器，用户可按模态类型、时间范围等条件快速过滤文档。文件上传采用拖拽交互设计，支持多格式文件上传，上传过程实时显示处理进度。系统支持PDF、Word、图片、音频、视频等多种格式，兼容性良好。文档管理界面和文档上传界面分别如图6.2和图6.3所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.2_文档管理界面.png"),
    figCaption("图6.2 文档管理界面"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.3_文档上传界面.png"),
    figCaption("图6.3 文档上传界面"),
    spacer(),

    h3("6.2.3 话题系统模块实现"),

    body("话题列表页面采用卡片流布局，每张话题卡片展示话题名称、描述摘要、订阅人数等信息。卡片设计采用圆角边框配合柔和阴影，视觉效果现代轻盈，能够吸引用户浏览和探索。"),

    body("系统基于用户兴趣标签进行智能推荐，将用户可能感兴趣的话题优先展示在推荐区域。话题详情页面采用双栏布局，左侧展示话题完整信息和关联文档列表，右侧提供订阅入口。话题列表界面和话题详情界面分别如图6.4和图6.5所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.4_话题列表界面.png"),
    figCaption("图6.4 话题列表界面"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.5_话题详情界面.png"),
    figCaption("图6.5 话题详情界面"),
    spacer(),

    h3("6.2.4 智能问答模块实现"),

    body("智能问答界面采用三栏布局，左侧为会话列表，中间为对话主区域，右侧为文档快速跳转入口。这种布局让用户在对话过程中可以随时查阅相关文档，实现边问边看的沉浸式体验。"),

    body("对话区域采用聊天气泡设计，用户消息和AI回复用不同颜色区分，视觉上清晰直观。AI回复支持Markdown渲染，包含代码高亮、表格展示等专业格式。回答下方展示来源引用卡片，每张卡片包含文件名、相关度得分和内容摘录，相关度采用进度条可视化展示。系统支持多轮对话，用户可在同一会话中追问，AI能够理解上下文语境给出连贯回答。智能问答界面如图6.6所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.6_智能问答界面.png"),
    figCaption("图6.6 智能问答界面"),
    spacer(),

    h2("6.3 后台功能实现"),

    h3("6.3.1 用户管理模块实现"),

    body("管理员后台采用仪表盘式布局，顶部为统计卡片区域，下方为数据表格区域。统计卡片实时展示用户总数、活跃用户数、禁用用户数等核心指标，数字大字号突出显示，便于管理员快速掌握系统用户概况。"),

    body("用户管理表格集成分页、排序、搜索等功能，管理员可按用户名或邮箱关键词快速定位目标用户。每行记录提供启用/禁用开关和角色变更下拉框，操作便捷直观。用户管理界面如图6.7所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.7_用户管理界面.png"),
    figCaption("图6.7 用户管理界面"),
    spacer(),

    h3("6.3.2 文档管理模块实现"),

    body("文档管理页面展示所有用户上传的文档记录，表格列包含文档名称、上传者、模态类型、处理状态、标签等信息。管理员可对违规文档执行删除操作，支持单选和批量选择，批量删除需二次确认防止误操作。"),

    body("标签修改功能集成在表格行内，管理员可直接点击标签进行编辑，无需打开新页面。这种行内编辑设计提升了操作效率，减少了页面跳转。文档管理界面如图6.8所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.8_文档管理界面.png"),
    figCaption("图6.8 文档管理界面"),
    spacer(),

    h3("6.3.3 标签管理模块实现"),

    body("标签管理页面采用表格展示系统全量标签，每条标签显示名称和使用次数统计。管理员可对低频或重复标签执行重命名和删除操作，保持标签库的规范整洁。标签管理界面如图6.9所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.9_标签管理界面.png"),
    figCaption("图6.9 标签管理界面"),
    spacer(),

    h3("6.3.4 话题管理模块实现"),

    body("话题管理页面展示系统所有公开和私有话题，管理员可查看话题的创建者、订阅人数、公开状态等信息。管理员可切换话题的公开状态，控制其在用户端的可见性，状态变更即时生效，操作响应迅速。话题管理界面如图6.10所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.10_话题管理界面.png"),
    figCaption("图6.10 话题管理界面"),
    spacer(),

    h3("6.3.5 数据统计模块实现"),

    body("数据统计页面集成统计卡片和可视化图表，直观展示系统运营状况。顶部统计卡片展示用户总数、文档总数、AI查询次数、访问总量等核心KPI。折线图展示本周查询趋势，柱状图展示用户活跃度Top榜，饼图展示文档模态分布。管理员可通过日期选择器自定义时间范围，查看不同周期的数据变化。数据统计界面如图6.11所示。"),
    spacer(),
    createImagePara("E:/答辩/论文/图6.11_数据统计界面.png"),
    figCaption("图6.11 数据统计界面"),
    spacer(),

    h2("6.4 本章小结"),

    body("本章详细介绍了系统的具体实现过程。首先说明了系统的开发环境配置，包括硬件软件环境、核心技术栈、Docker环境搭建步骤和项目组织结构。然后分别阐述了前台用户功能（用户注册认证、文档管理、话题系统、智能问答）和后台管理功能（用户管理、文档管理、标签管理、话题管理、数据统计）的实现细节。每个模块均通过界面截图展示了实现效果，为系统测试和部署奠定了基础。"),
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
    fs.writeFileSync("E:/答辩/论文/第6章_系统实现_单独_v2.docx", buffer);
    console.log("已生成：E:/答辩/论文/第6章_系统实现_单独_v2.docx");
}).catch(err => {
    console.error(err);
});