
package com.platform.modules.mnt.domain;

import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Entity
@Getter
@Setter
@Table(name="mnt_deploy_history")
public class DeployHistory implements Serializable {

    @Id
    @Column(name = "history_id")
	@ApiModelProperty(value = "ID", hidden = true)
    private String id;

    @ApiModelProperty(value = "应用名称")
    private String appName;

	@ApiModelProperty(value = "IP")
    private String ip;

	@CreationTimestamp
	@ApiModelProperty(value = "部署时间")
    private Timestamp deployDate;

	@ApiModelProperty(value = "部署者")
    private String deployUser;

	@ApiModelProperty(value = "部署ID")
	private Long deployId;

    public void copy(DeployHistory source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
