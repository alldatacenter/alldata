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

import React from 'react';
import {
  isPromise,
  isImage,
  removeProtocol,
  ossImg,
  uuid,
  isValidJsonStr,
  insertWhen,
  reorder,
  encodeHtmlTag,
  convertToFormData,
  getFileSuffix,
  filterOption,
  regRules,
  isClassComponent,
  countPagination,
  notify,
  equalByKeys,
  setApiWithOrg,
  sleep,
  extractPathParams,
  generatePath,
  batchExecute,
  validators,
  getTimeRanges,
  interpolationComp,
  colorToRgb,
  pickRandomlyFromArray,
} from 'common/utils';

class ClassComp extends React.Component {
  render() {
    return undefined;
  }
}

const FunComp = () => null;

describe('utils', () => {
  it('isImage ', () => {
    const suffixes = ['jpg', 'bmp', 'gif', 'png', 'jpeg', 'svg'];
    expect(isImage('images/a.doc')).toBe(false);
    suffixes.map((suffix) => {
      expect(isImage(`images/a.${suffix}`)).toBe(true);
      expect(isImage(`images/a.${suffix.toUpperCase()}`)).toBe(true);
    });
  });
  it('isPromise', () => {
    expect(isPromise(Promise.resolve())).toBe(true);
    expect(isPromise({ then: null })).toBe(false);
    expect(isPromise([])).toBe(false);
    expect(isPromise(null)).toBe(false);
  });
  it('removeProtocol', () => {
    expect(removeProtocol('http://www.erda.cloud')).toBe('//www.erda.cloud');
    expect(removeProtocol('www.erda.cloud')).toBe('www.erda.cloud');
  });
  it('ossImg', () => {
    expect(ossImg()).toBeUndefined();
    expect(ossImg(null)).toBeUndefined();
    expect(ossImg('http://file.erda.cloud')).toBe('http://file.erda.cloud');
    expect(ossImg('http://oss.erda.cloud')).toBe('//oss.erda.cloud?x-oss-process=image/resize,w_200,h_200');
    expect(
      ossImg('http://oss.erda.cloud', {
        op: 'op',
        h: 100,
        w: 100,
      }),
    ).toBe('//oss.erda.cloud?x-oss-process=image/op,h_100,w_100');
  });
  it('isValidJsonStr', () => {
    expect(isValidJsonStr('')).toBe(true);
    expect(isValidJsonStr('erda')).toBe(false);
    expect(isValidJsonStr('{"name":"erda"}')).toBe(true);
  });
  it('uuid', () => {
    expect(uuid()).toHaveLength(20);
    expect(uuid(0)).toHaveLength(36);
  });
  it('insertWhen', () => {
    expect(insertWhen(true, [1, 2, 3]).length).toBe(3);
    expect(insertWhen(false, [1, 2, 3]).length).toBe(0);
  });
  it('reorder', () => {
    expect(reorder([1, 2, 3], 0, 2)).toStrictEqual([2, 3, 1]);
  });
  it('encodeHtmlTag', () => {
    expect(encodeHtmlTag('<div>Erda Cloud</div>')).toBe('&lt;div&gt;Erda Cloud&lt;/div&gt;');
    expect(encodeHtmlTag('')).toBe('');
  });
  it('convertToFormData', () => {
    // jest.mock('FormData')
    const data = {
      name: 'ErdaCloud',
      id: 12,
    };
    const fData = new FormData();
    fData.append('name', data.name);
    fData.append('id', data.id);
    expect(convertToFormData(data)).toStrictEqual(fData);
  });
  it('getFileSuffix', () => {
    expect(getFileSuffix('')).toBe('');
    expect(getFileSuffix('erda.cloud')).toBe('cloud');
    expect(getFileSuffix('erda')).toBe('erda');
  });
  it('filterOption', () => {
    const data = {
      props: {
        children: 'Erda Cloud',
      },
    };
    expect(filterOption('erda', data)).toBe(true);
    expect(filterOption('dice', data)).toBe(false);
  });
  it('regRules', () => {
    expect(regRules.lenRange(1, 10).message).toBe('length is 1~10');
    expect(regRules.lenRange(1, 10).pattern.toString()).toBe('/^[\\s\\S]{1,10}$/');
  });
  it('isClassComponent', () => {
    expect(isClassComponent(ClassComp)).toBe(true);
    expect(isClassComponent(FunComp)).toBe(false);
  });
  it('countPagination', () => {
    const data = {
      pageSize: 10,
      total: 99,
      minus: 1,
      pageNo: 1,
    };
    expect(countPagination({ ...data })).toStrictEqual({ pageNo: 1, pageSize: 10 });
    expect(countPagination({ ...data, pageNo: 10, minus: 11 })).toStrictEqual({ pageNo: 9, pageSize: 10 });
  });
  it('notify', () => {
    notify('success', 'success info');
    expect(document.querySelectorAll('.ant-notification').length).toBe(1);
    expect(document.getElementsByClassName('ant-notification-notice-description')[0].innerHTML).toBe('success info');
  });
  it('should equalByKeys', () => {
    expect(equalByKeys({ a: 1, b: 2 }, { a: 1, b: 2 }, ['a', 'b'])).toBe(true);
    expect(equalByKeys({ a: 1, b: 1 }, { a: 1, b: 2 }, ['a', 'b'])).toBe(false);
  });
  it('setApiWithOrg should work fine', () => {
    expect(setApiWithOrg('/api/user')).toBe('/api/erda/user');
    expect(setApiWithOrg('/common/user')).toBe('/common/user');
  });
  it('sleep should work fine', () => {
    jest.useFakeTimers();
    const flagTruthy = sleep(1000, 123, true);
    jest.advanceTimersByTime(1000);
    expect(flagTruthy).resolves.toBe(123);
    const flagFalsy = sleep(1000, 123, false);
    jest.advanceTimersByTime(1000);
    expect(flagFalsy).rejects.toBe(123);
  });
  it('extractPathParams should work well', () => {
    expect(extractPathParams('/project/:id/detail', { id: 1, name: 'erda' })).toStrictEqual({
      pathParams: { id: 1 },
      bodyOrQuery: { name: 'erda' },
    });
  });
  it('generatePath should work well', () => {
    const spy = jest.spyOn(console, 'error').mockImplementation();
    expect(generatePath('/project/:id/detail', { id: 1 })).toBe('/project/1/detail');
    expect(() => {
      generatePath('/project/:id/detail', { name: 'erda' });
    }).toThrow('Expected "id" to be a string');
    expect(spy).toHaveBeenCalledTimes(1);
    spy.mockReset();
  });
  it('batchExecute should work well', () => {
    const p1 = Promise.resolve('success');
    const p2 = Promise.reject('failure');
    expect(batchExecute([p1, p2])).resolves.toStrictEqual(['success', null]);
  });
  it('validators should work well', () => {
    const validateNumberRangeFn = validators.validateNumberRange({ min: 1, max: 10 });
    const fn = jest.fn();
    validateNumberRangeFn('', '100', fn);
    expect(fn).toHaveBeenLastCalledWith('please enter a number between 1 ~ 10');
    validateNumberRangeFn('', '2', fn);
    expect(fn).toHaveBeenLastCalledWith();
  });
  it('getTimeRanges should work well', () => {
    const range = getTimeRanges();
    expect(Object.keys(range)).toHaveLength(7);
  });
  it('interpolationComp should work well', () => {
    const str = 'This is a test data, test <One />, test <Two />';
    const compMap = {
      One: 'one',
      Two: 'two',
    };
    expect(interpolationComp(str, compMap).join('')).toBe('This is a test data, test one, test two');
  });
  it('colorToRgb should work well', () => {
    expect(colorToRgb('#f1c')).toBe('rgb(255,17,204)');
    expect(colorToRgb('#f47201')).toBe('rgb(244,114,1)');
    expect(colorToRgb('#f47201', 0.1)).toBe('rgba(244,114,1,0.1)');
    expect(colorToRgb('#f47201cc')).toBe('#f47201cc');
  });
  it('pickRandomlyFromArray should work well', () => {
    const arr = [1, 2, 3, 4, 5, 6];

    expect(pickRandomlyFromArray(arr, 0)).toEqual([]);
    expect(pickRandomlyFromArray(arr, 4)).toHaveLength(4);
    expect(pickRandomlyFromArray(arr, 7)).toEqual(arr);
  });
});
