// Version 10
# 快速排序算法 / QuickSort Algorithm
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
