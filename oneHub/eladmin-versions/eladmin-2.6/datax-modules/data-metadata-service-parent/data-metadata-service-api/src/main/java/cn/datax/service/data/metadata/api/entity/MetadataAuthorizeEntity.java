package cn.datax.service.data.metadata.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.datax.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 数据授权信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-23
 */
@Data
@Accessors(chain = true)
@TableName("metadata_authorize")
public class MetadataAuthorizeEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 目标表主键ID
     */
    private String objectId;

    /**
     * 角色ID
     */
    private String roleId;

    /**
     * 目标表类型
     */
    private String objectType;
}
