package cn.datax.service.system.api.vo.route;

import lombok.Data;

import java.io.Serializable;

@Data
public class MetaVo implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 路由标题
     */
    private String title;
    /**
     * 路由图标
     */
    private String icon;
}
