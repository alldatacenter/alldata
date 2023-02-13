package cn.datax.service.codegen.api.vo;

import cn.datax.service.codegen.api.dto.GenColumnDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 代码生成信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-19
 */
@Data
public class GenTableVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String tableName;
    private String tableComment;
    private String className;
    private String packageName;
    private String moduleName;
    private String businessName;
    private String functionName;
    private String functionAuthor;
    private List<GenColumnDto> columns;
}
