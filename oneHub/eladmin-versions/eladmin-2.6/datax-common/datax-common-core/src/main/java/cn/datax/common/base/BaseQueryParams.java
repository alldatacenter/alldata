package cn.datax.common.base;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BaseQueryParams implements Serializable {

    private static final long serialVersionUID = 1L;

    // 关键字
    private String keyword;
    // 当前页码
    private Integer pageNum = 1;
    // 分页条数
    private Integer pageSize = 20;
    // 排序
    private List<OrderItem> orderList;
    // 数据权限
    private String dataScope;

    @Data
    public class OrderItem{
        private String column;
        private boolean asc;
    }
}
