{
    "roles": [
        {
            "roleName": "bcc_api_owner",
            "description": "BCC API Owner",
            "permissions": [
                {
                    "path": "26842:bcc:gateway/*"
                }
            ]
        },
        {
            "roleName": "bcc_api_form",
            "description": "BCC API Form Generator",
            "permissions": [
                {
                    "path": "26842:bcc:gateway/v2/ha/*"
                },
                {
                    "path": "26842:bcc:gateway/v2/metric/*"
                },
                {
                    "path": "26842:bcc:gateway/v2/info/*"
                },
                {
                    "path": "26842:bcc:gateway/v2/quota/*"
                },
                {
                    "path": "26842:bcc:gateway/v2/summary/*"
                },
                {
                    "path": "26842:bcc:gateway/v2/apps/ha/*"
                },
                {
                    "path": "26842:bcc:gateway/v2/apps/metric/*"
                },
                {
                    "path": "26842:bcc:gateway/v2/apps/info/*"
                },
                {
                    "path": "26842:bcc:gateway/v2/apps/quota/*"
                },
                {
                    "path": "26842:bcc:gateway/v2/apps/summary/*"
                }
            ]
        },
        {
            "roleName": "bcc_api_thirdparty_tongque",
            "description": "BCC API Third-Party: TongQue",
            "permissions": [
                {
                    "path": "26842:bcc:gateway/v2/quota/products/ODPS/services/pangu_master"
                },
                {
                    "path": "26842:bcc:gateway/v2/summary/alerts"
                },
                {
                    "path": "26842:bcc:gateway/v2/summary/quotas"
                },
                {
                    "path": "26842:bcc:gateway/v2/ha/clusters/*"
                },
                {
                    "path": "26842:bcc:gateway/v2/ha/quotas"
                },
                {
                    "path": "26842:bcc:gateway/v2/ha/quotas/*"
                }
            ]
        }
    ]
}