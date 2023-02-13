package cn.datax.service.data.quality.schedule;

import cn.datax.common.exception.DataException;
import cn.datax.service.data.quality.schedule.rules.RuleItem;
import cn.datax.service.data.quality.schedule.rules.RuleItemRegistry;

import java.util.Optional;

public class CheckRuleFactory {

    private static final RuleItemRegistry RULE_ITEM_REGISTRY = new RuleItemRegistry();

    public CheckRuleFactory() {
    }

    public static RuleItem getRuleItem(String code) {
        return Optional.ofNullable(RULE_ITEM_REGISTRY.getRuleItem(code)).orElseThrow(() -> new DataException(String.format("%s not supported.", code)));
    }
}
