package cn.datax.service.email.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmailQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String subject;
}
