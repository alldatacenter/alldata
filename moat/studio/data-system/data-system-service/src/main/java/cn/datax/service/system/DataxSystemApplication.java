package cn.datax.service.system;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"cn.datax.service.system.api.feign"})
@SpringCloudApplication
public class DataxSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataxSystemApplication.class);
    }

}
