namespace java com.netease.arctic.ams.api

include "arctic_commons.thrift"

enum OptimizeRangeType {
    Table,
    Partition,
    Node
}

struct TreeNode {
    1:i64 mask;
    2:i64 index;
}

struct OptimizeTask {
    1:OptimizeTaskId taskId;
    2:arctic_commons.TableIdentifier tableIdentifier;
    3:optional list<binary> insertFiles;
    4:optional list<binary> deleteFiles;
    5:optional list<binary> baseFiles;
    6:optional list<binary> posDeleteFiles;
    7:optional list<TreeNode> sourceNodes;
    8:optional map<string, string> properties;
}


struct ErrorMessage {
    1:i64 failTime;
    2:string failReason;
}

struct OptimizeTaskId {
    1:OptimizeType type;
    2:string traceId;
}


struct OptimizeTaskStat {
    1:JobId jobId;
    2:arctic_commons.TableIdentifier tableIdentifier;
    3:optional string attemptId;
    4:optional OptimizeTaskId taskId;
    5:optional OptimizeStatus status;
    6:optional list<binary> files;
    7:optional ErrorMessage errorMessage;
    8:optional i64 newFileSize;
    9:i64 reportTime;
    10:i64 costTime;
}

struct JobId {
    1:optional string id;
    2:optional JobType type;
}


enum JobType {
    Ingestion,
    Optimize
}


enum OptimizeType {
    Minor,
    Major,
    FullMajor
}


enum OptimizeStatus {
    Init,
    Pending,
    Executing,
    Failed,
    Prepared,
    Committed;
}

struct OptimizerStateReport {
    1: i64 optimizerId;
    2: map<string, string> optimizerState;
}

struct DataFileInfo {
    1: string path;
    2: string type;
    3: i64 size;
    4: i64 mask;
    5: i64 index;
    6: i64 specId;
    7: string partition;
    8: i64 commitTime;
    9: i64 recordCount;
    10: i64 sequence;
}

struct OptimizerRegisterInfo {
    1: string optimizerGroupName;
    2: i32 coreNumber;
    3: i64 memorySize;
}

struct OptimizerDescriptor {
    1: i32 optimizerId;
    2: i32 groupId;
    3: string groupName;
    4: i32 coreNumber;
    5: i64 memorySize;
    6: string container;
    7: string status;
    8: i64 updateTime;
}

/**
* replace TableContainer
**/
service OptimizeManager {

    void ping()

    OptimizeTask pollTask(1:i32 queueId, 2:JobId jobId, 3:string attemptId, 4:i64 waitTime)
        throws (1: arctic_commons.NoSuchObjectException e1)

    void reportOptimizeResult(1:OptimizeTaskStat optimizeTaskStat)

    void reportOptimizerState(1: OptimizerStateReport reportData)

    OptimizerDescriptor registerOptimizer(1: OptimizerRegisterInfo registerInfo)

    void stopOptimize(1: arctic_commons.TableIdentifier tableIdentifier)
        throws (1: arctic_commons.OperationErrorException e)

    void startOptimize(1: arctic_commons.TableIdentifier tableIdentifier)
        throws (1: arctic_commons.OperationErrorException e)
}