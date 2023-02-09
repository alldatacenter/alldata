package com.obs.test.tools;

import com.obs.services.ObsClient;
import com.obs.test.TestTools;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class PrepareTestBucket implements TestRule {
    File configFile = new File("./app/src/test/resource/test_data.properties");

    /**
     * 在用例开始前创建桶，并在用例执行完成后将桶删除
     *
     * @param statement   用例
     * @param description 用例描述，可以获取用例名
     * @return
     */
    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String location = PropertiesTools.getInstance(configFile).getProperties("environment.location");
                String bucketName = description.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
                boolean isPosix = Boolean.parseBoolean(PropertiesTools.getInstance(configFile).getProperties("isPosix"));
                ObsClient obsClient = TestTools.getPipelineEnvironment();
                assertEquals(200, TestTools.createBucket(obsClient, bucketName, location, isPosix).getStatusCode());
                try {
                    statement.evaluate();
                } finally {
                    assertEquals(204, TestTools.delete_bucket(obsClient, bucketName).getStatusCode());
                }

            }
        };
    }
}
