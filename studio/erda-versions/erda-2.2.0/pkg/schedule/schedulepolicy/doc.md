# label 调度说明

支持新的集群配置，将原来集群维度的配置下放到 org 维度甚至 workspace 维度，其中集群可包含多个 org，一个 org 可包含 dev，test，staging，prod 四种环境
小范围内的相同字段覆盖大范围的同字段值。原来的集群配置即针对 org 名为 “default” 的配置

针对一个 cpu 超卖比的配置示例如下：

```bash
{
    "cluster": "terminus-y",
    "kind": "MARATHON",
    "name": "MARATHONFORTERMINUSY",
    "options": {
        "ADDONS_DISABLE_AUTO_RESTART": "true",
        "ADDR": "master.mesos/service/marathon",
        "BASICAUTH": "admin:Terminus1234",
        "CPU_SUBSCRIBE_RATIO": "10",
        "DOCKER_CONFIG": "memory-swappiness=60",
        "ENABLETAG": "true",
        "FETCHURIS": "file:///netdata/docker-registry-ali/password.tar.gz",
        "FORCE_KILL": "true",
        "MIN_CPU_THROTTLE": "0.5",
        "PRESERVEPROJECTS": "58",
        "UNIQUE": "true"
    },
    "optionsPlus": {
        // 不同 org 可单独配置
        "orgs": [
            {
                // 不同 workspace 可单独配置
                "workspaces": [
                    {
                        "name": "prod",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "2"
                        }
                    },
                    {
                        "name": "staging",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "3"
                        }
                    }
                ],
                "name": "xxx",
                "options": {
                    "CPU_SUBSCRIBE_RATIO": "5"
                }
            },
            {
                "workspaces": [
                    {
                        "name": "prod",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "1"
                        }
                    }
                ],
                "name": "default"
            }
        ]
    }
}
```

针对这份配置，对 cpu 超卖比的设置可以达到的效果是：

- 未打 org 标的应用默认 cpu 超卖比为 10
- 打了 org 标为 defautl，打了 env 环境标签为 prod 的应用超卖比为 1
- 打了 org 标为 xxx 标签的，默认 cpu 超卖比为 5
- 打了 org 标为 xxx, 打了 workspace 标为 prod 的应用超卖比为 2
- 打了 org 标为 xxx, 打了 workspace 标为 staging 的应用超卖比为 3

### 注意事项
- 由于当前的服务默认都带了DICE_ORG的标签和DICE_WORKSPACE标签，所以需要额外设置是否开启 org 调度和 workspace 调度的环境变量
