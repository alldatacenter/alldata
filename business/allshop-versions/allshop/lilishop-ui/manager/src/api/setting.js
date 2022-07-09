import {
  getRequest,
  putRequest,
  deleteRequest,
  postRequest
} from '@/libs/axios';

// 查询短信站内信信息
export const getMessageData = params => {
  return getRequest("/other/message", params);
};
//修改短信模板
export const editSmsMessageTemplate = (id, params) => {
  return putRequest(`/sms/sms/${id}`, params)
}
//修改站内信模板
export const editNoticeMessage = (id, params) => {
  return putRequest(`/setting/noticeMessage/${id}`, params)
}

//微信消息同步
export const wechatMessageSync = (params) => {
  return getRequest("/wechat/wechatMessage/init", params)
}
//分页获取微信消息
export const getWechatMessagePage = (params) => {
  return getRequest("/wechat/wechatMessage", params)
}
//编辑微信消息模版
export const editWechatMessageTemplate = (id, params) => {
  return putRequest(`/wechat/wechatMessage/${id}`, params)
}
//删除微信消息模版
export const delWechatMessageTemplate = (id, params) => {
  return deleteRequest(`/wechat/wechatMessage/${id}`, params)
}


//微信消息订阅同步
export const wechatMPMessageSync = (params) => {
  return getRequest("/wechat/wechatMPMessage/init", params)
}
//分页获取微信消息订阅
export const getWechatMPMessagePage = (params) => {
  return getRequest("/wechat/wechatMPMessage", params)
}
//编辑微信消息订阅模版
export const editWechatMPMessageTemplate = (id, params) => {
  return putRequest(`/wechat/wechatMPMessage/${id}`, params)
}
//删除微信消息订阅模版
export const delWechatMPMessageTemplate = (id, params) => {
  return deleteRequest(`/wechat/wechatMPMessage/${id}`, params)
}



// 查询站内信模板信息
export const getNoticeMessageData = params => {
  return getRequest("/setting/noticeMessage", params);
};
//删除站内信
export const deleteMessage = (id) => {
  return deleteRequest(`/other/message/${id}`);
};
// 开启站内信模板
export const openNoticeMessage = (id) => {
  return putRequest(`/setting/noticeMessage/${id}/open`);
};
// 修改站内信状态
export const updateMessageStatus = (id,status) => {
  return putRequest(`/setting/noticeMessage/${id}/${status}`);
};
//获取短信模板
export const getSmsTemplatePage = (params) => {
  return getRequest(`sms/template/querySmsTemplatePage`, params);
};
//添加短信模板
export const addSmsTemplatePage = (params) => {
  return postRequest(`sms/template`, params);
};
//删除短信模板
export const deleteSmsTemplatePage = (params) => {
  return deleteRequest(`sms/template`, params);
};
//删除短信模板
export const editSmsTemplatePage = (params) => {
  return putRequest(`sms/template/modifySmsTemplate`, params);
};
//分页查询短信签名
export const getSmsSignPage = (params) => {
  return getRequest(`sms/sign/querySmsSignPage`, params);
};
//更新短信模板
export const syncTemplate = () => {
  return putRequest(`sms/template/querySmsSign`);
};
//更新短信模板
export const addSmsSign = (params) => {
  return postRequest(`sms/sign`, params);
};
//同步短信签名
export const syncSign = () => {
  return putRequest(`sms/sign/querySmsSign`);
};
//删除短信签名
export const deleteSign = (id) => {
  return deleteRequest(`sms/sign/${id}`);
};
//更新短信签名
export const editSmsSign = (params) => {
  return putRequest(`sms/sign/modifySmsSign`, params);
};
//查询签名详情
export const smsSignDetail = (id) => {
  return getRequest(`sms/sign/${id}`);
};

//分页查询app版本信息
export const appVersionPage = (params) => {
  return getRequest(`/other/appVersion`, params);
};
//添加app版本信息
export const addVersion = (params) => {
  return postRequest(`/other/appVersion`, params);
};
//修改app版本信息
export const editVersion = (params, id) => {
  return putRequest(`/other/appVersion/${id}`, params);
};
//删除app版本信息
export const deleteVersion = (id) => {
  return deleteRequest(`/other/appVersion/${id}`);
};
//查询同城配送列表
export const getInstantDelivery = (params) => {
  return getRequest(`instant-delivery/getByPage`, params);
};

//添加同城配送信息
export const editInstantDelivery = (bean, params, headers) => {
  return putRequest(`instant-delivery/${bean}/config`, params, headers);
};
//启用配送方案
export const openInstantDelivery = (bean) => {
  return putRequest(`instant-delivery/${bean}/open`);
};

//修改封面图片
export const editInstantDeliveryImage = (bean, params) => {
  return putRequest(`instant-delivery/${bean}/image`, params);
};

//查询短信发送记录
export const getSmsPage = (params) => {
  return getRequest(`sms/sms`, params);
};

//发送短信
export const sendSms = (params) => {
  return postRequest(`sms/sms`, params);
};
/*
 * 获取 支付支持设置开关
 */
export const getPaymentSupportForm = () => {
  return getRequest(`setting/settingx/paymentSupport`);
}

//  验证码源文件删除
export const delVerification = (id) => {
  return deleteRequest(`/other/verificationSource/${id}`)
}

//  添加验证码源文件
export const addVerification = (params) => {
  return postRequest(`/other/verificationSource`, params)
}

//  修改验证码源文件
export const editVerification = (id, params) => {
  return putRequest(`/other/verificationSource/${id}`, params)
}

// 验证码源文件详情
export const verificationDetail = (id) => {
  return getRequest(`/other/verificationSource/${id}`)
}

// 验证码源文件列表
export const verificationPage = (params) => {
  return getRequest(`/other/verificationSource`, params)
}

//获取通知类站内信
export const getNoticeMessageDetail = (id, params) => {
  return getRequest(`/setting/noticeMessage/${id}`, params)
}
