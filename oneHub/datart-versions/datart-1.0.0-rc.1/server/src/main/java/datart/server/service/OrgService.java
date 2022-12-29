package datart.server.service;

import datart.core.entity.Organization;
import datart.core.entity.User;
import datart.core.entity.ext.RoleBaseInfo;
import datart.core.entity.ext.UserBaseInfo;
import datart.core.mappers.ext.OrganizationMapperExt;
import datart.security.base.RoleType;
import datart.server.base.dto.InviteMemberResponse;
import datart.server.base.dto.OrganizationBaseInfo;
import datart.server.base.params.OrgCreateParam;
import datart.server.base.params.OrgUpdateParam;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface OrgService extends BaseCRUDService<Organization, OrganizationMapperExt> {

    List<OrganizationBaseInfo> listOrganizations();

    OrganizationBaseInfo getOrgDetail(String orgId);

    boolean updateOrganization(OrgUpdateParam updateParam);

    Organization createOrganization(OrgCreateParam createParam);

    void initOrganization(Organization organization, User creator);

    void createDefaultRole(RoleType roleType, User creator, Organization org);

    boolean deleteOrganization(String orgId);

    boolean updateAvatar(String orgId, String path) throws IOException;

    List<UserBaseInfo> listOrgMembers(String orgId);

    List<RoleBaseInfo> listOrgRoles(String orgId);

    InviteMemberResponse addMembers(String orgId, Set<String> emails, boolean sendMail);

    boolean confirmInvite(String token);

    boolean removeUser(String orgId, String userId);

    List<RoleBaseInfo> listUserRoles(String orgId, String userId);

    void addUserToOrg(String userId, String orgId);

    Organization checkTeamOrg();

}
