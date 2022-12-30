package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.api.model.CheckPermissionRequest;
import com.alibaba.tesla.authproxy.lib.exceptions.*;
import com.alibaba.tesla.authproxy.model.*;
import com.alibaba.tesla.authproxy.model.mapper.PermissionResMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserRoleRelMapper;
import com.alibaba.tesla.authproxy.model.vo.*;
import com.alibaba.tesla.authproxy.outbound.oam.OamClient;
import com.alibaba.tesla.authproxy.service.AuthServiceManager;
import com.alibaba.tesla.authproxy.service.TeslaAppService;
import com.alibaba.tesla.authproxy.service.TeslaMenuService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.LocaleUtil;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.common.utils.TeslaResult;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>Title: OamAuthServiceManager.java<／p>
 * <p>Description: OAM 权限认证逻辑处理 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Component("oamAuthServiceManager")
@Slf4j
public class OamAuthServiceManager implements AuthServiceManager {

    private static final String LOG_PRE = "[" + OamAuthServiceManager.class.getSimpleName() + "] ";

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private TeslaUserService teslaUserService;

//    @Autowired
//    private OamClient oamClient;

    @Autowired
    private TeslaMenuService teslaMenuService;

    @Autowired
    private TeslaAppService teslaAppService;

    @Autowired
    private PermissionResMapper permissionResMapper;

//    @Autowired
//    private UserRoleRelMapper userRoleRelMapper;
//
//    @Autowired
//    private LocaleUtil locale;
//
//    private String userKey;
//
//    private String userSecret;

    @Override
    public void initMenuByAppNew(UserDO loginUser, AppMenuVO appMenus) throws ApplicationException {
        List<MenuDO> menuDos = new ArrayList<>();

        for (int i = 0; i < appMenus.getMenus().size(); i++) {
            MenuVO menuVo = appMenus.getMenus().get(i);
            MenuDO menuDo = new MenuDO();
            menuDo.setAppId(appMenus.getAppId());
            menuDo.setMenuUrl(menuVo.getSref());
            menuDo.setMenuTitle(menuVo.getName());
            menuDo.setMenuName(menuVo.getName());
            menuDo.setMenuCode(UUID.randomUUID().toString());
            menuDo.setIsEnable(1);
            menuDo.setHeaderTitleSet(menuVo.getHeaderTitleSet());
            menuDo.setIcon(menuVo.getIcon());

            menuDo.setIdx(i + 1);
            //添加子菜单
            if (null != menuVo.getChildren() && menuVo.getChildren().size() > 0) {
                menuDo.setIsLeaf(0);
                menuDos.add(menuDo);
                addChildren(menuDos, menuDo, menuVo.getChildren());
            } else {
                menuDo.setIsLeaf(1);
                menuDos.add(menuDo);
            }
        }
        teslaMenuService.initMenuByAppNew(appMenus.getAppId(), Long.valueOf(loginUser.getAliyunPk()), menuDos);
    }

    @Override
    public UserExtDO getUserExtInfo(UserDO userDo) {
        return new UserExtDO();
    }

    @Override
    public UserDO getUserByEmpId(String empId) {
        return null;
    }

