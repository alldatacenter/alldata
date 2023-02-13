package com.elasticsearch.cloud.monitor.metric.common.rule.loader;

import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 健康服务告警规则loader
 *
 * @author: fangzong.lyj
 * @date: 2022/01/19 15:33
 */
@Slf4j
public class HealthAlertRulesLoader extends AbstractRulesLoader {

    private String rulesConfig;

    private String rulesFormatContent;

    private String version = "1.0";


    public HealthAlertRulesLoader(String rulesConfig) {
        this.rulesConfig = rulesConfig;
        this.rulesFormatContent = readObject(rulesConfig);
    }

    @Override
    protected String readObject(String key) {
        // 解析成emon可以识别的格式
        return "[" + key + "]";
    }

    @Override
    public List<Rule> load() throws Exception {
        if (StringUtils.isEmpty(rulesFormatContent)) {
            log.error("load " + rulesConfig + " empty");
            return null;
        }

        Set<Rule> rules = new HashSet();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

        List<Rule> ruleList;
        try {
            ruleList = objectMapper.readValue(rulesFormatContent, new TypeReference<List<Rule>>(){});
        } catch (Exception ex) {
            log.error("load " + rulesConfig + " failed", ex);
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
        version = DigestUtils.md5DigestAsHex(rulesConfig.getBytes());
        return version;
    }

    @Override
    public String getRuleFileOrDir() {
        return rulesConfig;
    }

    @Override
    public void close() {
    }
}
