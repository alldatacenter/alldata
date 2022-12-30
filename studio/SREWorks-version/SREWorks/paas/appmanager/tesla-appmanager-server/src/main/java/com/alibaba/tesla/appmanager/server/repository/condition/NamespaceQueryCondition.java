package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NamespaceQueryCondition extends BaseCondition {

    private String namespaceId;

    private String namespaceName;

    private String namespaceCreator;

    private String namespaceModifier;
}
