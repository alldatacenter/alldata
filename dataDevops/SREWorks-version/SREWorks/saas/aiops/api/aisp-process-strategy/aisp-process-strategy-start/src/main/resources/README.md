## 介绍
策略变更是按照一定的策略去执行变更。策略变更具有一个滑动窗口，按照步长参数进行滑动。通过对接作业平台，进而执行一系列需要滚动执行策略的场景。
适用场景：机器级别的资源扩容、应用版本升级等。
## 输入参数
请求方式：POST

| 参数名 | 参数类型 | 是否必须 | 参数示例 | 参数说明 |
| --- | --- | --- | --- | --- |
| param |  | 是 | ​|  |
| ├──taskType | string | 否 | sync/async | 任务类型 |
| ├──jobId | integer(64) | 否 | 0 | 作业id |
| ├──reqList | array | 是 | \[{"k1", "v1"},{"k2", "v2"}] | 启动作业的参数列表。 |
| ├──step | integer | 是 | 3 | 步长，每次执行的作业数。 |
| └──timeout | integer(64) | 是 | 600000 | 超时时间，单位：毫秒 |

## 输出结果
| 参数名 | 参数类型 | 参数示例 | 参数说明 |
| --- | --- | --- | --- |
| code | string | IllegalArgumentException | 请求错误码 |
| message | string |  | 异常返回信息 |
| taskUUID | string |  | 任务Uuid |
| status | string | running/success/failed | 任务状态 |
| data |  | ​| 返回结果对象 |
| ├──startFailed | array | \[{"k1", "v1"},{"k2", "v2"}] | 启动作业失败的参数列表 |
| ├──jobResult | object | ​| 作业运行结果 |
| ├──├──running | array |  | 正在运行的作业实例id列表 |
| └──├──success | array |  | 运行成功的作业实例id列表 |
| └──├──error | array |  | 运行失败的作业实例id列表 |

## 输入测试示例
```json
{
    "jobId": 0,
    "reqList": [
        {
            "k": "v"
        },
        {
            "k": "v"
        },
        {
            "k": "v"
        }
    ],
    "step": 2,
    "timeout": 600000
}
```
## 常见错误
| http.code | body.code | body.message | 说明 |
| --- | --- | --- | --- |
| 400 | IllegalArgumentException | Can not find key:xxx | 请求参数异常 |
| 400 | BindException | Can not convert object to type:xxx | 请求参数类型错误 |
| 500 | Detector_500 | Can not send request to taskplatform | 检测器内部执行错误 |

