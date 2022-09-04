package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageService;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 清理 DB 和 MinIO 中的历史应用包/组件包，并清理远端存储里的无用数据
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class CleanAppPackageJob {

    @Autowired
    private AppPackageService appPackageService;

    @Autowired
    private PackageProperties packageProperties;

    @Autowired
    private Storage storage;

    @Scheduled(cron = "${appmanager.cron-job.clean-app-package:-}")
    @SchedulerLock(name = "cleanAppPackage")
    public void execute() {
        Integer defaultKeepNumbers = packageProperties.getDefaultKeepNumbers();
    }

    /**
     * 清理当前存储中，没有在当前 DB 中存有记录的 AppPackage / ComponentPackage 包
     */
    private void cleanUselessStoragePackages() {
        String bucketName = packageProperties.getBucketName();
        Pagination<AppPackageDO> appPackages = appPackageService.list(AppPackageQueryCondition.builder().build());
        for (AppPackageDO appPackageDO : appPackages.getItems()) {
            String packagePath = appPackageDO.getPackagePath();
        }
    }
}
