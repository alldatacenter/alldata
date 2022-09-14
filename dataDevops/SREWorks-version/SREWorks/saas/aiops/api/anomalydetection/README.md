## 介绍
时间序列是以规律的时间间隔采集的测量值的有序集合，例如，每天的CPU水位，每分钟的Flink作业TPS等。
时序异常检测通过建立模型发现时间序列中不符合预期行为模式的数据
适用场景：集群黄金指标异常发现等。
​

图示：
​![image.png](https://intranetproxy.alipay.com/skylark/lark/0/2021/png/2774/1637228610227-ab1aa3b8-3130-4359-889a-f87c55d35aba.png#clientId=u7270b103-4393-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=105&id=u58fae5bf&margin=%5Bobject%20Object%5D&name=image.png&originHeight=266&originWidth=1914&originalType=binary&ratio=1&rotation=0&showTitle=false&size=90909&status=done&style=none&taskId=u87acf21c-637c-4b90-a26f-c28c757be6b&title=&width=753.0069580078125)

​

## 输入参数
请求方式：POST

| 参数名(缩进为内部属性) | 参数类型 | 是否必须 | 参数示例 | 参数说明 |
| --- | --- | --- | --- | --- |
| param |  | 否 | ​|  |
|     ├──series | array |  是 | \[\[1635216096, 22.45],\[1635216156, 32.43]] | 数组类型时序数据，每个元素也为一个数组类型，包含秒级时间戳，值两个元素。用于训练的数据至少包含15个点 |
|     ├──algoParam | object |  否 |  | 算法配置参数 |
|         ├──├──seriesInterval | integer(32) | 否 | 60默认值：None | 时序数据间隔，单位秒。 |
|  ├──├──abnormalConditions | object |  否 |  默认值{"tolerance": 3} | 控制异常判别的参数，容忍度tolerance(容忍度越大，检测出的异常点越少) |
|         ├──├──detectDirection | string | 否 | "up", "down", "both"默认值：both | 检测方向 |
|         ├──├──trainTsStart | timestamp |  否 | 1635216096默认值：输入数据的开始时间 | 用于训练的数据开始时间，训练数据应尽量不包含异常点 |
|         ├──├──trainTsEnd | timestamp |  否 | 1635216096默认值：输入数据的倒数第二个点对应时间 | 用于训练的数据结束时间，训练数据应尽量不包含异常点 |
|         ├──├──detectTsStart | timestamp | 否 | 1635216096默认值：输入数据的最后一个点对应时间 | 需要检测的数据开始时间 |
|         ├──├──detectTsEnd | timestamp | 否 | 1635216096默认值：输入数据的最后一个点对应时间 | 需要检测的数据结束时间 |
|         ├──├──returnBounds | boolean |  否 | True/False默认值：False | 是否返回正常范围的上下边界 |
|         ├──├──returnOriginSeries | boolean |  否 | True/False默认值：False | 是否返回原始数据 |

## 输出结果



| 参数名 | 参数类型 | 参数示例 | 参数说明 |
| --- | --- | --- | --- |
| code | integer | 200 | 请求状态码 |
| message | string |  | 异常返回信息 |
| data |  | ​| 返回结果对象 |
|     ├──detectSeries | array | \[\[1635216096, 1, 22.45, 32.45, 12.45],\[1635216156, 0, 32.43, 42.43, 22.43]] | 检测结果，数组类型时序数据，每个元素也为一个数组类型，至少包含检测时段时间戳，异常检测结果（1/0），原始值。当用户勾选返回上下界时，还包含正常模式上界，正常模式下界。 |
|     ├──detectSeriesColumns | array | \["timestamp","anomaly","value"," upperbound","lowerbound"]| 预测结果列名，其中upperbound和lowerbound仅当用户勾选返回上下界时返回 |
|     ├──originSeries | array | \[\[1635216096, 22.45],\[1635216156, 32.43\]] | 原始数据 |

## 常见错误
| http status code | body.code | body.message | 说明 |
| --- | --- | --- | --- |
| 400 | IllegalArgument.LackingData | Input data is not enough. | 输入的数据不足15个点 |
| 400 | IllegalArgument.Unsuitable | Input data is unsuitable for processing. | 输入的数据格式有误 |
| 400 | IllegalArgument | Invalid input for the parameters. | 入参错误 |
| 400 | MissingArgument | Missing input for the parameters. | 入参缺失 |
| 500 | InternalError.Convert | Failed to convert arguments. | 入参转换失败 |
| 500 | InternalError.Preprocess | Failed to preprocess data. | 预处理报错 |
| 500 | InternalError.DetectionAlgo | Failed to run the anomaly detection algorithm. | 运行检测算法报错 |



