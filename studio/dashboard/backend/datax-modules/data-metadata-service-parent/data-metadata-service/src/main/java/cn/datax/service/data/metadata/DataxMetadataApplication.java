package cn.datax.service.data.metadata;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"cn.datax.service.system.api.feign"
		, "cn.datax.service.data.standard.api.feign"
		, "cn.datax.service.data.quality.api.feign"
		, "cn.datax.service.data.market.api.feign"
		, "cn.datax.service.data.visual.api.feign"})
@SpringCloudApplication
public class DataxMetadataApplication {
	public static void main(String[] args) {
		SpringApplication.run(DataxMetadataApplication.class);
	}
}
