package cn.datax.service.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 部门关系表
 * </p>
 *
 * @author yuwei
 * @date 2022-11-22
 */
@Data
@Accessors(chain = true)
@TableName("sys_dept_relation")
public class DeptRelationEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 祖先节点
     */
    private String ancestor;

    /**
     * 后代节点
     */
    private String descendant;

}
