package com.datasophon.api.service;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface ClusterKerberosService {
    void downloadKeytab(Integer clusterId, String principal, String keytabName, String hostname,HttpServletResponse response) throws IOException;
}
