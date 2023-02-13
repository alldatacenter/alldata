
package cn.datax.service.data.metadata.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * 避免序列化问题
 * @author AllDataDC
 * @date 2023-01-27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityDto implements GrantedAuthority {

    private String authority;
}
