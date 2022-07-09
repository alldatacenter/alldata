// 统一请求路径前缀在libs/axios.js中修改
import {
  getRequest,
  postRequest,
  putRequest,
  deleteRequest,
  getRequestWithNoToken,
  postRequestWithNoTokenData,
  commonUrl,
} from "@/libs/axios";

//获取所有city
export const getAllCity = (params) => {
  return getRequest(commonUrl+'/common/common/region/allCity', params)
}

// 登陆
export const getHomeNotice = params => {
  return getRequest("/other/article/getByPage?type=STORE_ARTICLE&pageSize=15");
};



// 登陆
export const getSellerHomeData = params => {
  return getRequest("/statistics/index", params);
};


// 登陆
export const login = params => {
  return postRequestWithNoTokenData("/passport/login/userLogin", params);
};

// 登出
export const logout = () => {
  return postRequest("/passport/login/logout");
};

// 获取用户登录信息
export const userInfo = params => {
  return getRequest("/user/info", params);
};
// 获取登录信息
export const userMsg = params => {
  return getRequest('/settings/storeSettings', params)
}
// 注册
export const regist = params => {
  return postRequest("/user/regist", params);
};
// 初始化验证码
export const initCaptcha = params => {
  return getRequestWithNoToken("/common/captcha/init", params);
};
// 发送登录短信验证码
export const sendLoginSms = (mobile, params) => {
  return getRequest(`/common/captcha/sendLoginSms/${mobile}`, params);
};
// 发送注册短信验证码
export const sendRegistSms = (mobile, params) => {
  return getRequest(`/common/captcha/sendRegistSms/${mobile}`, params);
};
// 发送重置密码短信验证码
export const sendResetSms = (mobile, params) => {
  return getRequest(`${commonUrl}/common/captcha/sendResetSms/${mobile}`, params);
};
// 发送修改绑定手机短信验证码
export const sendEditMobileSms = (mobile, params) => {
  return getRequest(`/common/captcha/sendEditMobileSms/${mobile}`, params);
};
// 通过手机重置密码
export const resetByMobile = params => {
  return postRequest("/user/resetByMobile", params);
};
// 发送重置密码邮件验证码
export const sendResetEmail = (email, params) => {
  return getRequest(`/email/sendResetCode/${email}`, params);
};
// 发送修改绑定邮件验证码
export const sendEditEmail = (email, params) => {
  return getRequest(`/email/sendEditCode/${email}`, params);
};
// 通过邮件重置密码
export const resetByEmail = params => {
  return postRequest("/email/resetByEmail", params);
};
// 短信验证码登录
export const smsLogin = params => {
  return postRequest("/user/smsLogin", params);
};
// IP天气信息
export const ipInfo = params => {
  return getRequest("/common/ip/info", params);
};
// 个人中心编辑
export const userInfoEdit = params => {
  return postRequest("/user/edit", params);
};
// 个人中心发送修改邮箱验证邮件
export const sendCodeEmail = (email, params) => {
  return getRequest(`/email/sendCode/${email}`, params);
};
// 个人中心发送修改邮箱验证邮件
export const editEmail = params => {
  return postRequest("/email/editEmail", params);
};
// 个人中心修改密码
export const changePass = params => {
  return postRequest("/login/modifyPass", params);
};
// 个人中心修改手机
export const changeMobile = params => {
  return postRequest("/user/changeMobile", params);
};
// 获取绑定账号信息
export const relatedInfo = (username, params) => {
  return getRequest(`/relate/getRelatedInfo/${username}`, params);
};
// 解绑账号
export const unRelate = params => {
  return postRequest("/relate/delByIds", params);
};
// 分页获取绑定账号信息
export const getRelatedListData = params => {
  return getRequest("/relate/findByCondition", params);
};

