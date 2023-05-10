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

import i18n, { getCurrentLocale } from 'i18n';
import { isArray } from 'lodash';
import moment, { Moment } from 'moment';
import 'moment/locale/zh-cn';
import React from 'react';
import { Tooltip } from 'antd';

const locale = window.localStorage.getItem('locale') || 'zh';
const momentLangMap = {
  en: 'en',
  zh: 'zh-cn',
};
moment.locale(momentLangMap[locale]);

/**
 * transform camel case dash string: `CamelComponent -> camel-component / camel_component`
 */
export const camel2DashName = (_str: string, symbol = '-') => {
  const str = `${_str[0].toLowerCase()}${_str.substr(1)}`;
  return str.replace(/([A-Z])/g, ($1) => `${symbol}${$1.toLowerCase()}`);
};

export function getStrRealLen(strr: string, isByte: boolean, byteLen: number) {
  const str = strr || '';
  const l = str.length;
  let blen = 0;
  let rlen = 0;
  for (let i = 0; i < l; i++) {
    /* eslint-disable no-bitwise */
    if ((str.charCodeAt(i) & 0xff00) !== 0) {
      blen += 1;
    }
    blen += 1;
    if (isByte && blen < byteLen) rlen += 1;
  }
  return isByte ? rlen : blen;
}

export function px2Int(pxStr: string) {
  return parseInt(pxStr.replace('px', ''), 10);
}

/**
 * 获得时间差文案
 * @param {string|Date} startAt 开始时间
 * @param {string|Date} endAt 结束时间
 */
export function getDateDuration(startAt: string | number | Date, endAt: string | number | Date) {
  const startTime = +moment(startAt);
  const endTime = +moment(endAt);

  // TODO: 在i18n.js中的初始化无效，要看下原因
  moment.locale(getCurrentLocale().moment);
  const duration = moment.duration(endTime - startTime, 'ms');
  const seconds = duration.asSeconds();

  let timeCost = duration.humanize();
  if (timeCost === i18n.t('common:a few seconds')) {
    timeCost = `${Math.abs(+seconds.toFixed())} ${i18n.t('common:second(s)')}`;
  }

  return timeCost;
}

interface ICutOptions {
  suffix?: string;
  showTip?: boolean;
}
/**
 * 截取字符串
 * @param fullStr 字符串
 * @param limit 长度限制
 * @param options.suffix 截取添加的后缀，默认为...
 * @param options.showTip 是否超过长度后显示提示
 */
export function cutStr(fullStr: string, limit = 0, options?: ICutOptions) {
  if (typeof fullStr !== 'string') {
    return '';
  }
  const { suffix = '...', showTip = false } = options || {};
  const str = fullStr.length > limit ? `${fullStr.substring(0, limit)}${suffix}` : fullStr;
  const sameLength = fullStr.length === str.length;
  return showTip && !sameLength ? <Tooltip title={fullStr}>{str}</Tooltip> : str;
}

/**
 * 转换秒数为钟表格式 hh:mm:ss
 * @param second 秒数
 * @param zh 中文格式
 */
export const secondsToTime = (second: number, zh = false) => {
  if (second === undefined || second < 0) {
    return second;
  }
  const h = Math.floor(second / 3600);
  let m: number | string = Math.floor((second / 60) % 60);
  let ls: number | string = Math.floor(second % 60);
  if (zh) {
    if (h < 1) {
      if (m < 1) {
        return `${ls}${i18n.t('common:second(s)')}`;
      }
      return `${m}${i18n.t('common:minutes')}${ls}${i18n.t('common:second(s)')}`;
    }
    return `${h}${i18n.t('common:hour')}${m}${i18n.t('common:minutes')}${ls}${i18n.t('common:second(s)')}`;
  }
  if (m < 10) {
    m = `0${m}`;
  }
  if (ls < 10) {
    ls = `0${ls}`;
  }
  return h ? `${h}:${m}:${ls}` : `${m}:${ls}`;
};

/**
 * 获取过去{num}天内开始结束的时间点
 * @param num 天数
 */
export const daysRange = (num: number) => {
  const curDay = moment().startOf('day');
  return {
    start: curDay.subtract(num - 1, 'days').valueOf(),
    end: curDay.add(num, 'days').valueOf(),
  };
};

/**
 * 包装了moment的fromNow，添加了时间提示
 * @param time 传入moment的时间
 * @param config.prefix 提示前缀
 * @param config.edgeNow edge time can't overflow current time
 */
export const fromNow = (time: any, config?: { prefix?: string; edgeNow?: boolean }) => {
  const { prefix = '', edgeNow = false } = config || {};
  const _time = edgeNow && moment().isBefore(time) ? new Date() : time;
  return <Tooltip title={`${prefix}${moment(_time).format('YYYY-MM-DD HH:mm:ss')}`}>{moment(_time).fromNow()}</Tooltip>;
};

/**
 * 格式化时间，默认为YYYY-MM-DD
 * @param time 传入moment的时间
 * @param format 提示前缀
 */
export const formatTime = (time: string | number, format?: string) =>
  time ? moment(time).format(format || 'YYYY-MM-DD') : null;

export const getTimeSpan = (time?: number | Moment[] | number[]) => {
  const defaultTime = 1; // 默认取一小时
  let hours = defaultTime;
  let endTimeMs;
  let startTimeMs;

  if (isArray(time)) {
    [startTimeMs, endTimeMs] = time;
    if (moment.isMoment(startTimeMs)) {
      // moment对象
      endTimeMs = moment(endTimeMs).valueOf();
      startTimeMs = moment(startTimeMs).valueOf();
    }
    // hours = (Number(endTimeMs) - Number(startTimeMs)) / 3600000;
  } else {
    hours = time || defaultTime;
    endTimeMs = new Date().getTime();
    startTimeMs = endTimeMs - 3600000 * hours;
  }
  const endTime = parseInt(`${(endTimeMs as number) / 1000}`, 10);
  const startTime = parseInt(`${startTimeMs / 1000}`, 10);
  const endTimeNs = (endTimeMs as number) * 1000000;
  const startTimeNs = startTimeMs * 1000000;

  return {
    hours,
    seconds: Math.ceil(endTime - startTime),
    endTime,
    startTime,
    endTimeMs,
    startTimeMs,
    endTimeNs,
    startTimeNs,
    time: { startTime, endTime },
    timeMs: { startTimeMs, endTimeMs },
    timeNs: { startTimeNs, endTimeNs },
  } as ITimeSpan;
};
