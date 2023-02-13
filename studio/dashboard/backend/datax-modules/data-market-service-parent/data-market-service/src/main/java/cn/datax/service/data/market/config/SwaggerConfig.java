package cn.datax.service.data.market.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.*;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "swagger", name = "enable", havingValue = "true")
@EnableConfigurationProperties(SwaggerProperties.class)
@EnableSwagger2
@EnableKnife4j
@Import(BeanValidatorPluginsConfiguration.class)
public class SwaggerConfig {

    @Autowired
    private SwaggerProperties swaggerProperties;

    /**
     * 创建API应用
     * apiInfo() 增加API相关信息
     * 通过select()函数返回一个ApiSelectorBuilder实例,用来控制哪些接口暴露给Swagger来展现，
     * 本例采用指定扫描的包路径来定义指定要建立API的目录。
     *
     * @return
     */
    @Bean
    public Docket createRestApi(){
        //版本类型是swagger2
        return new Docket(DocumentationType.SWAGGER_2)
                //通过调用自定义方法apiInfo，获得文档的主要信息
                .apiInfo(apiInfo())
                //设置全局参数
                .globalOperationParameters(globalParamBuilder())
                //设置全局响应参数
                .globalResponseMessage(RequestMethod.GET,responseBuilder())
                .globalResponseMessage(RequestMethod.POST,responseBuilder())
                .globalResponseMessage(RequestMethod.PUT,responseBuilder())
                .globalResponseMessage(RequestMethod.DELETE,responseBuilder())
                .select()
                //扫描该包下面的API注解
                .apis(RequestHandlerSelectors.basePackage(swaggerProperties.getBasePackage()))
                .paths(PathSelectors.any())
                .build()
                //设置安全认证
                ;
    }
    /**
     * 创建该API的基本信息（这些基本信息会展现在文档页面中）
     * 访问地址：http://项目实际地址/swagger-ui.html
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(swaggerProperties.getTitle())
                .description(swaggerProperties.getDescription())
                .termsOfServiceUrl(swaggerProperties.getTermsOfServiceUrl())
                .version(swaggerProperties.getVersion())
                .contact(new Contact(swaggerProperties.getContact().getName(), swaggerProperties.getContact().getUrl(), swaggerProperties.getContact().getEmail()))
                .build();
    }

    /**
     * 安全认证参数
     * @return
     */
    private List<ApiKey> security() {
        List<ApiKey> apiKeys = new ArrayList<>();
        apiKeys.add(new ApiKey("Authorization", "Authorization", "header"));
        return apiKeys;
    }

    /**
     * 构建全局参数列表
     * @return
     */
    private List<Parameter> globalParamBuilder(){
        List<Parameter> pars = new ArrayList<>();
        pars.add(parameterBuilder("Authorization","令牌","string","header",false).build());
        return pars;
    }

    /**
     * 创建参数
     * @return
     */
    private ParameterBuilder parameterBuilder(String name, String desc, String type, String parameterType, boolean required) {
        ParameterBuilder tokenPar = new ParameterBuilder();
        tokenPar.name(name).description(desc).modelRef(new ModelRef(type)).parameterType(parameterType).required(required).build();
        return tokenPar;
    }

    /**
     * 创建全局响应值
     * @return
     */
    private List<ResponseMessage> responseBuilder() {
        List<ResponseMessage> responseMessageList = new ArrayList<>();
        responseMessageList.add(new ResponseMessageBuilder().code(200).message("响应成功").build());
        responseMessageList.add(new ResponseMessageBuilder().code(500).message("服务器内部错误").build());
        return responseMessageList;
    }
}
