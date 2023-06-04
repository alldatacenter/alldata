package cn.datax.service.system.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MenuVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String parentId;
    private String menuName;
    private String menuPath;
    private String menuComponent;
    private String menuRedirect;
    private String menuPerms;
    private String menuIcon;
    private String menuType;
    private String menuCode;
    private String menuHidden;
    private Integer menuSort;
}
