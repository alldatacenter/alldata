package datart.server.base.dto.chart;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import datart.core.entity.poi.format.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChartColumn {

    private String uid = "";

    private String colName = "";

    private String category = "";

    private String type = "";

    private String label = "";

    private ChartColumn.Alias alias = new ChartColumn.Alias();

    private String desc = "";

    private boolean isGroup;

    private String aggregate = "";

    private List<ChartColumn> children = new ArrayList<>();

    private JSONObject format;

    private int leafNum = 0;

    private int deepNum = 1;

    public void setChildren(List<ChartColumn> children) {
        this.children = children;
        this.leafNum = calLeafNum();
        this.deepNum = calDeepNum();
    }

    public String getDisplayName() {
        return isGroup ? label :
                StringUtils.isNotBlank(aggregate) ? aggregate+"("+colName+")" : colName;
    }

    public PoiNumFormat getNumFormat(){
        PoiNumFormat numFormat = new PoiNumFormat();
        if (format == null || !format.containsKey("type")){
            return numFormat;
        }
        String type = format.getString("type");
        switch (type) {
            case NumericFormat.type:
                numFormat = JSON.parseObject(format.getString(type), NumericFormat.class);
                break;
            case CurrencyFormat.type:
                numFormat = JSON.parseObject(format.getString(type), CurrencyFormat.class);
                break;
            case PercentageFormat.type:
                numFormat = JSON.parseObject(format.getString(type), PercentageFormat.class);
                break;
            case ScientificNotationFormat.type:
                numFormat = JSON.parseObject(format.getString(type), ScientificNotationFormat.class);
                break;
            default:
                break;
        }
        return numFormat;
    }

    public List<ChartColumn> getLeafNodes(){
        List<ChartColumn> leafNodes = new ArrayList<>();
        if (this.leafNum == 0 && !this.isGroup){
            leafNodes.add(this);
        }
        for (ChartColumn child : children) {
            leafNodes.addAll(child.getLeafNodes());
        }
        return leafNodes;
    }

    private int calLeafNum() {
        int num = 0;
        for (ChartColumn child : children) {
            if (child.getLeafNum()==0) {
                num++;
            } else {
                num += child.getLeafNum();
            }
        }
        return num;
    }

    private int calDeepNum() {
        int num = 0;
        for (ChartColumn child : children) {
            if (child.getDeepNum()>num){
                num = child.getDeepNum();
            }
        }
        return num+1;
    }

    @Data
    public class Alias {
        private String name;
    }

}
