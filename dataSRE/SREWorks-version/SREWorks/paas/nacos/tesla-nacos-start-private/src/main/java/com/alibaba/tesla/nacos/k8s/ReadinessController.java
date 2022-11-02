package com.alibaba.tesla.nacos.k8s;

import com.alibaba.nacos.naming.cluster.ServerListManager;
import com.alibaba.nacos.naming.cluster.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Objects;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@RestController
@RequestMapping()
public class ReadinessController {

    private boolean serverReady;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ServerListManager serverListManager;

    @GetMapping("/readiness")
    public String checkServerReady(HttpServletResponse response){
        if (serverReady) {
            return "success";
        }
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        return "failed";
    }

    @PostMapping("/readiness")
    public String serverReady(@RequestParam(value = "broadcast", required = false) boolean  broadcast){
        log.info("receive readiness access....");
        boolean readiness = true;
        if (!broadcast) {
            //获取所有的server并广播
            List<Server> servers = serverListManager.getServers();
            if (!CollectionUtils.isEmpty(servers)) {
                for (Server server : servers) {
                    String url = String.format("http://%s:%s/nacos/readiness?broadcast=true", server.getIp(), server.getServePort());
                    ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, null, String.class);
                    if (HttpStatus.OK.value() != responseEntity.getStatusCode().value()) {
                        readiness = false;
                        log.warn("readiness failed..., code={}", responseEntity.getStatusCode());
                    }
                }
            }
        }
        if (readiness) {
            serverReady = true;
            return "success";
        }

        return "failed";
    }


}
