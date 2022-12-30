package com.alibaba.tesla;

import com.alibaba.tesla.monitor.OkhttpExporter;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Pandora Boot应用的入口类
 *
 * @author chengxu
 */
@EnableAsync
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = {"com.alibaba.tesla"})
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
@EntityScan("com.alibaba.tesla")
public class Application implements CommandLineRunner {

//    @Autowired
//    private DatasourceExporter datasourceExporter;

    @Autowired
    private OkhttpExporter okhttpExporter;

    public static void main(String[] args) {
        try {
            SpringApplication.run(Application.class, args);
        } catch (Exception e) {
            System.err.println("ApplicationError: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void run(String... strings) throws Exception {
        DefaultExports.initialize();
//        datasourceExporter.register();
        okhttpExporter.register();
    }
}
