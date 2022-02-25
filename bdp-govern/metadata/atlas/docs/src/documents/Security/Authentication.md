---
name: Authentication
route: /Authentication
menu: Documentation
submenu: Security
---


import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Authentication in Apache Atlas.

### Authentication

Atlas supports following authentication methods

   * **File**
   * **Kerberos**
   * **LDAP**
   * **Keycloak (OpenID Connect / OAUTH2)**
   * **PAM**


Following properties should be set true to enable the authentication of that type in `atlas-application.properties` file.


<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authentication.method.kerberos=true|false
atlas.authentication.method.ldap=true|false
atlas.authentication.method.file=true|false
atlas.authentication.method.keycloak=true|false`}
 </SyntaxHighlighter>

If two or more authentication methods are set to true, then the authentication falls back to the latter method if the earlier one fails.
For example if Kerberos authentication is set to true and ldap authentication is also set to true then, if for a request without kerberos principal and keytab LDAP authentication will be used as a fallback scenario.

### FILE method.

File authentication requires users' login details in users credentials file in the format specified below and
the file path should set to property `atlas.authentication.method.file.filename` in `atlas-application.properties`.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authentication.method.file=true
atlas.authentication.method.file.filename=${'sys:atlas.home'}/conf/users-credentials.properties`}
 </SyntaxHighlighter>

The users credentials file should have below format
<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`username=group::sha256-password`}
 </SyntaxHighlighter>

 For e.g.
<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`admin=ADMIN::e7cf3ef4f17c3999a94f2c6f612e8a888e5b1026878e4e19398b23bd38ec221a`}
</SyntaxHighlighter>

Users group can be either **ADMIN**, **DATA_STEWARD** OR **DATA_SCIENTIST**

*Note*:-password is encoded with sha256 encoding method and can be generated using unix tool.

For e.g.
<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`echo -n "Password" | sha256sum
e7cf3ef4f17c3999a94f2c6f612e8a888e5b1026878e4e19398b23bd38ec221a  -`}
</SyntaxHighlighter>


### Kerberos Method.

To enable the authentication in Kerberos mode in Atlas, set the property `atlas.authentication.method.kerberos` to true in `atlas-application.properties`

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authentication.method.kerberos = true`}
 </SyntaxHighlighter>

Also following properties should be set.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authentication.method.kerberos.principal=<principal>/<fqdn>@EXAMPLE.COM
atlas.authentication.method.kerberos.keytab = /<key tab filepath>.keytab
atlas.authentication.method.kerberos.name.rules = RULE:[2:$1@$0](atlas@EXAMPLE.COM)s/.*/atlas/
atlas.authentication.method.kerberos.token.validity = 3600 [ in Seconds (optional)]`}
</SyntaxHighlighter>


### LDAP Method.

To enable the authentication in LDAP mode in Atlas, set the property `atlas.authentication.method.ldap` to true and also set Ldap type to property `atlas.authentication.method.ldap.type` to LDAP or AD in `atlas-application.properties`.
Use AD if connecting to Active Directory.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authentication.method.ldap=true
atlas.authentication.method.ldap.type=ldap|ad`}
 </SyntaxHighlighter>


For LDAP or AD the following configuration needs to be set in atlas application properties.


### Active Directory

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authentication.method.ldap.ad.domain= example.com
atlas.authentication.method.ldap.ad.url=ldap://<AD server ip>:389
atlas.authentication.method.ldap.ad.base.dn=DC=example,DC=com
atlas.authentication.method.ldap.ad.bind.dn=CN=Administrator,CN=Users,DC=example,DC=com
atlas.authentication.method.ldap.ad.bind.password=<password>
atlas.authentication.method.ldap.ad.referral=ignore
atlas.authentication.method.ldap.ad.user.searchfilter=(sAMAccountName={0})
atlas.authentication.method.ldap.ad.default.role=ROLE_USER`}
 </SyntaxHighlighter>

### LDAP Directroy

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authentication.method.ldap.url=ldap://<Ldap server ip>:389
atlas.authentication.method.ldap.userDNpattern=uid={0},ou=users,dc=example,dc=com
atlas.authentication.method.ldap.groupSearchBase=dc=example,dc=com
atlas.authentication.method.ldap.groupSearchFilter=(member=cn={0},ou=users,dc=example,dc=com
atlas.authentication.method.ldap.groupRoleAttribute=cn
atlas.authentication.method.ldap.base.dn=dc=example,dc=com
atlas.authentication.method.ldap.bind.dn=cn=Manager,dc=example,dc=com
atlas.authentication.method.ldap.bind.password=<password>
atlas.authentication.method.ldap.referral=ignore
atlas.authentication.method.ldap.user.searchfilter=(uid={0})
atlas.authentication.method.ldap.default.role=ROLE_USER`}
 </SyntaxHighlighter>

### Keycloak Method.

To enable Keycloak authentication mode in Atlas, set the property `atlas.authentication.method.keycloak` to true and also set the property `atlas.authentication.method.keycloak.file` to the localtion of your `keycloak.json` in `atlas-application.properties`.
Also set `atlas.authentication.method.keycloak.ugi-groups` to false if you want to pickup groups from Keycloak. By default the groups will be picked up from the *roles* defined in Keycloak. In case you want to use the groups
you need to create a mapping in keycloak and define `atlas.authentication.method.keycloak.groups_claim` equal to the token claim name. Make sure **not** to use the full group path and add the information to the access token.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authentication.method.keycloak=true
atlas.authentication.method.keycloak.file=/opt/atlas/conf/keycloak.json
atlas.authentication.method.keycloak.ugi-groups=false`}
 </SyntaxHighlighter>

Setup you keycloak.json per instructions from Keycloak. Make sure to include `"principal-attribute": "preferred_username"` to ensure readable user names and `"autodetect-bearer-only": true`.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`{
  "realm": "auth",
  "auth-server-url": "http://keycloak-server/auth",
  "ssl-required": "external",
  "resource": "atlas",
  "public-client": true,
  "confidential-port": 0,
  "principal-attribute": "preferred_username",
  "autodetect-bearer-only": true
}`}
 </SyntaxHighlighter>

 ### PAM.

The prerequisite for enabling PAM authentication, is to have login service file in */etc/pam.d/*

To enable the PAM authentication mode in Atlas.

* Set the atlas property `atlas.authentication.method.pam` to true in `atlas-application.properties`.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{
`atlas.authentication.method.pam=true`
}
</SyntaxHighlighter>

* Set the property `atlas.authentication.method.pam.service=<login service>` to use desired PAM login service.
  For example, set below property to use `/etc/pam.d/login`.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{
 `atlas.authentication.method.pam.service=login`
}
</SyntaxHighlighter>
