package com.elasticsearch.cloud.monitor.metric.common.rule.loader;

import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;

import java.util.List;

/**
 * 规则加载
 *
 * @author: fangzong.lyj
 * @date: 2021/09/02 14:17
 */
public interface RulesLoader {
    List<Rule> load() throws Exception;

    String getVersion() throws Exception;

    void setLastVersion(String version);

    String getLastVersion();

    String toString();

    String getRuleFileOrDir();

    void close();
}