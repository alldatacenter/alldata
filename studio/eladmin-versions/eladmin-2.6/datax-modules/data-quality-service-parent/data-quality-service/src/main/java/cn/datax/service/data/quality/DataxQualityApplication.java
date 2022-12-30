package cn.datax.service.data.quality;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableFeignClients(basePackages = {"cn.datax.service.system.api.feign", "cn.datax.service.data.metadata.api.feign"})
@SpringCloudApplication
public class DataxQualityApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataxQualityApplication.class);
    }
}
