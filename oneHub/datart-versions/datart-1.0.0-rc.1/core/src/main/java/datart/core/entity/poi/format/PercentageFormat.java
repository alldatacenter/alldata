package datart.core.entity.poi.format;

import datart.core.entity.poi.format.PoiNumFormat;
import lombok.Data;

@Data
public class PercentageFormat extends PoiNumFormat {

    public static final String type = "percentage";

    @Override
    public String getFormat() {
        return this.getDecimalPlaces() + "%";
    }
}
