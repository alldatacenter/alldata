package cn.datax.service.data.standard.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据标准字典表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("standard_dict")
public class DictEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 所属类别
     */
    private String typeId;

    /**
     * 标准类别编码
     */
    @TableField(exist = false)
    private String gbTypeCode;

    /**
     * 标准类别名称
     */
    @TableField(exist = false)
    private String gbTypeName;

    /**
     * 标准编码
     */
    private String gbCode;

    /**
     * 标准名称
     */
    private String gbName;
}
