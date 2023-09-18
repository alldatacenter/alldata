package datart.core.entity.poi.format;

import datart.core.base.consts.UnitKey;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;

@Data
public class PoiNumFormat {

    /** 小数位数 */
    private String decimalPlaces;
    /** 启用分隔符 */
    private boolean useThousandSeparator = false;
    /** 单位 */
    private String unitKey;

    public String getUseThousandSeparator() {
        return useThousandSeparator ? "#,##" : "";
    }

    public int getDecimalPlacesNum() {
        return StringUtils.isNumeric(decimalPlaces) ? NumberUtils.toInt(decimalPlaces) : 0;
    }

    public String getUnitKey() {
        if (StringUtils.isNotBlank(this.unitKey)){
            UnitKey unitKey = UnitKey.getUnitKeyByValue(this.unitKey);
            return unitKey.getFmt();
        }
        return unitKey;
    }

    public String getDecimalPlaces() {
        String format = "0";
        String decimalStr = "";
        for (int i = 0; i < getDecimalPlacesNum(); i++) {
            decimalStr += "0";
        }
        return StringUtils.isBlank(decimalStr) ? format : format+"."+decimalStr;
    }

    public Object parseValue(Object obj){
        if (obj!=null && org.apache.commons.lang.math.NumberUtils.isNumber(obj.toString()) && StringUtils.isNotBlank(this.unitKey)){
            UnitKey unitKey = UnitKey.getUnitKeyByValue(this.unitKey);
            BigDecimal val = new BigDecimal(obj.toString()).divide(new BigDecimal(unitKey.getUnit()));
            obj = val.setScale(getDecimalPlacesNum(), BigDecimal.ROUND_HALF_UP);
        }
        return obj;
    }

    public int getFixLength(){
        int num = getDecimalPlacesNum();
        if (StringUtils.isNotBlank(unitKey)){
            num = num+1;
        }
        return num;
    }

    public String getFormat(){
        return "";
    };
}
