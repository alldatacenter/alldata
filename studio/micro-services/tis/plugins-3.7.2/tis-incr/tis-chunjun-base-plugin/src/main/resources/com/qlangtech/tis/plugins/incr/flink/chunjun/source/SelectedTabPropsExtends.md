## polling

间隔轮询，开启后会根据pollingInterval轮询间隔时间周期性的从数据库拉取数据。开启间隔轮询还需配置参数pollingInterval，increColumn，可以选择配置参数startLocation。若不配置参数startLocation，任务启动时将会从数据库中查询增量字段最大值作为轮询的起始位置。

## splitPk

描述：当speed配置中的channel大于1时指定此参数，Reader插件根据并发数和此参数指定的字段拼接sql，使每个并发读取不同的数据，提升读取速率。

注意：
    推荐splitPk使用表主键，因为表主键通常情况下比较均匀，因此切分出来的分片也不容易出现数据热点。
    目前splitPk仅支持整形数据切分，不支持浮点、字符串、日期等其他类型。如果用户指定其他非支持类型，ChunJun将报错。
    如果channel大于1但是没有配置此参数，任务将置为失败。

必选：否
参数类型：String
默认值：无

## where

描述：筛选条件，reader插件根据指定的column、table、where条件拼接SQL，并根据这个SQL进行数据抽取。在实际业务场景中，往往会选择当天的数据进行同步，可以将where条件指定为gmt_create > time。
注意：不可以将where条件指定为limit 10，limit不是SQL的合法where子句。
必选：否
参数类型：String
默认值：无
