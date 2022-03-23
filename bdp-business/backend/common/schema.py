"""
GraphQL 的 Schema 定义
"""
import graphene
from graphene_django import DjangoObjectType
from graphql_jwt.decorators import login_required
from django.contrib.auth import models as auth_models
from common import models as common_models, models
from common import services



class UserRoleEnum(graphene.Enum):
    """
    用户角色枚举
    """

    guest = common_models.User.GUEST
    operator = common_models.User.OPERATOR
    lawyer = common_models.User.LAWYER
    admin = common_models.User.ADMIN


class User(DjangoObjectType):
    """
    用户
    """

    name = graphene.String(description="姓名")
    role = graphene.NonNull(UserRoleEnum, description="角色")

    class Meta:
        model = common_models.User
        exclude_fields = (
            "password",
            "is_superuser",
            "is_staff",
            "last_login",
            "date_joined",
            "creator_jobs",
        )


class UserFiltersInput(graphene.InputObjectType):
    class Meta:
        description = "用户过滤项"

    username__icontains = graphene.String(name="username_icontains")
    role = graphene.Argument(UserRoleEnum, required=False)


class Users(graphene.ObjectType):
    """
    用户列表
    """

    page = graphene.Int(description="分页索引", required=True)
    page_size = graphene.Int(description="分页数量", required=True)
    total = graphene.Int(description="总数", required=True)
    results = graphene.List(User, description="结果", required=True)


class Group(DjangoObjectType):
    """
    用户组
    """

    class Meta:
        model = auth_models.Group


class Permission(DjangoObjectType):
    """
    权限
    """

    class Meta:
        model = auth_models.Permission


class Query(graphene.ObjectType):
    """
    总的查询入口
    """

    users = graphene.Field(
        Users,
        filters=graphene.Argument(UserFiltersInput, description="过滤项"),
        order_by=graphene.String(description="排序", default_value="-created_at"),
        page=graphene.Int(description="分页索引", default_value=1),
        page_size=graphene.Int(description="分页数量", default_value=10),
    )

    @login_required
    def resolve_users(self, info, **kwargs):
        return services.get_users(info.context, kwargs)

    current_user = graphene.Field(User, description="当前用户")

    @login_required
    def resolve_current_user(self, info, **kwargs):
        return info.context.user

    permissions = graphene.List(Permission)

    @login_required
    def resolve_permissions(self, info, **kwargs):
        user = info.context.user
        if user.is_superuser:
            return auth_models.Permission.objects.all()
        return auth_models.Permission.objects.filter(group__user=user)