    @Override
    public List<MenuDoTreeVO> listMenuByRole(UserDO loginUser, String appId) throws ApplicationException {

        log.info("查询角色拥有的菜单数据,aliyunPk[{}],appId[{}]", loginUser.getAliyunPk(), appId);

        return null;

        //ListRoleByOperatorRequest request = new ListRoleByOperatorRequest();
        //request.setOperatorName(loginUser.getAliyunPk());
        //request.setUserType("User");
        //request.setPageIndex(0);
        //request.setPageSize(1000);
        //
        //List<String> userRoleNames = new ArrayList<>();
        //
        ////使用默认角色模式
        //if (StringUtils.isEmpty(authProperties.getPopOamKey()) || "unknown".equals(authProperties.getPopOamKey())) {
        //    AppDO app = teslaAppService.getByAppId(appId);
        //    if (null == app) {
        //        log.error("应用{}不存在", appId);
        //        return null;
        //    }
        //    if (StringUtils.isEmpty(app.getAdminRoleName())) {
        //        log.warn("应用{}的默认角色为空，请进行配置", appId);
        //        return null;
        //    }
        //    userRoleNames.add(app.getAdminRoleName());
        //} else {
        //    log.info("调用OAM接口:ListRoleByOperatorRequest,aliyunPK[{}],appId[{}]", loginUser.getAliyunPk(),
        //        appId);
        //    try {
        //        ListRoleByOperatorResponse response = getAcsClientByUserAk(loginUser).getAcsResponse(request);
        //        log.info("OAM返回结果:ListRoleByOperatorRequest:{}", response.getCode());
        //
        //        List<ListRoleByOperatorResponse.OamRole> oamRoles = response.getData();
        //        log.info("OAM返回角色信息size:{},detail:{}", oamRoles.size(), JSONObject.toJSON(oamRoles));
        //
        //        for (ListRoleByOperatorResponse.OamRole role : oamRoles) {
        //            userRoleNames.add(role.getRoleName());
        //            //为了防止角色名称被修改的情况，这里将roleId这作为查询条件
        //            userRoleNames.add(role.getRoleId());
        //        }
        //    } catch (Exception e) {
        //        log.error("查询菜单信息失败", e);
        //        throw new ApplicationException(TeslaResult.FAILURE, e.getMessage());
        //    }
        //    if (userRoleNames.size() == 0) {
        //        log.warn("OAM返回角色为空");
        //        return null;
        //    }
        //}
        //
        //try {
        //    return teslaMenuService.listMenuByRole(userRoleNames, appId);
        //} catch (Exception e) {
        //    log.error("查询菜单信息失败", e);
        //    throw new ApplicationException(TeslaResult.FAILURE, e.getMessage());
        //}
    }

