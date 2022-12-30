package cn.datax.service.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"cn.datax.service.system.api.feign"})
@SpringCloudApplication
public class DataxWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataxWorkflowApplication.class);
    }

}
