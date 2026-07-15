package com.platform.admin;

import com.platform.admin.entity.Common;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.core.env.Environment;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * Engine是入口类，该类负责数据的初始化
 **/
@EnableSwagger2
@SpringCloudApplication
@EnableFeignClients(basePackages = {"cn.datax.service.system.api.feign"})
@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class})
public class DataDtsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataDtsServiceApplication.class);
	}
}
