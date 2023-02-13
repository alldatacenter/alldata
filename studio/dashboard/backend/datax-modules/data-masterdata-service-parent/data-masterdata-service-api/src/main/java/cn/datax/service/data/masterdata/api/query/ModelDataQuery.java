package cn.datax.service.data.masterdata.api.query;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ModelDataQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    // 数据库表名
    private String tableName;
    // 查询条件
    private List<Condition> conditions;
    // 查询字段
    private List<String> columns;

    // 关键字
    private String keyword;
    // 当前页码
    private Integer pageNum = 1;
    // 分页条数
    private Integer pageSize = 20;
}
