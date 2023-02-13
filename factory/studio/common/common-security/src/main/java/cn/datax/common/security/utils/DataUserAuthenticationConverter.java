package cn.datax.common.security.utils;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.DataRole;
import cn.datax.common.core.DataUser;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据checktoken的结果转化用户信息
 *
 * @author AllDataDC
 * @date 2023/01/30
 */
public class DataUserAuthenticationConverter implements UserAuthenticationConverter {

	private static final String N_A = "N/A";

	private Collection<? extends GrantedAuthority> defaultAuthorities;

	public void setDefaultAuthorities(String[] defaultAuthorities) {
		this.defaultAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList(StringUtils.arrayToCommaDelimitedString(defaultAuthorities));
	}

	@Override
	public Map<String, ?> convertUserAuthentication(Authentication authentication) {
		Map<String, Object> response = new LinkedHashMap();
		response.put(USERNAME, authentication.getName());
		if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
			response.put(AUTHORITIES, AuthorityUtils.authorityListToSet(authentication.getAuthorities()));
		}
		return response;
	}

	@Override
	public Authentication extractAuthentication(Map<String, ?> map) {
		if (map.containsKey(USERNAME)) {
			Object principal = map.get(USERNAME);
			Collection<? extends GrantedAuthority> authorities = this.getAuthorities(map);

			String id = (String) map.get(DataConstant.UserAdditionalInfo.USERID.getKey());
			String username = (String) map.get(DataConstant.UserAdditionalInfo.USERNAME.getKey());
			String nickname = (String) map.get(DataConstant.UserAdditionalInfo.NICKNAME.getKey());

			String dept = (String) map.get(DataConstant.UserAdditionalInfo.DEPT.getKey());
			List<DataRole> roles = (List<DataRole>) map.get(DataConstant.UserAdditionalInfo.ROLE.getKey());
			List<String> posts = (List<String>) map.get(DataConstant.UserAdditionalInfo.POST.getKey());

			DataUser user = new DataUser(id, nickname, username, N_A, true
				, true, true, true, authorities);
			if (StrUtil.isNotBlank(dept)){
				user.setDept(dept);
			}
			if (CollUtil.isNotEmpty(roles)){
				user.setRoles(roles);
			}
			if (CollUtil.isNotEmpty(posts)){
				user.setPosts(posts);
			}
			return new UsernamePasswordAuthenticationToken(user, N_A, authorities);
		} else {
			return null;
		}
	}

	private Collection<? extends GrantedAuthority> getAuthorities(Map<String, ?> map) {
		if (!map.containsKey(AUTHORITIES)) {
			return this.defaultAuthorities;
		} else {
			Object authorities = map.get(AUTHORITIES);
			if (authorities instanceof String) {
				return AuthorityUtils.commaSeparatedStringToAuthorityList((String)authorities);
			} else if (authorities instanceof Collection) {
				return AuthorityUtils.commaSeparatedStringToAuthorityList(StringUtils.collectionToCommaDelimitedString((Collection)authorities));
			} else {
				throw new IllegalArgumentException("Authorities must be either a String or a Collection");
			}
		}
	}
}
