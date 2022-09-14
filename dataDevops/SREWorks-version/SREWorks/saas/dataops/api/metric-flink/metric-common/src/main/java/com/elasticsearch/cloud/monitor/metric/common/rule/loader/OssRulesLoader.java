package com.elasticsearch.cloud.monitor.metric.common.rule.loader;

import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;
import lombok.extern.slf4j.Slf4j;
import java.util.List;


/**
 * oss规则loader
 *
 * @author: fangzong.lyj
 * @date: 2021/09/01 15:33
 */
@Slf4j
public class OssRulesLoader extends AbstractRulesLoader {

    @Override
    protected String readObject(String var1) {
        return null;
    }

    @Override
    public List<Rule> load() throws Exception {
        return null;
    }

    @Override
    public String getVersion() throws Exception {
        return null;
    }

    @Override
    public String getRuleFileOrDir() {
        return null;
    }

    @Override
    public void close() {

    }
}
