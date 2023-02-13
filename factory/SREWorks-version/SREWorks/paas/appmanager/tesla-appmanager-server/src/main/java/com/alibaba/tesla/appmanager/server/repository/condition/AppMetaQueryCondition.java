package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 应用元信息查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppMetaQueryCondition extends BaseCondition {

    private String appId;

    private String appIdLike;

    private List<String> appIdList;

    private String optionKey;

    private String optionValue;
}
