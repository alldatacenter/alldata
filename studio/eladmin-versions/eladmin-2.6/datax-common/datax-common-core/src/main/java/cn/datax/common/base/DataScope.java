package cn.datax.common.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据权限查询参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataScope {

    /**
     * 表的部门字段
     */
    private String deptScopeName = "create_dept";

    /**
     * 表的用户字段
     */
    private String userScopeName = "create_by";
}
