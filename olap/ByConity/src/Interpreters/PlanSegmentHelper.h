#pragma once

#include <Interpreters/Context.h>
#include <Interpreters/InDepthNodeVisitor.h>
#include <Interpreters/Aliases.h>
#include <DataStreams/IBlockStream_fwd.h>
#include <Parsers/IAST_fwd.h>
#include <Storages/IStorage_fwd.h>
#include <Optimizer/QueryUseOptimizerChecker.h>

namespace DB
{

class ASTFunction;
class ASTSelectQuery;

class PlanSegmentAnalyzer;
using PlanSegmentAnalyzerPtr = std::shared_ptr<PlanSegmentAnalyzer>;

class PlanSegmentHelper
{
public:
    static bool supportDistributedStages(const ASTPtr & node);
    static bool hasJoin(const ASTPtr & node);
    static bool isConstantQuery(const ASTPtr & node);
};


}
