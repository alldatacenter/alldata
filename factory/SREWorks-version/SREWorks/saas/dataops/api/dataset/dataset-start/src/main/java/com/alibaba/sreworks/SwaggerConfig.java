package com.alibaba.sreworks;

import com.github.xiaoymin.swaggerbootstrapui.annotations.EnableSwaggerBootstrapUI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

@Configuration
@EnableSwaggerBootstrapUI
@EnableSwagger2
public class SwaggerConfig {

    @Value("${swagger.enable}")
    private boolean enable;

    @Bean
    public Docket api() {
        List<Parameter> pars = new ArrayList<>();
        ParameterBuilder ticketPar1 = new ParameterBuilder();
        ticketPar1.name("x-empid").description("请求人工号")
            .modelRef(new ModelRef("string"))
            .parameterType("header")
            .required(true)
            .defaultValue("test_user_id")
            .build();
        pars.add(ticketPar1.build());
        ParameterBuilder ticketPar2 = new ParameterBuilder();
        ticketPar2.name("x-auth-user").description("请求人邮箱前缀")
            .modelRef(new ModelRef("string"))
            .parameterType("header")
            .required(true)
            .defaultValue("test_user_name")
            .build();
        pars.add(ticketPar2.build());

        String swaggerHost = "127.0.0.1:7001";
        return new Docket(SWAGGER_2)
            .enable(enable)
            .host(swaggerHost)
            .apiInfo(apiInfo())
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.alibaba.sreworks.dataset"))
            .paths(PathSelectors.any())
            .build()
            .globalOperationParameters(pars);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("data")
            .description("dataset")
            .termsOfServiceUrl("https://tesla.alibaba-inc.com/")
            .version("1.0")
            .build();
    }

}
