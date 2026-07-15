package cn.datax.service.system.api.entity;

import cn.datax.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 
 * </p>
 *
 * @author yuwei
 * @date 2022-09-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "sys_user", autoResultMap = true)
public class UserEntity extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 出生日期
     */
    private LocalDate birthday;

    /**
     * 部门
     */
    private String deptId;

    @TableField(exist = false)
    private DeptEntity dept;

    @TableField(exist = false)
    private List<RoleEntity> roles;

    @TableField(exist = false)
    private List<PostEntity> posts;
}
