package datart.core.entity.poi;

import datart.core.entity.poi.format.PoiNumFormat;
import lombok.Data;
import org.apache.poi.ss.usermodel.CellStyle;

@Data
public class ColumnSetting {

    private int index;

    private PoiNumFormat numFormat;

    private int length;

    private CellStyle cellStyle;

    public void setLength(int length) {
        this.length = Math.max(length, 8);
        this.length = Math.min(this.length, 80);
    }

    public int getWidth() {
        return (numFormat.getFixLength()+length) * 300;
    }
}
