package org.dromara.cloudeon.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 */
@Data
@Component
public class CloudeonWebProperties {

    /**
     * api包下统一前缀
     */
    Api api = new Api("apiPre", "**.controller.**");



    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Api {

        private String prefix;

        private String controllerPath;
    }

}
