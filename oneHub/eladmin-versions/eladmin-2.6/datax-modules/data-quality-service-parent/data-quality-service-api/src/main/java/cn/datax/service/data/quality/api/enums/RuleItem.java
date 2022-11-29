package cn.datax.service.data.quality.api.enums;

public enum RuleItem {

    Unique("unique_key", "验证用户指定的字段是否具有唯一性"),
    AccuracyLength("accuracy_key_length", "验证长度是否符合规定"),
    Integrity("integrity_key", "验证表中必须出现的字段非空"),
    Relevance("relevance_key", "验证关联性"),
    Timeliness("timeliness_key", "验证及时性"),
    Consistent("consistent_key", "验证用户指定的字段枚举值是否合乎要求");

    private final String code;

    private final String desc;

    RuleItem(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public static RuleItem getRuleItem(String code) {
        for (RuleItem item : RuleItem.values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
