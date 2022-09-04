package com.alibaba.tesla.appmanager.domain.req;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PluginQueryReq extends BaseRequest {


    /**
     * Tag key
     */
    private String tagKey;

    /**
     * Tag Value
     */
    private String tagValue;
}
