package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyErrorCode;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyException;
import com.alibaba.tesla.authproxy.model.RoleDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.UserRoleRelDO;
import com.alibaba.tesla.authproxy.model.mapper.RoleMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserRoleRelMapper;
import com.alibaba.tesla.authproxy.model.repository.RoleRepository;
import com.alibaba.tesla.authproxy.service.RoleService;
import com.alibaba.tesla.authproxy.service.ao.*;
import com.alibaba.tesla.authproxy.util.DatabaseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class RoleServiceImpl implements RoleService {

    private static final String LOG_PRE = "[" + RoleServiceImpl.class.getSimpleName() + "] ";

    private static final String SPLITTER = ":";

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRelMapper userRoleRelMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取指定角色对应了哪些用户
     *
     * @param condition 查询条件
     * @return
     */
    @Override
    public UserListResultAO listRoleUsers(RoleUserListConditionAO condition) {
        String locale = condition.getLocale();
        String tenantId = condition.getTenantId();
        String roleId = condition.getRoleId();
        List<UserRoleRelDO> rels = userRoleRelMapper.findUserByTenantIdAndRoleId(locale, tenantId, roleId);
        List<String> userIds = rels.stream().map(UserRoleRelDO::getUserId).collect(Collectors.toList());
        List<UserDO> users = new ArrayList<>();
        if (userIds.size() > 0) {
            users = userMapper.findAllByUserIdIn(userIds);
        }

        // 组装数据
        UserListResultAO result = new UserListResultAO();
        result.setTotal((long) users.size());
        result.setItems(users.stream()
            .map(UserGetResultAO::from)
            .collect(Collectors.toList()));
        return result;
    }

    /**
     * 获取指定角色对应了哪些部门
     *
     * @param condition 查询条件
     * @return
     */
    @Override
    public RoleDepartmentListResultAO listRoleDepartments(RoleDepartmentListConditionAO condition) {
        String locale = condition.getLocale();
        String tenantId = condition.getTenantId();
        String roleId = condition.getRoleId();
        List<UserRoleRelDO> rels = userRoleRelMapper.findDepIdByTenantIdAndRoleId(locale, tenantId, roleId);

        // 组装数据
        RoleDepartmentListResultAO result = new RoleDepartmentListResultAO();
        List<RoleDepartmentGetResultAO> items = rels.stream()
            .map(RoleDepartmentGetResultAO::from)
            .collect(Collectors.toList());
        result.setTotal((long) items.size());
        result.setItems(items);
        return result;
    }

    /**
     * 根据指定的查询条件，查询当前的角色列表
     *
     * @param condition 查询条件
     */
    @Override
    public RoleListResultAO list(RoleListConditionAO condition) {
        // 拉取角色数据
        Specification<RoleDO> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("locale"), condition.getLocale()));
            predicates.add(cb.equal(root.get("tenantId"), condition.getTenantId()));
            if (!StringUtils.isEmpty(condition.getAppId())) {
                predicates.add(cb.like(root.get("roleId"), condition.getAppId() + ":%"));
            }
            if (!StringUtils.isEmpty(condition.getRoleId())) {
                predicates.add(cb.like(root.get("roleId"), "%" + condition.getRoleId() + "%"));
            }
            if (!StringUtils.isEmpty(condition.getName())) {
                predicates.add(cb.like(root.get("name"), "%" + condition.getName() + "%"));
            }
            return cb.createQuery(RoleDO.class)
                .select(root)
                .where(predicates.toArray(new Predicate[]{}))
                .getRestriction();
        };
        PageRequest pageable = DatabaseUtil.getPageRequest(condition);
        Page<RoleDO> roles = roleRepository.findAll(spec, pageable);

        // 组装数据
        RoleListResultAO result = new RoleListResultAO();
        result.setTotal(roles.getTotalElements());
        result.setItems(roles.getContent().stream()
            .map(RoleGetResultAO::from)
            .collect(Collectors.toList()));
        return result;
    }

    /**
     * 获取指定角色的详细信息
     *
     * @param condition 查询条件
     */
    @Override
    public RoleGetResultAO get(RoleGetConditionAO condition) {
        String tenantId = condition.getTenantId();
        String roleId = condition.getRoleId();
        String appId = condition.getAppId();
        String locale = condition.getLocale();
        if (!roleId.startsWith(appId + SPLITTER)) {
            throw new AuthProxyException(AuthProxyErrorCode.INVALID_USER_ARGS,
                "Invalid roleId, please use appId: as roleId prefix");
        }

        RoleDO role = roleMapper.findFirstByTenantIdAndRoleIdAndLocale(tenantId, roleId, locale);
        if (role == null) {
            throw new AuthProxyException(AuthProxyErrorCode.INVALID_USER_ARGS,
                String.format("Cannot find role %s in tenant %s", roleId, tenantId));
        }
        return RoleGetResultAO.from(role);
    }

    /**
     * 创建一个角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(RoleItemAO param) {
        String tenantId = param.getTenantId();
        String appId = param.getAppId();
        String roleId = param.getRoleId();
        if (!roleId.startsWith(appId + SPLITTER)) {
            throw new AuthProxyException(AuthProxyErrorCode.INVALID_USER_ARGS,
                "Invalid roleId, please use appId: as roleId prefix");
        }

        // 针对每个国际化记录进行新建或更新
        for (RoleItemAO.Option option : param.getOptions()) {
            String locale = option.getLocale();

            // 简单判定
            if (roleMapper.findFirstByTenantIdAndRoleIdAndLocale(tenantId, roleId, locale) != null) {
                continue;
            }

            RoleDO role = RoleDO.builder()
                .tenantId(tenantId)
                .roleId(roleId)
                .locale(locale)
                .name(option.getName())
                .description(option.getDescription())
                .build();
            roleMapper.insert(role);
        }
    }

    /**
     * 更新一个角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleItemAO param) {
        String tenantId = param.getTenantId();
        String appId = param.getAppId();
        String roleId = param.getRoleId();
        if (!roleId.startsWith(appId + SPLITTER)) {
            throw new AuthProxyException(AuthProxyErrorCode.INVALID_USER_ARGS, "appId and roleId are mismatched");
        }
        if (roleMapper.findAllByTenantIdAndRoleId(tenantId, roleId).size() == 0) {
            throw new AuthProxyException(AuthProxyErrorCode.INVALID_USER_ARGS,
                String.format("Cannot find specified roleId %s in tenant %s", roleId, tenantId));
        }
        List<String> availableLanguages = Arrays.asList(authProperties.getAvailableLanguages().split(","));

        // 针对每个国际化记录进行新建或更新
        for (RoleItemAO.Option option : param.getOptions()) {
            String locale = option.getLocale();
            if (!availableLanguages.contains(locale)) {
                throw new AuthProxyException(AuthProxyErrorCode.INVALID_USER_ARGS,
                    String.format("Locale %s is forbidden, available languages are %s", locale, availableLanguages));
            }

            // 当该语言尚未有记录时，直接新增
            RoleDO role = roleMapper.findFirstByTenantIdAndRoleIdAndLocale(tenantId, roleId, locale);
            if (role == null) {
                role = RoleDO.builder()
                    .tenantId(tenantId)
                    .roleId(roleId)
                    .locale(locale)
                    .name(option.getName())
                    .description(option.getDescription())
                    .build();
                roleMapper.insert(role);
            } else {
                role.setName(option.getName());
                role.setDescription(option.getDescription());
                roleMapper.update(role);
            }
        }
    }

    /**
     * 删除一个角色
     *
     * @return 删除个数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(String tenantId, String roleId) {
        return roleMapper.deleteAllByTenantIdAndRoleId(tenantId, roleId);
    }

    /**
     * 授予指定的若干用户（增量）
     *
     * @param condition 增量用户
     * @return
     */
    @Override
    @Transactional
    public void assignUsers(RoleUserAssigningConditionAO condition) {
        for (String userId : condition.getUserIds()) {
            userRoleRelMapper.insert(UserRoleRelDO.builder()
                .tenantId(condition.getTenantId())
                .userId(userId)
                .roleId(condition.getRoleId())
                .build());
            log.info("assign role {} to user {} success", condition.getRoleId(), userId);
        }
    }
}
