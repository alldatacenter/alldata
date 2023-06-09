## semantic
**描述：** sink 端是否支持二阶段提交

**注意：**
    如果此参数为空，默认不开启二阶段提交，即 sink 端不支持 exactly_once 语义；
    当前只支持 exactly-once 和 at-least-once
    
## batchSize

描述：一次性批量提交的记录数大小，该值可以极大减少 ChunJun 与数据库的网络交互次数，并提升整体吞吐量。但是该值设置过大可能会造成 ChunJun 运行进程 OOM 情况

## scriptType

TIS 为您自动生成 Flink Stream 脚本，现支持两种类型脚本：

* `SQL`: **优点**逻辑清晰，便于用户自行修改执行逻辑
* `Stream API`：**优点**基于系统更底层执行逻辑执行、轻量、高性能
    
