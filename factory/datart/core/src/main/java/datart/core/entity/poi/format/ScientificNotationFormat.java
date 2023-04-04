package datart.core.entity.poi.format;

import lombok.Data;

@Data
public class ScientificNotationFormat extends PoiNumFormat {

    public static final String type = "scientificNotation";

    @Override
    public String getFormat() {
        return this.getDecimalPlaces() + "E+0";
    }
}
