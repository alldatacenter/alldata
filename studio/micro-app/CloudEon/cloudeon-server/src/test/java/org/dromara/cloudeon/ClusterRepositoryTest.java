package org.dromara.cloudeon;
 
import org.dromara.cloudeon.dao.AlertQuotaRepository;
import org.dromara.cloudeon.dao.ClusterInfoRepository;
import org.dromara.cloudeon.dto.PrometheusAlertRule;
import org.dromara.cloudeon.entity.ClusterAlertQuotaEntity;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ClusterRepositoryTest {

    @Resource
    private ClusterInfoRepository clusterInfoRepository;

    @Resource
    private AlertQuotaRepository alertQuotaRepository;
 
    @Test
    public void dod() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>"+clusterInfoRepository.findAll());
    }

    @Test
    public void alert() throws IOException, TemplateException {
        List<ClusterAlertQuotaEntity> alerts = alertQuotaRepository.findAll();
        List<PrometheusAlertRule> prometheusAlertRules = alerts.stream().map(new Function<ClusterAlertQuotaEntity, PrometheusAlertRule>() {
            @Override
            public PrometheusAlertRule apply(ClusterAlertQuotaEntity clusterAlertQuota) {
                PrometheusAlertRule prometheusAlertRule = new PrometheusAlertRule();
                prometheusAlertRule.setAlertName(clusterAlertQuota.getAlertQuotaName());
                prometheusAlertRule.setAlertExpr(clusterAlertQuota.getAlertExpr() + " " + clusterAlertQuota.getCompareMethod() + " " + clusterAlertQuota.getAlertThreshold());
                prometheusAlertRule.setClusterId(1);
                prometheusAlertRule.setServiceRoleName(clusterAlertQuota.getServiceRoleName());
                prometheusAlertRule.setAlertLevel(clusterAlertQuota.getAlertLevel().getDesc());
                prometheusAlertRule.setAlertAdvice(clusterAlertQuota.getAlertAdvice());
                prometheusAlertRule.setTriggerDuration(clusterAlertQuota.getTriggerDuration());
                return prometheusAlertRule;
            }
        }).collect(Collectors.toList());

        // 1.加载模板
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.getVersion());
        // 设置加载的目录
        config.setDirectoryForTemplateLoading(new File("/Volumes/Samsung_T5/opensource/e-mapreduce/cloudeon-stack/UDH-1.0.0/monitor/render/rule"));

        // 数据对象
        Map<String, Object> data = new HashMap<>();
        data.put("itemList", prometheusAlertRules);
        data.put("serviceName", "internal-service");

        // 得到模板对象
        Template template = config.getTemplate("rules-internal.yml.ftl");
        FileWriter out = new FileWriter(new File("/Volumes/Samsung_T5/opensource/e-mapreduce/" + File.separator + "rules-alert.yml"));
        template.process(data, out);
        out.close();
    }
}