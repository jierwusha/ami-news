package com.project.aminewsbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.project.aminewsbackend.mapper")
@EnableScheduling
@EnableAsync
public class AminewsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AminewsBackendApplication.class, args);
    }

}
