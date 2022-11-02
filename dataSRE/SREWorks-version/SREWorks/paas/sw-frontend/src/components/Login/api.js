
import  httpClient from '../../utils/httpClient';
/*export type loginOptionProps = {
    status,
    info: {
        validation
    } 
}*/
const apiPrefix="gateway/v2/common/authProxy/";
export function loginUserData(){
    return httpClient.get(apiPrefix+'auth/user/info').then((res)=>{
        return res ? res : []
    })
}
export function getLoginOption(aliyunId) {
    return httpClient.get(apiPrefix+'auth/private/account/login/option' , {
        aliyunId: aliyunId
    })
}

export function getAccoutLoginSms(aliyunId , password){
    return httpClient.post(apiPrefix+'auth/private/account/login/sms', {
        aliyunId: aliyunId,
        password: password
    }, {'Content-Type':'application/json;charset=UTF-8'})
}
export function getAccoutLoginTo(aliyunId , password , lang , smsCode ){
    return httpClient.post(apiPrefix+'auth/private/account/login', {
        aliyunId:aliyunId,
        password:password,
        lang:lang,
        smsCode:smsCode,
    }, {'Content-Type':'application/json;charset=UTF-8'})
}
export function getAccoutLogin(loginName , password ){
    return httpClient.post(apiPrefix+'auth/login', {
        loginName:loginName,
        password:password,
    })
}

export function getUserLang(){//语言列表获取 httpClient
    return httpClient.get(apiPrefix+'auth/user/lang', {
    })
}

// // 云账号管理API
// export function getAccountType(){// 账号类型
//     return API.get('auth/private/account/type?appId=bcc').then((res) => {
//         return res ? res.data : {};
//     });
// }

// export function getList(search) {// 获取云账号列表
//     return API.get('auth/private/account', {
//         search: search ? search : '',
//         appId : 'bcc'
//     }).then((res) => {
//         return res ? res.data : [];
//     });
// }
export function getAccountCreate(aliyunId, password , phone , passwordChangeRequired) {// 创建云账号
    return API.post('auth/private/account/add?appId=bcc', {
        aliyunId: aliyunId,
        password: password,
        phone: phone !== 'undefined' ? phone : '',
        passwordChangeRequired: passwordChangeRequired,
    },              {'Content-Type': 'application/json'}).then((res) => {
        // console.log(res)
        return res ? res.data : {};
    });
}
// export function getAccountDelete(aliyunId) {// 删除云账号
//     return API.post('auth/private/account/delete?appId=bcc', {
//         aliyunId: aliyunId,
//     },              {'Content-Type': 'application/json;charset=UTF-8'}).then((res) => {
//         return res ? res.data : {};
//     });
// }
export function upDataPassword(aliyunId , password) {// 修改密码
    return API.post('auth/private/account/password/change?appId=bcc', {
        aliyunId: aliyunId,
        password: password,
    },              {'Content-Type': 'application/json;charset=UTF-8'}).then((res) => {
        return res ? res.data : {};
    });
}
// export function getAccountLock(aliyunId , isLock) {// 解锁或者开锁
//     return API.post('auth/private/account/lock?appId=bcc', {
//         aliyunId: aliyunId,
//         isLock: isLock,
//     },              {'Content-Type': 'application/json;charset=UTF-8'}).then((res) => {
//         return res ? res.data : {};
//     });
// }
// export function getAccountvalidation(validation) {// 手机验证方式的修改
//     return API.post('auth/private/account/validation?appId=bcc', {
//         validation: validation,
//     },              {'Content-Type': 'application/json;charset=UTF-8'}).then((res) => {
//         return res ? res.data : {};
//     });
// }

export function accountInfoChange(aliyunId , phone) {// 账号信息修改
    return API.post('auth/private/account/info/change?appId=bcc', {
        aliyunId: aliyunId,
        phone: phone ? phone : '',
    },              {'Content-Type': 'application/json;charset=UTF-8'}).then((res) => {
        return res ? res.data : {};
    });
}

// export function gatewaySms(search) {// 当前短信网关获取
//     return API.get('auth/private/gateway/sms', {
//         appId : 'bcc'
//     }).then((res) => {
//         return res ? res.data : [];
//     });
// }
export function SmsRegister(endpoint , token) {// 注册短信网关
    return API.post('auth/private/gateway/sms/register', {
        endpoint: endpoint,
        token: token,
        appId : 'bcc'
    },              {'Content-Type': 'application/json;charset=UTF-8'}).then((res) => {
        return res ? res.data : {};
    });
}

// // 权限管理API
// export function getPermissionRole() {// 角色获取列表
//     return API.get('auth/private/permission/role' , {
//         appId : 'bcc'
//     }).then((res) => {
//         return res ? res.data : {};
//     });
// }
// export function getPermisssionRoleAccount(roleName , filter) {// 角色已经被授权的云账号列表
//     return API.get('auth/private/permission/role/account' , {
//         roleName: roleName,
//         filter: filter,
//         appId : 'bcc'
//     }).then((res) => {
//         return res ? res.data : {};
//     });
// }
// export function PostPermissionRole(roleName , aliyunId) {// 对云账号进行授权
//     return API.post('auth/private/permission/role/account/add?appId=bcc', {
//         roleName: roleName,
//         aliyunId: aliyunId,
//     },              {'Content-Type': 'application/json;charset=UTF-8'}).then((res) => {
//         return res ? res.data : {};
//     });
// }

// export function deletePermissionRole(roleName , aliyunId) {// 对云账号取消授权
//     return API.post('auth/private/permission/role/account/delete?appId=bcc', {
//         roleName: roleName,
//         aliyunId: aliyunId,
//     },              {'Content-Type': 'application/json;charset=UTF-8'}).then((res) => {
//         return res ? res.data : {};
//     });
// }
