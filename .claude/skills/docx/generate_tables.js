const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, PageBreak } = require('docx');
const fs = require('fs');

// A4页面参数 (DXA单位)
// 2.5cm = 1418 DXA (1cm ≈ 567 DXA)
const PAGE_WIDTH = 11906;
const PAGE_HEIGHT = 16838;
const MARGIN = 1418;
const CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2; // 9070

// 三线表边框：1.5磅=12, 0.75磅=6
const BORDER_1_5 = { style: BorderStyle.SINGLE, size: 12, color: "000000" };
const BORDER_0_75 = { style: BorderStyle.SINGLE, size: 6, color: "000000" };
const BORDER_NONE = { style: BorderStyle.NONE };

// 表头边框：顶1.5磅，底0.75磅
const headerBorders = {
    top: BORDER_1_5, bottom: BORDER_0_75,
    left: BORDER_NONE, right: BORDER_NONE
};
// 数据行边框：无
const dataBorders = {
    top: BORDER_NONE, bottom: BORDER_NONE,
    left: BORDER_NONE, right: BORDER_NONE
};
// 末行边框：底1.5磅
const lastRowBorders = {
    top: BORDER_NONE, bottom: BORDER_1_5,
    left: BORDER_NONE, right: BORDER_NONE
};
// 表级别边框：无（由单元格控制）
const tableBorders = {
    top: BORDER_NONE, bottom: BORDER_NONE,
    left: BORDER_NONE, right: BORDER_NONE,
    insideHorizontal: BORDER_NONE, insideVertical: BORDER_NONE
};

// 五号字体 = 21 half-points
const FONT_SIZE_FIG = 21;
const FONT_SIZE_BODY = 21;

