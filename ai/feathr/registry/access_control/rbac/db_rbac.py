from fastapi import HTTPException, status
from typing import Any
from rbac import config
from rbac.database import connect
from rbac.models import AccessType, UserRole, RoleType, SUPER_ADMIN_SCOPE, _to_uuid
from rbac.interface import RBAC
import os
import logging

class BadRequest(HTTPException):
    def __init__(self, detail: Any = None) -> None:
        super().__init__(status_code=status.HTTP_400_BAD_REQUEST,
                         detail=detail, headers={"WWW-Authenticate": "Bearer"})


class DbRBAC(RBAC):
    def __init__(self):
        if not os.environ.get("RBAC_CONNECTION_STR"):
            os.environ["RBAC_CONNECTION_STR"] = config.RBAC_CONNECTION_STR
        self.conn = connect()
        self.get_userroles()
        self.projects_ids = {}

    def get_userroles(self):
        # Cache is not supported in cluster, make sure every operation read from database.
        self.userroles = self._get_userroles()

    def _get_userroles(self) -> list[UserRole]:
        """query all the active user role records in SQL table
        """
        rows = self.conn.query(
            fr"""select record_id, project_name, user_name, role_name, create_by, create_reason, create_time, delete_by, delete_reason, delete_time
            from userroles
            where delete_reason is null""")
        ret = []
        for row in rows:
            ret.append(UserRole(**row))
        logging.info(f"{ret.__len__} user roles are get.")
        return ret

    def get_global_admin_users(self) -> list[str]:
        self.get_userroles()
        return [u.user_name for u in self.userroles if (u.project_name == SUPER_ADMIN_SCOPE and u.role_name == RoleType.ADMIN.value)]

    def validate_project_access_users(self, project: str, user: str, access: str = AccessType.READ) -> bool:
        self.get_userroles()
        for u in self.userroles:
            if (u.user_name == user.lower() and u.project_name in [project.lower(), SUPER_ADMIN_SCOPE] and (access in u.access)):
                return True
        return False

    def get_userroles_by_user(self, user_name: str, role_name: str = None) -> list[UserRole]:
        """query the active user role of certain user
        """
        query = fr"""select record_id, project_name, user_name, role_name, create_by, create_reason, create_time, delete_by, delete_reason, delete_time
            from userroles
            where delete_reason is null and user_name ='%s'"""
        if role_name:
            query += fr"and role_name = '%s'"
            rows = self.conn.query(query % (user_name.lower(), role_name.lower()))
        else:
            rows = self.conn.query(query % (user_name.lower()))
        ret = []
        for row in rows:
            ret.append(UserRole(**row))
        return ret

    def get_userroles_by_project(self, project_name: str, role_name: str = None) -> list[UserRole]:
        """query the active user role of certain project.
        """
        query = fr"""select record_id, project_name, user_name, role_name, create_by, create_reason, create_time, delete_reason, delete_time
            from userroles
            where delete_reason is null and project_name ='%s'"""
        if role_name:
            query += fr"and role_name = '%s'"
            rows = self.conn.query(query % (project_name.lower(), role_name.lower()))
        else:
            rows = self.conn.query(query % (project_name.lower()))
        ret = []
        for row in rows:
            ret.append(UserRole(**row))
        return ret

    def list_userroles(self, user_name: str) -> list[UserRole]:
        ret = []
        if user_name in self.get_global_admin_users():
            return list([r.to_dict() for r in self.userroles])
        else:
            admin_roles = self.get_userroles_by_user(
                user_name, RoleType.ADMIN.value)
            ret = []
            for r in admin_roles:
                ret.extend(self.get_userroles_by_project(r.project_name))
        return list([r.to_dict() for r in ret])

    def add_userrole(self, project_name: str, user_name: str, role_name: str, create_reason: str, by: str):
        """insert new user role relationship into sql table
        """
        # check if record already exist
        self.get_userroles()
        for u in self.userroles:
            if u.project_name == project_name.lower() and u.user_name == user_name.lower() and u.role_name == role_name:
                logging.warning(
                    f"User {user_name} already have {role_name} role of {project_name}.")
                return True

        # insert new record
        query = fr"""insert into userroles (project_name, user_name, role_name, create_by, create_reason, create_time)
            values ('%s','%s','%s','%s' ,'%s', getutcdate())"""
        self.conn.update(query % (project_name.lower(), user_name.lower(),
                         role_name.lower(), by, create_reason.replace("'", "''")))
        logging.info(
            f"Userrole added with query: {query%(project_name, user_name, role_name, by, create_reason)}")
        self.get_userroles()
        return

    def delete_userrole(self, project_name: str, user_name: str, role_name: str, delete_reason: str, by: str):
        """mark existing user role relationship as deleted with reason
        """
        query = fr"""UPDATE userroles SET
            [delete_by] = '%s',
            [delete_reason] = '%s',
            [delete_time] = getutcdate()
            WHERE [user_name] = '%s' and [project_name] = '%s' and [role_name] = '%s'
            and [delete_time] is null"""
        self.conn.update(query % (by, delete_reason.replace("'", "''"),
                         user_name.lower(), project_name.lower(), role_name.lower()))
        logging.info(
            f"Userrole removed with query: {query%(by, delete_reason, user_name, project_name, role_name)}")
        self.get_userroles()
        return

    def init_userrole(self, creator_name: str, project_name:str):
        """Project name validation and project admin initialization
        """
        # project name cannot be `global`
        if project_name.casefold() == SUPER_ADMIN_SCOPE.casefold():
            raise BadRequest(f"{SUPER_ADMIN_SCOPE} is keyword for Global Admin (admin of all projects), please try other project name.")
        else:
            # check if project already exist (have valid rbac records)
            # no 400 exception to align the registry api behaviors
            query = fr"""select project_name, user_name, role_name, create_by, create_reason, create_time, delete_reason, delete_time
                from userroles
                where delete_reason is null and project_name ='%s'"""
            rows = self.conn.query(query%(project_name.lower()))
            if len(rows) > 0:
                logging.warning(f"{project_name} already exist, please pick another name.")
                return
            else:
                # initialize project admin if project not exist: 
                self.init_project_admin(creator_name, project_name)
          
            
    def init_project_admin(self, creator_name: str, project_name: str):
        """initialize the creator as project admin when a new project is created
        """
        create_by = "system"
        create_reason = "creator of project, get admin by default."
        query = fr"""insert into userroles (project_name, user_name, role_name, create_by, create_reason, create_time)
            values ('%s','%s','%s','%s','%s', getutcdate())"""
        self.conn.update(query % (project_name.lower(), creator_name.lower(), RoleType.ADMIN.value, create_by, create_reason))
        logging.info(f"Userrole initialized with query: {query%(project_name, creator_name, RoleType.ADMIN.value, create_by, create_reason)}")
        return self.get_userroles()