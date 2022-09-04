package com.alibaba.tesla;

import com.alibaba.tesla.monitor.DatasourceExporterPrivate;
import com.alibaba.tesla.monitor.OkhttpExporter;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * <p>Title: Application.java<／p>
 * <p>Description: PandoraBoot启动入口 <／p>
 * <p>Copyright: alibaba (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@EnableTransactionManagement
@MapperScan("com.alibaba.tesla.authproxy.model.mapper")
@SpringBootApplication(scanBasePackages = {"com.alibaba.tesla"})
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
@EntityScan("com.alibaba.tesla")
public class ApplicationPrivate implements CommandLineRunner {

    @Autowired
    private DatasourceExporterPrivate datasourceExporter;

    @Autowired
    private OkhttpExporter okhttpExporter;

    public static void main(String[] args) {
        try {
            SpringApplication.run(ApplicationPrivate.class, args);
        } catch (Exception e) {
            System.err.println("ApplicationError: " + e.getMessage());
            System.exit(1);
        }
    }


    @Override
    public void run(String... strings) throws Exception {
        DefaultExports.initialize();
        datasourceExporter.register();
        okhttpExporter.register();
    }
}
