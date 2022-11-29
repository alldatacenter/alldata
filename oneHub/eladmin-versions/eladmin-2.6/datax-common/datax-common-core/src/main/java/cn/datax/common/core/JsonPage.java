package cn.datax.common.core;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class JsonPage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pageNum;
    private Integer pageSize;
    private Integer total;
    private List<T> data;

    public JsonPage(Long pageNum, Long pageSize, Long total, List<T> data) {
        this.pageNum = pageNum.intValue();
        this.pageSize = pageSize.intValue();
        this.total = total.intValue();
        this.data = data;
    }
}
