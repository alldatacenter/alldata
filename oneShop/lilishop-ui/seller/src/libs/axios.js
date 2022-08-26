import axios from "axios";
import { getStore, setStore } from "./storage";
import { router } from "../router/index";
import { Message } from "view-design";
import Cookies from "js-cookie";
import { handleRefreshToken } from "@/api/index";
import {v4 as uuidv4} from 'uuid';

// 统一请求路径前缀
export const baseUrl =
  (process.env.NODE_ENV === "development"
    ?  BASE.API_DEV.seller
    : BASE.API_PROD.seller) + BASE.PREFIX;
export const commonUrl =
  process.env.NODE_ENV === "development"
    ? BASE.API_DEV.common
    : BASE.API_PROD.common;
// 文件上传接口
export const uploadFile = commonUrl + "/common/common/upload/file";
var isRefreshToken = 0;
const refreshToken = getTokenDebounce();
const service = axios.create({
  timeout: 10000,
  baseURL: baseUrl
});
service.interceptors.request.use(
  config => {
    if (config.method == "get") {
      config.params = {
        _t: Date.parse(new Date()) / 1000,
        ...config.params
      };
    }
    let uuid = getStore("uuid");
    if (!uuid) {
      uuid = uuidv4();
      setStore('uuid', uuid);
    }

    config.headers["uuid"] = uuid;
    return config;
  },
  err => {
    Message.error("请求超时");
    return Promise.resolve(err);
  }
);

// http response 拦截器
service.interceptors.response.use(
  response => {
    const data = response.data;
    // 根据返回的code值来做不同的处理(和后端约定)
    if (!data.success && data.message) {
      Message.error(data.message);
    }
    switch (data.code) {
      case 400:
        if (data.message !== null) {
          Message.error(data.message);
        } else {
          Message.error("系统异常");
        }
        break;
      case 401:
        // 未登录 清除已登录状态
        Cookies.set("userInfoSeller", "");
        setStore("accessToken", "");
        if (router.history.current.name != "login") {
          if (data.message !== null) {
            Message.error(data.message);
          } else {
            Message.error("未知错误，请重新登录");
          }
          router.push("/login");
        }
        break;
      case 500:
        // 系统异常
        if (data.message !== null) {
          Message.error(data.message);
        } else {
          Message.error("系统异常");
        }
        break;
      default:
        return data;
    }
  },
  async error => {
    // 返回状态码不为200时候的错误处理
    if (error.response) {
      if (error.response.status === 401) {
        // 这种情况一般调到登录页
      } else if (error.response.status === 404) {
        // 避免刷新token报错
      } else if (error.response.status === 403) {
        isRefreshToken++;
        if (isRefreshToken === 1) {
          const getTokenRes = await refreshToken();
          if (getTokenRes === "success") {
            // 刷新token
            if (isRefreshToken === 1) {
              error.response.config.headers.accessToken = getStore(
                "accessToken"
              );
              return service(error.response.config);
            } else {
              router.go(0);
            }
          } else {
            Cookies.set("userInfoSeller", "");
            router.push("/login");
          }
          isRefreshToken = 0;
        }
      } else {
        // 其他错误处理
        Message.error(error.response.data.message);
      }
    }

    /* router.push("/login") */
    return Promise.resolve(error);
  }
);

// 防抖闭包来一波
function getTokenDebounce() {
  let lock = false;
  let success = false;
  return function() {
    if (!lock) {
      lock = true;
      let oldRefreshToken = getStore("refreshToken");
      handleRefreshToken(oldRefreshToken)
        .then(res => {
          if (res.success) {
            let { accessToken, refreshToken } = res.result;
            setStore("accessToken", accessToken);
            setStore("refreshToken", refreshToken);

            success = true;
            lock = false;
          } else {
            success = false;
            lock = false;
            // router.push('/login')
          }
        })
        .catch(err => {
          success = false;
          lock = false;
        });
    }
    return new Promise(resolve => {
      // 一直看lock,直到请求失败或者成功
      const timer = setInterval(() => {
        if (!lock) {
          clearInterval(timer);
          if (success) {
            resolve("success");
          } else {
            resolve("fail");
          }
        }
      }, 500); // 轮询时间间隔
    });
  };
}