// 创建三线表
function createThreeLineTable(headers, rows, widths) {
    // 计算列宽总和
    const totalWidth = widths.reduce((a, b) => a + b, 0);

    // 表头行
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

    // 数据行
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
                        children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_BODY })]
                    })]
                })
            )
        })
    );

    // 末行
    const lastRowCells = rows[rows.length - 1].map((cell, i) =>
        new TableCell({
            borders: lastRowBorders,
            width: { size: widths[i], type: WidthType.DXA },
            margins: { top: 40, bottom: 40, left: 100, right: 100 },
            children: [new Paragraph({
                alignment: AlignmentType.LEFT,
                spacing: { line: 240, lineRule: "exact" },
                children: [new TextRun({ text: cell, font: "宋体", size: FONT_SIZE_BODY })]
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

// 表题：黑体五号居中
function tableCaption(num, name) {
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 200, after: 100, line: 240, lineRule: "exact" },
        children: [new TextRun({ text: `表${num} ${name}`, font: "黑体", size: FONT_SIZE_FIG })]
    });
}

// 空行
function spacer() {
    return new Paragraph({ spacing: { line: 200 } });
}

// 换页
function pageBreak() {
    return new Paragraph({ children: [new PageBreak()] });
}

// ============ 表格数据 ============

const tableNum = "4.1";
const children = [
    tableCaption(tableNum, "向量数据库对比"),
    createThreeLineTable(
        ["数据库", "类型", "索引算法", "优点", "适用场景"],
        [
            ["Milvus", "分布式向量数据库", "HNSW/IVF/PQ", "性能强，扩展性好", "大规模生产环境"],
            ["PGVector", "PostgreSQL扩展", "HNSW/IVFFlat", "SQL兼容，运维简单", "中小规模，混合检索"],
            ["Chroma", "嵌入式向量数据库", "HNSW", "轻量易用", "快速原型开发"],
            ["Weaviate", "分布式向量搜索引擎", "HNSW", "混合检索强", "多模态检索"]
        ],
        [1500, 1800, 1500, 1800, 2470]
    ),
    spacer(),

    tableCaption("4.2", "系统用户角色权限定义"),
    createThreeLineTable(
        ["角色", "权限描述"],
        [
            ["普通会员", "上传与管理个人知识文档、进行语义检索与RAG问答、查看公开知识库、使用笔记与分享功能"],
            ["高级会员", "享受更高的检索配额与问答频率、优先使用知识推荐功能、创建知识专题"],
            ["管理员", "用户管理与权限配置、系统运营数据分析、全局知识库监督与审核"]
        ],
        [1500, 7570]
    ),
    spacer(),

    tableCaption("4.3", "系统性能指标要求"),
    createThreeLineTable(
        ["指标", "要求"],
        [
            ["系统并发用户数", "≥300用户同时在线"],
            ["页面响应时间", "≤3秒（检索、文档列表等页面操作）"],
            ["智能问答生成时间", "≤8秒（首字输出延迟）"],
            ["向量检索召回率", "Top-K相关性文档召回准确率≥85%"],
            ["多模态解析吞吐量", "单个PDF文档（50页）解析≤10秒"]
        ],
        [3000, 6070]
    ),
    spacer(),

    tableCaption("4.4", "用户表(users)"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "用户唯一标识"],
            ["username", "VARCHAR(50)", "UNIQUE, NOT NULL", "用户名"],
            ["email", "VARCHAR(100)", "UNIQUE, NOT NULL", "邮箱"],
            ["password_hash", "VARCHAR(255)", "NOT NULL", "密码哈希（BCrypt）"],
            ["full_name", "VARCHAR(100)", "NOT NULL", "真实姓名"],
            ["is_active", "TINYINT(1)", "DEFAULT 1", "是否激活"],
            ["created_at", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "创建时间"]
        ],
        [2000, 2000, 2500, 2570]
    ),
    spacer(),

    tableCaption("4.5", "文档表(multimodal_documents)"),
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
            ["upload_time", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "上传时间"]
        ],
        [2000, 2000, 2500, 2570]
    ),
    spacer(),

    tableCaption("4.6", "会话表(chat_sessions)"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "会话唯一标识"],
            ["user_id", "BIGINT", "FOREIGN KEY (users.id)", "所属用户ID"],
            ["title", "VARCHAR(200)", "NULL", "会话标题（取首条问题）"],
            ["created_at", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "创建时间"],
            ["updated_at", "DATETIME", "ON UPDATE CURRENT_TIMESTAMP", "最后更新时间"]
        ],
        [2000, 2000, 2500, 2570]
    ),
    spacer(),

    tableCaption("4.7", "消息表(chat_messages)"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "消息唯一标识"],
            ["session_id", "BIGINT", "FOREIGN KEY (chat_sessions.id)", "所属会话ID"],
            ["role", "VARCHAR(20)", "NOT NULL", "角色：USER / ASSISTANT"],
            ["content", "TEXT", "NOT NULL", "消息内容"],
            ["sources", "JSON", "NULL", "引用来源（JSON数组）"],
            ["created_at", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "创建时间"]
        ],
        [2000, 2000, 2500, 2570]
    ),
    spacer(),

    tableCaption("4.8", "笔记表(notes)"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "笔记唯一标识"],
            ["document_id", "BIGINT", "FOREIGN KEY (multimodal_documents.id)", "关联文档ID"],
            ["user_id", "BIGINT", "FOREIGN KEY (users.id)", "创建用户ID"],
            ["title", "VARCHAR(200)", "NOT NULL", "笔记标题"],
            ["content", "TEXT", "NOT NULL", "笔记内容"],
            ["created_at", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "创建时间"]
        ],
        [2000, 2000, 2500, 2570]
    ),
    spacer(),

    tableCaption("4.9", "查询日志表(query_logs)"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "日志唯一标识"],
            ["user_id", "BIGINT", "FOREIGN KEY (users.id)", "查询用户ID"],
            ["query_text", "TEXT", "NOT NULL", "查询文本"],
            ["response_time_ms", "INT", "NOT NULL", "响应耗时（毫秒）"],
            ["documents_found", "INT", "DEFAULT 0", "检索到的文档数量"],
            ["created_at", "DATETIME", "DEFAULT CURRENT_TIMESTAMP", "查询时间"]
        ],
        [2000, 2000, 2500, 2570]
    ),
    spacer(),

    tableCaption("4.10", "向量表(embeddings)"),
    createThreeLineTable(
        ["字段名", "数据类型", "约束", "说明"],
        [
            ["id", "BIGINT", "PRIMARY KEY, AUTO_INCREMENT", "向量记录唯一标识"],
            ["content", "TEXT", "NOT NULL", "文本内容片段"],
            ["metadata", "JSONB", "NOT NULL", "元数据（userId, documentId, fileType等）"],
            ["embedding", "VECTOR(1024)", "NOT NULL", "1024维语义向量（HNSW索引）"]
        ],
        [2000, 2000, 2500, 2570]
    ),
    spacer(),

    tableCaption("4.11", "认证接口API"),
    createThreeLineTable(
        ["接口", "方法", "路径", "说明"],
        [
            ["用户注册", "POST", "/api/auth/register", "注册新用户，返回JWT"],
            ["用户登录", "POST", "/api/auth/login", "登录验证，返回AccessToken与RefreshToken"],
            ["刷新Token", "POST", "/api/auth/refresh", "使用RefreshToken换取新AccessToken"]
        ],
        [1500, 1000, 2500, 4070]
    ),
    spacer(),

    tableCaption("4.12", "知识库接口API"),
    createThreeLineTable(
        ["接口", "方法", "路径", "说明"],
        [
            ["上传文档", "POST", "/api/knowledge/upload", "上传文件，自动模态检测与解析"],
            ["文档列表", "GET", "/api/knowledge/documents", "查询用户文档列表（分页）"],
            ["删除文档", "DELETE", "/api/knowledge/{id}", "删除文档及关联向量"],
            ["切换公开", "POST", "/api/knowledge/{id}/toggle-public", "切换文档公开/私有状态"],
            ["更新标签", "PUT", "/api/knowledge/{id}/tags", "更新文档标签"]
        ],
        [1500, 1000, 2500, 4070]
    ),
    spacer(),

    tableCaption("4.13", "聊天接口API"),
    createThreeLineTable(
        ["接口", "方法", "路径", "说明"],
        [
            ["发送问题", "POST", "/api/chat", "发送问题，返回回答与来源"],
            ["会话列表", "GET", "/api/chat/sessions", "查询用户会话历史"],
            ["会话消息", "GET", "/api/chat/sessions/{id}/messages", "查询指定会话的消息记录"]
        ],
        [1500, 1000, 2500, 4070]
    ),
    spacer(),

    tableCaption("4.14", "管理员接口API"),
    createThreeLineTable(
        ["接口", "方法", "路径", "说明"],
        [
            ["用户列表", "GET", "/api/admin/users", "查询用户列表（分页）"],
            ["更新角色", "PUT", "/api/admin/users/{id}/role", "更新用户角色"],
            ["运营统计", "GET", "/api/admin/statistics", "系统运营数据统计"]
        ],
        [1500, 1000, 2500, 4070]
    ),
];

// 创建文档
const doc = new Document({
    sections: [{
        properties: {
            page: {
                size: { width: PAGE_WIDTH, height: PAGE_HEIGHT },
                margin: { top: MARGIN, right: MARGIN, bottom: MARGIN, left: MARGIN }
            }
        },
        children: children
    }]
});

// 写入文件
Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("E:/答辩/论文/三线表汇总.docx", buffer);
    console.log("Done: E:/答辩/论文/三线表汇总.docx");
}).catch(err => {
    console.error(err);
});