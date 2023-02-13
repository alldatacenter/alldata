package cn.datax.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class DataxGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataxGatewayApplication.class, args);
    }

}
