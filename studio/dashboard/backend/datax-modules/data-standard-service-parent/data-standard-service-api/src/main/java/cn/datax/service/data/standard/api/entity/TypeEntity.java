package cn.datax.service.data.standard.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.datax.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据标准类别表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("standard_type")
public class TypeEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 标准类别编码
     */
    private String gbTypeCode;

    /**
     * 标准类别名称
     */
    private String gbTypeName;
}
