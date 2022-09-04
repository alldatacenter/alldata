package com.elasticsearch.cloud.monitor.metric.common.rule.loader;

import com.elasticsearch.cloud.monitor.metric.common.client.MinioConfig;
import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

/**
 * minio规则loader
 *
 * @author: fangzong.lyj
 * @date: 2021/09/01 15:33
 */
@Slf4j
public class MinioRulesLoader extends AbstractRulesLoader {

    private MinioConfig minioConfig;

    private MinioClient minioClient;

    public MinioRulesLoader(MinioConfig minioConfig) {
        this.minioConfig = minioConfig;
        minioClient = MinioClient.builder().endpoint(minioConfig.getEndpoint()).credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey()).build();
    }

    @Override
    protected String readObject(String key) {
        return key.endsWith("zip") ? this.readByZipFile(key) : this.readNormalFile(key);
    }

    @Override
    public List<Rule> load() throws Exception {
        Set<Rule> rules = new HashSet();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        String content = this.readObject(minioConfig.getFile());
        if (StringUtils.isEmpty(content)) {
            log.error("load " + minioConfig.getFile() + " empty");
            return null;
        }

        List<Rule> ruleList;
        try {
            ruleList = objectMapper.readValue(content, new TypeReference<Object>(){});
        } catch (Exception ex) {
            log.error("load " + minioConfig.getFile() + " failed", ex);
            return null;
        }

        ruleList.forEach(rule -> {
            try {
                this.ruleToLowerCase(rule);
                rule.validate();
                rules.add(rule);
            } catch (Exception ex) {
                log.error(String.format("rule loader validate rule %s content %s error %s", rule.getId(), rule.toString(), ex.getMessage()), ex);
            }
        });

        return new ArrayList(rules);
    }

    @Override
    public String getVersion() throws Exception {
        String version = null;
        try (GetObjectResponse content = minioClient.getObject(GetObjectArgs.builder().bucket(minioConfig.getBucket()).object(minioConfig.getFile()).build())) {
            Headers objectHeaders = content.headers();
            version = objectHeaders.get("x-amz-version-id");
        } catch (Exception ex) {
            log.error("load object" + minioConfig.getFile() + " version failed", ex);
        }
        return version;
    }

    @Override
    public String getRuleFileOrDir() {
        return minioConfig.getBucket() + "/" + minioConfig.getFile();
    }

    @Override
    public void close() {
    }

    private String readNormalFile(String key) {
        StringBuilder data = new StringBuilder();

        try (GetObjectResponse content = minioClient.getObject(GetObjectArgs.builder().bucket(minioConfig.getBucket()).object(minioConfig.getFile()).build())) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            while(true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                data.append(line);
            }
        } catch (Exception ex) {
            log.error("load " + minioConfig.getFile() + " failed", ex);
        }

        return data.toString();
    }

    private String readByZipFile(String key) {
        try(
                GetObjectResponse content = minioClient.getObject(GetObjectArgs.builder().bucket(minioConfig.getBucket()).object(minioConfig.getFile()).build());
                ZipInputStream zin = new ZipInputStream(content);
                ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()
        ) {
            if (zin.getNextEntry() == null) {
                throw new RuntimeException("unzip rule file error");
            } else {
                for(int c = zin.read(); c != -1; c = zin.read()) {
                    byteOutputStream.write(c);
                }
                zin.closeEntry();
                return byteOutputStream.toString();
            }
        } catch (Exception ex) {
            log.error("Unzip exception", ex);
            throw new RuntimeException("unzip rule file error");
        }
    }
}
