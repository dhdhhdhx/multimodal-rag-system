// Version 11
// 二分查找算法 / Binary Search Algorithm
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
