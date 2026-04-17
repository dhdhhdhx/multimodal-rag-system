import requests
import os
import random
import time

BASE_URL = "http://localhost:8080/api/knowledge/upload"
TEMP_DIR = "temp_seeding_v2"

if not os.path.exists(TEMP_DIR):
    os.makedirs(TEMP_DIR)

def upload_file(file_path):
    with open(file_path, 'rb') as f:
        files = {'file': f}
        try:
            response = requests.post(BASE_URL, files=files)
            if response.status_code == 200:
                print(f"✓ Successfully uploaded: {os.path.basename(file_path)}")
            else:
                print(f"✗ Failed to upload {os.path.basename(file_path)}: {response.status_code}")
        except Exception as e:
            print(f"✗ Error uploading {os.path.basename(file_path)}: {e}")

# 高质量中英文混合内容模板
BILINGUAL_CONTENT = {
    "Quantum Physics": """# 量子物理学 / Quantum Physics

## 核心概念 / Core Concepts

### 1. 波粒二象性 / Wave-Particle Duality
量子物理学的基本原理之一是波粒二象性，即微观粒子既具有波动性又具有粒子性。这一概念最早由德布罗意提出。

One of the fundamental principles of quantum physics is wave-particle duality, which states that microscopic particles exhibit both wave-like and particle-like properties. This concept was first proposed by Louis de Broglie.

### 2. 测不准原理 / Uncertainty Principle
海森堡测不准原理指出，我们无法同时精确测量粒子的位置和动量。这不是测量技术的限制，而是自然界的基本属性。

Heisenberg's Uncertainty Principle states that we cannot simultaneously measure a particle's position and momentum with perfect accuracy. This is not a limitation of measurement technology, but a fundamental property of nature.

### 3. 量子叠加 / Quantum Superposition
量子叠加态描述了量子系统可以同时处于多个状态的能力，直到被观测时才坍缩到某一确定状态。薛定谔的猫是这一概念的著名思想实验。

Quantum superposition describes the ability of quantum systems to exist in multiple states simultaneously until observed, at which point they collapse into a definite state. Schrödinger's cat is a famous thought experiment illustrating this concept.

### 4. 量子纠缠 / Quantum Entanglement
量子纠缠是指两个或多个粒子之间存在的一种特殊关联，即使它们相距遥远，对其中一个粒子的测量也会瞬间影响另一个粒子的状态。

Quantum entanglement refers to a special correlation between two or more particles, where measuring one particle instantaneously affects the state of another, even when they are far apart.

## 应用 / Applications
量子物理学的应用包括量子计算、量子通信、量子传感器等前沿技术领域。

Applications of quantum physics include quantum computing, quantum communication, quantum sensors, and other cutting-edge technological fields.
""",
    
    "AI and Machine Learning": """# 人工智能与机器学习 / AI and Machine Learning

## 定义 / Definition
人工智能是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。

Artificial Intelligence is a branch of computer science dedicated to creating systems capable of performing tasks that typically require human intelligence.

## 机器学习类型 / Types of Machine Learning

### 1. 监督学习 / Supervised Learning
监督学习使用标记数据训练模型，通过输入-输出对学习映射关系。常见应用包括图像分类、语音识别等。

Supervised learning trains models using labeled data, learning the mapping between inputs and outputs. Common applications include image classification and speech recognition.

### 2. 无监督学习 / Unsupervised Learning
无监督学习从未标记的数据中发现模式和结构，如聚类分析和降维。

Unsupervised learning discovers patterns and structures in unlabeled data, such as clustering and dimensionality reduction.

### 3. 强化学习 / Reinforcement Learning
强化学习通过与环境交互并接收奖励信号来学习最优策略，广泛应用于游戏AI和机器人控制。

Reinforcement learning learns optimal strategies by interacting with an environment and receiving reward signals, widely used in game AI and robotic control.

## 深度学习 / Deep Learning
深度学习是机器学习的子领域，使用多层神经网络处理复杂数据。卷积神经网络（CNN）和循环神经网络（RNN）是两种重要架构。

Deep learning is a subfield of machine learning that uses multi-layer neural networks to process complex data. Convolutional Neural Networks (CNN) and Recurrent Neural Networks (RNN) are two important architectures.
""",

    "Space Exploration": """# 太空探索 / Space Exploration

## 历史里程碑 / Historical Milestones

### 1957: 人造卫星时代 / The Satellite Era
苏联发射的斯普特尼克1号开启了太空时代，这是人类首颗人造地球卫星。

The Soviet Union's launch of Sputnik 1 marked the beginning of the Space Age, being humanity's first artificial Earth satellite.

### 1969: 登月 / Moon Landing
阿波罗11号任务实现了人类首次登月，尼尔·阿姆斯特朗成为第一个踏上月球的人类。

The Apollo 11 mission achieved humanity's first moon landing, with Neil Armstrong becoming the first human to walk on the Moon.

### 现代探索 / Modern Exploration
当代太空探索包括火星探测器、国际空间站、詹姆斯·韦伯太空望远镜等重大项目。

Contemporary space exploration includes major projects such as Mars rovers, the International Space Station, and the James Webb Space Telescope.

## 未来展望 / Future Prospects
人类计划建立月球基地、载人登陆火星，并继续探索太阳系外行星。私营航天公司如SpaceX和Blue Origin正在推动商业太空旅行。

Humanity plans to establish lunar bases, conduct manned Mars landings, and continue exploring exoplanets. Private space companies like SpaceX and Blue Origin are driving commercial space travel.
""",

    "Climate Change": """# 气候变化 / Climate Change

## 原因 / Causes
气候变化主要由温室气体排放增加引起，包括二氧化碳、甲烷和氧化亚氮。人类活动是主要驱动因素。

Climate change is primarily caused by increased greenhouse gas emissions, including carbon dioxide, methane, and nitrous oxide. Human activities are the main driver.

## 影响 / Impacts

### 全球变暖 / Global Warming
全球平均温度持续上升，导致极地冰川融化、海平面上升和极端天气事件频发。

Global average temperatures continue to rise, leading to melting polar ice, rising sea levels, and increased frequency of extreme weather events.

### 生态系统破坏 / Ecosystem Disruption
气候变化威胁生物多样性，许多物种面临栖息地丧失和灭绝风险。

Climate change threatens biodiversity, with many species facing habitat loss and extinction risks.

## 解决方案 / Solutions
应对气候变化需要减少碳排放、发展可再生能源、保护森林和推广可持续发展。

Addressing climate change requires reducing carbon emissions, developing renewable energy, protecting forests, and promoting sustainable development.
""",

    "Blockchain Technology": """# 区块链技术 / Blockchain Technology

## 基本原理 / Basic Principles
区块链是一种分布式账本技术，通过密码学方法确保数据的安全性和不可篡改性。

Blockchain is a distributed ledger technology that ensures data security and immutability through cryptographic methods.

## 核心特征 / Core Features

### 去中心化 / Decentralization
区块链网络不依赖单一中心机构，由多个节点共同维护，提高了系统的安全性和可靠性。

Blockchain networks do not rely on a single central authority but are maintained by multiple nodes, enhancing system security and reliability.

### 透明性 / Transparency
所有交易记录对网络参与者可见，但通过加密保护用户隐私。

All transaction records are visible to network participants, while encryption protects user privacy.

### 共识机制 / Consensus Mechanisms
常见的共识机制包括工作量证明（PoW）、权益证明（PoS）和实用拜占庭容错（PBFT）。

Common consensus mechanisms include Proof of Work (PoW), Proof of Stake (PoS), and Practical Byzantine Fault Tolerance (PBFT).

## 应用领域 / Application Areas
区块链技术应用于加密货币、供应链管理、智能合约、数字身份验证等多个领域。

Blockchain technology is applied in areas such as cryptocurrency, supply chain management, smart contracts, and digital identity verification.
"""
}

