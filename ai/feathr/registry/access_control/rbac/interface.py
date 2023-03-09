from abc import ABC, abstractmethod
from rbac.models import UserRole


class RBAC(ABC):
    @abstractmethod
    def _get_userroles(self) -> list[UserRole]:
        """Get List of All User Role Records
        """
        pass

    @abstractmethod
    def add_userrole(self, userrole: UserRole):
        """Add a Role to a User
        """
        pass

    @abstractmethod
    def delete_userrole(self, userrole: UserRole):
        """Delete a Role of a User
        """
        pass

    @abstractmethod
    def init_userrole(self, creator_name: str, project_name: str):
        """Default User Role Relationship when a new project is created
        """
        pass

    @abstractmethod
    def get_userroles_by_user(self, user_name: str) -> list[UserRole]:
        """Get List of All User Role Records for a User
        """
        pass

    @abstractmethod
    def get_userroles_by_project(self, project_name: str) -> list[UserRole]:
        """Get List of All User Role Records for a Project
        """
        pass

    @abstractmethod
    def validate_project_access_users(self, project: str, user: str, access: str) -> bool:
        """A Common function Validate if a user has certain project access
        """
        pass
