package cn.datax.service.data.metadata.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class SqlConsoleVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String sql;
    private Long time;
    private Boolean success;
    private Integer count;
    private List<String> columnList;
    private List<Map<String, Object>> dataList;
}
