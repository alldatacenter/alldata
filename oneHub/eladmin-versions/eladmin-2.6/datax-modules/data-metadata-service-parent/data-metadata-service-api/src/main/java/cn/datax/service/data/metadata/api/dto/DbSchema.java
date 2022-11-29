package cn.datax.service.data.metadata.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel(value = "数据源连接信息Model")
@Data
public class DbSchema implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主机")
    @NotBlank(message = "主机不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String host;
    @ApiModelProperty(value = "端口")
    @NotNull(message = "端口不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private Integer port;
    @ApiModelProperty(value = "用户名")
    @NotBlank(message = "用户名不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String username;
    @ApiModelProperty(value = "密码")
    @NotBlank(message = "密码不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String password;
    @ApiModelProperty(value = "数据库")
    private String dbName;
    @ApiModelProperty(value = "服务名")
    private String sid;
}
