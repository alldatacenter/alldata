package datart.server.base.dto.chart;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class WidgetConfig {

    private JSONObject content = new JSONObject();

    public String getChartConfig(){
        if (content.containsKey("dataChart")){
            JSONObject obj = content.getJSONObject("dataChart");
            if (obj.containsKey("config")){
                return obj.getString("config");
            }
        }
        return "";
    }
}
