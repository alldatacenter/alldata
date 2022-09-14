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
            "isEnable": 1,
            "services": [
                {
                    "serviceCode": "clusterService",
                    "serviceName": "集群管理",
                    "memo": "集群管理",
                    "isOpen": 1,
                    "permissions": [
                        {
                            "permissionCode": "clusterService.api.access",
                            "permissionType": 0,
                            "isEnable": 1,
                            "permissionName": "集群管理访问权限",
                            "applyUrl": "",
                            "memo": "集群管理访问权限"
                        }
                    ]
                },
                {
                    "serviceCode": "aliyunAccount",
                    "serviceName": "账号管理",
                    "memo": "账号管理",
                    "isOpen": 1,
                    "permissions": [
                        {
                            "permissionCode": "aliyunAccount.api.access",
                            "permissionType": 0,
                            "isEnable": 1,
                            "permissionName": "账号管理访问权限",
                            "applyUrl": "",
                            "memo": "账号管理访问权限"
                        }
                    ]
                }
            ]
        },
        {
            "appId": "essen",
            "loginUrl": "${DNS_PAAS_HOME}",
            "isEnable": 1,
            "services": [
                {
                    "serviceCode": "aliyunAccount",
                    "serviceName": "账号管理",
                    "memo": "账号管理",
                    "isOpen": 1,
                    "permissions": [
                        {
                            "permissionCode": "aliyunAccount.api.access",
                            "permissionType": 0,
                            "isEnable": 1,
                            "permissionName": "账号管理访问权限",
                            "applyUrl": "",
                            "memo": "账号管理访问权限"
                        }
                    ]
                }
            ]
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
        }
    ],
    "services": [

    ]
}
