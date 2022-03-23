"""
GraphQL 的 Schema 定义
"""
import graphene
from common.schema import Query as CommonQuery
from common.mutation import Mutation as CommonMutation


class Query(
    CommonQuery,
    graphene.ObjectType,
):
    """
    总的查询入口
    """

    pass


class Mutation(
    CommonMutation,
    graphene.ObjectType,
):
    """
    总的操作入口
    """

    pass


schema = graphene.Schema(query=Query, mutation=Mutation)
