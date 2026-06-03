package com.multimodal.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MultimodalRagApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultimodalRagApplication.class, args);
    }
}
