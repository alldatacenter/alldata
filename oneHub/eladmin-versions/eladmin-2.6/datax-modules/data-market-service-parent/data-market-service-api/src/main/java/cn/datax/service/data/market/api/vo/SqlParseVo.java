package cn.datax.service.data.market.api.vo;

import cn.datax.service.data.market.api.dto.ReqParam;
import cn.datax.service.data.market.api.dto.ResParam;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SqlParseVo implements Serializable {

    private static final long serialVersionUID=1L;

    private List<ReqParam> reqParams;
    private List<ResParam> resParams;
}
