package cn.datax.service.workflow.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 业务流程配置表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-22
 */
@Data
public class BusinessVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String businessCode;
    private String businessName;
    private String businessComponent;
    private String businessAuditGroup;
    private String processDefinitionId;
    private String businessTempalte;
}
