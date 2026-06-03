"""
数据结构 - 排序算法Python实现
软件设计师考试 - 排序算法对比与实现

本文件包含常见排序算法的Python实现，
每个算法都附有时间复杂度分析和考试要点。

关联资料：
- Java版本见《数据结构_排序算法.java》
- 排序算法可视化见《排序算法动态演示.mp4》
- 详见《题库_数据结构与算法.json》中的排序相关题目
"""

import time
import random


def bubble_sort(arr):
    """
    冒泡排序
    时间复杂度：最好O(n) / 平均O(n²) / 最坏O(n²)
    空间复杂度：O(1)
    稳定性：稳定
    """
    arr = arr.copy()
    n = len(arr)
    for i in range(n - 1):
        swapped = False
        for j in range(n - 1 - i):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:  # 优化：无交换则已有序
            break
    return arr


def selection_sort(arr):
    """
    简单选择排序
    时间复杂度：始终O(n²)
    空间复杂度：O(1)
    稳定性：不稳定（如 5a, 5b, 3 → 3, 5b, 5a）
    """
    arr = arr.copy()
    n = len(arr)
    for i in range(n - 1):
        min_idx = i
        for j in range(i + 1, n):
            if arr[j] < arr[min_idx]:
                min_idx = j
        arr[i], arr[min_idx] = arr[min_idx], arr[i]
    return arr


def insertion_sort(arr):
    """
    直接插入排序
    时间复杂度：最好O(n) / 平均O(n²) / 最坏O(n²)
    空间复杂度：O(1)
    稳定性：稳定
    """
    arr = arr.copy()
    for i in range(1, len(arr)):
        key = arr[i]
        j = i - 1
        while j >= 0 and arr[j] > key:
            arr[j + 1] = arr[j]
            j -= 1
        arr[j + 1] = key
    return arr


def quick_sort(arr):
    """
    快速排序
    时间复杂度：最好O(nlogn) / 平均O(nlogn) / 最坏O(n²)
    空间复杂度：O(logn)（递归栈）
    稳定性：不稳定
    """
    arr = arr.copy()

    def _quick_sort(a, low, high):
        if low < high:
            pivot_idx = _partition(a, low, high)
            _quick_sort(a, low, pivot_idx - 1)
            _quick_sort(a, pivot_idx + 1, high)

    def _partition(a, low, high):
        pivot = a[high]
        i = low - 1
        for j in range(low, high):
            if a[j] <= pivot:
                i += 1
                a[i], a[j] = a[j], a[i]
        a[i + 1], a[high] = a[high], a[i + 1]
        return i + 1

    _quick_sort(arr, 0, len(arr) - 1)
    return arr


def merge_sort(arr):
    """
    归并排序
    时间复杂度：始终O(nlogn)
    空间复杂度：O(n)
    稳定性：稳定
    """
    if len(arr) <= 1:
        return arr.copy()

    mid = len(arr) // 2
    left = merge_sort(arr[:mid])
    right = merge_sort(arr[mid:])

    return _merge(left, right)


def _merge(left, right):
    """归并两个有序数组"""
    result = []
    i = j = 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            result.append(left[i])
            i += 1
        else:
            result.append(right[j])
            j += 1
    result.extend(left[i:])
    result.extend(right[j:])
    return result


def heap_sort(arr):
    """
    堆排序
    时间复杂度：始终O(nlogn)
    空间复杂度：O(1)
    稳定性：不稳定
    """
    arr = arr.copy()
    n = len(arr)

    def heapify(a, n, i):
        """调整堆"""
        largest = i
        left = 2 * i + 1
        right = 2 * i + 2

        if left < n and a[left] > a[largest]:
            largest = left
        if right < n and a[right] > a[largest]:
            largest = right
        if largest != i:
            a[i], a[largest] = a[largest], a[i]
            heapify(a, n, largest)

    # 构建最大堆
    for i in range(n // 2 - 1, -1, -1):
        heapify(arr, n, i)

    # 逐个取出堆顶
    for i in range(n - 1, 0, -1):
        arr[0], arr[i] = arr[i], arr[0]
        heapify(arr, i, 0)

    return arr


def radix_sort(arr):
    """
    基数排序（LSD - 最低位优先）
    时间复杂度：O(d × (n + k))，d为位数，k为基数
    空间复杂度：O(n + k)
    稳定性：稳定
    """
    if not arr:
        return []

    arr = arr.copy()
    max_val = max(arr)
    exp = 1

    while max_val // exp > 0:
        # 按当前位进行计数排序
        output = [0] * len(arr)
        count = [0] * 10

        for num in arr:
            index = (num // exp) % 10
            count[index] += 1

        for i in range(1, 10):
            count[i] += count[i - 1]

        for i in range(len(arr) - 1, -1, -1):
            index = (arr[i] // exp) % 10
            output[count[index] - 1] = arr[i]
            count[index] -= 1

        arr = output
        exp *= 10

    return arr


# ==================== 算法性能测试 ====================

def benchmark(sort_func, data, name):
    """对排序算法进行性能测试"""
    start = time.perf_counter()
    result = sort_func(data)
    elapsed = time.perf_counter() - start
    print(f"{name:12s}: {elapsed:.6f}秒 | 结果前10个: {result[:10]}")
    return result


if __name__ == "__main__":
    # 生成测试数据
    random.seed(42)
    test_data = [random.randint(1, 10000) for _ in range(1000)]

    print("=" * 60)
    print("排序算法性能对比测试")
    print(f"数据规模: {len(test_data)} 个随机整数")
    print("=" * 60)

    algorithms = [
        (bubble_sort, "冒泡排序"),
        (selection_sort, "选择排序"),
        (insertion_sort, "插入排序"),
        (quick_sort, "快速排序"),
        (merge_sort, "归并排序"),
        (heap_sort, "堆排序"),
        (radix_sort, "基数排序"),
    ]

    for func, name in algorithms:
        benchmark(func, test_data, name)

    print()
    print("=" * 60)
    print("考试知识点总结")
    print("=" * 60)
    print("""
    | 排序算法 | 最好时间 | 平均时间 | 最坏时间 | 空间 | 稳定 |
    |---------|---------|---------|---------|------|------|
    | 冒泡排序 | O(n)    | O(n²)   | O(n²)   | O(1) | 稳定 |
    | 选择排序 | O(n²)   | O(n²)   | O(n²)   | O(1) | 不稳定|
    | 插入排序 | O(n)    | O(n²)   | O(n²)   | O(1) | 稳定 |
    | 希尔排序 | O(nlogn)| O(n^1.3)| O(n²)   | O(1) | 不稳定|
    | 快速排序 | O(nlogn)| O(nlogn)| O(n²)   |O(logn)|不稳定|
    | 归并排序 | O(nlogn)| O(nlogn)| O(nlogn)| O(n) | 稳定 |
    | 堆排序   | O(nlogn)| O(nlogn)| O(nlogn)| O(1) | 不稳定|
    | 基数排序 | O(d(n+k))|O(d(n+k))|O(d(n+k))|O(n+k)|稳定 |

    考试必记：
    1. 时间复杂度始终为O(nlogn)：归并排序、堆排序
    2. 不稳定排序：快排、堆排、希尔、选择
    3. 稳定排序：冒泡、插入、归并、基数
    """)