    @Override
    public List<MenuDoTreeVO> listMenuByApp(UserDO loginUser, String appId) throws ApplicationException {
        log.info("查询应用下所有菜单,aliyunPk[{}],appId[{}]", loginUser.getAliyunPk(), appId);
        try {
            return teslaMenuService.listMenuByApp(appId);
        } catch (Exception e) {
            log.error("查询菜单信息失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, e.getMessage());
        }
    }

    @Override
    public List<TeslaRoleVO> getAllRoles(UserDO loginUser, String appId) throws ApplicationException {
        //log.info("查询所有角色,aliyunPk[{}],appId[{}]", loginUser.getAliyunPk(), appId);
        //ListRolesByOwnerRequest request = new ListRolesByOwnerRequest();
        //request.setRoleOwnerName(String.valueOf(loginUser.getAliyunPk()));
        //request.setRoleOwnerType("User");
        //request.setPageIndex(0);
        //request.setPageSize(1000);
        //
        //List<TeslaRoleVO> ret = new ArrayList<>();
        //
        //if (log.isDebugEnabled()) {
        //    log.debug("Call Oam -> ListRolesByOwnerRequest");
        //}
        //
        //try {
        //    ListRolesByOwnerResponse response = this.getAcsClientByUserAk(loginUser).getAcsResponse(request);
        //    List<ListRolesByOwnerResponse.OamRole> oamRoles = response.getData();
        //    for (ListRolesByOwnerResponse.OamRole role : oamRoles) {
        //        TeslaRoleVO roleVo = new TeslaRoleVO();
        //        roleVo.setRoleCode(role.getRoleId());
        //        roleVo.setRoleName(role.getRoleName());
        //        roleVo.setRoleOwner(role.getOwner());
        //        ret.add(roleVo);
        //    }
        //} catch (Exception e) {
        //    log.error("查询角色失败", e);
        //    throw new ApplicationException(TeslaResult.FAILURE, e.getMessage());
        //}
        //if (log.isDebugEnabled()) {
        //    log.debug("Call Oam -> ListRoleByOperatorRequest:{}", ret.size());
        //}
        //return ret;
        return null;
    }

    @Override
    public String getUserId(String empId) {
        return null;
    }

    @Override
    public boolean checkPermission(UserDO loginUser, String permissionName, String appId)
        throws ApplicationException {
        log.info("验证权限，调用OAM接口:CheckPermissionRequest,aliyunPK[{}],permissionName[{}]", loginUser.getAliyunPk(),
            permissionName);
        //try {
        //    CheckPermissionRequest request = new CheckPermissionRequest();
        //    request.setResource(permissionName);
        //    request.setActionField("read");
        //    request.setAliUid(Long.valueOf(loginUser.getAliyunPk()));
        //
        //    CheckPermissionResponse result = this.getAcsClientByUserAk(loginUser).getAcsResponse(request);
        //
        //    boolean checkRet = result.getData().booleanValue();
        //    log.info("验证权限，调用OAM接口:CheckPermissionRequest,结果{}，permissionName:{}", checkRet, permissionName);
        //    if (log.isDebugEnabled()) {
        //        log.debug("OAM返回结果数据为:{}", JSONObject.toJSONString(result));
        //    }
        //    return checkRet;
        //} catch (ServerException e) {
        //    log.error("验权失败,errorCode:{},errMsg:{}", e.getErrCode(), e.getErrMsg());
        //} catch (Exception e) {
        //    log.error("验权失败", e);
        //}
        return false;
    }

    @Override
    public int batchAddMenu(UserDO loginUser, AppMenuVO appMenus) throws ApplicationException {
        List<MenuDO> menuDos = new ArrayList<MenuDO>();

        if (null == appMenus || null == appMenus.getMenus() || appMenus.getMenus().size() == 0) {
            return 0;
        }

        for (MenuVO menuVo : appMenus.getMenus()) {
            MenuDO menuDo = new MenuDO();
            menuDo.setAppId(appMenus.getAppId());
            menuDo.setMenuUrl(menuVo.getSref());
            menuDo.setMenuTitle(menuVo.getName());
            menuDo.setMenuName(menuVo.getName());
            menuDo.setMenuCode(UUID.randomUUID().toString());
            menuDo.setIsEnable(1);
            menuDo.setIcon(menuVo.getIcon());
            menuDo.setHeaderTitleSet(menuVo.getHeaderTitleSet());
            //添加子菜单
            if (null != menuVo.getChildren() && menuVo.getChildren().size() > 0) {
                menuDo.setIsLeaf(0);
                menuDos.add(menuDo);
                addChildren(menuDos, menuDo, menuVo.getChildren());
            } else {
                menuDo.setIsLeaf(1);
                menuDos.add(menuDo);
            }
        }
        return teslaMenuService.batchInsert(menuDos);
    }

    /**
     * 递归添加子菜单
     *
     * @param menuDos
     * @param parent
     * @param childrens
     */
    private void addChildren(List<MenuDO> menuDos, MenuDO parent, List<MenuVO> childrens) {

        for (int i = 0; i < childrens.size(); i++) {
            MenuVO menu = childrens.get(i);
            MenuDO menuDo = new MenuDO();
            menuDo.setAppId(parent.getAppId());
            menuDo.setMenuUrl(menu.getSref());
            menuDo.setMenuTitle(menu.getName());
            menuDo.setMenuName(menu.getName());
            menuDo.setIsEnable(1);
            menuDo.setMenuCode(UUID.randomUUID().toString());
            menuDo.setParentCode(parent.getMenuCode());
            menuDo.setIcon(menu.getIcon());
            menuDo.setIdx(Integer.valueOf(String.valueOf(parent.getIdx()) + String.valueOf(i + 1)));
            menuDo.setHeaderTitleSet(menu.getHeaderTitleSet());
            if (null != menu.getChildren() && menu.getChildren().size() > 0) {
                menuDo.setIsLeaf(0);
                menuDos.add(menuDo);
                addChildren(menuDos, menuDo, menu.getChildren());
            } else {
                menuDo.setIsLeaf(1);
                menuDos.add(menuDo);
            }
        }
    }

    @Override
    public Map<String, Object> addRoles(UserDO loginUser, List<TeslaRoleVO> roleVos) throws ApplicationException {

        Map<String, Object> ret = new HashMap<String, Object>();

        //CreateRoleRequest request = new CreateRoleRequest();
        //for (TeslaRoleVO role : roleVos) {
        //    request.setRoleName(role.getRoleName());
        //    request.setDescription(role.getMemo());
        //    request.setRoleType("OAM");
        //    try {
        //        CreateRoleResponse response = this.getAcsClientByUserAk(loginUser).getAcsResponse(request);
        //        ret.put(role.getRoleCode(), response);
        //    } catch (ClientException e) {
        //        log.error("添加角色失败", e);
        //        ret.put(role.getRoleCode(), e.getErrMsg());
        //    } catch (Exception e) {
        //        log.error("添加角色失败", e);
        //        ret.put("result", false);
        //        ret.put("data", e.getMessage());
        //    }
        //}
        return ret;
    }

    @Override
    public Map<String, Object> addPermission(UserDO loginUser, AddPermissionVO addPermission)
        throws ApplicationException {

        Map<String, Object> ret = new HashMap<>();
        //AddRoleCellToRoleRequest addReq = new AddRoleCellToRoleRequest();
        //addReq.setRoleName(addPermission.getRoleName());
        //addReq.setRoleCellId(UUID.randomUUID().toString());
        //addReq.setActionLists(addPermission.getActionList());
        //addReq.setResource(addPermission.getPermissionName());
        //
        //try {
        //    AddRoleCellToRoleResponse response = this.getAcsClientByUserAk(loginUser).getAcsResponse(addReq);
        //    ret.put("result", true);
        //    ret.put("data", response);
        //} catch (ClientException e) {
        //    log.error("添加角色失败", e);
        //    ret.put("result", false);
        //    ret.put("data", e.getErrMsg());
        //} catch (Exception e) {
        //    log.error("添加角色失败", e);
        //    ret.put("result", false);
        //    ret.put("data", e.getMessage());
        //}

        return ret;
    }

    @Override
    public boolean delPermission(String userEmployeeId, String appId, String permissionName) throws ApplicationException {
        UserDO teslaUser = teslaUserService.getUserByAliyunId(userEmployeeId);
        Assert.notNull(teslaUser, userEmployeeId + " not exits, can't delete permission.");

        AppDO appDo = teslaAppService.getByAppId(appId);
        Assert.notNull(appDo, "app " + appId + " not exits.");

        try {
            /**
             * 始终使用admin用户去进行oam权限管理操作
             */
            UserDO adminUser = this.getAdminRoleUserDo(teslaUser.getAliyunPk(), teslaUser.getBid());
            //oamClient.removeRoleCellFromRole(adminUser.getAliyunPk(), adminUser.getBid(), permissionName);
        } catch (AuthProxyThirdPartyError e) {
            log.error("delete permission failed", e);
            throw new ApplicationException(500, "init permission " + permissionName + " failed: "
                    + e.getComponentName() + "_" + e.getLocalizedMessage());
        }
        return true;
    }

    /**
     * 根据PK和ticket查询用户AK
     *
     * @param loginUser
     */
    private IAcsClient getAcsClientByUserAk(UserDO loginUser) throws Exception {

//        if (log.isDebugEnabled()) {
//            log.debug("获取AcsClient客户端，aasPopKey:{},aasPopSecret:{}", authProperties.getAasKey(),
//                authProperties.getAasSecret());
//        }
        IAcsClient ret = null;

        //AkClient akClient = new AkClient();
        //akClient.setBaseUrl(authProperties.getAasGetAkUrl());
        //akClient.setKey(authProperties.getAasKey());
        //akClient.setSecret(authProperties.getAasSecret());
        //
        //log.info("调用AAS接口，aliyunPk:{}获取用户的ak信息", loginUser.getAliyunPk());
        //
        //List<AccessKeyInfo> userAks = null;
        //try {
        //    userAks = akClient.getUserAccessKeyList(Long.valueOf(loginUser.getAliyunPk()), loginUser.getBid(),
        //        AccessKeyStatus.ENABLED, AccessKeyType.SYMMETRIC);
        //} catch (Exception e) {
        //    log.error("获取用户AK异常，参数aliyunPk:{},bid:{}", loginUser.getAliyunPk(), loginUser.getBid(), e);
        //    throw e;
        //}
        //if (null != userAks && userAks.size() > 0) {
        //    userKey = userAks.get(0).getKey();
        //    userSecret = userAks.get(0).getSecret();
        //    log.info("获取到的用户AK信息为:{}", JSONObject.toJSONString(userAks));
        //} else {
        //    log.error("用户的ak信息为空");
        //    throw new Exception("用户AK为空");
        //}
        //try {
        //    DefaultProfile.addEndpoint(authProperties.getPopEndpointName(), authProperties.getPopRegionId(), "Oam",
        //        authProperties.getPopOamDomain());
        //    IClientProfile profile = DefaultProfile.getProfile(authProperties.getPopRegionId(), userKey, userSecret);
        //    ret = new DefaultAcsClient(profile);
        //} catch (Exception e) {
        //    log.error("获取用户AK失败", e);
        //    throw e;
        //}
        return ret;
    }

    @Override
    public List<UserPermissionsVO> getPermissionsByUserId(UserDO loginUser, String appId)
        throws ApplicationException {
        List<UserPermissionsVO> ret = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        for (PermissionResDO item : permissionResMapper.query(params)) {
            UserPermissionsVO userPermission = new UserPermissionsVO();
            userPermission.setPermissionName(item.getPermissionId());
            userPermission.setAccessible(true);
            userPermission.setReqPath(item.getResPath());
            userPermission.setApplyLink("http://fakeurl");
            userPermission.setName(item.getMemo());
            ret.add(userPermission);
        }
        return ret;
    }

    @Override
    public List<PermissionDO> getDataPermission(UserDO loginUser, String dataName, String appId)
        throws ApplicationException {
        log.info("查询数据权限:getDataPermission,aliyunPK[{}],permissionName[{}]", loginUser.getAliyunPk(), dataName);

        List<PermissionDO> ret = new ArrayList<>();

        //ListRoleByOperatorRequest lrbor = new ListRoleByOperatorRequest();
        //lrbor.setOperatorName(loginUser.getLoginName());
        //lrbor.setUserType("user");
        //lrbor.setPageIndex(0);
        //lrbor.setPageSize(1000);
        //ListRoleByOperatorResponse userRoles = null;
        //try {
        //    userRoles = this.getAcsClientByUserAk(loginUser).getAcsResponse(lrbor);
        //    for (ListRoleByOperatorResponse.OamRole role : userRoles.getData()) {
        //
        //    }
        //} catch (Exception e) {
        //    log.error("查询角色资源权限失败", e);
        //    throw new ApplicationException(TeslaResult.FAILURE, "error.permission.list");
        //}
        //
        //ListRoleCellsByRoleNameRequest request = new ListRoleCellsByRoleNameRequest();
        //request.setRoleName(userRoles.getData().get(0).getRoleName());
        //request.setPageIndex(0);
        //request.setPageSize(1000);
        //
        //List<PermissionDO> ret = new ArrayList<PermissionDO>();
        //try {
        //    ListRoleCellsByRoleNameResponse response = this.getAcsClientByUserAk(loginUser).getAcsResponse(request);
        //    List<ListRoleCellsByRoleNameResponse.OamRoleCell> oamRoleCells = response.getData();
        //    for (ListRoleCellsByRoleNameResponse.OamRoleCell roleCell : oamRoleCells) {
        //        if (!roleCell.getResource().equals(dataName)) {
        //            continue;
        //        }
        //        PermissionDO permission = new PermissionDO();
        //        permission.setAppId(appId);
        //        permission.setPermissionCode(roleCell.getRoleCellId());
        //        permission.setPermissionName(roleCell.getResource());
        //        permission.setPermissionName(roleCell.getDescription());
        //        permission.setPermissionTitle(roleCell.getDescription());
        //        ret.add(permission);
        //    }
        //} catch (Exception e) {
        //    log.error("查询角色资源权限失败", e);
        //    throw new ApplicationException(TeslaResult.FAILURE, e.getMessage());
        //}
        return ret;
    }

    /**
     * 导入 Permissions 列表
     *
     * @param user  用户信息
     * @param param 权限列表
     */
    @Override
    public void importPermissions(UserDO user, ImportPermissionsVO param) throws AuthProxyException {
        // 获取当前系统中存在哪些角色
//        UserDO adminUser = getAdminRoleUserDo(user.getAliyunPk(), user.getBid());
//        String adminPk = adminUser.getAliyunPk();
//        String adminBid = adminUser.getBid();
        //Set<String> currentRoleSet = oamClient.listRoles(adminPk, adminBid, param.getPrefix())
        //    .stream()
        //    .map(ListRolesResponse.OamRole::getRoleName)
        //    .collect(Collectors.toSet());
        //log.info(LOG_PRE + "Current roles: {}", TeslaGsonUtil.toJson(currentRoleSet));
        //
        //// 针对不存在的角色进行新增
        //for (ImportPermissionsVO.Role role : param.getRoles()) {
        //    String roleName = role.getRoleName();
        //    String description = role.getDescription();
        //    if (currentRoleSet.contains(roleName)) {
        //        log.info(LOG_PRE + "Role {} is existed now, continue", roleName);
        //        continue;
        //    }
        //    oamClient.createRole(adminPk, adminBid, roleName, description);
        //    log.info(LOG_PRE + "Role {} not exist, and create it successfully", roleName);
        //}
        //
        //// 如果不需要权限点，则直接退出
        //if (param.getIgnorePermissions()) {
        //    return;
        //}
        //
        //// 针对每一个角色进行权限点更新
        //for (ImportPermissionsVO.Role role : param.getRoles()) {
        //    String roleName = role.getRoleName();
        //    List<ListRoleCellsByRoleNameResponse.OamRoleCell> roleCells = oamClient
        //        .listRoleCellsByRoleName(adminPk, adminBid, roleName);
        //    Map<String, String> currentResourceMap = new HashMap<>();
        //    for (ListRoleCellsByRoleNameResponse.OamRoleCell roleCell : roleCells) {
        //        currentResourceMap.put(roleCell.getResource(), roleCell.getRoleCellId());
        //    }
        //    Set<String> usedResourceSet = role.getPermissions()
        //        .stream()
        //        .map(ImportPermissionsVO.Permission::getPath)
        //        .collect(Collectors.toSet());
        //
        //    // 针对所有配置中的权限进行维护
        //    for (ImportPermissionsVO.Permission permission : role.getPermissions()) {
        //        String resource = permission.getPath();
        //
        //        // 已经存在，则跳过
        //        if (currentResourceMap.containsKey(resource)) {
        //            log.info(LOG_PRE + "Role {}, permission {} is already exists, skip", roleName, resource);
        //            continue;
        //        }
        //
        //        // 没有，则新建
        //        oamClient.addRoleCellToRole(adminPk, adminBid, roleName, resource);
        //        log.info(LOG_PRE + "Role {}, permission {} has added", roleName, resource);
        //    }
        //
        //    // 针对所有移除的权限进行删除
        //    for (Map.Entry<String, String> entry : currentResourceMap.entrySet()) {
        //        String resource = entry.getKey();
        //        String cellId = entry.getValue();
        //        if (!usedResourceSet.contains(resource)) {
        //            oamClient.removeRoleCellFromRole(adminPk, adminBid, cellId);
        //            log.info(LOG_PRE + "Role {}, permission {}({}) has deleted, useless", roleName, resource, cellId);
        //        }
        //    }

            //log.info(LOG_PRE + "Role {} permissions has maintained", roleName);
        //}
    }

    /**
     * 从 OAM 中同步每个角色绑定的人员，到本地的 user_role_rel 表中
     */
    @Override
    public void syncFromRoleOperator(UserDO user, List<RoleDO> localRoles) {
        // 获取超级管理员的用户
//        UserDO adminUserDo = getAdminRoleUserDo(user.getAliyunPk(), user.getBid());
//        if (null == adminUserDo) {
//            throw new PrivateInternalError("Cannot get roles by super admin account");
//        }
//        String adminPk = adminUserDo.getAliyunPk();
//        String adminBid = adminUserDo.getBid();
//
//        // 本地角色映射 (转换为 OAM role name 风格)，key 为 OAM role name, value 为实际要绑定的用户 loginId 集合
//        Map<String, Set<String>> localRoleNameMapping = new HashMap<>();
//        for (RoleDO role : localRoles) {
//            String roleName = generateOamRoleName(role.getRoleId());
//            localRoleNameMapping.putIfAbsent(roleName, new HashSet<>());
//        }

        // 获取全量 OAM 角色
        //List<ListRolesResponse.OamRole> oamRoles = oamClient.listRoles(adminPk, adminBid, "");
        //for (ListRolesResponse.OamRole oamRole : oamRoles) {
        //    String roleName = oamRole.getRoleName();
        //
        //    // 当前角色匹配本地角色后，直接记录当前角色被赋予了哪些用户权限
        //    if (localRoleNameMapping.containsKey(roleName)) {
        //        try {
        //            localRoleNameMapping.get(roleName).addAll(oamClient.listOperatorByRole(adminPk, adminBid, roleName)
        //                .stream()
        //                .map(ListOperatorByRoleResponse.OamUser::getLoginId)
        //                .collect(Collectors.toList()));
        //        } catch (ClientException e) {
        //            log.error("Cannot list operator by role, roleName={}, exception={}",
        //                roleName, ExceptionUtils.getStackTrace(e));
        //        }
        //
        //        // 扫描所有组对应的所有的人，加入到角色对应的人的列表中
        //        List<ListGroupsByRoleResponse.OamGroup> groups;
        //        try {
        //            groups = oamClient.listGroupsByRole(adminPk, adminBid, roleName);
        //        } catch (ClientException e) {
        //            log.error("Cannot list groups by role {}, exception={}",
        //                roleName, ExceptionUtils.getStackTrace(e));
        //            continue;
        //        }
        //        for (ListGroupsByRoleResponse.OamGroup group : groups) {
        //            String groupName = group.getGroupName();
        //            try {
        //                localRoleNameMapping.get(roleName).addAll(
        //                    oamClient.listUsersForGroup(adminPk, adminBid, groupName)
        //                        .stream()
        //                        .map(ListUsersForGroupResponse.OamUser::getLoginId)
        //                        .collect(Collectors.toList()));
        //            } catch (ClientException e) {
        //                log.error("Cannot list users for group {}, exception={}",
        //                    groupName, ExceptionUtils.getStackTrace(e));
        //            }
        //        }
        //        continue;
        //    }
        //
        //    // 发现一个未知角色后，获取该角色的所有继承自的角色清单，对于每个继承角色仍然重复上面的扫描
        //    List<ListBaseRolesByRoleResponse.OamRole> baseRoleNames;
        //    try {
        //        baseRoleNames = oamClient.listBaseRolesByRole(adminPk, adminBid, roleName);
        //    } catch (ClientException e) {
        //        log.error("Cannot get base roles by role name {}, exception={}",
        //            roleName, ExceptionUtils.getStackTrace(e));
        //        continue;
        //    }
        //    for (ListBaseRolesByRoleResponse.OamRole baseRole : baseRoleNames) {
        //        String baseRoleName = baseRole.getRoleName();
        //        if (localRoleNameMapping.containsKey(baseRoleName)) {
        //            // 当发现继承自 bcc 的角色后，扫描该角色的所有赋予的人，并加入到继承的 bcc 角色的人的列表中
        //            try {
        //                localRoleNameMapping.get(baseRoleName).addAll(
        //                    oamClient.listOperatorByRole(adminPk, adminBid, roleName)
        //                        .stream()
        //                        .map(ListOperatorByRoleResponse.OamUser::getLoginId)
        //                        .collect(Collectors.toList()));
        //            } catch (ClientException e) {
        //                log.error("Cannot list operator by role {}, exception={}",
        //                    roleName, ExceptionUtils.getStackTrace(e));
        //            }
        //
        //            // 当发现继承自 bcc 的角色后，扫描该角色的所有赋予的组，并扫描所有组对应的所有的人，加入到继承的 bcc 角色的人的列表中
        //            List<ListGroupsByRoleResponse.OamGroup> groups;
        //            try {
        //                groups = oamClient.listGroupsByRole(adminPk, adminBid, roleName);
        //            } catch (ClientException e) {
        //                log.error("Cannot list groups by role {}, exception={}",
        //                    roleName, ExceptionUtils.getStackTrace(e));
        //                continue;
        //            }
        //            for (ListGroupsByRoleResponse.OamGroup group : groups) {
        //                String groupName = group.getGroupName();
        //                try {
        //                    localRoleNameMapping.get(baseRoleName).addAll(
        //                        oamClient.listUsersForGroup(adminPk, adminBid, groupName)
        //                            .stream()
        //                            .map(ListUsersForGroupResponse.OamUser::getLoginId)
        //                            .collect(Collectors.toList()));
        //                } catch (ClientException e) {
        //                    log.error("Cannot list users for group {}, exception={}",
        //                        groupName, ExceptionUtils.getStackTrace(e));
        //                }
        //            }
        //        }
        //    }
        //}
        //
        //// 遍历所有的本地角色列表
        //for (RoleDO role : localRoles) {
        //    String roleId = role.getRoleId();
        //    String oamRoleName = generateOamRoleName(roleId);
        //    Set<String> oamUserIdSet = localRoleNameMapping.get(oamRoleName).stream()
        //        .map(p -> "empid::" + p)
        //        .collect(Collectors.toSet());
        //    // add aliyuntest account always
        //    oamUserIdSet.add("empid::999999999");
        //
        //    // 与当前本地保存的数据进行比对，进行整体更新
        //    List<UserRoleRelDO> rels = userRoleRelMapper.findAllByTenantIdAndRoleId(Constants.DEFAULT_TENANT_ID, roleId);
        //    for (UserRoleRelDO rel : rels) {
        //        if (!oamUserIdSet.contains(rel.getUserId()) && !rel.getUserId().equals(adminUserDo.getUserId())) {
        //            userRoleRelMapper
        //                .deleteAllByTenantIdAndUserIdAndRoleId(rel.getTenantId(), rel.getUserId(), rel.getRoleId());
        //            log.info(LOG_PRE + "Delete useless user role rel||tenantId={}||userId={}||roleId={}",
        //                rel.getTenantId(), rel.getUserId(), rel.getRoleId());
        //        }
        //        oamUserIdSet.remove(rel.getUserId());
        //    }
        //
        //    // 针对还不存在的数据进行插入
        //    for (String userId : oamUserIdSet) {
        //        userRoleRelMapper.insert(UserRoleRelDO.builder()
        //            .tenantId(Constants.DEFAULT_TENANT_ID)
        //            .roleId(roleId)
        //            .userId(userId)
        //            .build());
        //        log.info(LOG_PRE + "Sync new user role rel||tenantId={}||userId={}||roleId={}",
        //            Constants.DEFAULT_TENANT_ID, localRoles, userId);
        //    }
        //}
    }

    @Override
    public void initPermission(String userId, String appId, List<PermissionMetaDO> permissionMetaDOS) {
        // TODO: 测试重复初始化权限的情况下，是否可以顺利进行 by yaoxing.gyx
        UserDO teslaUser = teslaUserService.getUserByAliyunPk(userId);
        Assert.notNull(teslaUser, userId + " not exits, can't init permission.");
        AppDO appDo = teslaAppService.getByAppId(appId);
        Assert.notNull(appDo, "app " + appId + " not exits.");

        //permissionMetaDOS.parallelStream().forEach(permissionMeta ->{
        //
        //    String permissionName = createPermissionId(appId, permissionMeta);
        //    try {
        //        String roleName = createRoleNameByAppId(appId);
        //        /**
        //         * 1、根据规则查询appId在OAM对应的角色是否存在，如果角色不存在抛出异常
        //         */
        //        GetRoleResponse.Data roleData = oamClient.getRole(teslaUser.getAliyunPk(), teslaUser.getBid(), roleName);
        //        if(null == roleData){
        //            throw new PrivateOamRoleNotExitsException(roleName);
        //        }
        //        //始终使用admin用户进行权限操作
        //        UserDO adminUserDo = this.getAdminRoleUserDo(teslaUser.getAliyunPk(), teslaUser.getBid());
        //        /**
        //         * 2、给角色先添加资源权限
        //         */
        //        //oamClient.addRoleCellToRole(adminUserDo.getAliyunPk(), adminUserDo.getBid(), roleName, permissionName);
        //    } catch (AuthProxyThirdPartyError e) {
        //        log.error("init permission failed", e);
        //        throw new ApplicationException(500, "init permission " + permissionName + " failed: "
        //                + e.getComponentName() + "_" + e.getLocalizedMessage());
        //    } catch (ClientException e) {
        //        log.error("init permission failed, ClientException", e);
        //        throw new ApplicationException(500, "init permission " + permissionName + " failed, ClientException: " + e.getErrCode() + "_" + e.getErrMsg());
        //    }catch (Exception e) {
        //        log.error("init permission failed, Exception", e);
        //        throw new ApplicationException(500, "init permission " + permissionName + " failed, Exception: " + e.getLocalizedMessage());
        //    }
        //});
    }

    @Override
    public String createPermissionId(String appId, PermissionMetaDO permissionMetaDO) {
        /**
         * authProperties.getPermissionPrefix() - 按照oam要求资源权限使用默认的前缀
         */
        return authProperties.getPermissionPrefix() + appId + "_" + permissionMetaDO.getPermissionCode();
    }

    @Override
    public String createRoleNameByAppId(String appId) {
        return authProperties.getOamAdminRole() + "_appmgr_" + appId;
    }

    /**
     * 获取 bcc_admin 管理角色的拥有者用户
     *
     * @param aliyunPk 当前用户 aliyun pk
     * @param bid      当前用户 bid
     */
    private UserDO getAdminRoleUserDo(String aliyunPk, String bid) throws AuthProxyThirdPartyError {
//        GetRoleResponse.Data data;
//        try {
//            data = oamClient.getRole(aliyunPk, bid, authProperties.getOamAdminRole());
//        } catch (ClientException e) {
//            switch (e.getErrCode()) {
//                case "USER_NOT_EXIST":
//                    try {
//                        oamClient.getOamUserByUsername(aliyunPk);
//                        data = oamClient.getRole(aliyunPk, bid, authProperties.getOamAdminRole());
//                    } catch (ClientException ie) {
//                        AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
//                            "GetOamUserByUserName/GetRole failed: " + ie.getMessage());
//                        se.initCause(ie);
//                        throw se;
//                    }
//                    break;
//                default:
//                    AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
//                        "GetRole failed: " + e.getMessage());
//                    se.initCause(e);
//                    throw se;
//            }
//        }
        return teslaUserService.getUserByAliyunId(authProperties.getAasSuperUser());
    }

    /**
     * 将权代中的 roleId 转换为 OAM 识别的 OAM roleName
     * @param roleId Role ID
     * @return
     */
    private String generateOamRoleName(String roleId) {
        return Constants.ABM_ROLE_PREFIX_API + roleId.replace(":", "_");
    }
}
