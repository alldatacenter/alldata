package com.alibaba.datax.plugin.writer.elasticsearchwriter;

import com.alibaba.datax.common.util.Configuration;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;

import java.util.concurrent.TimeUnit;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-26 13:01
 **/
public class ESInitialization {
    public JestClient jestClient;

    public static ESInitialization create(Configuration conf) {
        return create(conf, false, 300000, false, false);
    }

    public static ESInitialization create(Configuration conf, boolean multiThread,
                                          int readTimeout,
                                          boolean compression,
                                          boolean discovery) {
        String endpoint = Key.getEndpoint(conf);
        String user = Key.getAccessID(conf);
        String passwd = Key.getAccessKey(conf);
        return create(endpoint, user, passwd, multiThread, readTimeout, compression, discovery);
    }

    public static ESInitialization create(String endpoint, String user, String passwd, boolean multiThread,
                                          int readTimeout,
                                          boolean compression,
                                          boolean discovery) {

//        String endpoint = Key.getEndpoint(conf);
//        String user = Key.getAccessID(conf);
//        String passwd = Key.getAccessKey(conf);
        ESInitialization es = new ESInitialization();

        JestClientFactory factory = new JestClientFactory();
        HttpClientConfig.Builder httpClientConfig = new HttpClientConfig
                .Builder(endpoint)
                //.setPreemptiveAuth(new HttpHost(endpoint))
                .multiThreaded(multiThread)
                .connTimeout(30000)
                .readTimeout(readTimeout)
                .maxTotalConnection(200)
                .requestCompressionEnabled(compression)
                .discoveryEnabled(discovery)
                .discoveryFrequency(5l, TimeUnit.MINUTES);

        //if (!("".equals(user) || "".equals(passwd))) {
        if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(passwd)) {
            httpClientConfig.setPreemptiveAuth(HttpHost.create(endpoint));
            httpClientConfig.defaultCredentials(user, passwd);
        }

        factory.setHttpClientConfig(httpClientConfig.build());

        es.jestClient = factory.getObject();
        return es;
    }
}
