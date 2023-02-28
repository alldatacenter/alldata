## connectionOptions

The ampersand-separated connection options of MongoDB. eg: `replicaSet=test&connectTimeoutMS=300000`

Default: none

## errorsTolerance

Whether to continue processing messages if an error is encountered. 
Accept `none` or `all`. 

* `none`: the connector reports an error and blocks further processing of the rest of the records when it encounters an error. 

* `all`: the connector silently ignores any bad messages.

Default: `none`

## copyExistingPipeline

An array of JSON objects describing the pipeline operations to run when copying existing data.

This can improve the use of indexes by the copying manager and make copying more efficient. 
eg. 
```json
[{"$match": {"closed": "false"}}] 
```
ensures that only documents in which the closed field is set to false are copied.

## copyExisting

Whether copy existing data from source collections.

## errorsLogEnable

Whether details of failed operations should be written to the log file.

Default: `true`

## copyExistingMaxThreads

The number of threads to use when performing the data copy.

Default: `Processors Count`

## copyExistingQueueSize

The max size of the queue to use when copying data.

Default: `16000`

## pollMaxBatchSize

Maximum number of change stream documents to include in a single batch when polling for new data.

Default: `1000`

## pollAwaitTimeMillis

The amount of time to wait before checking for new results on the change stream.

Default: `1500`

## heartbeatIntervalMillis

The length of time in milliseconds between sending heartbeat messages. Use 0 to disable.

Default: `0` 
