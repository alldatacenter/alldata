package cn.datax.service.data.quality.schedule.rules;

import java.util.HashMap;
import java.util.Map;

public class RuleItemRegistry {

    private final Map<String, RuleItem> rule_item_map = new HashMap<>();

    public RuleItemRegistry() {
        this.rule_item_map.put("unique_key", new UniqueRule());
        this.rule_item_map.put("consistent_key", new ConsistentRule());
        this.rule_item_map.put("integrity_key", new IntegrityRule());
        this.rule_item_map.put("relevance_key", new RelevanceRule());
        this.rule_item_map.put("timeliness_key", new TimelinessRule());
        this.rule_item_map.put("accuracy_key_length", new AccuracyLengthRule());
    }

    public RuleItem getRuleItem(String code) {
        return this.rule_item_map.get(code);
    }
}
