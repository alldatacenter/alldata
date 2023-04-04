package datart.security.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Permission {

    private String orgId;

    private String roleId;

    private String resourceType;

    private String resourceId;

    private int permission;

    @Override
    public String toString() {
        return "Permission{" +
                "resourceType='" + resourceType + '\'' +
                ", orgId='" + orgId + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", permission=" + permission +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return permission == that.permission && orgId.equals(that.orgId) && roleId.equals(that.roleId) && resourceType.equals(that.resourceType) && resourceId.equals(that.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, resourceType, resourceId, permission);
    }
}