package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.tesla.appmanager.api.provider.UserProfileProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.domain.dto.UserProfileDTO;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 应用元信息 Controller
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@RequestMapping("/profile")
@RestController
@Slf4j
public class UserProfileController extends AppManagerBaseController {

    @Autowired
    private UserProfileProvider userProfileProvider;

    @GetMapping
    public TeslaBaseResult get(
            @RequestParam("namespaceId") String namespaceId,
            @RequestParam("stageId") String stageId,
            OAuth2Authentication auth) {
        UserProfileDTO userProfileDTO = userProfileProvider.queryProfile(getOperator(auth), namespaceId, stageId);
        return buildSucceedResult(userProfileDTO.getProfile());
    }

    /**
     * @api {put} /apps/:appId 更新指定应用信息
     * @apiName PutApp
     * @apiGroup 应用 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (JSON Body) {Number} id 应用主键 ID
     * @apiParam (JSON Body) {String} appName 应用名称
     * @apiParam (JSON Body) {String} appExt 应用扩展信息
     */
    @PutMapping
    public TeslaBaseResult update(
            @RequestParam("namespaceId") String namespaceId,
            @RequestParam("stageId") String stageId,
            @RequestBody String profile,
            OAuth2Authentication auth) {
        int result = userProfileProvider.save(
                UserProfileDTO.builder()
                        .userId(getOperator(auth))
                        .profile(JSON.parseObject(profile))
                        .namespaceId(namespaceId)
                        .stageId(stageId)
                        .build()
        );
        return buildSucceedResult(result);
    }
}
