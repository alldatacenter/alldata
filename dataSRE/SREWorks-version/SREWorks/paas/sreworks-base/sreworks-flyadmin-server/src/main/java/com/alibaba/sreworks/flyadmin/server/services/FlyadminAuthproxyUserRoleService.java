package com.alibaba.sreworks.flyadmin.server.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.Requests;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FlyadminAuthproxyUserRoleService extends FlyadminAuthproxyService {

    public JSONArray getRole(String appId, String user) throws IOException, ApiException {
        return new Requests(getAuthProxyEndpoint() + "/roles")
            .headers(
                "X-Biz-App", appId,
                "X-EmpId", user
            )
            .get().isSuccessful()
            .getJSONObject().getJSONObject("data").getJSONArray("items");
    }

    public JSONArray getUserRole(String appId, String user) throws IOException, ApiException {
        return new Requests(String.format("%s/users/%s/roles", getAuthProxyEndpoint(), user))
            .headers(
                "X-Biz-App", appId,
                "X-EmpId", user
            )
            .get().isSuccessful()
            .getJSONObject().getJSONObject("data").getJSONArray("items");
    }

    public JSONArray listRole(String appId, String user) throws IOException, ApiException {
        JSONArray roles = getRole(appId, user);
        JSONArray userRoles = getUserRole(appId, user);
        List<String> userRoleKeys = userRoles.toJavaList(JSONObject.class).stream()
            .map(userRole -> userRole.getString("roleId"))
            .collect(Collectors.toList());
        roles.toJavaList(JSONObject.class).forEach(role -> {
            String roleId = role.getString("roleId");
            role.put("exists", userRoleKeys.contains(roleId));
        });
        return roles;
    }

    private void addUserRole(String user, String roleId) throws IOException, ApiException {
        String url = String.format("%s/users/%s/roles/%s", getAuthProxyEndpoint(), user, roleId);
        new Requests(url)
            .headers("X-EmpId", user)
            .post().isSuccessful()
            .getJSONObject();
    }

    private void delUserRole(String user, String roleId) throws IOException, ApiException {
        String url = String.format("%s/users/%s/roles/%s", getAuthProxyEndpoint(), user, roleId);
        new Requests(url)
            .headers("X-EmpId", user)
            .delete().isSuccessful()
            .getJSONObject();
    }

    public String authproxyAppId(String appId, String localBizAppId) {
        String[] words = localBizAppId.split(",");
        return String.format("%s,%s,%s", appId, words[1], words[2]);
    }

    public void setRole(String localBizAppId, String user, JSONObject postJson) throws IOException, ApiException {
        List<String> roleIds = new ArrayList<>();
        List<String> appIds = new ArrayList<>();
        for (String key : postJson.keySet()) {
            if (key.startsWith("UserRole_")) {
                appIds.add(authproxyAppId(key.replace("UserRole_", ""), localBizAppId));
                roleIds.addAll(postJson.getJSONArray(key).toJavaList(String.class));
            }
        }
        for (String roleId : roleIds) {
            addUserRole(user, roleId);
        }
        List<String> nowRoleIds = new ArrayList<>();
        for (String appId : appIds) {
            JSONArray roles = listRole(appId, user);
            nowRoleIds.addAll(roles.toJavaList(JSONObject.class).stream()
                .map(x -> x.getString("roleId")).collect(Collectors.toList()));
        }
        for (String nowRoleId : nowRoleIds) {
            if (!roleIds.contains(nowRoleId)) {
                delUserRole(user, nowRoleId);
            }
        }
    }

}
