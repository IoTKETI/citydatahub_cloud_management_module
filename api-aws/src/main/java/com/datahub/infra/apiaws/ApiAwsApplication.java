package com.datahub.infra.apiaws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.datahub.infra", exclude = DataSourceAutoConfiguration.class)
public class ApiAwsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiAwsApplication.class, args);
    }
}