def gen_bilingual_text(count):
    """生成高质量中英文混合文本文档"""
    topics = list(BILINGUAL_CONTENT.keys())
    additional_topics = ["Neural Networks", "Renewable Energy", "Genomics", "Cybersecurity"]
    
    for i in range(count):
        if i < len(BILINGUAL_CONTENT):
            topic = topics[i % len(topics)]
            content = BILINGUAL_CONTENT[topic]
        else:
            # 为额外文档生成变化的内容
            topic = random.choice(topics + additional_topics)
            if topic in BILINGUAL_CONTENT:
                content = BILINGUAL_CONTENT[topic]
                content += f"\n\n## 补充说明 {i+1} / Additional Note {i+1}\n这是第{i+1}个文档的扩展内容。\nThis is extended content for document {i+1}."
            else:
                content = f"# {topic}\n\n## 概述 / Overview\n这是关于{topic}的文档 {i+1}。\nThis is document {i+1} about {topic}.\n\n"
                content += "详细内容将在后续版本中补充。\nDetailed content will be added in future versions."
        
        file_path = os.path.join(TEMP_DIR, f"bilingual_{i+1:03d}_{topic.replace(' ', '_')}.txt")
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        upload_file(file_path)
        time.sleep(0.15)

def gen_code_with_comments(count):
    """生成带有中英文注释的代码"""
    code_templates = [
        ("quicksort.py", """# 快速排序算法 / QuickSort Algorithm
# 这是一个经典的排序算法实现 / This is a classic sorting algorithm implementation

def quicksort(arr):
    '''
    快速排序：分治法排序算法
    QuickSort: Divide and conquer sorting algorithm
    时间复杂度 / Time Complexity: O(n log n) average
    '''
    if len(arr) <= 1:
        return arr
    
    pivot = arr[len(arr) // 2]  # 选择中间元素作为基准 / Choose middle element as pivot
    left = [x for x in arr if x < pivot]  # 小于基准的元素 / Elements less than pivot
    middle = [x for x in arr if x == pivot]  # 等于基准的元素 / Elements equal to pivot
    right = [x for x in arr if x > pivot]  # 大于基准的元素 / Elements greater than pivot
    
    return quicksort(left) + middle + quicksort(right)

# 测试 / Test
if __name__ == "__main__":
    test_array = [3, 6, 8, 10, 1, 2, 1]
    print("排序前 / Before:", test_array)
    print("排序后 / After:", quicksort(test_array))
"""),
        ("binary_search.java", """// 二分查找算法 / Binary Search Algorithm
// 用于在有序数组中快速查找元素 / Used for fast element lookup in sorted arrays

public class BinarySearch {
    /**
     * 二分查找实现
     * Binary search implementation
     * @param arr 已排序数组 / Sorted array
     * @param target 目标值 / Target value
     * @return 目标索引或-1 / Target index or -1
     */
    public static int binarySearch(int[] arr, int target) {
        int left = 0;
        int right = arr.length - 1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;  // 防止溢出 / Prevent overflow
            
            if (arr[mid] == target) {
                return mid;  // 找到目标 / Found target
            } else if (arr[mid] < target) {
                left = mid + 1;  // 在右半部分查找 / Search in right half
            } else {
                right = mid - 1;  // 在左半部分查找 / Search in left half
            }
        }
        
        return -1;  // 未找到 / Not found
    }
    
    public static void main(String[] args) {
        int[] sortedArray = {1, 3, 5, 7, 9, 11, 13};
        int target = 7;
        int result = binarySearch(sortedArray, target);
        System.out.println("查找结果 / Search result: " + result);
    }
}
"""),
        ("data_structure.js", """// 数据结构：链表实现 / Data Structure: Linked List Implementation
// 这是一个单向链表的JavaScript实现 / This is a JavaScript implementation of a singly linked list

class Node {
    constructor(data) {
        this.data = data;      // 节点数据 / Node data
        this.next = null;      // 下一个节点 / Next node
    }
}

class LinkedList {
    constructor() {
        this.head = null;      // 链表头 / List head
        this.size = 0;         // 链表大小 / List size
    }
    
    // 在末尾添加节点 / Add node at the end
    append(data) {
        const newNode = new Node(data);
        
        if (!this.head) {
            this.head = newNode;
        } else {
            let current = this.head;
            while (current.next) {
                current = current.next;
            }
            current.next = newNode;
        }
        this.size++;
    }
    
    // 打印链表 / Print the list
    print() {
        let current = this.head;
        let result = [];
        while (current) {
            result.push(current.data);
            current = current.next;
        }
        console.log("链表内容 / List content:", result.join(' -> '));
    }
}

// 测试 / Test
const list = new LinkedList();
list.append(10);
list.append(20);
list.append(30);
list.print();
"""),
    ]
    
    for i in range(count):
        filename, code = code_templates[i % len(code_templates)]
        # 为每个文件添加版本号
        base, ext = os.path.splitext(filename)
        versioned_filename = f"{base}_v{i+1}{ext}"
        
        file_path = os.path.join(TEMP_DIR, versioned_filename)
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(f"// Version {i+1}\n" + code)
        upload_file(file_path)
        time.sleep(0.1)

