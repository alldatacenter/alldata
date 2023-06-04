import {  message } from 'antd';
export const resultMessage = {
    'start':'启动成功',
    'stop':'停止成功',
    'restart':'重启成功',
    'delete':'删除成功',
    'update':'更新成功'
  }

export const dealResult = (result: API.normalResult, key: string) => {
    if(result?.success && key){
        message.success(resultMessage[key]);
    }
}

