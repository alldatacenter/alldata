package cn.datax.service.data.quality.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 规则类型信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
@Accessors(chain = true)
@TableName("quality_rule_type")
public class RuleTypeEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 类型编码
     */
    private String code;
}
