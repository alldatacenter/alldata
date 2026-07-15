package cn.datax.service.system.api.entity;

import cn.datax.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

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
@TableName("sys_menu")
public class MenuEntity extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 父资源ID
     */
    private String parentId;

    /**
     * 资源名称
     */
    private String menuName;

    /**
     * 对应路由地址path
     */
    private String menuPath;

    /**
     * 对应路由组件component
     */
    private String menuComponent;

    /**
     * 对应路由默认跳转地址redirect
     */
    private String menuRedirect;

    /**
     * 权限标识
     */
    private String menuPerms;

    /**
     * 资源图标
     */
    private String menuIcon;

    /**
     * 资源类型（0模块，1菜单，2按钮）
     */
    private String menuType;

    /**
     * 资源编码
     */
    private String menuCode;

    /**
     * 资源隐藏（0否，1是）
     */
    private String menuHidden;

    /**
     * 排序
     */
    private Integer menuSort;

}
