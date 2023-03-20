package cn.datax.service.data.metadata.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"cn.datax.service.system.api.feign", "cn.datax.service.data.metadata.api.feign"})
@SpringBootApplication
public class DataxConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataxConsoleApplication.class);
    }
}
