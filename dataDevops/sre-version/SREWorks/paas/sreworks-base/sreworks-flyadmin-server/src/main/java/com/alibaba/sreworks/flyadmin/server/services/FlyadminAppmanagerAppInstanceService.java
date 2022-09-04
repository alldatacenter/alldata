package com.alibaba.sreworks.flyadmin.server.services;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.AppmanagerServiceUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Service
public class FlyadminAppmanagerAppInstanceService {

    public JSONObject list(String stageIdList, String clusterId, String appId, Long page, Long pageSize, String source)
        throws Exception {
        String url = AppmanagerServiceUtil.getEndpoint()
            + "/realtime/app-instances?"
            + "stageIdList=" + stageIdList
            + "&appId=" + appId
            + "&clusterId=" + clusterId
            + "&page=" + page
            + "&pageSize=" + pageSize;
        if (source != null){
            url += "&optionKey=source&optionValue=" + source;
        }
        log.info("FlyadminAppmanagerAppInstanceService.list url: {}", url);
        return new Requests(url)
            .get()
            .headers(HttpHeaderNames.X_EMPL_ID, "")
            .get().isSuccessful().getJSONObject()
            .getJSONObject("data");
    }

    public JSONObject get(String appInstanceId) throws Exception {
            String url = AppmanagerServiceUtil.getEndpoint() + "/realtime/app-instances/" + appInstanceId;
            log.info("FlyadminAppmanagerAppInstanceService.get url: {}", url);
            return new Requests(url)
                    .get()
                    .headers(HttpHeaderNames.X_EMPL_ID, "")
                    .get().isSuccessful().getJSONObject()
                    .getJSONObject("data");
    }

}
