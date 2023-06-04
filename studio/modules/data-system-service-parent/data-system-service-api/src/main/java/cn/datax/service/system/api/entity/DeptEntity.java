package cn.datax.service.system.api.entity;

import cn.datax.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author yuwei
 * @date 2022-09-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_dept")
public class DeptEntity extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 父部门ID
     */
    private String parentId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 一级：10
     * 二级：10 001、10 002
     * 三级：10 001 001、10 001 002、10 002 001
     * 部门编码（数据权限优化查询速度 like '10001%'）
     */
    private String deptNo;
}
