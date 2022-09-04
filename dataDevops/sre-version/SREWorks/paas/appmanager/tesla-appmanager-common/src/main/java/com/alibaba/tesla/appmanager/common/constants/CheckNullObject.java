package com.alibaba.tesla.appmanager.common.constants;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName:CheckNullObject
 * @author yangjie.dyj@alibaba-inc.com
 * @DATE: 2020-12-01
 * @Description:
 **/
@Data
@Builder
public class CheckNullObject {
    private Object checkObject;
    private String actionName;
    private String objectName;
    private List<String> fields;
}
