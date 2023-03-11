package com.hw.lineage.server.start;

import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * There is a custom implementation in Mybatis Generator, and an error will be reported when using the maven plugin.
 * At this time, it will be run through the Java program MybatisGenerator
 *
 * @description: MybatisGenerator
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class MybatisGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(MybatisGenerator.class);

    private static final String MYBATIS_GENERATOR_CONFIG = "mybatis/mybatis-generator.xml";

    public static void main(String[] args) {
        try {
            InputStream input = MybatisGenerator.class.getClassLoader().getResourceAsStream(MYBATIS_GENERATOR_CONFIG);
            List<String> warnings = new ArrayList<>();
            ConfigurationParser cp = new ConfigurationParser(warnings);

            Configuration config = cp.parseConfiguration(input);
            DefaultShellCallback callback = new DefaultShellCallback(true);
            MyBatisGenerator myBatisGenerator = new MyBatisGenerator(config, callback, warnings);
            // generate file
            myBatisGenerator.generate(null);
            warnings.forEach(LOG::warn);
        } catch (Exception e) {
            LOG.error("Mybatis Generator is error", e);
            Thread.currentThread().interrupt();
        }
    }
}
