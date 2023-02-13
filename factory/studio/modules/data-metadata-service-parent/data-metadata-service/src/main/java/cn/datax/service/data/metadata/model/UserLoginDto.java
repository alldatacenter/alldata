
package cn.datax.service.data.metadata.model;

import cn.datax.service.system.api.dto.UserDto;

/**
 * @author AllDataDC
 * @description 用户缓存时使用
 * @date 2023-01-27 
 **/
public class UserLoginDto extends UserDto {

    private String password;

    public Boolean isAdmin;
}
