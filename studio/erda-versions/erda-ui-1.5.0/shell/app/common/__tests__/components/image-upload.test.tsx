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
import { ImageUpload } from 'common';
import { mount } from 'enzyme';

const hintText = 'this is a hint text';
const imgUrl = 'https://zos.alipayobjects.com/rmsportal/jkjgkEfvpUPVyRjUImniVslZfWPnJuuZ.png';
const imgUrlWithoutProtocol = imgUrl.replace(/^http(s)?:/g, '');

describe('ImageUpload', () => {
  it('ImageUpload render with single', () => {
    const fn = jest.fn();
    const setFieldsValue = jest.fn();
    const restProps: { value?: string } = {
      value: undefined,
    };
    const afterUpload = (res?: string) => {
      fn(res);
      restProps.value = res;
    };
    const wrapper = mount(
      <ImageUpload
        form={{ setFieldsValue }}
        id="single-upload"
        showHint
        hintText={hintText}
        afterUpload={afterUpload}
        {...restProps}
      />,
    );
    expect(wrapper.find('.pure-upload')).toExist();
    expect(wrapper.find('.hint')).toExist();
    expect(wrapper.find('.hint').text()).toBe(hintText);
    wrapper.find('Upload.pure-upload').prop('onChange')({ file: {} });
    expect(fn).not.toHaveBeenCalled();
    expect(setFieldsValue).not.toHaveBeenCalled();
    wrapper.find('Upload.pure-upload').prop('onChange')({
      file: {
        response: {
          data: {
            url: imgUrl,
          },
        },
      },
    });
    expect(fn).toHaveBeenLastCalledWith(imgUrlWithoutProtocol);
    expect(setFieldsValue).toHaveBeenLastCalledWith({ 'single-upload': imgUrlWithoutProtocol });
    wrapper.setProps(restProps);
    expect(wrapper.find('.image-content')).toExist();
    expect(wrapper.find('.pure-upload')).not.toExist();
    wrapper.find('.remove-image').simulate('click');
    wrapper.setProps(restProps);
    expect(fn).toHaveBeenLastCalledWith(undefined);
    expect(setFieldsValue).toHaveBeenLastCalledWith({ 'single-upload': undefined });
    expect(wrapper.find('.image-content')).not.toExist();
    expect(wrapper.find('.pure-upload')).toExist();
  });
  it('ImageUpload render with multi', () => {
    const fn = jest.fn();
    const restProps: { value?: string[] } = {
      value: undefined,
    };
    const setFieldsValue = jest.fn();
    const afterUpload = (res?: string[]) => {
      fn(res);
      restProps.value = res;
    };
    const wrapper = mount(
      <ImageUpload
        isMulti
        id="multi-upload"
        showHint
        form={{ setFieldsValue }}
        afterUpload={afterUpload}
        {...restProps}
      />,
    );
    wrapper.find('Upload.pure-upload').prop('onChange')({
      file: {
        response: {
          data: {
            url: imgUrl,
          },
        },
      },
    });
    expect(fn).toHaveBeenLastCalledWith([imgUrlWithoutProtocol]);
    expect(setFieldsValue).toHaveBeenLastCalledWith({ 'multi-upload': [imgUrlWithoutProtocol] });
    wrapper.setProps(restProps);
    expect(wrapper.find('.image-content')).toHaveLength(restProps.value?.length);
    wrapper.find('.remove-image').simulate('click');
    wrapper.setProps(restProps);
    expect(fn).toHaveBeenLastCalledWith([]);
    expect(setFieldsValue).toHaveBeenLastCalledWith({ 'multi-upload': [] });
  });
});
