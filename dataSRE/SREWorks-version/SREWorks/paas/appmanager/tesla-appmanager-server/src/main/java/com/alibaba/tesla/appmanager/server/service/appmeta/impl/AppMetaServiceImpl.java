package com.alibaba.tesla.appmanager.server.service.appmeta.impl;

import com.alibaba.tesla.appmanager.auth.service.PermissionService;
import com.alibaba.tesla.appmanager.auth.util.PermissionUtil;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.req.permission.CheckPermissionReq;
import com.alibaba.tesla.appmanager.domain.res.permission.CheckPermissionRes;
import com.alibaba.tesla.appmanager.server.repository.AppMetaRepository;
import com.alibaba.tesla.appmanager.server.repository.AppOptionRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AppOptionQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppOptionDO;
import com.alibaba.tesla.appmanager.server.service.appmeta.AppMetaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 应用元信息服务
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Service
@Slf4j
public class AppMetaServiceImpl implements AppMetaService {

    @Autowired
    private AppMetaRepository appMetaRepository;

    @Autowired
    private AppOptionRepository appOptionRepository;

    @Autowired
    private PermissionService permissionService;

    /**
     * 根据条件过滤应用元信息
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public Pagination<AppMetaDO> list(AppMetaQueryCondition condition) {
        String optionKey = condition.getOptionKey();
        String optionValue = condition.getOptionValue();
        if (StringUtils.isNotEmpty(optionKey)
                && StringUtils.isNotEmpty(optionValue)
                && StringUtils.isEmpty(condition.getAppId())) {
            List<AppOptionDO> options = appOptionRepository.selectByCondition(AppOptionQueryCondition.builder()
                    .key(optionKey)
                    .value(optionValue)
                    .page(condition.getPage())
                    .pageSize(condition.getPageSize())
                    .build());
            if (options.size() == 0) {
                return Pagination.valueOf(new ArrayList<>(), Function.identity());
            }

            // 将 option 映射转为 appId 列表
            List<String> appIdList = options.stream().map(AppOptionDO::getAppId).collect(Collectors.toList());
            condition.setOptionKey(null);
            condition.setOptionValue(null);
            condition.setAppIdList(appIdList);
        }
        List<AppMetaDO> page = appMetaRepository.selectByCondition(condition);
        return Pagination.valueOf(page, Function.identity());
    }

    /**
     * 根据条件查询某个应用元信息
     *
     * @param condition 查询条件
     * @return AppMetaDO
     */
    @Override
    public AppMetaDO get(AppMetaQueryCondition condition) {
        List<AppMetaDO> records = appMetaRepository.selectByCondition(condition);
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }
        return records.get(0);
    }

    /**
     * 更新指定的应用元信息
     *
     * @param record App Meta 记录
     */
    @Override
    public int update(AppMetaDO record, AppMetaQueryCondition condition) {
        return appMetaRepository.updateByCondition(record, condition);
    }

    /**
     * 更新指定的应用元信息
     *
     * @param record App Meta 记录
     */
    @Override
    public int create(AppMetaDO record) {
        return appMetaRepository.insert(record);
    }

    /**
     * 根据条件删除应用元信息
     *
     * @param condition 查询条件
     */
    @Override
    public int delete(AppMetaQueryCondition condition) {
        return appMetaRepository.deleteByCondition(condition);
    }

    /**
     * 获取指定 operator 用户有权限的所有应用 ID 列表
     *
     * @param operator 用户
     * @return List of AppID
     */
    @Override
    public List<String> listUserPermittedApp(String operator) {
        CheckPermissionReq request = CheckPermissionReq.builder()
                .checkPermissions(generateEntireAppPermissions())
                .operator(operator)
                .build();
        CheckPermissionRes result = permissionService.checkPermission(request);
        return extractAppFromPermissions(result.getPermissions());
    }

    /**
     * 获取当前系统的全量应用，并产出应用 ID Permission 列表
     *
     * @return 应用 ID Permission 列表
     */
    private List<String> generateEntireAppPermissions() {
        List<String> permissions = new ArrayList<>();
        List<AppMetaDO> records = appMetaRepository.selectByCondition(AppMetaQueryCondition.builder().build());
        records.forEach(record -> permissions.add(PermissionUtil.generate("__app__", record.getAppId(), "manage")));
        return permissions;
    }

    /**
     * 从给定的应用 ID Permission 列表反解出实际的应用 ID 列表
     *
     * @return 应用 ID 列表
     */
    private List<String> extractAppFromPermissions(List<String> permissions) {
        return permissions.stream().map(PermissionUtil::extract).collect(Collectors.toList());
    }
}
