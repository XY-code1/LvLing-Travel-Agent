package com.guido.scenicai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.guido.scenicai.module.**.mapper")
@EnableScheduling
public class ScenicAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScenicAiApplication.class, args);
    }
}
