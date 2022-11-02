package com.alibaba.tesla.gateway.server.config;

import com.alibaba.tesla.gateway.server.util.RouteMetaDataKeyConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
@Primary
@AllArgsConstructor
public class SwaggerResourceConfig implements SwaggerResourcesProvider {

    private final RouteDefinitionRepository routeDefinitionRepository;


    @Override
    public List<SwaggerResource> get() {
        List<SwaggerResource> resources = new ArrayList<>();
        routeDefinitionRepository.getRouteDefinitions().subscribe(routeDefinition -> {
            Map<String, Object> metadata = routeDefinition.getMetadata();
            boolean enableDoc = false;
            if (!CollectionUtils.isEmpty(metadata)) {
                Object enabledDoc = metadata.get(RouteMetaDataKeyConstants.ENABLED_DOC);
                enableDoc =  enabledDoc != null && BooleanUtils.isTrue((Boolean) enabledDoc);
            }
            if (enableDoc) {
                routeDefinition.getPredicates().stream()
                    .filter(predicateDefinition -> ("Path").equalsIgnoreCase(predicateDefinition.getName()))
                    .forEach(predicateDefinition -> {
                        String docUri = "v2/api-docs";
                        if (metadata.get(RouteMetaDataKeyConstants.DOC_URI) != null) {
                            docUri = (String) metadata.get(RouteMetaDataKeyConstants.DOC_URI);
                        }
                        resources.add(swaggerResource(routeDefinition.getId(),
                            predicateDefinition.getArgs().get(NameUtils.GENERATED_NAME_PREFIX + "0")
                                .replace("**", docUri)));
                    });
            }
        });
        return resources;
    }

    private SwaggerResource swaggerResource(String name, String location) {
        SwaggerResource swaggerResource = new SwaggerResource();
        swaggerResource.setName(name);
        swaggerResource.setLocation(location);
        swaggerResource.setSwaggerVersion("2.0");
        return swaggerResource;
    }

}
