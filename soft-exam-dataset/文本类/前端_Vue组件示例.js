/**
 * 前端开发 - Vue 3 组件示例
 * 软件设计师考试 - Web前端基础知识点
 * 
 * 本文件展示了Vue 3组合式API（Composition API）的使用，
 * 包含组件、响应式数据、计算属性、生命周期钩子等核心概念。
 * 
 * 关联资料：样式文件见《前端_响应式布局.css》
 */

// ==================== 响应式数据 ====================

import { ref, reactive, computed, watch, onMounted, defineComponent } from 'vue';

/**
 * ref：用于基本类型的响应式数据
 * 使用 .value 访问和修改值
 */
const count = ref(0);
const examName = ref('软件设计师考试');

/**
 * reactive：用于对象类型的响应式数据
 * 直接访问属性，不需要 .value
 */
const examState = reactive({
  currentQuestion: 1,
  totalQuestions: 75,
  score: 0,
  answers: {},
  timeRemaining: 150,  // 分钟
  isSubmitted: false
});

// ==================== 计算属性 ====================

/**
 * computed：根据依赖自动计算的属性
 * 只有当依赖变化时才会重新计算
 */
const progress = computed(() => {
  return Math.round(
    (Object.keys(examState.answers).length / examState.totalQuestions) * 100
  );
});

const timeDisplay = computed(() => {
  const hours = Math.floor(examState.timeRemaining / 60);
  const minutes = examState.timeRemaining % 60;
  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
});

const passStatus = computed(() => {
  return examState.score >= 45 ? '通过' : '未通过';
});

// ==================== 侦听器 ====================

/**
 * watch：侦听响应式数据的变化
 */
watch(count, (newVal, oldVal) => {
  console.log(`计数从 ${oldVal} 变为 ${newVal}`);
});

watch(
  () => examState.currentQuestion,
  (newQuestion) => {
    // 当题目切换时，可以保存答题进度
    console.log(`切换到第 ${newQuestion} 题`);
  }
);

// ==================== 组件定义 ====================

/**
 * 考试答题卡组件
 * 展示如何使用组合式API定义组件
 */
const ExamAnswerSheet = defineComponent({
  name: 'ExamAnswerSheet',
  props: {
    questions: {
      type: Array,
      required: true,
      // 验证器函数
      validator: (value) => {
        return value.every(q => q.id && q.content);
      }
    },
    maxTime: {
      type: Number,
      default: 150
    }
  },
  emits: ['submit', 'timeout'],
  
  setup(props, { emit }) {
    const currentQuestion = ref(1);
    const userAnswers = reactive({});
    const selectedAnswer = ref(null);

    // 选择答案
    function selectAnswer(questionId, option) {
      userAnswers[questionId] = option;
      selectedAnswer.value = option;
    }

    // 上一题
    function prevQuestion() {
      if (currentQuestion.value > 1) {
        currentQuestion.value--;
        selectedAnswer.value = userAnswers[currentQuestion.value] || null;
      }
    }

    // 下一题
    function nextQuestion() {
      if (currentQuestion.value < props.questions.length) {
        currentQuestion.value++;
        selectedAnswer.value = userAnswers[currentQuestion.value] || null;
      }
    }

    // 提交试卷
    function submitExam() {
      if (!window.confirm('确认提交试卷？')) return;
      emit('submit', { ...userAnswers });
    }

    // 标记题目
    const markedQuestions = reactive(new Set());
    function toggleMark(questionId) {
      if (markedQuestions.has(questionId)) {
        markedQuestions.delete(questionId);
      } else {
        markedQuestions.add(questionId);
      }
    }

    // 生命周期钩子
    onMounted(() => {
      console.log('考试组件已挂载，开始计时');
    });

    return {
      currentQuestion,
      userAnswers,
      selectedAnswer,
      markedQuestions,
      selectAnswer,
      prevQuestion,
      nextQuestion,
      submitExam,
      toggleMark
    };
  }
});

// ==================== HTTP请求封装 ====================

/**
 * 封装API请求工具
 * 考试中涉及Fetch API和Promise的知识点
 */
class ApiClient {
  constructor(baseURL) {
    this.baseURL = baseURL;
    this.defaultHeaders = {
      'Content-Type': 'application/json'
    };
  }

  // 设置认证Token
  setToken(token) {
    this.defaultHeaders['Authorization'] = `Bearer ${token}`;
  }

  // GET请求
  async get(url, params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const fullUrl = queryString 
      ? `${this.baseURL}${url}?${queryString}` 
      : `${this.baseURL}${url}`;

    const response = await fetch(fullUrl, {
      method: 'GET',
      headers: this.defaultHeaders
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
  }

  // POST请求
  async post(url, data) {
    const response = await fetch(`${this.baseURL}${url}`, {
      method: 'POST',
      headers: this.defaultHeaders,
      body: JSON.stringify(data)
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
  }
}

// ==================== 考试知识点总结 ====================

/*
 * 软件设计师考试 - 前端知识点：
 * 
 * 1. JavaScript基础：
 *    - 数据类型：原始类型（string, number, boolean, null, undefined, symbol, bigint）
 *    - 引用类型：object, array, function
 *    - var/let/const 区别：var函数作用域，let/const块级作用域
 *    - 箭头函数没有自己的this
 * 
 * 2. ES6+特性：
 *    - 解构赋值、模板字符串、展开运算符
 *    - Promise（三种状态：pending, fulfilled, rejected）
 *    - async/await
 *    - Map/Set
 * 
 * 3. HTTP协议：
 *    - GET/POST/PUT/DELETE 方法
 *    - 状态码：200成功、301永久重定向、302临时重定向、404未找到、500服务器错误
 *    - Cookie/Session/Token 认证机制
 * 
 * 4. CSS布局：
 *    - 详见《前端_响应式布局.css》
 *    - Flexbox、Grid布局
 *    - 响应式设计：媒体查询、vw/vh、rem/em
 * 
 * 5. Vue核心概念：
 *    - 组件化开发
 *    - 响应式原理（Proxy）
 *    - 虚拟DOM和Diff算法
 *    - 组件通信：props/emit、provide/inject、事件总线
 */
