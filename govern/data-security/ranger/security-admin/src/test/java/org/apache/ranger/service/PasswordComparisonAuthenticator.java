/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.service;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticator;
import org.springframework.util.Assert;

/**
 * An {@link org.springframework.security.providers.ldap.LdapAuthenticator
 * LdapAuthenticator} which compares the login password with the value stored in
 * the directory using a remote LDAP "compare" operation.
 *
 * <p>
 * If passwords are stored in digest form in the repository, then a suitable
 * {@link PasswordEncoder} implementation must be supplied. By default,
 * passwords are encoded using the {@link LdapShaPasswordEncoder}.
 *
 * @author Luke Taylor
 * @version $Id: PasswordComparisonAuthenticator.java 2729 2008-03-13 16:49:19Z
 *          luke_t $
 */
public final class PasswordComparisonAuthenticator extends
		AbstractLdapAuthenticator {
	// ~ Static fields/initializers
	// =====================================================================================

	private static final Logger logger = LoggerFactory
			.getLogger(PasswordComparisonAuthenticator.class);

	// ~ Instance fields
	// ================================================================================================

	private PasswordEncoder passwordEncoder = new LdapShaPasswordEncoder();
	private String passwordAttributeName = "userPassword";

	// ~ Constructors
	// ===================================================================================================

	public PasswordComparisonAuthenticator(
			BaseLdapPathContextSource contextSource) {
		super(contextSource);
	}

	// ~ Methods
	// ========================================================================================================

	public DirContextOperations authenticate(final Authentication authentication) {
		Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class,
				authentication,
				"Can only process UsernamePasswordAuthenticationToken objects");
		// locate the user and check the password

		DirContextOperations user = null;
		String username = authentication.getName();
		String password = (String) authentication.getCredentials();

		Iterator dns = getUserDns(username).iterator();

		SpringSecurityLdapTemplate ldapTemplate = new SpringSecurityLdapTemplate(
				getContextSource());

		while (dns.hasNext() && user == null) {
			final String userDn = (String) dns.next();

			try {
				user = ldapTemplate.retrieveEntry(userDn, getUserAttributes());
			} catch (NameNotFoundException ignore) {
			}
		}

		if (user == null && getUserSearch() != null) {
			user = getUserSearch().searchForUser(username);
		}

		if (user == null) {
			throw new UsernameNotFoundException("User not found: " + username);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Performing LDAP compare of password attribute '"
					+ passwordAttributeName + "' for user '" + user.getDn()
					+ "'");
		}

		String encodedPassword = passwordEncoder.encode(password);
		byte[] passwordBytes = encodedPassword.getBytes();

		if (!ldapTemplate.compare(user.getDn().toString(),
				passwordAttributeName, passwordBytes)) {
			throw new BadCredentialsException(messages.getMessage(
					"PasswordComparisonAuthenticator.badCredentials",
					"Bad credentials"));
		}

		return user;
	}

	public void setPasswordAttributeName(String passwordAttribute) {
		Assert.hasLength(passwordAttribute,
				"passwordAttributeName must not be empty or null");
		this.passwordAttributeName = passwordAttribute;
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		Assert.notNull(passwordEncoder, "passwordEncoder must not be null.");
		this.passwordEncoder = passwordEncoder;
	}
}
