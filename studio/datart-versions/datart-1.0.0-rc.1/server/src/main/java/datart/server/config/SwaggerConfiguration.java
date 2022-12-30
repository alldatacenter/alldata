/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2020 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package datart.server.config;

import com.fasterxml.classmate.ResolvedType;
import com.google.common.collect.Lists;
import datart.server.base.dto.ResponseData;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ResolvedTypes;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;

import springfox.documentation.spi.schema.contexts.ModelContext;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

//    @Autowired
//    private TypeNameExtractor nameExtractor;

//    @Autowired
//    private TypeResolver typeResolver;

    @Bean
    public Docket createRestApi() {

        List<ResponseMessage> responseMessageList = new ArrayList<>();
        return new Docket(DocumentationType.SWAGGER_2)
                .globalResponseMessage(RequestMethod.GET, responseMessageList)
                .globalResponseMessage(RequestMethod.POST, responseMessageList)
                .globalResponseMessage(RequestMethod.PUT, responseMessageList)
                .globalResponseMessage(RequestMethod.DELETE, responseMessageList)
                .apiInfo(apiInfo()).ignoredParameterTypes(Exception.class)
                .select()
                .apis(RequestHandlerSelectors.basePackage("datart"))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(Lists.newArrayList(apiKey()));
    }


//    @Bean
//    public OperationBuilderPlugin operationBuilderPlugin() {
//        return new OperationBuilderPlugin() {
//
//            @Override
//            public void apply(OperationContext context) {
//                ResolvedType returnType = context.getReturnType();
//                returnType = context.alternateFor(returnType);
//                ParameterizedType parameterize = TypeUtils.parameterize(ResponseData.class, returnType);
//                returnType = typeResolver.resolve(parameterize);
//                ModelContext modelContext = ModelContext.returnValue(returnType, context.getDocumentationType(), context.getAlternateTypeProvider(), context.getGenericsNamingStrategy(), context.getIgnorableParameterTypes());
//                context.operationBuilder().responseModel(ResolvedTypes.modelRefFactory(modelContext, SwaggerConfiguration.this.nameExtractor).apply(returnType));
//            }
//
//            @Override
//            public boolean supports(DocumentationType documentationType) {
//                return true;
//            }
//        };
//    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("datart api")
                .version("1.0")
                .build();
    }

    private ApiKey apiKey() {
        return new ApiKey("Authorization", "Authorization", "header");
    }

}
