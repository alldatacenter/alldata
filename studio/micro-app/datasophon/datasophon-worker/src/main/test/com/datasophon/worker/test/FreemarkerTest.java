package com.datasophon.worker.test;

import com.alibaba.fastjson.JSONObject;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.worker.utils.FreemakerUtils;
import freemarker.template.TemplateException;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FreemarkerTest {

    @Test
    public void generateCustomTemplate() throws IOException, TemplateException {
        Generators generators = new Generators();
        generators.setConfigFormat("custom");
        generators.setFilename("alertmanager.yml");
        generators.setTemplateName("alertmanager.yml");
        generators.setOutputDirectory("D:\\360downloads\\test");

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName("apiHost");
        serviceConfig.setValue("ddp1016");

        ServiceConfig serviceConfig2 = new ServiceConfig();
        serviceConfig2.setName("apiPort");
        serviceConfig2.setValue("8081");

        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
        serviceConfigs.add(serviceConfig);
        serviceConfigs.add(serviceConfig2);

        FreemakerUtils.generateConfigFile(generators,serviceConfigs,"");
    }


    @Test
    public void generatePRCustomTemplate() throws IOException, TemplateException {

        Generators nodeGenerators = new Generators();
        nodeGenerators.setFilename("linux.json");
        nodeGenerators.setOutputDirectory("");
        nodeGenerators.setConfigFormat("custom");
        nodeGenerators.setTemplateName("scrape.ftl");
        ArrayList<ServiceConfig> workerServiceConfigs = new ArrayList<>();
        ArrayList<ServiceConfig> nodeServiceConfigs = new ArrayList<>();


        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName("worker_1");
        serviceConfig.setValue("yc1:8585");
        serviceConfig.setRequired(true);
        workerServiceConfigs.add(serviceConfig);

        ServiceConfig nodeServiceConfig = new ServiceConfig();
        nodeServiceConfig.setName("node_1");
        nodeServiceConfig.setValue("yc1:9100");
        nodeServiceConfig.setRequired(true);
        nodeServiceConfigs.add(nodeServiceConfig);


        FreemakerUtils.testGenerateConfigFile(nodeGenerators,nodeServiceConfigs,"");
    }
    @Test
    public void generateSRCustomTemplate() throws IOException, TemplateException {

        HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();


        ArrayList<String> feList = new ArrayList<>();
        ArrayList<String> beList = new ArrayList<>();

        feList.add("yc1:9030");
        feList.add("yc2:9030");
        beList.add("yc3:9050");
        beList.add("yc2:9050");
        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
        Generators generators = new Generators();
        generators.setFilename("starrocks.json");
        generators.setOutputDirectory("");
        generators.setConfigFormat("custom");
        generators.setTemplateName("starrocks-prom.ftl");

        ServiceConfig feServiceConfig = new ServiceConfig();
        feServiceConfig.setName("feList");
        feServiceConfig.setValue(feList);
        feServiceConfig.setConfigType("map");

        ServiceConfig beServiceConfig = new ServiceConfig();
        beServiceConfig.setName("beList");
        beServiceConfig.setValue(beList);
        beServiceConfig.setConfigType("map");
        serviceConfigs.add(feServiceConfig);
        serviceConfigs.add(beServiceConfig);
        configFileMap.put(generators,serviceConfigs);

        FreemakerUtils.testGenerateConfigFile(generators,serviceConfigs,"");
    }

    @Test
    public void generateFairSchdulerXml() throws IOException, TemplateException {
        Generators generators = new Generators();
        generators.setFilename("fair-scheduler.xml");
        generators.setOutputDirectory("D:\\360downloads\\test");
        generators.setConfigFormat("custom");
        generators.setTemplateName("fair-scheduler.ftl");
        ArrayList<JSONObject> queueList = new ArrayList<>();
        JSONObject queue = new JSONObject();
        queue.put("queueName","test6");
        queue.put("minResources","1cores,1024mb");
        queue.put("maxResources","4cores,4096mb");
        queue.put("amShare","0.1");
        queue.put("weight",1);
        queue.put("schedulePolicy","fair");
        queue.put("appNum",4);
        queueList.add(queue);
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName("queueList");
        serviceConfig.setConfigType("map");
        serviceConfig.setValue(queueList);


        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
        serviceConfigs.add(serviceConfig);


        FreemakerUtils.generateConfigFile(generators,serviceConfigs,"");
    }

    @Test
    public void generateCustomTemplate1() throws IOException, TemplateException {
        Generators generators = new Generators();
        generators.setFilename("worker.json");
        generators.setOutputDirectory("configs");
        generators.setConfigFormat("custom");
        generators.setTemplateName("scrape.ftl");

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setValue("ddp1016:9100");

        ServiceConfig serviceConfig2 = new ServiceConfig();
        serviceConfig.setValue("ddp1017:9100");

        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
        serviceConfigs.add(serviceConfig);
        serviceConfigs.add(serviceConfig2);

        FreemakerUtils.generateConfigFile(generators,serviceConfigs,"");
    }
    @Test
    public void generateHadoopEnv() throws IOException, TemplateException {
        Generators generators = new Generators();
        generators.setFilename("hadoop-env.sh");
        generators.setOutputDirectory("D:\\360downloads\\test");
        generators.setConfigFormat("custom");
        generators.setTemplateName("hadoop-env.ftl");

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName("namenodeHeapSize");
        serviceConfig.setConfigType("map");
        serviceConfig.setValue("1024");

        ServiceConfig serviceConfig2 = new ServiceConfig();
        serviceConfig2.setName("datanodeHeapSize");
        serviceConfig2.setConfigType("map");
        serviceConfig2.setValue("1024");

        ServiceConfig serviceConfig3 = new ServiceConfig();
        serviceConfig3.setName("hadoopLogDir");
        serviceConfig3.setConfigType("map");
        serviceConfig3.setValue("/var/log/hadoop");

        ServiceConfig serviceConfig4 = new ServiceConfig();
        serviceConfig4.setName("HADOOP_EXPORT_DIR");
        serviceConfig4.setValue("/var/log/hadoop");

        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
        serviceConfigs.add(serviceConfig);
        serviceConfigs.add(serviceConfig2);
        serviceConfigs.add(serviceConfig3);
        serviceConfigs.add(serviceConfig4);
        FreemakerUtils.generateConfigFile(generators,serviceConfigs,"");
    }

    @Test
    public void testKrb5Ftl() throws IOException, TemplateException {
        Generators generators = new Generators();
        generators.setFilename("krb5.conf");
        generators.setOutputDirectory("D:\\360downloads\\test");
        generators.setConfigFormat("custom");
        generators.setTemplateName("krb5.ftl");

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName("ticketLifetime");
        serviceConfig.setConfigType("map");
        serviceConfig.setValue("24h");

        ServiceConfig serviceConfig2 = new ServiceConfig();
        serviceConfig2.setName("renewLifetime");
        serviceConfig2.setConfigType("map");
        serviceConfig2.setValue("7d");

        ServiceConfig serviceConfig3 = new ServiceConfig();
        serviceConfig3.setName("realm");
        serviceConfig3.setConfigType("map");
        serviceConfig3.setValue("HADOOP.COM");

        ServiceConfig serviceConfig4 = new ServiceConfig();
        serviceConfig4.setName("kdcHost");
        serviceConfig4.setConfigType("map");
        serviceConfig4.setValue("ddp2");

        ServiceConfig serviceConfig5 = new ServiceConfig();
        serviceConfig5.setName("kadminHost");
        serviceConfig5.setConfigType("map");
        serviceConfig5.setValue("ddp2");

        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
        serviceConfigs.add(serviceConfig);
        serviceConfigs.add(serviceConfig2);
        serviceConfigs.add(serviceConfig3);
        serviceConfigs.add(serviceConfig4);
        serviceConfigs.add(serviceConfig5);
        FreemakerUtils.generateConfigFile(generators,serviceConfigs,"");
    }


}
