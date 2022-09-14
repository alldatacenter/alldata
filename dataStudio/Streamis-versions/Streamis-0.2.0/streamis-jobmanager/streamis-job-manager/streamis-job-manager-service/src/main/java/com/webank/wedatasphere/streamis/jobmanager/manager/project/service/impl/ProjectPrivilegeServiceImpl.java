package com.webank.wedatasphere.streamis.jobmanager.manager.project.service.impl;

import com.webank.wedatasphere.streamis.jobmanager.manager.project.service.ProjectPrivilegeService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.linkis.common.conf.Configuration;
import org.apache.linkis.server.conf.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service("projectManagerPrivilegeServiceImpl")
public class ProjectPrivilegeServiceImpl implements ProjectPrivilegeService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectPrivilegeServiceImpl.class);

    @Autowired
    RestTemplate restTemplate;

    private String url_prefix = Configuration.getGateWayURL()+ ServerConfiguration.BDP_SERVER_RESTFUL_URI().getValue()+ "/streamis/project/projectPrivilege";

    @Override
    public Boolean hasReleasePrivilege(HttpServletRequest req, String projectName) {
        if(StringUtils.isBlank(projectName)) return false;
        Map<String, Object> responseData = getResponseData("/hasReleasePrivilege?projectName="+projectName, req);
        return (Boolean)Optional.ofNullable(responseData).orElse(new HashMap<>()).getOrDefault("releasePrivilege",false);
    }

    @Override
    public Boolean hasEditPrivilege(HttpServletRequest req, String projectName) {
        if(StringUtils.isBlank(projectName)) return false;
        Map<String, Object> responseData = getResponseData("/hasEditPrivilege?projectName="+projectName, req);
        return (Boolean)Optional.ofNullable(responseData).orElse(new HashMap<>()).getOrDefault("editPrivilege",false);
    }

    @Override
    public Boolean hasAccessPrivilege(HttpServletRequest req, String projectName) {
        if(StringUtils.isBlank(projectName)) return false;
        Map<String, Object> responseData = getResponseData("/hasAccessPrivilege?projectName="+projectName, req);
        return (Boolean)Optional.ofNullable(responseData).orElse(new HashMap<>()).getOrDefault("accessPrivilege",false);
    }

    @Override
    public Boolean hasReleasePrivilege(HttpServletRequest req, List<String> projectNames) {
        if(CollectionUtils.isEmpty(projectNames)) return false;
        Map<String, Object> responseData = getResponseData("/bulk/hasReleasePrivilege?projectNames="+projectNames, req);
        return (Boolean)Optional.ofNullable(responseData).orElse(new HashMap<>()).getOrDefault("releasePrivilege",false);
    }

    @Override
    public Boolean hasEditPrivilege(HttpServletRequest req, List<String> projectNames) {
        if(CollectionUtils.isEmpty(projectNames)) return false;
        Map<String, Object> responseData = getResponseData("/bulk/hasEditPrivilege?projectNames="+projectNames, req);
        return (Boolean)Optional.ofNullable(responseData).orElse(new HashMap<>()).getOrDefault("editPrivilege",false);
    }

    @Override
    public Boolean hasAccessPrivilege(HttpServletRequest req, List<String> projectNames) {
        if(CollectionUtils.isEmpty(projectNames)) return false;
        Map<String, Object> responseData = getResponseData("/bulk/hasAccessPrivilege?projectNames="+projectNames, req);
        return (Boolean)Optional.ofNullable(responseData).orElse(new HashMap<>()).getOrDefault("accessPrivilege",false);
    }

    private Map<String, Object> getResponseData(String reqPath, HttpServletRequest req){
        String url = url_prefix + reqPath;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie",req.getHeader("Cookie"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        LOG.info("obtain the operation privilege of the user,request url {}", url);
        ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);
        LOG.info("obtain the operation privilege of the user,return response body:{}", responseEntity.getBody());
        if(responseEntity.getBody()!=null && ((int)(responseEntity.getBody().get("status")))==0){
            return (Map<String, Object>) responseEntity.getBody().get("data");
        }else{
            LOG.error("user failed to obtain the privilege information");
            return null;
        }
    }

}
