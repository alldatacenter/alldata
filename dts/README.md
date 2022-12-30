# DATA Integrate FOR ALL DATA PLATFORM 数据集成引擎

## AllData社区项目数据集成平台

### 基于Canal/Debezium/FlinkCDC的原理机制，设计开发CDC异常恢复程序，保障数据同步链路的可靠性和准确性
* 一、监控canal/dbz的失活状态，触发DTalk告警
* 二、获取Kafka Topic最新时间值的数据
* 三、获取恢复数据-先统一获取mysql/oracle最大时间戳字段
* 四、获取源表近[最新起始，最新起始+10s]的操作最新的数据

> 1、DataX
>
> 2、flink cdc
>
> 3、FlinkX
>
> 4、InLong
>
> 5、Canal
> 
> 6、Debezium
> 