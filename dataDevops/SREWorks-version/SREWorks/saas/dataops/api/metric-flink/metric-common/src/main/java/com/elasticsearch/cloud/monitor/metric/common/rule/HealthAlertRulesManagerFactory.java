package com.elasticsearch.cloud.monitor.metric.common.rule;


/**
 * 健康管理告警规则管理工厂类
 *
 * @author: fangzong.lyj
 * @date: 2022/01/19 14:32
 */
public class HealthAlertRulesManagerFactory extends SreworksRulesManagerFactory<HealthAlertRulesManager> {

    public HealthAlertRulesManagerFactory(Long refreshPeriod, Long shufflePeriod) {
        super(refreshPeriod, shufflePeriod);
    }

    @Override
    protected HealthAlertRulesManager load(Object rulesConfig) {
        HealthAlertRulesManager rulesManager = new HealthAlertRulesManager((String)rulesConfig);
        // rulemanager层面关闭定时更新机制, 由于相对轻量, 告警检测规则放在factory层面定时更新
//        rulesManager.startShuffleTimingUpdate(this.RULE_REFRESH_PERIOD_DEF, this.RULE_REFRESH_SHUFFLE_DEF);
        return rulesManager;
    }
}
