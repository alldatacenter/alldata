package cn.datax.service.data.quality.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 规则类型信息表 实体VO
 * </p>
 *
 * @author yuwei
 * @since 2020-09-27
 */
@Data
public class RuleTypeVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String name;
    private String code;
}
