package com.elasticsearch.cloud.monitor.metric.common.rule;

import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.event.Event;
import com.elasticsearch.cloud.monitor.metric.common.rule.compose.Monitor;
import com.elasticsearch.cloud.monitor.metric.common.rule.loader.RulesLoader;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 规则管理类
 *
 * @author: fangzong.lyj
 * @date: 2021/09/01 10:45
 */
@Slf4j
public abstract class ClientRulesManager extends SreworksRulesManager {
    private RulesLoader loader;
    private RulesHolder holder;
    private Timer updateTimer;
    private boolean updated = false;

    public ClientRulesManager(Object clientConfig) {
        this(clientConfig, null);
    }

    public ClientRulesManager(Object clientConfig, RulesLoader loader) {
        if (loader != null) {
            this.loader = loader;
        } else {
            this.loader = this.initRuleLoader(clientConfig);
        }

        this.holder = new RulesHolder(Collections.emptyList());
        this.checkAndLoad();
    }

    public List<SubQuery> match(DataPoint dp) {
        Preconditions.checkNotNull(this.holder, "holder is null");
        return holder.match(dp);
    }

    public List<Rule> match(Event event) {
        List<Rule> ret = new ArrayList();
        int ruleId = 0;
        Set<String> tags = event.getTags();
        if (tags == null) {
            return ret;
        } else {
            Iterator var5 = tags.iterator();

            while(var5.hasNext()) {
                String tag = (String)var5.next();
                String[] kv = tag.split("=");
                if (kv[0].equals("__ruleid")) {
                    ruleId = Integer.valueOf(kv[1]);
                }
            }

            if (ruleId == 0) {
                return ret;
            } else {
                List<Rule> rules = this.holder.getRules();
                if (rules == null) {
                    return ret;
                } else {
                    Iterator var12 = rules.iterator();

                    while(true) {
                        Rule rule;
                        do {
                            if (!var12.hasNext()) {
                                return ret;
                            }

                            rule = (Rule)var12.next();
                        } while(rule.getMonitorCompose() == null);

                        boolean match = false;
                        Iterator var9 = rule.getMonitorCompose().getMonitors().iterator();

                        while(var9.hasNext()) {
                            Monitor monitor = (Monitor)var9.next();
                            if (monitor.getRuleId() == ruleId) {
                                match = true;
                            }
                        }

                        if (match) {
                            ret.add(rule);
                        }
                    }
                }
            }
        }
    }

    public List<AggRule> matchForAgg(DataPoint dp) {
        List<AggRule> ret = new ArrayList();

        List<SubQuery> matchedRule = match(dp);
        if (matchedRule != null && matchedRule.size() > 0) {
            Map<String, AggRule> aggRuleMap = new HashMap();

            matchedRule.forEach(subQuery -> {
                String aggId = subQuery.getAggregateInfo();
                AggRule aggRule = aggRuleMap.get(aggId);
                if (aggRule == null) {
                    aggRule = AggRule.from(subQuery);
                    aggRuleMap.put(aggId, aggRule);
                }

                if (subQuery instanceof Rule && ((Rule)subQuery).getNoDataCondition() != null) {
                    aggRule.addNodataId(subQuery.getParentId());
                } else {
                    aggRule.addId(subQuery.getParentId());
                }
            });

            ret.addAll(aggRuleMap.values());

        }

        return ret;
    }

    public List<Rule> getAllRules() {
        Preconditions.checkNotNull(this.holder, "holder is null");
        return holder.getRules();
    }

    public Rule getRule(long id) {
        Optional<Rule> opt = holder.getRules().stream().filter(rule -> rule.getId() == id).findFirst();
        return opt.orElse(null);
    }

    public void startShuffleTimingUpdate(long period, long shuffle) {
        Random random = new Random();
        long delta = (long)(random.nextInt((int)(shuffle / 1000L)) * 1000L);
        startTimingUpdate(period + delta);
    }

    public void startTimingUpdate(long period) {
        Preconditions.checkArgument(period > 0L, "period <= 0");
        this.updateTimer = new Timer(true);
        this.updateTimer.schedule(new TimerTask() {
            public void run() {
                checkAndLoad();
            }
        }, period, period);
    }

    public void stopUpdateTiming() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer.purge();
        }

        loader.close();
    }

    /** @deprecated */
    @Deprecated
    public long getCurVersion() {
        return -9223372036854775808L;
    }

    protected abstract RulesLoader initRuleLoader(Object clientConfig);

    private void checkAndLoad() {
        int ruleSize = 0;
        boolean updated = false;
        try {
            List<Rule> all = new ArrayList();
            try {
                String curVersion = loader.getVersion();
                if (curVersion.equals(loader.getLastVersion())) {
                    log.info("Rule version does not change, version=" + curVersion + ", path=" + loader.getRuleFileOrDir());
                } else {
                    updated = true;
                    loader.setLastVersion(curVersion);
                }
            } catch (Exception ex) {
                log.error("Get rule version failed. path=" + loader.getRuleFileOrDir(), ex);
            }

            try {
                List<Rule> rules = loader.load();
                if (rules == null || rules.isEmpty()) {
                    log.warn("Load rules maybe failed. the size of rules is zero");
                } else {
                    ruleSize += rules.size();
                    all.addAll(rules);
                    log.info("Load rules success, path=" + loader.getRuleFileOrDir() + ", rulesSize: " + rules.size());
                }
            } catch (Exception ex) {
                log.error("Load rules failed. path=" + loader.getRuleFileOrDir(), ex);
            }

            if (all.size() > 0) {
                log.info("Load rules success, total rulesSize: " + ruleSize);
                holder = new RulesHolder(all);
            } else {
                log.warn("Load rules maybe fail, total rulesSize: " + ruleSize);
            }
        } catch (Exception ex) {
            log.error("Load rules failed", ex);
        }
        this.updated = updated;
    }

    public MetricStat getMetricStatCache() {
        return holder.getMetricStat();
    }

    public boolean isUpdated() {
        return updated;
    }

    public String toString() {
        return "RuleManager{holder=" + holder + '}';
    }

}
