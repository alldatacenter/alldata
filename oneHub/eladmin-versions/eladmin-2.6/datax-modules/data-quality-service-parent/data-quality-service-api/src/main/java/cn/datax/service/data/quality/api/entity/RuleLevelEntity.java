package cn.datax.service.data.quality.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 规则级别信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
@Accessors(chain = true)
@TableName("quality_rule_level")
public class RuleLevelEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 规则级别编码
     */
    private String code;

    /**
     * 规则级别名称
     */
    private String name;
}
