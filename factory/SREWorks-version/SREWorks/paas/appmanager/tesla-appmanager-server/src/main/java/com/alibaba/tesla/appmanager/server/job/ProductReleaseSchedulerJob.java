package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.ProductReleaseProvider;
import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.constants.ProductReleaseConstant;
import com.alibaba.tesla.appmanager.common.enums.ProductReleaseSchedulerTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.ProductReleaseTaskStatusEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.service.GitService;
import com.alibaba.tesla.appmanager.domain.req.git.GitCloneReq;
import com.alibaba.tesla.appmanager.domain.req.productrelease.CheckProductReleaseTaskReq;
import com.alibaba.tesla.appmanager.domain.req.productrelease.CreateAppPackageTaskInProductReleaseTaskReq;
import com.alibaba.tesla.appmanager.domain.req.productrelease.CreateProductReleaseTaskReq;
import com.alibaba.tesla.appmanager.domain.res.productrelease.CheckProductReleaseTaskRes;
import com.alibaba.tesla.appmanager.domain.res.productrelease.CreateAppPackageTaskInProductReleaseTaskRes;
import com.alibaba.tesla.appmanager.domain.res.productrelease.CreateProductReleaseTaskRes;
import com.alibaba.tesla.appmanager.server.repository.domain.*;
import com.alibaba.tesla.appmanager.server.service.productrelease.ProductReleaseService;
import com.alibaba.tesla.appmanager.server.service.productrelease.business.ProductReleaseBO;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 产品发布版本计划调度器 Job
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class ProductReleaseSchedulerJob {

    /**
     * 存储每个 task 对应的当前 task executor (shedlock)
     */
    private final ConcurrentMap<String, ThreadPoolTaskScheduler> taskExecutorMap = new ConcurrentHashMap<>();

    /**
     * 存储每个 task 对应的当前 cron 配置值，用于检测定时配置是否存在变动
     */
    private final ConcurrentMap<String, String> taskExecutorCronValueMap = new ConcurrentHashMap<>();

    /**
     * Task Executor Lock
     */
    private final Object taskExecutorLock = new Object();

    @Autowired
    private ProductReleaseProvider productReleaseProvider;

    @Autowired
    private ProductReleaseService productReleaseService;

    @Autowired
    private GitService gitService;

    @Autowired
    private LockProvider lockProvider;

    @Autowired
    private PackageProperties packageProperties;

    @Autowired
    private SystemProperties systemProperties;

    /**
     * 手工触发指定 product + release 的执行计划
     *
     * @param productId 产品 ID
     * @param releaseId 发布版本 ID
     */
    public void trigger(String productId, String releaseId) {
        if (!systemProperties.isEnableProductTaskExecutor()) {
            return;
        }

        ProductReleaseSchedulerDO scheduler = productReleaseService.getScheduler(productId, releaseId);
        ProductReleaseBO productReleaseBO = productReleaseService.get(productId, releaseId);
        if (productReleaseBO == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot get full information|productId=%s|releaseId=%s", productId, releaseId));
        }
        String key = keyGenerator(scheduler);
        ThreadPoolTaskScheduler executor = taskExecutorMap.get(key);
        if (executor == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot get task executor|productId=%s|releaseId=%s", productId, releaseId));
        }

        // 提交任务
        RunProductReleaseSchedulerTask task = new RunProductReleaseSchedulerTask(
                RunProductReleaseSchedulerTaskStorage.builder()
                        .key(key)
                        .lockProvider(lockProvider)
                        .schedulerType(scheduler.getSchedulerType())
                        .schedulerValue(scheduler.getSchedulerValue())
                        .productId(productId)
                        .releaseId(releaseId)
                        .product(productReleaseBO.getProduct())
                        .release(productReleaseBO.getRelease())
                        .productReleaseRel(productReleaseBO.getProductReleaseRel())
                        .productReleaseScheduler(scheduler)
                        .productReleaseAppRelList(productReleaseBO.getAppRelList())
                        .build()
        );
        executor.submit(task);
        log.info("product release task has triggered manually|productId={}|releaseId={}", productId, releaseId);
    }

    /**
     * 启动时初始化，外加定时触发更新增量数据
     */
    @Scheduled(cron = "${appmanager.cron-job.product-release-scheduler:0/10 * * * * *}")
    @SchedulerLock(name = "productReleaseSchedulerJob")
    public void run() {
        if (!systemProperties.isEnableProductTaskExecutor()) {
            return;
        }

        List<ProductReleaseSchedulerDO> schedulers = productReleaseService.listScheduler();
        Set<String> keySet = schedulers.stream()
                .map(ProductReleaseSchedulerJob::keyGenerator)
                .collect(Collectors.toSet());
        for (ProductReleaseSchedulerDO scheduler : schedulers) {
            String productId = scheduler.getProductId();
            String releaseId = scheduler.getReleaseId();
            String key = keyGenerator(scheduler);
            String cronValue = cronValueGenerator(scheduler);

            // 非定时直接跳过
            if (!ProductReleaseSchedulerTypeEnum.CRON.toString().equals(scheduler.getSchedulerType())) {
                continue;
            }

            // 不需要变动则直接继续
            if (cronValue.equals(taskExecutorCronValueMap.get(key))) {
                continue;
            }

            // 准备预置数据
            ProductReleaseBO productReleaseBO = productReleaseService.get(productId, releaseId);
            if (productReleaseBO == null) {
                log.warn("cannot find full product release data, skip|productId={}|releaseId={}",
                        productId, releaseId);
                continue;
            }

            // 需要变动 task executor 进行更新
            synchronized (taskExecutorLock) {

                // double check
                if (cronValue.equals(taskExecutorCronValueMap.get(key))) {
                    continue;
                }

                try {
                    ThreadPoolTaskScheduler executor = taskExecutorMap.get(key);
                    if (executor != null) {
                        taskExecutorMap.remove(key);
                        try {
                            executor.shutdown();
                        } catch (Exception e) {
                            log.error("shutdown product release scheduler failed|config={}|exception={}",
                                    JSONObject.toJSONString(scheduler), ExceptionUtils.getStackTrace(e));
                        }
                    }

                    // 创建线程池并更新 Map
                    ThreadPoolTaskScheduler newExecutor = new ThreadPoolTaskScheduler();
                    newExecutor.setPoolSize(10);
                    newExecutor.setThreadFactory(r -> new Thread(r, "product-release-scheduler-" + r.hashCode()));
                    newExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
                    newExecutor.initialize();
                    RunProductReleaseSchedulerTask newTask = new RunProductReleaseSchedulerTask(
                            RunProductReleaseSchedulerTaskStorage.builder()
                                    .key(key)
                                    .lockProvider(lockProvider)
                                    .schedulerType(scheduler.getSchedulerType())
                                    .schedulerValue(scheduler.getSchedulerValue())
                                    .productId(productId)
                                    .releaseId(releaseId)
                                    .product(productReleaseBO.getProduct())
                                    .release(productReleaseBO.getRelease())
                                    .productReleaseRel(productReleaseBO.getProductReleaseRel())
                                    .productReleaseScheduler(scheduler)
                                    .productReleaseAppRelList(productReleaseBO.getAppRelList())
                                    .build()
                    );
                    newExecutor.schedule(newTask, new CronTrigger(scheduler.getSchedulerValue()));
                    taskExecutorMap.put(key, newExecutor);
                    taskExecutorCronValueMap.put(key, cronValue);
                    log.info("task executor has launched|key={}|config={}", key, JSONObject.toJSONString(scheduler));
                } catch (Exception e) {
                    log.error("cannot update task executor with scheduler configuration|config={}|exception={}",
                            JSONObject.toJSONString(scheduler), ExceptionUtils.getStackTrace(e));
                }
            }
        }

        // 删除所有已经不在 DB 中的 executors
        HashSet<String> executorKeySet = new HashSet<>(taskExecutorCronValueMap.keySet());
        executorKeySet.removeAll(keySet);
        if (executorKeySet.size() > 0) {
            synchronized (taskExecutorLock) {
                for (String key : executorKeySet) {
                    ThreadPoolTaskScheduler executor = taskExecutorMap.get(key);
                    if (executor != null) {
                        executor.shutdown();
                    }
                    taskExecutorMap.remove(key);
                    taskExecutorCronValueMap.remove(key);
                    log.info("task executor has shutdown now because of useless|key={}", key);
                }
            }
        }
    }

    /**
     * Task Executor Key 生成器
     *
     * @param record 计划调度记录
     * @return Key String
     */
    private static String keyGenerator(ProductReleaseSchedulerDO record) {
        return String.format("%s-%s", record.getProductId(), record.getReleaseId());
    }

    /**
     * Cron Value 生成器
     *
     * @param record 计划调度记录
     * @return Key String
     */
    private static String cronValueGenerator(ProductReleaseSchedulerDO record) {
        return String.format("%s-%s", record.getSchedulerType(), record.getSchedulerValue());
    }

    /**
     * 生成锁名称
     *
     * @param key Key
     * @return 锁名称
     */
    private static String lockNameGenerator(String key) {
        return "product-release-" + key;
    }

    /**
     * 执行单次 Product Release Scheduler 任务 (调度侧)
     */
    class RunProductReleaseSchedulerTask implements Runnable {

        private final RunProductReleaseSchedulerTaskStorage storage;

        RunProductReleaseSchedulerTask(RunProductReleaseSchedulerTaskStorage storage) {
            this.storage = storage;
        }

        @Override
        public void run() {
            try {
                execute();
            } catch (Exception e) {
                log.error("run product release scheduler task failed|storage={}|exception={}",
                        JSONObject.toJSONString(storage), ExceptionUtils.getStackTrace(e));
            }
        }

        /**
         * 启动带锁调度执行
         */
        private void execute() {
            LockingTaskExecutor executor = new DefaultLockingTaskExecutor(lockProvider);
            Instant now = Instant.now();
            Duration lockFor;
            // 每次带锁调度，如果成功，那么至少要锁 SCHEDULER_WAIT_SECONDS 时长
            if (packageProperties.getInUnitTest()) {
                lockFor = Duration.ofSeconds(1);
            } else {
                lockFor = Duration.ofSeconds(ProductReleaseConstant.SCHEDULER_WAIT_SECONDS);
            }
            String lockName = lockNameGenerator(storage.getKey());
            LockConfiguration lockConfiguration = new LockConfiguration(now, lockName, lockFor, lockFor);
            executor.executeWithLock(new RunProductReleaseSchedulerWithLockTask(storage), lockConfiguration);
        }
    }

    /**
     * 执行单次 Product Release Scheduler 任务 (执行侧，带锁)
     */
    class RunProductReleaseSchedulerWithLockTask implements Runnable {

        private final RunProductReleaseSchedulerTaskStorage storage;

        RunProductReleaseSchedulerWithLockTask(RunProductReleaseSchedulerTaskStorage storage) {
            this.storage = storage;
        }

        @Override
        public void run() {
            StringBuilder logContent = new StringBuilder();

            // 如果当前存在已经运行的记录，则终止当前任务
            CheckProductReleaseTaskRes checkRes = productReleaseService.checkProductReleaseTask(
                    CheckProductReleaseTaskReq.builder()
                            .productId(storage.getProductId())
                            .releaseId(storage.getReleaseId())
                            .build());
            if (checkRes.isExists()) {
                CreateProductReleaseTaskRes res = productReleaseService.createProductReleaseTask(
                        CreateProductReleaseTaskReq.builder()
                                .logContent(logContent)
                                .productId(storage.getProductId())
                                .releaseId(storage.getReleaseId())
                                .schedulerType(storage.getProductReleaseScheduler().getSchedulerType())
                                .schedulerValue(storage.getProductReleaseScheduler().getSchedulerValue())
                                .status(ProductReleaseTaskStatusEnum.SKIP)
                                .build());
                String taskId = res.getTaskId();
                log.info("check product release task failed, another process is running now, skip|productId={}|" +
                        "releaseId={}|taskId={}", storage.getProductId(), storage.getReleaseId(), taskId);
                return;
            }

            // 创建临时目录用于 Git Clone
            Path cloneDir;
            try {
                cloneDir = Files.createTempDirectory("appmanager_product_release_scheduler");
            } catch (IOException e) {
                throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot create temp directory", e);
            }

            try {
                execute(logContent, cloneDir);
            } catch (Exception e) {
                log.error("run product release scheduler task with lock failed|storage={}|exception={}",
                        JSONObject.toJSONString(storage), ExceptionUtils.getStackTrace(e));
            } finally {
                try {
                    FileUtils.deleteDirectory(cloneDir.toFile());
                } catch (Exception ignored) {
                    log.warn("cannot delete product release git clone dir|directory={}", cloneDir);
                }
            }
        }

        /**
         * 执行 scheduler 任务
         */
        private void execute(StringBuilder logContent, Path cloneDir) {
            // Clone Git
            gitService.cloneRepo(logContent, GitCloneReq.builder()
                    .repo(storage.getProduct().getBaselineGitAddress())
                    .ciAccount(storage.getProduct().getBaselineGitUser())
                    .ciToken(storage.getProduct().getBaselineGitToken())
                    .keepGitFiles(true)
                    .build(), cloneDir);

            // 创建 Task 记录
            CreateProductReleaseTaskRes res = productReleaseService.createProductReleaseTask(
                    CreateProductReleaseTaskReq.builder()
                            .logContent(logContent)
                            .productId(storage.getProductId())
                            .releaseId(storage.getReleaseId())
                            .schedulerType(storage.getProductReleaseScheduler().getSchedulerType())
                            .schedulerValue(storage.getProductReleaseScheduler().getSchedulerValue())
                            .status(ProductReleaseTaskStatusEnum.PENDING)
                            .build());
            String taskId = res.getTaskId();

            // 遍历所有当前关联的应用列表，进行应用包任务新建
            for (ProductReleaseAppRelDO item : storage.getProductReleaseAppRelList()) {
                String branch = item.getBaselineGitBranch();
                String appId = item.getAppId();
                String tag = storage.getProductReleaseRel().getAppPackageTag();
                if (StringUtils.isNotEmpty(item.getTag())) {
                    tag = item.getTag();
                }
                String buildPath = item.getBaselineBuildPath();
                CreateAppPackageTaskInProductReleaseTaskRes appPackageTaskRes = productReleaseProvider
                        .createAppPackageTaskInProductReleaseTask(
                                CreateAppPackageTaskInProductReleaseTaskReq.builder()
                                        .logContent(logContent)
                                        .productId(storage.getProductId())
                                        .releaseId(storage.getReleaseId())
                                        .taskId(taskId)
                                        .appId(appId)
                                        .tag(tag)
                                        .dir(cloneDir)
                                        .branch(branch)
                                        .buildPath(buildPath)
                                        .schedulerType(storage.getProductReleaseScheduler().getSchedulerType())
                                        .schedulerValue(storage.getProductReleaseScheduler().getSchedulerValue())
                                        .build());
                log.info("create app package task in product release task success|taskId={}|appPackageTaskId={}|" +
                                "productId={}|releaseId={}|appId={}|tag={}|dir={}|branch={}|buildPath={}",
                        taskId, appPackageTaskRes.getAppPackageTaskId(), storage.getProductId(), storage.getReleaseId(),
                        appId, tag, cloneDir.toString(), branch, buildPath);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            }

            // 标记任务为运行状态
            productReleaseService.markProductReleaseTaskStatus(taskId,
                    ProductReleaseTaskStatusEnum.PENDING, ProductReleaseTaskStatusEnum.RUNNING);
            log.info("all product release app package task has generated|productId={}|releaseId={}",
                    storage.getProductId(), storage.getReleaseId());
        }
    }

    /**
     * 用于主线程和子线程交互结果的对象
     *
     * @author yaoxing.gyx@alibaba-inc.com
     */
    @Data
    @Builder
    static class RunProductReleaseSchedulerTaskStorage {

        /**
         * 唯一标识 Key
         */
        private String key;

        /**
         * 锁提供者
         */
        private LockProvider lockProvider;

        /**
         * 调度类型
         */
        private String schedulerType;

        /**
         * 调度值
         */
        private String schedulerValue;

        /**
         * 产品 ID
         */
        private String productId;

        /**
         * 发布版本 ID
         */
        private String releaseId;

        /**
         * 产品详情
         */
        private ProductDO product;

        /**
         * 发布版本详情
         */
        private ReleaseDO release;

        /**
         * 产品发布版本引用对象
         */
        private ProductReleaseRelDO productReleaseRel;

        /**
         * 产品发布版本计划调度信息对象
         */
        private ProductReleaseSchedulerDO productReleaseScheduler;

        /**
         * 产品发布版本关联应用信息列表
         */
        private List<ProductReleaseAppRelDO> productReleaseAppRelList;
    }
}
