package org.dromara.cloudeon.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceInstanceWebUrlVO {


    private String hostnameUrl;
    private String ipUrl;
    private String name;

}
