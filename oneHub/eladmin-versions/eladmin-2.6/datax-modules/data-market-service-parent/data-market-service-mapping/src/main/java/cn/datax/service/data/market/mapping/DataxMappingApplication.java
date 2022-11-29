package cn.datax.service.data.market.mapping;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"cn.datax.service.system.api.feign", "cn.datax.service.data.metadata.api.feign", "cn.datax.service.data.market.api.feign"})
@SpringCloudApplication
public class DataxMappingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataxMappingApplication.class);
    }
}
