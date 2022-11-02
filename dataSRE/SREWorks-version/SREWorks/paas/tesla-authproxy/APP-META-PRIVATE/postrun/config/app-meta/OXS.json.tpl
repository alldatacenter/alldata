{
    "apps": [
        {
            "appId": "tesla-frontend-framework",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1
        },
        {
            "appId": "tesla-sre",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1
        },
        {
            "appId": "eblink",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1
        },
        {
            "appId": "eodps",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1
        },
        {
            "appId": "common",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1
        },
        {
            "appId": "pangu",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1
        },
        {
            "appId": "fuxi",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1
        },
        {
            "appId": "tianji",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1
        },
        {
            "appId": "appstudio_console_web",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1
        },
        {
            "appId": "tesla-eodps",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1
        }
    ],
    "services": [
        {
            "serviceCode": "flow",
            "serviceName": "流程服务",
            "memo": "流程服务",
            "isOpen": "1",
            "permissions": [
                {
                    "permissionCode": "flow.api.access",
                    "permissionType": 0,
                    "isEnable": 1,
                    "permissionName": "流程服务访问权限",
                    "applyUrl": "",
                    "memo": "流程服务访问权限"
                }
            ]
        },
        {
            "serviceCode": "taskplatform",
            "serviceName": "作业服务",
            "memo": "作业服务",
            "isOpen": 1,
            "permissions": [
                {
                    "permissionCode": "taskplatform.api.access",
                    "permissionType": 0,
                    "isEnable": 1,
                    "permissionName": "作业服务访问权限",
                    "applyUrl": "",
                    "memo": "作业服务访问权限"
                }
            ]
        },
        {
            "serviceCode": "alarmConfig",
            "serviceName": "告警配置",
            "memo": "告警配置",
            "isOpen": 1,
            "permissions": [
                {
                    "permissionCode": "alarmConfig.api.access",
                    "permissionType": 0,
                    "isEnable": 1,
                    "permissionName": "告警配置访问权限",
                    "applyUrl": "",
                    "memo": "告警配置访问权限"
                }
            ]
        },
        {
            "serviceCode": "patchManage",
            "serviceName": "补丁管理",
            "memo": "补丁管理",
            "isOpen": 1,
            "permissions": [
                {
                    "permissionCode": "patchManage.api.access",
                    "permissionType": 0,
                    "isEnable": 1,
                    "permissionName": "补丁管理访问权限",
                    "applyUrl": "",
                    "memo": "补丁管理访问权限"
                }
            ]
        },
        {
            "serviceCode": "upgradeManage",
            "serviceName": "热升级",
            "memo": "热升级",
            "isOpen": 1,
            "permissions": [
                {
                    "permissionCode": "upgradeManage.api.access",
                    "permissionType": 0,
                    "isEnable": 1,
                    "permissionName": "热升级访问权限",
                    "applyUrl": "",
                    "memo": "热升级访问权限"
                }
            ]
        },
        {
            "serviceCode": "tunnel",
            "serviceName": "通道服务",
            "memo": "通道服务",
            "isOpen": 1,
            "permissions": [
                {
                    "permissionCode": "tunnel.api.access",
                    "permissionType": 0,
                    "isEnable": 1,
                    "permissionName": "通道服务访问权限",
                    "applyUrl": "",
                    "memo": "通道服务访问权限"
                }
            ]
        },
        {
            "serviceCode": "health",
            "serviceName": "健康管理",
            "memo": "健康管理",
            "isOpen": 1,
            "permissions": [
                {
                    "permissionCode": "health.api.access",
                    "permissionType": 0,
                    "isEnable": 1,
                    "permissionName": "健康管理访问权限",
                    "applyUrl": "",
                    "memo": "健康管理访问权限"
                }
            ]
        }
    ]
}
