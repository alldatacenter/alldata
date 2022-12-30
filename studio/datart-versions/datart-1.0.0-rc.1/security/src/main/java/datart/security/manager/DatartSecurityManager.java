
package datart.security.manager;

import datart.core.entity.*;
import datart.security.base.Permission;
import datart.security.exception.AuthException;
import datart.security.exception.PermissionDeniedException;
import datart.security.base.PasswordToken;

public interface DatartSecurityManager {

    void login(PasswordToken token) throws AuthException;

    boolean validateUser(String username,String password) throws AuthException;

    String login(String jwtToken) throws AuthException;

    void logoutCurrent();

    boolean isAuthenticated();

    void requireAllPermissions(Permission... permission) throws PermissionDeniedException;

    void requireAnyPermission(Permission... permissions) throws PermissionDeniedException;

    void requireOrgOwner(String orgId) throws PermissionDeniedException;

    boolean isOrgOwner(String orgId);

    boolean hasPermission(Permission... permission);

    User getCurrentUser();

    void runAs(String userNameOrEmail);

    void releaseRunAs();

}