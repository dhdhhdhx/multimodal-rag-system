// Java示例代码 - 模块34
package com.example.demo;

import java.util.*;
import java.util.stream.Collectors;

public class DataAnalyzer {
    private List<Integer> data;
    
    public DataAnalyzer(List<Integer> data) {
        this.data = data;
    }
    
    public double calculateAverage() {
        return data.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
    }
    
    public int findMax() {
        return data.stream()
            .max(Integer::compareTo)
            .orElse(0);
    }
    
    public static void main(String[] args) {
        List<Integer> dataset = Arrays.asList(1, 2, 3, 4, 5);
        DataAnalyzer analyzer = new DataAnalyzer(dataset);
        System.out.println("Average: " + analyzer.calculateAverage());
        System.out.println("Max: " + analyzer.findMax());
    }
}
