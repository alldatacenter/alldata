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

import { isString, every, map, chunk, sortBy, isEmpty } from 'lodash';
import i18n from 'i18n';
// 根据ip获取数字组： 192.168.0.0/16 => [192,168,0,0,16]
export const getSubnetNum = (subnet: string) => {
  if (!subnet) return [];
  const num = [] as string[];
  map(subnet.split('.'), (item: string) => {
    const val = item.includes('/') ? item.split('/') : [item];
    num.push(...val);
  });
  return map(num, (item) => (item ? +item : undefined));
};

// 获取可用ip数：规则同阿里云，减去4个
export const getSubnetCount = (mask: number) => Math.pow(2, 32 - mask) - 4;

// 根据网段获取子网ip的取值范围: subnet: 192.168.0.0/16 toMask: 24
// => [[192],[168],[0,...255],[0]]
export const getIPItemOption = (subnet: string, _toMask = 32) => {
  const ipNum = getSubnetNum(subnet);
  if (!validIp(ipNum.slice(0, 4))) return Array(4).fill([0]);
  const mask = +(ipNum.pop() as number); // 网段掩码
  const toMask = _toMask > 32 ? 32 : _toMask;
  const itemRange = [] as number[][];
  if (toMask >= mask && validIp(ipNum)) {
    const [fixLen, floatLen] = [mask, toMask];
    const binaryArr = ip2Binary(ipNum);
    const flatBinArr = binaryArr.join('').split('');
    const newBinArr = [] as number[][];
    map(flatBinArr, (item, index) => {
      if (fixLen >= index + 1) {
        // 固定位
        newBinArr.push([+item]);
      } else if (floatLen >= index + 1) {
        // 浮动位
        newBinArr.push([0, 1]);
      } else {
        // 末尾固定位0
        newBinArr.push([0]);
      }
    });
    map(chunk(newBinArr, 8), (item, index) => {
      itemRange[index] = sortBy(map(serialArray(item), (bin) => parseInt(bin, 2)));
    });
  }
  return itemRange;
};

// 判断sNet是否是pNet的子网段
const validIsSubnet = (pNet: string, sNet: string) => {
  const pIPNum = getSubnetNum(pNet);
  const sIPNum = getSubnetNum(sNet);
  const pMask = pIPNum.pop() as number;
  const sMask = sIPNum.pop() as number;
  if (sMask < pMask) return false;
  const pNetOptions = getIPItemOption(pNet, sMask);
  let pass = true;
  for (let i = 0, len = pNetOptions.length; i < len; i++) {
    if (!pNetOptions[i].includes(sIPNum[i])) {
      pass = false;
      break;
    }
  }
  return pass;
};

// 数组全排列：[[a,b],[1,2]] => ['a1','a2','b1','b2']
const serialArray = (arr: number[][]) => {
  const lengthArr = [] as number[];
  const productArr = [] as number[];
  const result = [] as string[];
  let length = 1;
  for (let i = 0; i < arr.length; i++) {
    const len = arr[i].length;
    lengthArr.push(len);
    const product = i === 0 ? 1 : arr[i - 1].length * productArr[i - 1];
    productArr.push(product);
    length *= len;
  }
  for (let i = 0; i < length; i++) {
    let resultItem = '';
    for (let j = 0; j < arr.length; j++) {
      resultItem += arr[j][Math.floor(i / productArr[j]) % lengthArr[j]];
    }
    result.push(resultItem);
  }
  return result;
};

// 有效的ip校验
const validIp = (ip: any[] | string) => {
  if (isEmpty(ip)) return false;
  const ipNum = isString(ip) ? ip.split('.') : ip;
  return every(ipNum, (item) => !isNaN(+item) && item <= 255 && item >= 0);
};

// ip转为二进制
const ip2Binary = (ip: any[]) => {
  if (!validIp(ip)) return [];
  const ipNum = isString(ip) ? ip.split('.') : ip;
  return map(ipNum, (item) => d2b(+item));
};

// 数字 -> 二进制（补全8位）
const d2b = (num: number) => {
  const binary = num.toString(2);
  return `${'0'.repeat(8 - binary.length)}${binary}`;
};

// 是否是连续数字的数组
export const isContinuityNum = (numArr: number[]) => {
  let num = numArr[0];
  let isContinuation = true;
  for (let i = 0, len = numArr.length; i < len; i++) {
    if (numArr[i] !== num) {
      isContinuation = false;
      break;
    }
    num += 1;
  }
  return isContinuation;
};

// 获取ip取值范围提示
export const getIPTooltipText = (numArr: number[]) => {
  const len = numArr.length;
  let str = numArr.join(',');
  if (len > 2 && isContinuityNum(numArr)) {
    str = `${numArr[0]}~${numArr[len - 1]}`;
  } else if (len > 20) str = `${numArr.slice(0, 3).join(',')}...${numArr.slice(len - 3, len).join(',')}`;
  return str;
};

// 有效网段校验(掩码范围8-24)
const validSubnetMask = (subnet: string) => {
  const ipNum = getSubnetNum(subnet);
  if (ipNum.length !== 5) return false;
  const mask = +(ipNum.pop() as number); // 掩码
  if (isNaN(mask) || mask < 8 || mask > 24) return false;
  if (!validIp(ipNum)) return false;
  const len = Math.ceil(mask / 8); // 位数
  const emptyPos = ipNum.slice(len, ipNum.length);
  if (!every(emptyPos, (item) => item === 0)) return false;
  return true;
};

// 子网段校验
export const validateIsSubnet = (pNets: string[]) => (rule: any, value: string, callback: Function) => {
  if (!value) return callback();
  if (!validSubnetMask(value || '')) {
    return callback(i18n.t('cmp:please fill in the correct CIDR according to the prompt'));
  }
  let isSubnet = false;
  for (let i = 0, len = pNets.length; i < len; i++) {
    if (validIsSubnet(pNets[i], value)) {
      isSubnet = true;
      break;
    }
  }
  return callback(isSubnet ? undefined : i18n.t('cmp:please fill in the correct CIDR according to the prompt'));
};
