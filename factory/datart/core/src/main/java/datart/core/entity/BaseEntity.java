package datart.core.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
public class BaseEntity implements Serializable {

    private String id;

    private String createBy;

    private Date createTime;

    private String updateBy;

    private Date updateTime;

    private Integer permission;

}