package cn.datax.service.email.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.File;
import java.io.Serializable;
import java.util.List;

@ApiModel(value = "邮件信息表Model")
@Data
public class EmailDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "标题")
    @NotBlank(message = "标题不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String subject;
    @ApiModelProperty(value = "内容")
    @NotBlank(message = "内容不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String text;
    @ApiModelProperty(value = "接收人")
    @NotEmpty(message = "接收人不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Size(min = 1, message="接收人长度不能少于{min}位")
    private List<String> tos;
    @ApiModelProperty(value = "抄送人")
    private List<String> ccs;
    @ApiModelProperty(value = "密送人")
    private List<String> bccs;
    @ApiModelProperty(value = "附件")
    private List<File> files;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
