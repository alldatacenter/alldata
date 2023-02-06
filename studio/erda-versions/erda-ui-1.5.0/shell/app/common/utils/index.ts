// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import { notification } from 'antd';
import i18n from 'i18n';
import { get } from 'lodash';
import { PAGINATION } from 'app/constants';
import moment from 'moment';
import { Key, pathToRegexp, compile } from 'path-to-regexp';
import { AxiosResponse } from 'axios';
import AnsiUp from 'ansi_up';

export { connectCube } from './connect-cube';
export { getBrowserInfo, getCookies, getLS, removeLS, setLS, clearLS, LSObserver } from './browser';
export { replaceEmoji } from './emoji';
export { goTo, pages, resolvePath } from './go-to';
export { qs, mergeSearch, updateSearch, setSearch } from './query-string';
// 字符串、数字、时间工具
export {
  camel2DashName,
  cutStr,
  secondsToTime,
  fromNow,
  getDateDuration,
  getStrRealLen,
  getTimeSpan,
  px2Int,
  daysRange,
  formatTime,
} from './str-num-date';

export { getLabel } from './component-utils';

export const isPromise = (obj: any) => {
  return !!obj && (typeof obj === 'object' || typeof obj === 'function') && typeof obj.then === 'function';
};

export const isClassComponent = (Component: any) =>
  Boolean(Component && Component.prototype && typeof Component.prototype.render === 'function');

// 始终使用唯一的timer对象，确保执行clear一定会清除
let timer: any;
/**
 * 定时循环执行函数
 * @param fn 执行函数
 * @param duration 间隔时间
 * @return clear 清理定时的函数
 */
export const loopTimer = (fn: Function, duration = 3000) => {
  timer = setTimeout(() => {
    fn(clear);
    loopTimer(fn, duration);
  }, duration);
  const clear = () => clearTimeout(timer);
  return clear;
};

interface ICountPagination {
  pageNo: number;
  pageSize: number;
  total: number;
  minus: number;
}
export function countPagination({ pageNo, pageSize, total, minus }: ICountPagination) {
  const totalPage = Math.ceil(total / pageSize);
  const newTotalPage = Math.ceil((total - minus) / pageSize);
  if (pageNo === totalPage && newTotalPage !== totalPage) {
    // 当前最后一页
    return { pageNo: newTotalPage, pageSize };
  }
  return { pageNo, pageSize };
}

export function notify(type: string, desc: string | React.ReactNode, dur = 3) {
  const messageName = {
    success: i18n.t('succeed'),
    error: i18n.t('error'),
    info: i18n.t('tip'),
    warning: i18n.t('warning'),
  };
  return notification[type]({
    message: messageName[type],
    description: desc,
    duration: dur,
  });
}

/**
 * 检查两个对象keys对应的值是否一致
 * @param a 对象A
 * @param b 对象B
 * @param keys 检查的keys
 * @return {boolean} isAllEqual
 */
export const equalByKeys = (a: object, b: object, keys: string[]) => {
  return keys.reduce((all: boolean, k: string) => {
    return all && a[k] === b[k];
  }, true);
};

