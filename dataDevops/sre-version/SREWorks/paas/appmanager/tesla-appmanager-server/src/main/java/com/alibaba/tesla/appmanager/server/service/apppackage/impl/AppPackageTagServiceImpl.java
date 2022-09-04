package com.alibaba.tesla.appmanager.server.service.apppackage.impl;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.AppPackageTagRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTagQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageService;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTagService;
import com.alibaba.tesla.appmanager.server.service.rtappinstance.RtAppInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author QianMo
 * @date 2021/03/29.
 */
@Service
@Slf4j
public class AppPackageTagServiceImpl implements AppPackageTagService {

    @Autowired
    private AppPackageTagRepository appPackageTagRepository;

    @Autowired
    private AppPackageService appPackageService;

    @Autowired
    private RtAppInstanceService rtAppInstanceService;

    @Override
    public int insert(AppPackageTagDO record) {
        int result = appPackageTagRepository.insert(record);
        Pagination<RtAppInstanceDO> appInstances = rtAppInstanceService.list(RtAppInstanceQueryCondition.builder()
                .appId(record.getAppId())
                .build());
        appInstances.getItems().forEach(item -> rtAppInstanceService.asyncTriggerStatusUpdate(item.getAppInstanceId()));
        return result;
    }

    /**
     * 批量更新指定 app package 的 tag 列表
     *
     * @param appPackageId 应用包 ID
     * @param tags         Tag 列表，字符串列表
     * @return 更新数量
     */
    @Override
    public int batchUpdate(Long appPackageId, List<String> tags) {
        AppPackageQueryCondition condition = AppPackageQueryCondition.builder().id(appPackageId).build();
        Pagination<AppPackageDO> appPackages = appPackageService.list(condition);
        if (appPackages.isEmpty() || appPackages.getTotal() != 1) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot update tags for app package %d, not exists", appPackageId));
        }
        String appId = appPackages.getItems().get(0).getAppId();

        // 删除并重建
        delete(AppPackageTagQueryCondition.builder().appPackageId(appPackageId).build());
        tags.forEach(tag -> appPackageTagRepository.insert(AppPackageTagDO.builder()
                .appId(appId)
                .tag(tag)
                .appPackageId(appPackageId)
                .build()));
        return 0;
    }

    @Override
    public List<AppPackageTagDO> query(List<Long> appPackageIdList, String tag) {
        return appPackageTagRepository.query(appPackageIdList, tag);
    }

    @Override
    public int delete(AppPackageTagQueryCondition condition) {
        return appPackageTagRepository.deleteByCondition(condition);
    }
}