export const getRequest = (url, params, resBlob) => {
  let accessToken = getStore("accessToken");
  let data = {
    method: "get",
    url: `${url}`,
    params: params,
    headers: {
      accessToken: accessToken
    },
    responseType: "blob"
  };
  if (resBlob != "blob") {
    delete data.responseType;
  }


  return service(data);
};

export const postRequest = (url, params, headers) => {
  let accessToken = getStore("accessToken");
  return service({
    method: "post",
    url: `${url}`,
    data: params,
    transformRequest: headers
      ? undefined
      : [
          function(data) {
            let ret = "";
            for (let it in data) {
              ret +=
                encodeURIComponent(it) +
                "=" +
                encodeURIComponent(data[it]) +
                "&";
            }
            ret = ret.substring(0, ret.length - 1);
            return ret;
          }
        ],
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      accessToken: accessToken,
      ...headers
    }
  });
};

/** 不带form表单不带transformRequest */
export const postRequestWithNoForm = (url, params) => {
  let accessToken = getStore("accessToken");
  return service({
    method: "post",
    url: `${url}`,
    data: params,

    headers: {
      accessToken: accessToken
    }
  });
};

export const postRequestWithHeaders = (url, params) => {
  let accessToken = getStore("accessToken");
  return axios({
    method: "post",
    url: `${url}`,
    data: params,

    headers: {
      accessToken: accessToken,
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
};

export const putRequest = (url, params, headers) => {
  let accessToken = getStore("accessToken");
  return service({
    method: "put",
    url: `${url}`,
    data: params,
    transformRequest: headers
      ? undefined
      : [
          function(data) {
            let ret = "";
            for (let it in data) {
              ret +=
                encodeURIComponent(it) +
                "=" +
                encodeURIComponent(data[it]) +
                "&";
            }
            ret = ret.substring(0, ret.length - 1);
            return ret;
          }
        ],
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      accessToken: accessToken,
      ...headers
    }
  });
};

export const putRequestWithNoForm = (url, params) => {
  let accessToken = getStore("accessToken");
  return service({
    method: "put",
    url: `${url}`,
    data: params,

    headers: {
      accessToken: accessToken
    }
  });
};

export const deleteRequest = (url, params) => {
  let accessToken = getStore("accessToken");
  return service({
    method: "delete",
    url: `${url}`,
    params: params,
    headers: {
      accessToken: accessToken
    }
  });
};

export const importRequest = (url, params) => {
  let accessToken = getStore("accessToken");
  return service({
    method: "post",
    url: `${url}`,
    data: params,
    headers: {
      accessToken: accessToken
    }
  });
};

export const uploadFileRequest = (url, params) => {
  let accessToken = getStore("accessToken");
  return service({
    method: "post",
    url: `${url}`,
    data: params,
    headers: {
      accessToken: accessToken,
      'Content-Type': 'multipart/form-data'
    }
  });
};



/**
 * 无需token验证的请求 避免旧token过期导致请求失败
 * @param {*} url
 * @param {*} params
 */
export const getRequestWithNoToken = (url, params) => {
  return service({
    method: "get",
    url: `${url}`,
    params: params
  });
};

/**
 * 无需token验证的请求 避免旧token过期导致请求失败
 * @param {*} url
 * @param {*} params
 */
export const postRequestWithNoToken = (url, params) => {
  return service({
    method: "post",
    url: `${url}`,
    params: params
  });
};

/**
 * 无需token验证的请求 避免旧token过期导致请求失败
 * @param {*} url
 * @param {*} params
 */
 export const postRequestWithNoTokenData = (url, params) => {
  return service({
    method: "post",
    url: `${url}`,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    data: params
  });
};