// antd form 校验规则已有的内建校验类型 https://github.com/yiminghe/async-validator#type，以下为自定义规则
export const regRules = {
  mobile: { pattern: /^(1[3|4|5|7|8|9])\d{9}$/, message: i18n.t('dop:please enter the correct phone number') },
  header: { pattern: /^[a-zA-Z0-9]/, message: i18n.t('dop:must start with a letter or number') },
  commonStr: {
    pattern: /^[a-zA-Z0-9_-]*$/,
    message: i18n.t('dop:can only contain characters, numbers, underscores and hyphens'),
  },
  port: {
    pattern: /^([1-9]\d{0,3}|[1-5]\d{4}|6[0-5]{2}[0-3][0-5])(-([1-9]\d{0,3}|[1-5]\d{4}|6[0-5]{2}[0-3][0-5]))?$/,
    message: i18n.t('dop:please fill in the correct port number'),
  },
  ip: {
    pattern: /^(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[0-9]{1,2})(\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[0-9]{1,2})){3}$/,
    message: i18n.t('dop:please fill in the correct ip'),
  },
  noSpace: { pattern: /^[^ \f\r\t\v]*$/, message: i18n.t('dop:do not start with a space') },
  http: {
    pattern: /^https?:\/\/(www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)/,
    message: i18n.t('dop:please enter the correct http address'),
  },
  url: {
    pattern:
      /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([-.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/,
    message: i18n.t('dop:please enter the correct url address'),
  },
  email: { pattern: /^(\w-*\.*)+@(\w-?)+(\.\w{2,})+$/, message: i18n.t('dop:please enter the correct email') },
  xmind: { pattern: /\.(xmind|xmt|xmap|xmind|xmt|xmap)$/, message: i18n.t('dop:not an xmind file') },
  excel: { pattern: /\.(xlsx|xls|XLSX|XLS)$/, message: i18n.t('dop:not an excel file') },
  excelOrXmind: {
    pattern: /\.(xlsx|xls|XLSX|XLS|xmind|xmt|xmap|xmind|xmt|xmap)$/,
    message: i18n.t('dop:not excel or xmind files'),
  },
  banFullWidthPunctuation: {
    pattern:
      /^[^\u3002|\uff1f|\uff01|\uff0c|\u3001|\uff1b|\uff1a|\u201c|\u201d|\u2018|\u2019|\uff08|\uff09|\u300a|\u300b|\u3008|\u3009|\u3010|\u3011|\u300e|\u300f|\u300c|\u300d|\ufe43|\ufe44|\u3014|\u3015|\u2026|\u2014|\uff5e|\ufe4f|\uffe5]+$/,
    message: i18n.t('common:full-width punctuation not allowed'),
  },
  dingding: {
    pattern: /^https:\/\/oapi\.dingtalk\.com\//g,
    message: i18n.t('dop:please enter the DingTalk address with prefix {prefix}', {
      prefix: 'https://oapi.dingtalk.com/',
      interpolation: { escapeValue: false },
    }),
  },
  clusterName: {
    pattern: /^[a-z0-9]{1,20}(-[a-z0-9]{1,20})+$/,
    message: i18n.t('cmp:for example, terminus-test'),
  },
  lenRange: (min: number, max: number) => ({
    pattern: new RegExp(`^[\\s\\S]{${min},${max}}$`),
    message: i18n.t('length is {min}~{max}', { min, max }),
  }),
  specialLetter: {
    pattern: /[`~!@#$%^&*()+=<>?:"{}|,./;'\\[\]·~！@#￥%……&*（）——+={}|《》？：“”【】、；‘'，。、]/im,
    message: i18n.t('dop:cannot enter special characters'),
  },
};

// 表单校验
export const validators = {
  validateNumberRange:
    ({ min, max }: { min: number; max: number }) =>
    (rule: any, value: string, callback: Function) => {
      const reg = /^[0-9]+$/;
      return !value || (reg.test(value) && Number(value) >= min && Number(value) <= max)
        ? callback()
        : callback(i18n.t('please enter a number between {min} ~ {max}', { min, max }));
    },
};

export const isImage = (filename: string) => /\.(jpg|bmp|gif|png|jpeg|svg)$/i.test(filename);

// t: stepTime  b: begin  c: end  d: duration
/* eslint-disable */
/* tslint:disable */
export function easeInOutCubic(t: number, b: number, c: number, d: number) {
  const cc = c - b;
  t /= d / 2;
  if (t < 1) {
    return (cc / 2) * t * t * t + b;
  }
  return (cc / 2) * ((t -= 2) * t * t + 2) + b;
}
/* tslint:enable */
/* eslint-enable */

interface IOssConfig {
  w?: number;
  h?: number;
  op?: string;
}
export function ossImg(src: string | undefined | null, config: IOssConfig = { w: 200, h: 200 }) {
  if (src === undefined || src === null) {
    return undefined;
  }
  if (!src.includes('oss')) {
    // local image
    return src;
  }
  const { op, ...params } = config;
  const _params = Object.keys(params).reduce((all, key) => `${all},${key}_${params[key]}`, '');
  return `${removeProtocol(src)}?x-oss-process=image/${op || 'resize'}${_params}`;
}

export function removeProtocol(src: string) {
  if (src && src.startsWith('http://')) {
    return src.slice('http:'.length);
  }
  return src;
}

// for debug use
export function sleep(time: number, data?: any, flag = true) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const fn = flag ? resolve : reject;
      fn(data);
    }, time);
  });
}

// 用于向其他系统提供接口获取models，因为models目录下不一定有index文件，所以无法直接import
// 而项目根目录下的文件不会被打到npm包里，故不能直接放到regist-model.js中
// export function getDiceModels(rqAll: (arg0: object, fn: (item: string) => boolean) => any) {
//   // 向上两层为 app 目录或 @terminus-paas-ui 目录
//   const context = require.context('../../', true, /\/models\/(\w|-)+\.[j|t]s$/);
//   // exclude file which name startsWith('_')
//   return rqAll(context, (item: string) => !item.includes('/_'));
// }

export function encodeHtmlTag(text: string) {
  if (!text) {
    return text;
  }
  return text.replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

/* eslint-disable */
export function guid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    // tslint:disable-next-line:one-variable-per-declaration
    let r = (Math.random() * 16) | 0,
      v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
/* eslint-enable */

export const getDefaultPaging = (overwrite = {}): IPaging => ({
  total: 0,
  pageNo: 1,
  pageSize: PAGINATION.pageSize,
  hasMore: false,
  ...overwrite,
});

export const filterOption = (inputValue: string, option: any) =>
  option.props.children.toLowerCase().indexOf(inputValue.toLowerCase()) >= 0;

export function getFileSuffix(name: string) {
  if (!name) {
    return '';
  }
  const temp = name.split('.');
  return temp[temp.length - 1];
}

export function loadJsFile(src: string) {
  const id = src.split('/').reverse()[0];
  if (document.getElementById(id)) {
    return Promise.resolve(id);
  }
  return new Promise((resolve) => {
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = src;
    script.id = id;
    document.body.appendChild(script);
    script.onload = () => {
      resolve(id);
    };
  });
}

/**
 * 转为FormData数据，用于上传等场景
 * @param object对象
 */
export function convertToFormData(dataObj: object) {
  return Object.keys(dataObj).reduce((formData: any, key) => {
    formData.append(key, dataObj[key]);
    return formData;
  }, new FormData());
}

/**
 * 列表排序，从fromIndex移到toIndex，返回新的数组
 * @param list 列表
 * @param fromIndex
 * @param toIndex
 */
export const reorder = (list: any[], fromIndex: number, toIndex: number) => {
  const result = Array.from(list);
  const [removed] = result.splice(fromIndex, 1);
  result.splice(toIndex, 0, removed);

  return result;
};

/**
 * 动态插入项到数组
 * @param condition 插入条件
 * @param list 插入项数组
 * @return condition ? list : []
 */
export const insertWhen = <T = any>(condition: boolean, list: T[]): T[] => {
  return condition ? list : [];
};

/**
 * 启用某个Iconfont css, 同时禁用除layout外其他的
 * @param target 目标类名
 */
export function enableIconfont(target: string) {
  const elements = document.querySelectorAll('.icon-style[rel="stylesheet"]');
  Array.from(elements).forEach((el: any) => {
    const stylesheet = el.sheet || el.styleSheet;
    stylesheet.disabled = !el.className.includes(target);
  });
}

export const getTimeRanges = () => {
  // 快捷时间选择
  const now = moment();
  return {
    [`1${i18n.t('common:hour')}`]: [moment().subtract(1, 'hours'), now],
    [`3${i18n.t('common:hour')}`]: [moment().subtract(3, 'hours'), now],
    [`6${i18n.t('common:hour')}`]: [moment().subtract(6, 'hours'), now],
    [`12${i18n.t('common:hour')}`]: [moment().subtract(12, 'hours'), now],
    [`1${i18n.t('common:day')}`]: [moment().subtract(1, 'days'), now],
    [`3${i18n.t('common:day')}`]: [moment().subtract(3, 'days'), now],
    [`7${i18n.t('common:day')}`]: [moment().subtract(7, 'days'), now],
  } as any;
};

// 生成uuid
export const uuid = (len = 20, radix = 0) => {
  const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split('');
  const _uuid = [];
  let i;
  const currentRadix = radix || chars.length;

  if (len) {
    // Compact form
    for (i = 0; i < len; i++) _uuid[i] = chars[Math.floor(Math.random() * currentRadix)];
  } else {
    // rfc4122, version 4 form
    let r;

    // rfc4122 requires these characters
    _uuid[8] = '-';
    _uuid[13] = '-';
    _uuid[18] = '-';
    _uuid[23] = '-';
    _uuid[14] = '4';

    // Fill in random data.  At i==19 set the high bits of clock sequence as
    // per rfc4122, sec. 4.1.5
    for (i = 0; i < 36; i++) {
      if (!_uuid[i]) {
        r = Math.floor(Math.random() * 16);
        _uuid[i] = chars[i === 19 ? (r % 4) + 8 : r];
      }
    }
  }
  return _uuid.join('');
};

export const isValidJsonStr = (_jsonStr = '') => {
  try {
    _jsonStr && JSON.parse(_jsonStr);
    return true;
  } catch (e) {
    return false;
  }
};

export const getOrgFromPath = () => {
  return get(location.pathname.split('/'), '[1]') || '-';
};

export const setApiWithOrg = (api: string) => {
  return api.startsWith('/api/') ? api.replace('/api/', `/api/${getOrgFromPath()}/`) : api;
};

/**
 * Use path to match the incoming parameters, extract query and params
 * @param path Paths that may contain parameters, such as /dop/:id/detail, path can not include `?`
 * @param params The incoming parameters may be query or params
 */
export const extractPathParams = (path: string, params?: Obj) => {
  const keys: Key[] = [];
  pathToRegexp(path, keys);
  const pathParams = {};
  const bodyOrQuery = { ...params };
  if (keys.length > 0) {
    keys.forEach(({ name }) => {
      pathParams[name] = bodyOrQuery[name];
      delete bodyOrQuery[name];
    });
  }
  return {
    pathParams,
    bodyOrQuery,
  };
};

/**
 * Fill in the actual value for the path with parameters by path-to-regexp
 * @param path Paths that may contain parameters, such as /dop/:id/detail, path can not include `?`
 * @param params The incoming parameters may be query or params
 * @returns
 */
export const generatePath = (path: string, params?: Obj) => {
  try {
    const toPathRepeated = compile(path);
    return toPathRepeated(params);
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error('path:', path, 'Error parsing url parameters');
    throw error;
  }
};

type Unpromise<T> = T extends Promise<infer U> ? U : T;

// @ts-ignore batchExecute6
export async function batchExecute<T1, T2, T3, T4, T5, T6>(
  promises: [T1, T2, T3, T4, T5, T6],
): Promise<
  [
    Unpromise<Nullable<T1>>,
    Unpromise<Nullable<T2>>,
    Unpromise<Nullable<T3>>,
    Unpromise<Nullable<T4>>,
    Unpromise<Nullable<T5>>,
    Unpromise<Nullable<T6>>,
  ]
>;
// eslint-disable-next-line no-redeclare
export async function batchExecute<T1, T2, T3, T4, T5>(
  promises: [T1, T2, T3, T4, T5],
): Promise<
  [
    Unpromise<Nullable<T1>>,
    Unpromise<Nullable<T2>>,
    Unpromise<Nullable<T3>>,
    Unpromise<Nullable<T4>>,
    Unpromise<Nullable<T5>>,
  ]
>;
// eslint-disable-next-line no-redeclare
export async function batchExecute<T1, T2, T3, T4>(
  promises: [T1, T2, T3, T4],
): Promise<[Unpromise<Nullable<T1>>, Unpromise<Nullable<T2>>, Unpromise<Nullable<T3>>, Unpromise<Nullable<T4>>]>;
// eslint-disable-next-line no-redeclare
export async function batchExecute<T1, T2, T3>(
  promises: [T1, T2, T3],
): Promise<[Unpromise<Nullable<T1>>, Unpromise<Nullable<T2>>, Unpromise<Nullable<T3>>]>;
// eslint-disable-next-line no-redeclare
export async function batchExecute<T1, T2>(
  promises: [T1, T2],
): Promise<[Unpromise<Nullable<T1>>, Unpromise<Nullable<T2>>]>;
// eslint-disable-next-line no-redeclare
export async function batchExecute<T>(promises: [T]): Promise<[Unpromise<Nullable<T>>]> {
  const fetchResult = await Promise.allSettled(promises);
  return fetchResult.map((ret) => {
    if (ret.status === 'fulfilled') {
      return ret.value;
    } else {
      // eslint-disable-next-line no-console
      console.error('execute failed', ret.reason);
      return null;
    }
  }) as unknown as Promise<[Unpromise<Nullable<T>>]>;
}

/**
 * download file by axios
 * @param response axios xhr response
 */
export const downloadFileAxios = (response: AxiosResponse<any>) => {
  const { headers, data } = response;
  const type = headers['content-type'];
  const disposition = headers['content-disposition'];
  const [, fileName] = disposition.split('=');
  const blob = new Blob([data], { type });
  const fileUrl = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = fileUrl;
  anchor.target = '_blank';
  anchor.download = decodeURIComponent(fileName);
  anchor.style.display = 'none';
  document.body.appendChild(anchor);
  anchor.click();
  setTimeout(() => {
    document.body.removeChild(anchor);
    window.URL.revokeObjectURL(fileUrl);
  }, 100);
};

/**
 * Replace strings with components
 * @param str strings
 * @param compMap components
 */
export const interpolationComp = (str: string, compMap: Record<string, JSX.Element | string>) => {
  let left = str;
  const parts: Array<JSX.Element | string> = [];
  //match components character such as <CompA />
  const compNames = str.match(/<(\w+) \/>/g);
  (compNames || []).forEach((m) => {
    const k = m.slice(1, -3);
    const [before, after] = left.split(m);
    parts.push(before, compMap[k]);
    left = after;
  });
  parts.push(left);
  return parts;
};

const AU = new AnsiUp();
export const transformLog = (content: string) => {
  return AU.ansi_to_html(content)
    .replace(/&quot;/g, '"')
    .replace(/&#x27;/g, "'"); // restore escaped quotes
};

/**
 * Hexadecimal color is converted to RGB format
 * @param color
 */
export const colorToRgb = (color: string, opacity?: number) => {
  let sColor = color.toLowerCase();
  const reg = /^#([0-9a-fA-f]{3}|[0-9a-fA-f]{6})$/;
  if (sColor && reg.test(sColor)) {
    if (sColor.length === 4) {
      let sColorNew = '#';
      for (let i = 1; i < 4; i += 1) {
        sColorNew += sColor.slice(i, i + 1).concat(sColor.slice(i, i + 1));
      }
      sColor = sColorNew;
    }
    const sColorChange = [];
    for (let i = 1; i < 7; i += 2) {
      sColorChange.push(parseInt(`0x${sColor.slice(i, i + 2)}`, 16));
    }
    return opacity ? `rgba(${sColorChange.concat(opacity).join(',')})` : `rgb(${sColorChange.join(',')})`;
  }
  return sColor;
};

export const pickRandomlyFromArray = (array: any[], num: number) => {
  if (array.length > num) {
    return new Array(num).fill(null).map(() => {
      const index = Math.floor(Math.random() * array.length);
      return array.splice(index, 1)[0];
    });
  } else {
    return array;
  }
};

/*
Display rule of avatar chars:
1) 王小刚 --> 小刚
2）diceop  --> dice
3）miwMio  --> miw
4) micW  --> micW

comment: letter like m, w, M, W is wider than others , so we limit the counts of these
*/
export const getAvatarChars = (name: string) => {
  const pattern = /[\u4e00-\u9fa5]/;

  if (pattern.test(name)) {
    return name.slice(-2);
  } else {
    const longLetterPattern = new RegExp(/[mwMW]/g);
    const longLetterCount = name.match(longLetterPattern)?.length || 0;
    const maxLength = longLetterCount > 2 ? 3 : 4;
    return name.slice(0, maxLength);
  }
};
