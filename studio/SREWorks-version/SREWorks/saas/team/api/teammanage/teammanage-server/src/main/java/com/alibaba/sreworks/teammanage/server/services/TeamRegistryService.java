package com.alibaba.sreworks.teammanage.server.services;

import java.io.IOException;
import java.util.Base64;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.K8sUtil;
import com.alibaba.sreworks.domain.DO.TeamRegistry;

import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.Namespaces;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Data
public class TeamRegistryService {

    public void createSecret(TeamRegistry teamRegistry) throws IOException, ApiException {

        String auth = new String(Base64.getEncoder().encode(teamRegistry.getAuth().getBytes()));
        String url = teamRegistry.getUrl();
        url = url.split("/")[0];
        JSONObject body = JsonUtil.map(
            "auths", JsonUtil.map(
                url, JsonUtil.map(
                    "auth", auth
                )
            )
        );
        //byte[] dockerconfigjson = Base64.getEncoder().encode(body.toJSONString().getBytes());
        String podNamespace = Namespaces.getPodNamespace();
        CoreV1Api coreV1Api = new CoreV1Api(K8sUtil.client);
        V1Secret secret = new V1Secret()
            .apiVersion("v1")
            .kind("Secret")
            .metadata(new V1ObjectMeta()
                .name(String.format("team-%s-%s", teamRegistry.getTeamId(), teamRegistry.getName()))
            )
            .data(ImmutableMap.of(".dockerconfigjson", body.toJSONString().getBytes()))
            .type("kubernetes.io/dockerconfigjson");
        coreV1Api.createNamespacedSecret(
            podNamespace, secret, null, null, null
        );

    }

    public void deleteSecret(TeamRegistry teamRegistry) throws IOException, ApiException {

        String podNamespace = Namespaces.getPodNamespace();
        CoreV1Api coreV1Api = new CoreV1Api(K8sUtil.client);
        try {
            coreV1Api.deleteNamespacedSecret(
                    String.format("team-%s-%s", teamRegistry.getTeamId(), teamRegistry.getId()),
                    podNamespace,
                    null, null, null, null, null, null
            );
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                throw e;
            }
        }


    }

}
