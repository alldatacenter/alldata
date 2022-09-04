package com.alibaba.tesla.appmanager.server.service.apppackage;

import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTagQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDO;

import java.util.List;

/**
 * @author QianMo
 * @date 2021/03/29.
 */
public interface AppPackageTagService {

    int insert(AppPackageTagDO record);

    /**
     * 批量更新指定 app package 的 tag 列表
     *
     * @param appPackageId 应用包 ID
     * @param tags         Tag 列表，字符串列表
     * @return 更新数量
     */
    int batchUpdate(Long appPackageId, List<String> tags);

    List<AppPackageTagDO> query(List<Long> appPackageIdList, String tag);

    /**
     * 根据条件删除Tag
     *
     * @param condition 查询条件
     */
    int delete(AppPackageTagQueryCondition condition);
}
