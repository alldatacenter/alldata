package datart.core.base.consts;

public enum Currency {
    CNY("¥"),
    USD("$", "US"),
    GBP("£"),
    AUD("$", "AU"),
    EUR("€"),
    JPY("¥", "JP"),
    CAD("$", "CA");

    private String unit;

    private String prefix;

    Currency(String unit) {
        this.unit = unit;
        this.prefix = "";
    }

    Currency(String unit, String prefix) {
        this.unit = unit;
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getUnit() {
        return unit;
    }
}
