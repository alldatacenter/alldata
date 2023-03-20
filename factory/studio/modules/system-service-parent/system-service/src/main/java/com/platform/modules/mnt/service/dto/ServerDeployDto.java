
package com.platform.modules.mnt.service.dto;

import lombok.Getter;
import lombok.Setter;
import com.platform.base.BaseDTO;
import java.io.Serializable;
import java.util.Objects;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Getter
@Setter
public class ServerDeployDto extends BaseDTO implements Serializable {

    private Long id;

	private String name;

	private String ip;

	private Integer port;

	private String account;

	private String password;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ServerDeployDto that = (ServerDeployDto) o;
		return Objects.equals(id, that.id) &&
				Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}
}
