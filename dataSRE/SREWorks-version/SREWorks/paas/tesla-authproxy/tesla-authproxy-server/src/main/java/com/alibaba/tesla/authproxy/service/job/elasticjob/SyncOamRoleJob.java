package com.alibaba.tesla.authproxy.service.job.elasticjob;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.RoleDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.mapper.RoleMapper;
import com.alibaba.tesla.authproxy.model.vo.ImportPermissionsVO;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.TeslaUtils;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ElasticJob 定时调度 - 同步 roles 表数据到 OAM 角色中
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class SyncOamRoleJob implements SimpleJob {

    private static final String LOG_PRE = "[" + SyncOamRoleJob.class.getSimpleName() + "] ";

    static final Map<String, String> ROLE_DESCRIPTION_MAP = new HashMap<String, String>() {{
        put("eodps_admin", "MaxCompute Admin");
        put("graphcompute_admin", "GraphCompute Admin");
        put("minilvs_admin", "MiniLVS Admin");
        put("pai_admin", "PAI Admin");
        put("quickbi_admin", "QuickBI Admin");
        put("asap_admin", "AIMaster Admin");
        put("biggraph_admin", "BigGraph Admin");
        put("common_admin", "Common Admin");
        put("dataphin_admin", "DataPhin Admin");
        put("datatunnel_admin", "DataHub Admin");
        put("dtboost_admin", "DtBoost Admin");
        put("elasticsearch_admin", "Elasticsearch Admin");
        put("hologres_admin", "Hologres Admin");
        put("iplus_admin", "I+ Admin");
        put("minirds_admin", "MiniRDS Admin");
        put("streamcompute_admin", "StreamCompute Admin");
        put("base_admin", "DataWorks Admin");
        put("bcc_admin", "ABM Admin");
    }};

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private AuthPolicy authPolicy;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private TeslaUserService teslaUserService;

    @Override
    public void execute(ShardingContext shardingContext) {
//        if (Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())) {
//            return;
//        }
//        try {
//            if (!TeslaUtils.isPrimaryCluster()) {
//                log.info("[ElasticJob-SyncOamRole] Not primary cluster, skip");
//                return;
//            }
//        } catch (UnknownHostException ignored) {}
//        run();
    }

    /**
     * 启动同步
     */
    public void run() {
        // 当 DB 中获取当前的所有角色 (除 guest)
//        List<RoleDO> localRoles = roleMapper
//            .findAllByTenantIdAndLocale(Constants.DEFAULT_TENANT_ID, Constants.DEFAULT_LOCALE)
//            .stream()
//            .filter(p -> !p.getRoleId().contains(Constants.DEFAULT_GUEST_ROLE))
//            .collect(Collectors.toList());
//        try {
//            syncToOamRole(localRoles);
//        } catch (Exception e) {
//            log.error(LOG_PRE + "Sync to oam roles failed, exception={}", ExceptionUtils.getStackTrace(e));
//        }
    }

    /**
     * 将本地的 role 同步到 OAM 中
     */
    public void syncToOamRole(List<RoleDO> localRoles) {
        // 从 OAM 中获取当前的所有角色
//        UserDO userDo = teslaUserService.getUserByLoginName(authProperties.getAasSuperUser());
//        ImportPermissionsVO param = ImportPermissionsVO.builder()
//            .roles(localRoles.stream()
//                .map(p -> ImportPermissionsVO.Role.builder()
//                    .roleName(Constants.ABM_ROLE_PREFIX_API + p.getRoleId().replace(":", "_"))
//                    .description(ROLE_DESCRIPTION_MAP
//                        .getOrDefault(p.getRoleId().replace(":", "_"), p.getRoleId().replace(":", "_")))
//                    .build())
//                .collect(Collectors.toList()))
//            .prefix(Constants.ABM_ROLE_PREFIX_API)
//            .ignorePermissions(true)
//            .build();
//        authPolicy.getAuthServiceManager().importPermissions(userDo, param);
//        log.info(LOG_PRE + "All oam roles has synced, param={}", TeslaGsonUtil.toJson(param));
//        syncFromOamRoleBindingUser(localRoles);
    }

    /**
     * 将 OAM 中角色绑定的用户同步到本地表中
     */
    public void syncFromOamRoleBindingUser(List<RoleDO> localRoles) {
//        UserDO userDo = teslaUserService.getUserByLoginName(authProperties.getAasSuperUser());
//        authPolicy.getAuthServiceManager().syncFromRoleOperator(userDo, localRoles);
//        log.info(LOG_PRE + "All oam role bindings has synced");
    }
}
