package cn.datax.service.system.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@ApiModel(value = "用户Model")
@Data
public class UserDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;

    @ApiModelProperty(value = "用户名称")
    @NotBlank(message = "用户名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Length(min=3, max = 12, message="用户名称长度必须位于{min}-{max}之间")
    private String username;

    @ApiModelProperty(value = "用户昵称")
    @NotBlank(message = "用户昵称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String nickname;

    @ApiModelProperty(value = "用户密码")
    @NotBlank(message = "用户密码不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String password;

    @ApiModelProperty(value = "电子邮箱")
    @NotBlank(message = "电子邮箱不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Email(message = "请输入正确的邮箱")
    private String email;

    @ApiModelProperty(value = "手机号码")
    @NotBlank(message = "手机号码不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String phone;

    @ApiModelProperty(value = "出生日期", example = "2019-09-09")
    @NotNull(message = "出生日期不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @ApiModelProperty(value = "部门")
    @NotBlank(message = "部门不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String deptId;

    @ApiModelProperty(value = "角色")
    @NotEmpty(message = "角色不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Size(min = 1, max = 5, message="角色长度必须位于{min}-{max}之间")
    private List<String> roleList;

    @ApiModelProperty(value = "岗位")
    @NotEmpty(message = "岗位不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Size(min = 1, max = 1, message="岗位长度必须位于{min}-{max}之间")
    private List<String> postList;

    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;

    @ApiModelProperty(value = "备注")
    private String remark;
}
