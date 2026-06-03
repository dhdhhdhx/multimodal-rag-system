#!/bin/bash
# ============================================================
# 信息安全 - 加密算法演示脚本
# 软件设计师考试 - 信息安全知识点
# 
# 本脚本使用OpenSSL演示常见加密算法，
# 涵盖对称加密、非对称加密、哈希算法、数字签名等。
# 
# 运行环境：Linux / macOS（需要安装OpenSSL）
# 关联资料：详见《信息安全_加密算法总结.txt》
# ============================================================

echo "============================================================"
echo "   软件设计师考试 - 信息安全加密算法演示"
echo "============================================================"
echo ""

# ==================== 一、对称加密算法 ====================
echo "===== 一、对称加密算法 ====="
echo "特点：加密和解密使用相同的密钥"
echo "常见算法：DES、3DES、AES、RC4、Blowfish"
echo ""

# AES加密（考试重点）
# AES密钥长度：128位、192位、256位
echo "[1] AES-256-CBC 加密演示"
echo "明文: 软件设计师考试-信息安全重点"
echo -n "软件设计师考试-信息安全重点" > plaintext.txt

# 使用AES-256-CBC加密
openssl enc -aes-256-cbc -salt -in plaintext.txt -out aes_encrypted.bin -pass pass:exam2024 -pbkdf2
echo "加密完成: aes_encrypted.bin"

# 解密验证
openssl enc -aes-256-cbc -d -in aes_encrypted.bin -out aes_decrypted.txt -pass pass:exam2024 -pbkdf2
echo "解密结果: $(cat aes_decrypted.txt)"
echo ""

# DES加密（了解即可，已被AES替代）
echo "[2] DES 加密演示（已淘汰，仅供参考）"
openssl enc -des -in plaintext.txt -out des_encrypted.bin -pass pass:exam2024 -pbkdf2
echo "DES加密完成（密钥仅56位，不安全）"
echo ""

# ==================== 二、非对称加密算法 ====================
echo "===== 二、非对称加密算法 ====="
echo "特点：使用公钥加密、私钥解密（或反之）"
echo "常见算法：RSA、DSA、ECC"
echo ""

# RSA密钥对生成
echo "[3] RSA 密钥对生成"
echo "生成2048位RSA私钥..."
openssl genrsa -out private_key.pem 2048 2>/dev/null
echo "私钥: private_key.pem"

# 从私钥提取公钥
openssl rsa -in private_key.pem -pubout -out public_key.pem 2>/dev/null
echo "公钥: public_key.pem"
echo ""

# RSA加密与解密
echo "[4] RSA 加密/解密演示"
echo "明文: 软件设计师2024"
echo -n "软件设计师2024" > rsa_plain.txt

# 使用公钥加密
openssl rsautl -encrypt -inkey public_key.pem -pubin -in rsa_plain.txt -out rsa_encrypted.bin
echo "公钥加密完成: rsa_encrypted.bin"

# 使用私钥解密
openssl rsautl -decrypt -inkey private_key.pem -in rsa_encrypted.bin -out rsa_decrypted.txt
echo "私钥解密结果: $(cat rsa_decrypted.txt)"
echo ""

# ==================== 三、哈希算法（摘要算法） ====================
echo "===== 三、哈希/摘要算法 ====="
echo "特点：单向不可逆，固定长度输出"
echo "常见算法：MD5、SHA-1、SHA-256、SHA-512"
echo ""

echo "[5] 各种哈希算法对比"
echo -n "软件设计师考试" | openssl md5
echo "MD5输出: 128位（32个十六进制字符）- 已不安全"

echo -n "软件设计师考试" | openssl sha1
echo "SHA-1输出: 160位（40个十六进制字符）- 已不安全"

echo -n "软件设计师考试" | openssl sha256
echo "SHA-256输出: 256位（64个十六进制字符）- 推荐"

echo -n "软件设计师考试" | openssl sha512
echo "SHA-512输出: 512位（128个十六进制字符）- 更安全"
echo ""

# ==================== 四、数字签名 ====================
echo "===== 四、数字签名 ====="
echo "流程：发送方用私钥签名 → 接收方用公钥验证"
echo "用途：验证数据完整性和发送方身份"
echo ""

echo "[6] 数字签名演示"
# 创建待签名文件
echo "这是软件设计师考试的机密信息" > sign_doc.txt

# 用私钥生成签名
openssl dgst -sha256 -sign private_key.pem -out signature.bin sign_doc.txt
echo "签名文件: signature.bin"

# 用公钥验证签名
openssl dgst -sha256 -verify public_key.pem -signature signature.bin sign_doc.txt
echo ""

# ==================== 五、数字证书 ====================
echo "===== 五、数字证书与PKI ====="
echo ""
echo "PKI（公钥基础设施）组成："
echo "  - CA（证书颁发机构）"
echo "  - RA（注册机构）"
echo "  - 证书库"
echo "  - 密钥管理"
echo ""
echo "证书格式：X.509"
echo "证书内容：版本号、序列号、签名算法、颁发者、有效期、主体、公钥信息"
echo ""

# ==================== 六、清理临时文件 ====================
echo "[7] 清理临时文件..."
rm -f plaintext.txt aes_encrypted.bin aes_decrypted.txt
rm -f des_encrypted.bin private_key.pem public_key.pem
rm -f rsa_plain.txt rsa_encrypted.bin rsa_decrypted.txt
rm -f sign_doc.txt signature.bin
echo "清理完成"
echo ""

echo "============================================================"
echo "   考试重点总结"
echo "============================================================"
echo ""
echo "1. 对称加密 vs 非对称加密："
echo "   - 对称加密：速度快，密钥分发困难（AES推荐）"
echo "   - 非对称加密：速度慢，密钥管理方便（RSA常用）"
echo "   - 实际应用：HTTPS使用非对称加密交换对称密钥，再用对称加密传输数据"
echo ""
echo "2. 哈希算法特性："
echo "   - 单向性：无法从摘要恢复原文"
echo "   - 抗碰撞性：找不到两个不同的输入产生相同摘要"
echo "   - 雪崩效应：输入微小变化导致输出巨大变化"
echo ""
echo "3. 数字签名与加密的区别："
echo "   - 数字签名：私钥签名，公钥验证 → 身份认证和完整性"
echo "   - 加密传输：公钥加密，私钥解密 → 保密性"
echo ""
echo "4. 网络安全协议层次："
echo "   - SSL/TLS：传输层安全"
echo "   - HTTPS = HTTP + SSL/TLS"
echo "   - IPSec：网络层安全"
echo "   - PGP：应用层安全（邮件）"
echo ""
echo "5. 常见攻击类型："
echo "   - 中间人攻击、重放攻击、DDoS攻击、SQL注入、XSS"
echo "   - 防御：加密通信、数字签名、防火墙、入侵检测系统"
echo ""
echo "============================================================"
