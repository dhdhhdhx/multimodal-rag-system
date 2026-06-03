/**
 * 设计模式 - 策略模式（Strategy Pattern）
 * 软件设计师考试 - 行为型设计模式示例
 * 
 * 策略模式核心：定义一系列算法，将每一个算法封装起来，并使它们可以互相替换。
 * 策略模式让算法的变化独立于使用它的客户端。
 * 
 * 关联资料：
 * - UML图见《UML_类图XMI描述.xml》
 * - 更多设计模式见《面向对象设计模式.pptx》
 */
package com.softexam.designpattern;

import java.util.Arrays;

// ==================== 策略接口 ====================
/**
 * 排序策略接口
 * 所有具体排序策略都需要实现此接口
 */
interface SortStrategy {
    /**
     * 排序方法
     * @param arr 待排序数组
     * @return 排序后的数组
     */
    int[] sort(int[] arr);
    
    /**
     * 获取算法名称
     */
    String getAlgorithmName();
}

// ==================== 具体策略A：冒泡排序 ====================
class BubbleSortStrategy implements SortStrategy {
    @Override
    public int[] sort(int[] arr) {
        int[] result = Arrays.copyOf(arr, arr.length);
        int n = result.length;
        // 外层循环控制比较轮数
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            // 内层循环进行相邻元素比较
            for (int j = 0; j < n - 1 - i; j++) {
                if (result[j] > result[j + 1]) {
                    // 交换相邻元素
                    int temp = result[j];
                    result[j] = result[j + 1];
                    result[j + 1] = temp;
                    swapped = true;
                }
            }
            // 优化：如果没有交换，说明已经有序
            if (!swapped) break;
        }
        return result;
    }
    
    @Override
    public String getAlgorithmName() {
        return "冒泡排序 - 时间复杂度O(n²)，稳定排序，空间复杂度O(1)";
    }
}

// ==================== 具体策略B：快速排序 ====================
class QuickSortStrategy implements SortStrategy {
    @Override
    public int[] sort(int[] arr) {
        int[] result = Arrays.copyOf(arr, arr.length);
        quickSort(result, 0, result.length - 1);
        return result;
    }
    
    private void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(arr, low, high);
            quickSort(arr, low, pivotIndex - 1);
            quickSort(arr, pivotIndex + 1, high);
        }
    }
    
    private int partition(int[] arr, int low, int high) {
        int pivot = arr[high];  // 选择最后一个元素作为基准
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                // 交换arr[i]和arr[j]
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        // 将基准放到正确位置
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        return i + 1;
    }
    
    @Override
    public String getAlgorithmName() {
        return "快速排序 - 平均O(nlogn)，最坏O(n²)，不稳定排序，空间O(logn)";
    }
}

// ==================== 具体策略C：归并排序 ====================
class MergeSortStrategy implements SortStrategy {
    @Override
    public int[] sort(int[] arr) {
        return mergeSort(Arrays.copyOf(arr, arr.length));
    }
    
    private int[] mergeSort(int[] arr) {
        if (arr.length <= 1) return arr;
        
        int mid = arr.length / 2;
        int[] left = mergeSort(Arrays.copyOfRange(arr, 0, mid));
        int[] right = mergeSort(Arrays.copyOfRange(arr, mid, arr.length));
        
        return merge(left, right);
    }
    
    private int[] merge(int[] left, int[] right) {
        int[] result = new int[left.length + right.length];
        int i = 0, j = 0, k = 0;
        
        while (i < left.length && j < right.length) {
            if (left[i] <= right[j]) {
                result[k++] = left[i++];
            } else {
                result[k++] = right[j++];
            }
        }
        
        while (i < left.length) result[k++] = left[i++];
        while (j < right.length) result[k++] = right[j++];
        
        return result;
    }
    
    @Override
    public String getAlgorithmName() {
        return "归并排序 - 稳定O(nlogn)，稳定排序，空间复杂度O(n)";
    }
}

// ==================== 上下文类 ====================
/**
 * 排序上下文 - 持有策略对象的引用
 * 这是策略模式中的Context角色
 */
class SortContext {
    private SortStrategy strategy;
    
    // 通过构造方法注入策略
    public SortContext(SortStrategy strategy) {
        this.strategy = strategy;
    }
    
    // 运行时可以切换策略
    public void setStrategy(SortStrategy strategy) {
        this.strategy = strategy;
    }
    
    // 执行排序（委托给策略对象）
    public void executeSort(int[] data) {
        System.out.println("使用算法: " + strategy.getAlgorithmName());
        int[] result = strategy.sort(data);
        System.out.println("排序结果: " + Arrays.toString(result));
        System.out.println();
    }
}

// ==================== 客户端 ====================
public class StrategyPatternDemo {
    public static void main(String[] args) {
        int[] testData = {64, 34, 25, 12, 22, 11, 90};
        
        System.out.println("原始数据: " + Arrays.toString(testData));
        System.out.println("======== 策略模式演示 ========");
        System.out.println();
        
        // 创建上下文
        SortContext context = new SortContext(new BubbleSortStrategy());
        
        // 使用冒泡排序
        System.out.println("--- 冒泡排序 ---");
        context.executeSort(testData);
        
        // 动态切换为快速排序
        System.out.println("--- 快速排序 ---");
        context.setStrategy(new QuickSortStrategy());
        context.executeSort(testData);
        
        // 动态切换为归并排序
        System.out.println("--- 归并排序 ---");
        context.setStrategy(new MergeSortStrategy());
        context.executeSort(testData);
        
        /*
         * 考试要点总结：
         * 1. 策略模式三要素：Context（上下文）、Strategy（策略接口）、ConcreteStrategy（具体策略）
         * 2. 策略模式属于【行为型】设计模式
         * 3. 优点：算法可以自由切换、避免多重条件判断、扩展性好
         * 4. 缺点：客户端必须知道所有策略类、策略类数量增多
         * 5. 与状态模式的区别：策略模式由客户端选择策略，状态模式由内部状态自动切换
         */
    }
}
