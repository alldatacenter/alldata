import re
from typing import List, Optional
from pydantic import BaseModel
from datetime import datetime
from enum import Enum
from uuid import UUID

class User(BaseModel):
    id: str
    name: str
    username: str
    type: str
    roles: List[str]

class UserType(str, Enum):
    AAD_USER = "aad_user",
    USER_IMPERSONATION = "user_impersonation"
    AAD_APP = "aad_application",
    COMMON_USER = "common_user",
    UNKNOWN = "unknown",

SUPER_ADMIN_SCOPE = "global"


class AccessType(str, Enum):
    READ = "read",
    WRITE = "write",
    MANAGE = "manage",


class RoleType(str, Enum):
    ADMIN = "admin",
    CONSUMER = "consumer",
    PRODUCER = "producer",
    DEFAULT = "default",


RoleAccessMapping = {
    RoleType.ADMIN: ["read", "write", "manage"],
    RoleType.CONSUMER: ["read"],
    RoleType.PRODUCER: ["read", "write"],
    RoleType.DEFAULT: []
}


class UserRole():
    def __init__(self,
                 record_id: int,
                 project_name: str,
                 user_name: str,
                 role_name: str,
                 create_by: str,
                 create_reason: str,
                 create_time: datetime,
                 delete_by: Optional[str] = None,
                 delete_reason: Optional[str] = None,
                 delete_time: Optional[datetime] = None,
                 **kwargs):
        self.record_id = record_id
        self.project_name = project_name.lower()
        self.user_name = user_name.lower()
        self.role_name = role_name.lower()
        self.create_by = create_by.lower()
        self.create_reason = create_reason
        self.create_time = create_time
        self.delete_by = delete_by
        self.delete_reason = delete_reason
        self.delete_time = delete_time
        self.access = RoleAccessMapping[RoleType(self.role_name)]

    def to_dict(self) -> dict:
        return {
            "id": str(self.record_id),
            "scope": self.project_name,
            "userName": self.user_name,
            "roleName": str(self.role_name),
            "createBy": self.create_by,
            "createReason": self.create_reason,
            "createTime": str(self.create_time),
            "deleteBy": str(self.delete_by),
            "deleteReason": self.delete_reason,
            "deleteTime": str(self.delete_time),
            "access": self.access
        }


class Access():
    def __init__(self,
                 record_id: int,
                 project_name: str,
                 access_name: str) -> None:
        self.record_id = record_id
        self.project_name = project_name
        self.access_name = access_name

    def to_dict(self) -> dict:
        return {
            "record_id": str(self.record_id),
            "project_name": self.project_name,
            "access": self.access_name,
        }

class UserAccess():
    def __init__(self,
                 user_name: str,
                 project_name: str):
        self.user_name = user_name
        self.project_name = project_name

def to_snake(d, level: int = 0):
    """
    Convert `string`, `list[string]`, or all keys in a `dict` into snake case
    The maximum length of input string or list is 100, or it will be truncated before being processed, for dict, the exception will be thrown if it has more than 100 keys.
    the maximum nested level is 10, otherwise the exception will be thrown
    """
    if level >= 10:
        raise ValueError("Too many nested levels")
    if isinstance(d, str):
        d = d[:100]
        return re.sub(r'(?<!^)(?=[A-Z])', '_', d).lower()
    if isinstance(d, list):
        d = d[:100]
        return [to_snake(i, level + 1) if isinstance(i, (dict, list)) else i for i in d]
    if len(d) > 100:
        raise ValueError("Dict has too many keys")
    return {to_snake(a, level + 1): to_snake(b, level + 1) if isinstance(b, (dict, list)) else b for a, b in d.items()}



def _to_type(value, type):
    """
    Convert `value` into `type`,
    or `list[type]` if `value` is a list
    NOTE: This is **not** a generic implementation, only for objects in this module
    """
    if isinstance(value, type):
        return value
    if isinstance(value, list):
        return list([_to_type(v, type) for v in value])
    if isinstance(value, dict):
        if hasattr(type, "new"):
            try:
                # The convention is to use `new` method to create the object from a dict
                return type.new(**to_snake(value))
            except TypeError:
                pass
        return type(**to_snake(value))
    if issubclass(type, Enum):
        try:
            n = int(value)
            return type(n)
        except ValueError:
            pass
        if hasattr(type, "new"):
            try:
                # As well as Enum types, some of them have alias that cannot be handled by default Enum constructor
                return type.new(value)
            except KeyError:
                pass
        return type[value]
    return type(value)


def _to_uuid(value):
    return _to_type(value, UUID)
