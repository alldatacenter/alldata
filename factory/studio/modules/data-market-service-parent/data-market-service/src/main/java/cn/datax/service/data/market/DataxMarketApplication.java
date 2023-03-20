package cn.datax.service.data.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"cn.datax.service.system.api.feign"})
@SpringBootApplication
public class DataxMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataxMarketApplication.class);
    }
}
