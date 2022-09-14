package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用包查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageQueryCondition extends BaseCondition {

    private Long id;

    private String appId;

    private String packageVersion;

    private String packageVersionGreaterThan;

    private String packageVersionLessThan;

    private String packageCreator;

    private String orderBy;

    private List<String> tags = new ArrayList<>();
}
