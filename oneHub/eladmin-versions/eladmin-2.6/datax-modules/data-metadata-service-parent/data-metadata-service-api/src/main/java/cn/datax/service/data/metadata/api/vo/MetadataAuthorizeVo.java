package cn.datax.service.data.metadata.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 数据授权信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-23
 */
@Data
public class MetadataAuthorizeVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String objectId;
    private String roleId;
    private String objectType;
}
