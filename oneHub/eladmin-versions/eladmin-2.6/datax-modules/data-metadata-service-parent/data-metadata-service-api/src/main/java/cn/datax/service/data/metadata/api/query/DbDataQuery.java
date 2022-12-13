package cn.datax.service.data.metadata.api.query;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>
 * 数据查询 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
public class DbDataQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "数据源不能为空")
    private String dataSourceId;
    @NotBlank(message = "查询sql不能为空")
    private String sql;
    // 当前页码
    private Integer pageNum = 1;
    // 分页条数
    private Integer pageSize = 20;

    public Integer getOffset() {
        pageSize = pageSize == null ? 20 : pageSize;
        pageNum = pageNum == null ? 1 : pageNum;
        int offset = pageNum > 0 ? (pageNum - 1) * pageSize : 0;
        return offset;
    }
}
