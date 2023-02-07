package cn.datax.service.data.standard;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"cn.datax.service.system.api.feign"})
@SpringCloudApplication
public class DataxStandardApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataxStandardApplication.class);
    }
}
