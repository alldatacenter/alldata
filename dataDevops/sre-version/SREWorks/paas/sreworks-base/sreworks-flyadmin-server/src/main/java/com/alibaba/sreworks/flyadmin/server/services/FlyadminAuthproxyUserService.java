package com.alibaba.sreworks.flyadmin.server.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.AppmanagerServiceUtil;
import com.alibaba.sreworks.common.util.K8sUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.sreworks.flyadmin.server.DTO.FlyadminAuthproxyModifyUserParam;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FlyadminAuthproxyUserService extends FlyadminAuthproxyService {

    public void createUser(
        String nickName, String loginName, String password, String email, String phone, String avatar, String user
    ) throws IOException, ApiException {

        String url = getAuthProxyEndpoint() + "/auth/tesla/user/create";
        new Requests(url, true)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .postJson(
                "nickName", nickName,
                "loginName", loginName,
                "password", password,
                "email", email,
                "avatar", avatar,
                "phone", phone
            )
            .post().isSuccessful().getString();

    }

    public String modifyUser(
        String nickName, String loginName, String password, String email, String phone, String avatar, String user
    ) throws IOException, ApiException {

        String url = getAuthProxyEndpoint() + "/auth/tesla/user/modify";
        return new Requests(url, true)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .postJson(
                "nickName", nickName,
                "loginName", loginName,
                "password", password,
                "email", email,
                "avatar", avatar,
                "phone", phone
            )
            .post().isSuccessful().getString();

    }

    public void deleteUser(Long id, String user) throws IOException, ApiException {
        String url = getAuthProxyEndpoint() + "/auth/tesla/user/delete?id=" + id;
        new Requests(url, true)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .delete().isSuccessful().getString();
    }

    public JSONObject getUser(String user) throws IOException, ApiException {
        String url = getAuthProxyEndpoint() + "/auth/user/get?empId=" + user;
        return new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject().getJSONObject("data");
    }

    public List<JSONObject> userList(String user) throws IOException, ApiException {
        String url = getAuthProxyEndpoint() + "/auth/user/list?userName=";
        return new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject().getJSONArray("data").toJavaList(JSONObject.class);
    }

    public Map<String, String> userEmpIdNameMap(String user) throws IOException, ApiException {
        return userList(user).stream().collect(Collectors.toMap(
            jsonObject -> jsonObject.getString("empId"),
            jsonObject -> jsonObject.getString("nickName"),
            (v1, v2) -> v1
        ));
    }

    public Map<String, JSONObject> userEmpIdJsonMap(String user) throws IOException, ApiException {
        return userList(user).stream().collect(Collectors.toMap(
            jsonObject -> jsonObject.getString("empId"),
            jsonObject -> jsonObject,
            (v1, v2) -> v1
        ));
    }

    public void patchNickName(JSONObject jsonObject, String user, String... nameArray)
        throws IOException, ApiException {
        Map<String, String> map = userEmpIdNameMap("");
        for (String name : nameArray) {
            jsonObject.put(name + "Cn", map.get(jsonObject.getString(name)));
        }
    }

    public void patchNickName(List<JSONObject> jsonObjectList, String user, String... nameArray)
        throws IOException, ApiException {
        Map<String, String> map = userEmpIdNameMap("");
        for (JSONObject jsonObject : jsonObjectList) {
            for (String name : nameArray) {
                jsonObject.put(name + "Cn", map.get(jsonObject.getString(name)));
            }
        }
    }

    public void patchNickName(JSONArray jsonArray, String user, String... nameArray)
        throws IOException, ApiException {
        List<JSONObject> jsonObjectList = jsonArray.toJavaList(JSONObject.class);
        Map<String, String> map = userEmpIdNameMap("");
        for (JSONObject jsonObject : jsonObjectList) {
            for (String name : nameArray) {
                jsonObject.put(name + "Cn", map.get(jsonObject.getString(name)));
            }
        }
    }

}
