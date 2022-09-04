package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品发布版本运行任务查询条件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReleaseTaskQueryCondition extends BaseCondition {

    private String productId;

    private String releaseId;

    private String taskId;

    private List<String> status = new ArrayList<>();

    private List<String> tags = new ArrayList<>();
}
