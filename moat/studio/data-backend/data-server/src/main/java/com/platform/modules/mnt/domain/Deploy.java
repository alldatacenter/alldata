
package com.platform.modules.mnt.domain;

import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import lombok.Getter;
import lombok.Setter;
import com.platform.base.BaseEntity;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Entity
@Getter
@Setter
@Table(name="mnt_deploy")
public class Deploy extends BaseEntity implements Serializable {

    @Id
	@Column(name = "deploy_id")
	@ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@ManyToMany
	@ApiModelProperty(name = "服务器", hidden = true)
	@JoinTable(name = "mnt_deploy_server",
			joinColumns = {@JoinColumn(name = "deploy_id",referencedColumnName = "deploy_id")},
			inverseJoinColumns = {@JoinColumn(name = "server_id",referencedColumnName = "server_id")})
	private Set<ServerDeploy> deploys;

	@ManyToOne
    @JoinColumn(name = "app_id")
	@ApiModelProperty(value = "应用编号")
    private App app;

    public void copy(Deploy source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
