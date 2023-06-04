package com.datasophon.api.controller;

import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.ClusterKerberosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("cluster/kerberos")
public class ClusterKerberosController {

    @Autowired
    private ClusterKerberosService kerberosService;

    /**
     * download keytab
     */
    @UserPermission
    @GetMapping("/downloadKeytab")
    public void list(Integer clusterId,String principal,String keytabName,String hostname, HttpServletResponse response) throws IOException {
        kerberosService.downloadKeytab(clusterId,principal,keytabName,hostname,response);
    }
}
