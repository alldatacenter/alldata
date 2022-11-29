package cn.datax.service.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"cn.datax.service.system.api.feign"})
@SpringBootApplication
public class DataxFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataxFileApplication.class);
    }

}
