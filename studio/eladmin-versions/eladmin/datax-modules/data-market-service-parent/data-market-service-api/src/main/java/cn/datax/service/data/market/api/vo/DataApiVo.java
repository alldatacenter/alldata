package cn.datax.service.data.market.api.vo;

import cn.datax.service.data.market.api.dto.ExecuteConfig;
import cn.datax.service.data.market.api.dto.RateLimit;
import cn.datax.service.data.market.api.dto.ReqParam;
import cn.datax.service.data.market.api.dto.ResParam;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 数据API信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Data
public class DataApiVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String apiName;
    private String apiVersion;
    private String apiUrl;
    private String reqMethod;
    private String deny;
    private String resType;
    private RateLimit rateLimit;
    private ExecuteConfig executeConfig;
    private List<ReqParam> reqParams;
    private List<ResParam> resParams;
}
