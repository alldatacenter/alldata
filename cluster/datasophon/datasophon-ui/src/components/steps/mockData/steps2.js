/*
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-06-15 10:00:47
 * @LastEditTime: 2022-06-15 10:03:42
 * @FilePath: \ddh-ui\src\components\steps\mockData\steps2.js
 */
export const data = {
    "msg": "success",

    "total": 3,

    "code": 200,

    "data": [{
            "hostname": "iflytek016",

            "ip": "172.31.96.16",

            "managed": false,

            "checkResult": {
                "msg": "主机连接失败",

                "code": 10003

            },

            "sshUser": "root",

            "sshPort": 22,

            "progress": 0,

            "clusterCode": "test1",

            "installState": "正在安装"

        },
        {
            "hostname": "iflytek017",

            "ip": "172.31.96.17",

            "managed": true,

            "checkResult": {
                "msg": "主机连接失败",

                "code": 10003

            },

            "sshUser": "root",

            "sshPort": 22,

            "progress": 100,

            "clusterCode": "test1",

            "installState": "安装成功"

        }
    ]

}