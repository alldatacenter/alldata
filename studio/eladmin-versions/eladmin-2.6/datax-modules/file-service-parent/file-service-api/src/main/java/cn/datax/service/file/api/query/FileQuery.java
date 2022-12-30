package cn.datax.service.file.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String fileName;
}
