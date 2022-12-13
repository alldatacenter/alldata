package cn.datax.service.data.quality.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 规则核查类型信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Data
@Accessors(chain = true)
@TableName("quality_rule_item")
public class RuleItemEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 规则类型
     */
    private String ruleTypeId;

    /**
     * 核查类型编码
     */
    private String itemCode;

    /**
     * 核查类型解释
     */
    private String itemExplain;
}
