package com.alibaba.tesla.appmanager.server.repository.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 附加组件实例查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonInstanceQueryCondition implements Serializable {

    private static final long serialVersionUID = 8008723430208527719L;

    private String namespaceId;

    private String addonId;

    private String addonName;

    private Map<String, String> addonAttrs;

    private String addonInstanceId;
}
