package com.platform.mall.mobile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * swagger在线文档配置<br>
 * 项目启动后可通过地址：http://host:ip/swagger-ui.html 查看在线文档
 *
 * @author enilu
 * @version 2018-07-24
 */

@Configuration
@EnableSwagger2
public class WxSwagger2Configuration {
    @Value("${requesthandlerselectors.basepackage}")
    private String webPackage;

    @Bean
    public Docket wxDocket() {

        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("wx")
                .apiInfo(wxApiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage(webPackage))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo wxApiInfo() {
        return new ApiInfoBuilder()
                .title("商城小程序API")
                .description("商城小程序接口文档")
                .contact("http://120.77.155.220:8080")
                .version("1.0")
                .build();
    }
}