// 获取用户数据 多条件
export const getUserListData = params => {
  return getRequest("/user/getByCondition", params);
};
// 通过用户名搜索
export const searchUserByName = (username, params) => {
  return getRequest("/user/searchByName/" + username, params);
};
// 获取全部用户数据
export const getAllUserData = params => {
  return getRequest("/user/getAll", params);
};

// 添加用户
export const addUser = params => {
  return postRequest("/user/admin/add", params);
};
// 编辑用户
export const editUser = params => {
  return postRequest("/user/admin/edit", params);
};
// 启用用户
export const enableUser = (id, params) => {
  return postRequest(`/user/admin/enable/${id}`, params);
};
// 禁用用户
export const disableUser = (id, params) => {
  return postRequest(`/user/admin/disable/${id}`, params);
};
// 删除用户
export const deleteUser = (ids, params) => {
  return deleteRequest(`/user/delByIds/${ids}`, params);
};
// 重置用户密码
export const resetUserPass = params => {
  return postRequest("/user/resetPass", params);
};
/****************************** 权限结束 */

// 分页获取日志数据
export const getLogListData = params => {
  return getRequest("/log/getAllByPage", params);
};
// 分页获取消息数据
export const getMessageData = params => {
  return getRequest("/message/storeMessage/getByCondition", params);
};
// 获取单个消息详情
export const getMessageDataById = (id, params) => {
  return getRequest(`/message/storeMessage/get/${id}`, params);
};
// 添加消息
export const addMessage = params => {
  return postRequest("/message/storeMessage/add", params);
};
// 编辑消息
export const editMessage = params => {
  return postRequest("/message/storeMessage/edit", params);
};
// 回收站还原消息
export const reductionMessage = (ids, params) => {
  return putRequest(`/message/storeMessage/${ids}/reduction`, params);
};
// 彻底删除消息
export const clearMessage = (ids, params) => {
  return deleteRequest(`/message/storeMessage/${ids}`, params);
};
// 已读消息放入回收站
export const deleteMessage = (ids, params) => {
  return deleteRequest(`/message/storeMessage/${ids}/delete`, params);
};
// 分页获取消息推送数据
export const getMessageSendData = params => {
  return getRequest("/message/storeMessage", params);
};
// 进入消息中心首次加载全部数据
export const getAllMessage = params => {
  return getRequest("/message/storeMessage/all", params);
};
// 已读消息
export const read = (id) => {
  return putRequest(`/message/storeMessage/${id}/read`);
};
// 删除发送消息
export const deleteMessageSend = (ids, params) => {
  return deleteRequest(`/message/storeMessageSend/delByIds/${ids}`, params);
};

// 分页获取文件数据
export const getFileListData = params => {
  return getRequest("/file", params);
};


// 复制文件
export const copyFile = params => {
  return postRequest("/file/copy", params);
};
// 重命名文件
export const renameFile = params => {
  return postRequest("/file/rename", params);
};
// 删除文件
export const deleteFile = (ids, params) => {
  return deleteRequest(`/file/delete/${ids}`, params);
};
// 下载文件
export const aliDownloadFile = (fKey, params) => {
  return getRequest(`/file/ali/download/${fKey}`, params);
};


// base64上传
export const base64Upload = params => {
  return postRequest("/common/common/upload/file", params);
};




// 添加商品计量单位
export const addGoodsUnit = (params) => {
  return postRequest(`/goods/goodsUnit`, params);
};
// 分页获取商品计量单位
export const getGoodsUnitPage = (params) => {
  return getRequest(`/goods/goodsUnit`, params);
};
// 编辑商品计量单位
export const updateGoodsUnit = (id, params) => {
  return putRequest(`/goods/goodsUnit/${id}`, params);
};
// 删除商品计量单位
export const delGoodsUnit = (ids) => {
  return deleteRequest(`/goods/goodsUnit/delete/${ids}`);
};

//
export const handleRefreshToken = (token) => {
  return getRequestWithNoToken(`/login/refresh/${token}`);
};


