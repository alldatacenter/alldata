#include <Interpreters/getTableExpressions.h>
#include <Interpreters/PlanSegmentHelper.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>

namespace DB
{

bool PlanSegmentHelper::supportDistributedStages(const ASTPtr & node)
{
    /// TODO: support insert select
    if (node->as<ASTSelectQuery>() || node->as<ASTSelectWithUnionQuery>())
    {
        if (isConstantQuery(node))
            return false;

        if (hasJoin(node))
            return true;
    }

    return false;
}

bool PlanSegmentHelper::hasJoin(const ASTPtr & node)
{
    if (!node)
        return false;

    if (node->as<ASTTableJoin>())
        return true;
    else
    {
        for (const auto & child: node->children)
        {
            if (hasJoin(child))
                return true;
        }
    }

    return false;
}

bool PlanSegmentHelper::isConstantQuery(const ASTPtr & node)
{
    if (!node)
        return false;

    if (auto * select = node->as<ASTSelectQuery>())
    {
        for (const auto * table_expression : getTableExpressions(*select))
        {
            if (table_expression->database_and_table_name)
                return false;
        }

    }

    for (auto & child : node->children)
    {
        if (!isConstantQuery(child))
            return false;
    }

    return true;
}

}
