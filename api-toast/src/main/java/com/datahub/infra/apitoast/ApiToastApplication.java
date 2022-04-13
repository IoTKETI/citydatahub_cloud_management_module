package com.datahub.infra.apitoast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.datahub.infra", exclude = DataSourceAutoConfiguration.class)
public class ApiToastApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiToastApplication.class, args);
    }

}
