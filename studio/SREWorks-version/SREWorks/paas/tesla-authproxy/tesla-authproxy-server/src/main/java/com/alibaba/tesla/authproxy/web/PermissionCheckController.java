package com.alibaba.tesla.authproxy.web;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.BaseController;
import com.alibaba.tesla.authproxy.model.AppExtDO;
import com.alibaba.tesla.authproxy.model.PermissionDO;
import com.alibaba.tesla.authproxy.model.PermissionResDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.vo.UserPermissionsVO;
import com.alibaba.tesla.authproxy.service.AppExtService;
import com.alibaba.tesla.authproxy.service.AuthServiceManager;
import com.alibaba.tesla.authproxy.service.PermissionResService;
import com.alibaba.tesla.common.base.TeslaResult;
import com.alibaba.tesla.common.utils.TeslaResultBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 权限 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Controller
@RequestMapping("permission")
@Slf4j
public class PermissionCheckController extends BaseController {

    /**
     * 权限资源服务接口
     */
    @Autowired
    private PermissionResService permissionResService;

    @Autowired
    private AppExtService appExtService;

    @Autowired
    private AuthProperties authProperties;

    /**
     * 验权，包含模式，兼容专有云需求
     *
     * @param request        请求对象
     * @param appId          应用ID
     * @param permissionName 权限名称
     * @return
     */
    @RequestMapping(value = "/check", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> check(HttpServletRequest request, @RequestParam String appId,
                                     @RequestParam String permissionName) {
        if (StringUtils.isEmpty(appId)) {
            log.error("验权错误，参数appId为空");
            return buildErrorResult(TeslaResult.FAILURE, "验权错误,ProductName不能为空");
        }
        if (StringUtils.isEmpty(permissionName)) {
            log.error("验权错误，参数permissionName为空");
            return buildErrorResult(TeslaResult.FAILURE, "验权错误,资源URL不能为空");
        }
        PermissionResDO permissionResDo = permissionResService.getByResPath(appId, permissionName);
        if (null == permissionResDo) {
            log.info("权限资源[{}]数据为空，验证通过", permissionName);
            UserDO loginUser = getLoginUser(request);
            return buildResult(loginUser);
        }

        AuthServiceManager authManager = authPolicy.getAuthServiceManager();
        UserDO loginUser = getLoginUser(request);

        boolean ret = authManager.checkPermission(loginUser, permissionResDo.getPermissionId(), appId);
        log.info("权限验证，permissionId:{}, appId:{}", permissionResDo.getPermissionId(), appId);
        if (ret) {
            return buildResult(loginUser);
        } else {
            return buildResult("403", loginUser, "验权不通过，请申请资源:" + permissionResDo.getPermissionId());
        }
    }

    /**
     * 验权，相等模式模式
     *
     * @param request 请求对象
     * @param appId   应用ID
     * @param reqPath 要验证的请求url path
     * @return
     */
    @RequestMapping(value = {"/check_equal", "/checkEqual"}, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> checkEqual(HttpServletRequest request, @RequestParam String appId,
                                          @RequestParam String reqPath) {
        if (StringUtils.isEmpty(appId)) {
            log.error("验权错误，参数appId为空");
            return buildErrorResult(TeslaResult.FAILURE, "验权错误，参数appId为空");
        }
        if (StringUtils.isEmpty(reqPath)) {
            log.error("验权错误，参数reqPath为空");
            return buildErrorResult(TeslaResult.FAILURE, "验权错误，参数reqPath为空");
        }
        PermissionResDO permissionResDo = permissionResService.getByAppAndPath(appId, reqPath);
        if (null == permissionResDo) {
            UserDO loginUser = getLoginUser(request);
            return buildResult(loginUser);
        }

        AuthServiceManager authManager = authPolicy.getAuthServiceManager();
        UserDO loginUser = getLoginUser(request);
        log.info("权限验证，permissionId:{}, appId:{}, loginUser:{}", permissionResDo.getPermissionId(), appId,
            JSONObject.toJSONString(loginUser));
        boolean ret = authManager.checkPermission(loginUser, permissionResDo.getPermissionId(), appId);
        if (ret) {
            return buildResult(loginUser);
        } else {
            String applyUrl;
            if (authProperties.getAuthPolicy().contains("AclAuthServiceManager")) {
                applyUrl = authProperties.getProxyServerAddress() + "/apply/instance/index.htm?type=permission&keyword="
                    + permissionResDo.getPermissionId();
                return buildResult("403", loginUser, applyUrl);
            } else {
                return buildResult("403", loginUser, "验权不通过，请申请资源:" + permissionResDo.getPermissionId());
            }
        }
    }

    /**
     * 验权 (严格模式)，没有配置权限资源对应关系则不通过
     *
     * @param request        请求对象
     * @param appId          应用ID
     * @param permissionName 权限名称
     * @return
     */
    @RequestMapping(value = {"/check_strict", "/checkStrict"}, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> checkStrict(HttpServletRequest request, @RequestParam String appId,
                                           @RequestParam String permissionName) {
        if (StringUtils.isEmpty(appId)) {
            log.error("验权错误，参数appId为空");
            return buildErrorResult(TeslaResult.BAD_REQUEST, "验权错误,ProductName不能为空");
        }

        if (StringUtils.isEmpty(permissionName)) {
            log.error("验权错误，参数permissionName为空");
            return buildErrorResult(TeslaResult.BAD_REQUEST, "验权错误,资源URL不能为空");
        }

        PermissionResDO permissionResDo = permissionResService.getByResPath(appId, permissionName);
        if (null == permissionResDo) {
            log.info("Permission check strict failed, cannot find it in db. appId={}, permissionName={}",
                appId, permissionName);
            return buildErrorResult(403, "验权不通过");
        }

        AuthServiceManager authManager = authPolicy.getAuthServiceManager();
        UserDO loginUser = getLoginUser(request);

        boolean ret = authManager.checkPermission(loginUser, permissionResDo.getPermissionId(), appId);
        log.info("权限验证，permissionId:{}, appId:{}, loginUser:{}", permissionResDo.getPermissionId(), appId,
            JSONObject.toJSONString(loginUser));
        if (ret) {
            return buildResult(loginUser);
        } else {
            return buildResult("403", loginUser, "验权不通过，请申请资源:" + permissionResDo.getPermissionId());
        }
    }

    /**
     * 验权（扩展）tesla请求header验权
     *
     * @param extAppName
     * @param extAppKey
     * @return
     */
    @RequestMapping(value = "/checkExt", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> checkExt(@RequestParam String extAppName, @RequestParam String extAppKey) {
        if (null == extAppName || extAppName.length() == 0) {
            log.error("验权错误，参数extAppName为空");
            return buildErrorResult(TeslaResult.FAILURE, "验权错误，参数x-auth-app不能为空");
        }
        if (null == extAppKey || extAppKey.length() == 0) {

            log.error("验权错误，参数extAppKey为空");
            return buildErrorResult(TeslaResult.FAILURE, "验权错误，参数x-auth-key不能为空");
        }
        AppExtDO appExt = appExtService.getByName(extAppName);

        if (null == appExt) {
            log.error("验权错误，AppKey不存在");
            return buildErrorResult(TeslaResult.FAILURE, "验权错误，x-auth-key不存在");
        }
        String key = appExt.getExtAppKey();

        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        Date today = Calendar.getInstance().getTime();
        String todayStr = df.format(today);

        byte[] theDigest;
        try {
            String rawHash = String.format("%s%s%s", extAppName, todayStr, key);
            byte[] bytesOfMessage = rawHash.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            theDigest = md.digest(bytesOfMessage);
        } catch (Exception e) {
            log.error("验权失败", e);
            return buildErrorResult(TeslaResult.FAILURE, "验权失败");
        }
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] str = new char[16 * 2];
        int k = 0;
        for (int i = 0; i < 16; i++) {
            byte byte0 = theDigest[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        String compareKey = new String(str);
        if (compareKey.equals(extAppKey)) {
            return buildResult();
        } else {
            return buildErrorResult("403", "验权不通过，请检查x-auth-app和x-auth-key的合法性");
        }
    }

    /**
     * 获取当前登录用户角色下的某个数据权限
     *
     * @param request
     * @param appId          应用ID,如tesla_ads
     * @param permissionName 数据权限名称，如"db_data"为ads分站的DB实例数据权限
     * @return
     */
    @RequestMapping(value = "/data", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult getDataPermission(HttpServletRequest request, @RequestParam String appId,
                                         @RequestParam String permissionName) {
        if (null == appId || appId.length() == 0) {
            log.error("验权错误，参数appId为空");
            return TeslaResultBuilder.errorResult(TeslaResult.FAILURE, "验权错误,ProductName不能为空");
        }
        if (null == permissionName || permissionName.length() == 0) {
            log.error("验权错误，参数permissionName为空");
            return TeslaResultBuilder.errorResult(TeslaResult.FAILURE, "验权错误,资源URL不能为空");
        }

        AuthServiceManager authManager = authPolicy.getAuthServiceManager();
        UserDO loginUser = getLoginUser(request);
        List<PermissionDO> dataPermission = authManager.getDataPermission(loginUser, permissionName, appId);

        String retJson = JSONObject.toJSONString(dataPermission);
        log.info("返回的数据权限数据:{}", retJson);
        return TeslaResultBuilder.successResult(retJson);
    }

    /**
     * 根据当前登录用户和appId验证用户在该appId下有权限的资源集合
     *
     * @param request
     * @param appId
     * @return
     */
    @RequestMapping(value = "/check/list", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult getUserPermission(HttpServletRequest request, @RequestParam String appId) {
        AuthServiceManager authManager = authPolicy.getAuthServiceManager();
        UserDO loginUser = getLoginUser(request);
        List<UserPermissionsVO> userPermissionsVOS = authManager.getPermissionsByUserId(loginUser, appId);
        return TeslaResultBuilder.successResult(userPermissionsVOS);
    }
}
