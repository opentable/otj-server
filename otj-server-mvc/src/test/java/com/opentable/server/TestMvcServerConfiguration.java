package com.opentable.server;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opentable.service.ServiceInfo;

@Configuration
@MVCServer
@RestController
@RequestMapping("/api")
public class TestMvcServerConfiguration {

    @Bean
    public ServiceInfo getServiceInfo() {
        return new ServiceInfo() {
            @Override
            public String getName() {
                return "test";
            }
        };
    }

    @RequestMapping("test")
    public String test() {
        return "test";
    }

    public static void main(String[] args) {
        SpringApplication.run(TestMvcServerConfiguration.class, args);
    }

}
