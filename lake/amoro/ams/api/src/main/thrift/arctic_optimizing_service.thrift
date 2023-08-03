namespace java com.netease.arctic.ams.api

include "arctic_commons.thrift"

struct OptimizingTask {
    1: OptimizingTaskId taskId;
    2: optional binary taskInput;
    3: optional map<string, string> properties;
}

struct OptimizingTaskId {
    1: i64 processId;
    2: i32 taskId;
}

struct OptimizingTaskResult {
    1: OptimizingTaskId taskId;
    2: i32 threadId;
    3: optional binary taskOutput;
    4: optional string errorMessage;
    5: optional map<string, string> summary;
}

struct OptimizerRegisterInfo {
    1: optional string resourceId;
    2: i32 threadCount;
    3: i32 memoryMb;
    4: i64 startTime;
    5: string groupName;
    6: optional map<string, string> properties;
}

service OptimizingService {

    void ping()

    void touch(1: string authToken) throws(1: arctic_commons.ArcticException e1)

    OptimizingTask pollTask(1: string authToken, 2: i32 threadId)
            throws (1: arctic_commons.ArcticException e1)

    void ackTask(1: string authToken, 2: i32 threadId, 3: OptimizingTaskId taskId)
            throws(1: arctic_commons.ArcticException e1)

    void completeTask(1: string authToken, 2: OptimizingTaskResult taskResult)
            throws (1: arctic_commons.ArcticException e1)

    string authenticate(1: OptimizerRegisterInfo registerInfo)
            throws (1: arctic_commons.ArcticException e1)
}