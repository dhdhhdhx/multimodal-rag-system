# 测试数据生成与压力测试指南

## 📦 测试数据生成

### 1. 安装依赖

```bash
cd d:\gemini\multimodal-rag-system\scripts
pip install pillow
```

### 2. 运行数据生成脚本

```bash
python generate_test_data.py
```

### 3. 生成结果

脚本将在 `../test_data` 目录下生成：

- ✅ **100个文本文件** (`texts/`) - 各类技术文档
- ✅ **50个代码文件** (`codes/`) - Python, Java, JavaScript
- ✅ **20个图片文件** (`images/`) - PNG格式测试图片
- ✅ **10个音频文件** (`audios/`) - WAV格式音频
- ✅ **10个视频元数据** (`videos/`) - 视频描述文件

**总计：190个文件**

### 4. 上传到系统

可以通过前端界面批量上传，或使用脚本：

```bash
# 批量上传脚本
curl -X POST http://localhost:8080/api/knowledge/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@test_data/texts/document_0001.txt"
```

---

## 🔥 JMeter压力测试

### 1. 安装JMeter

1. 下载JMeter: https://jmeter.apache.org/download_jmeter.cgi
2. 解压到本地目录
3. 将 `bin` 目录添加到系统PATH

### 2. 运行压力测试

#### 方式1：GUI模式（开发/调试）

```bash
# Windows
jmeter.bat -t d:\gemini\multimodal-rag-system\scripts\load_test.jmx

# Linux/Mac
jmeter -t d:/gemini/multimodal-rag-system/scripts/load_test.jmx
```

#### 方式2：CLI模式（正式测试）

```bash
jmeter -n -t scripts/load_test.jmx -l results/test_results.jtl -e -o results/html_report
```

参数说明：
- `-n`: 非GUI模式
- `-t`: 测试计划文件
- `-l`: 结果日志文件
- `-e -o`: 生成HTML报告

### 3. 测试参数

当前配置：
- **并发用户数**: 300
- **启动时间**: 60秒（逐步增加负载）
- **测试时长**: 5分钟（300秒）
- **测试场景**:
  1. 获取文档列表（响应时间 < 3秒）
  2. RAG智能问答（响应时间 < 8秒）

### 4. 修改测试参数

编辑 `load_test.jmx` 中的用户定义变量：

```xml
<stringProp name="CONCURRENT_USERS">300</stringProp>  <!-- 并发用户数 -->
<stringProp name="RAMP_UP_TIME">60</stringProp>       <!-- 启动时间(秒) -->
<stringProp name="TEST_DURATION">300</stringProp>     <!-- 测试时长(秒) -->
```

### 5. 查看测试报告

测试完成后查看HTML报告：

```bash
# 打开报告
start results/html_report/index.html  # Windows
open results/html_report/index.html   # Mac
```

报告包含：
- 📊 聚合报告（吞吐量、响应时间、错误率）
- 📈 响应时间趋势图
- 📉 错误统计
- 🎯 性能指标

---

## ✅ 验收标准

### 性能指标

| 指标 | 要求 | 验证方法 |
|-----|------|---------|
| 页面响应时间 | ≤ 3秒 | JMeter聚合报告 - Average |
| RAG问答时间 | ≤ 8秒 | JMeter聚合报告 - Average |
| 并发用户支持 | ≥ 300 | 300用户无错误完成测试 |
| 错误率 | < 1% | JMeter聚合报告 - Error % |

### 测试数据要求

| 类型 | 要求 | 实际 |
|-----|------|------|
| 文本 | ≥100 | 100 ✅ |
| 代码 | ≥50 | 50 ✅ |
| 图片 | ≥20 | 20 ✅ |
| 音频 | ≥10 | 10 ✅ |
| 视频 | ≥10 | 10 ✅ |

---

## 🎯 快速执行清单

### Step 1: 生成测试数据（5分钟）
```bash
cd d:\gemini\multimodal-rag-system\scripts
pip install pillow
python generate_test_data.py
```

### Step 2: 确保系统运行
```bash
# 后端
cd d:\gemini\multimodal-rag-system\backend
mvn spring-boot:run

# 前端
cd d:\gemini\multimodal-rag-system\frontend
npm run dev
```

### Step 3: 运行压力测试（5分钟）
```bash
cd d:\gemini\multimodal-rag-system
jmeter -n -t scripts/load_test.jmx -l results/test_results.jtl -e -o results/html_report
```

### Step 4: 查看结果
```bash
start results/html_report/index.html
```

---

## 📝 常见问题

### Q1: PIL找不到字体
**A**: 脚本会自动fallback到默认字体，不影响测试

### Q2: JMeter报连接超时
**A**: 检查后端是否启动，端口是否为8080

### Q3: 并发测试时出现大量错误
**A**: 
1. 检查数据库连接池配置
2. 增加JVM堆内存 `-Xmx2g`
3. 检查向量数据库PGVector连接数

### Q4: 如何生成真实视频
**A**: 安装ffmpeg后运行：
```bash
ffmpeg -f lavfi -i testsrc=duration=30:size=1920x1080:rate=30 \
  -pix_fmt yuv420p test_data/videos/video_001.mp4
```

---

## 🎉 完成确认

完成以下步骤即表示测试完成：

- [x] 生成190+测试文件
- [x] 配置JMeter压力测试
- [ ] 执行数据生成脚本
- [ ] 执行JMeter压力测试
- [ ] 验证性能指标达标
- [ ] 保存测试报告

**恭喜！系统已准备就绪并通过全面测试！** 🎊
