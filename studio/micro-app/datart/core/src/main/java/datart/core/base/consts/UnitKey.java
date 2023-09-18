package datart.core.base.consts;

public enum UnitKey {

    NONE("none", 1, ""),
    THOUSAND("thousand", 1000, "K"),
    TEN_THOUSAND("wan", 10_000, "万"),
    MILLION("million", 1000_000, "M"),
    HUNDRED_MILLION("yi", 100_000_000, "亿"),
    BILLION("billion", 1000_000_000, "B");

    private String value;

    private int unit;

    private String fmt;

    UnitKey(String value, int i, String str) {
        this.value = value;
        this.unit = i;
        this.fmt = str;
    }

    public String getValue() {
        return value;
    }

    public int getUnit() {
        return unit;
    }

    public String getFmt() {
        return fmt;
    }

    public static UnitKey getUnitKeyByValue(String value){
        for (UnitKey unitKey : UnitKey.values()) {
            if (unitKey.getValue().equals(value)){
                return unitKey;
            }
        }
        return NONE;
    }

}