def gen_binary(count, extension, prefix):
    """生成二进制文件（保持原样）"""
    for i in range(count):
        file_path = os.path.join(TEMP_DIR, f"{prefix}_{i+1}.{extension}")
        with open(file_path, 'wb') as f:
            f.write(os.urandom(2048))  # 2KB
        upload_file(file_path)
        time.sleep(0.1)

if __name__ == "__main__":
    print("=" * 60)
    print("开始生成高质量中英文混合数据 / Starting bilingual data generation")
    print("=" * 60)
    
    print("\n[1/5] 生成中英文文本文档 / Generating bilingual text documents...")
    gen_bilingual_text(50)  # 减少到50个高质量文档
    
    print("\n[2/5] 生成带注释的代码文件 / Generating commented code files...")
    gen_code_with_comments(30)
    
    print("\n[3/5] 生成图片文件 / Generating image files...")
    gen_binary(15, "png", "image")
    
    print("\n[4/5] 生成音频文件 / Generating audio files...")
    gen_binary(8, "mp3", "audio")
    
    print("\n[5/5] 生成视频文件 / Generating video files...")
    gen_binary(7, "mp4", "video")
    
    print("\n" + "=" * 60)
    print("✓ 数据生成完成！/ Data generation complete!")
    print(f"✓ 总计: 50文本 + 30代码 + 15图片 + 8音频 + 7视频 = 110个文件")
    print(f"✓ Total: 50 texts + 30 codes + 15 images + 8 audios + 7 videos = 110 files")
    print("=" * 60)