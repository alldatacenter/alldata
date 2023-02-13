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
import { KVPair } from 'common';
import { mount } from 'enzyme';
import { act } from 'react-dom/test-utils';

const defaultValue = [
  {
    key: 'name',
    value: 'erda-dev',
  },
  {
    key: 'env',
    value: 'dev',
  },
  {
    key: 'org',
    value: 'erda',
  },
];

const defaultProps = {
  keyName: 'key',
  descName: 'desc',
  valueName: 'value',
  keyDesc: 'keyDesc',
};

const renderChildren = ({ CompList, fullData }) => {
  return (
    <div>
      {CompList.map((item, index) => {
        return (
          <div className="key-val-item" key={fullData[index].key || index}>
            {item.Key}
            {item.Value}
            {item.Desc}
            {item.KeyDescComp}
            {item.Op}
          </div>
        );
      })}
    </div>
  );
};

describe('KVPair', () => {
  it('should ', () => {
    const fn = jest.fn();
    const wrapper = mount(
      <KVPair {...defaultProps} value={defaultValue} onChange={fn} autoAppend>
        {renderChildren}
      </KVPair>,
    );
    expect(wrapper.find('.key-val-item')).toHaveLength(defaultValue.length);
    wrapper
      .find('.key-val-item')
      .at(1)
      .find('Input')
      .at(0)
      .simulate('change', { target: { value: 'erda.cloud' } });
    expect(wrapper.find('.key-val-item').at(1).find('Input').at(0).prop('value')).toBe('erda.cloud');
    wrapper
      .find('.key-val-item')
      .at(1)
      .find('Input')
      .at(1)
      .simulate('change', { target: { value: 'erda.cloud' } });
    expect(wrapper.find('.key-val-item').at(1).find('Input').at(1).prop('value')).toBe('erda.cloud');
    expect(fn).toHaveBeenCalled();
    wrapper.find('.key-val-item').at(1).find('DefaultOp').simulate('click');
    expect(wrapper.find('.key-val-item')).toHaveLength(defaultValue.length - 1);
  });
  it('should render with empty value', () => {
    const fn = jest.fn();
    const DescComp = (props = {}) => {
      return <div {...props} className="desc-comp" />;
    };
    const KeyDescComp = (props = {}) => {
      return <div {...props} className="key-desc-comp" />;
    };
    const wrapper = mount(
      <KVPair
        value={[]}
        onChange={fn}
        emptyHolder
        DescComp={DescComp}
        KeyDescComp={KeyDescComp}
        compProps={{ disabled: true }}
      >
        {renderChildren}
      </KVPair>,
    );
    act(() => {
      wrapper.find('.desc-comp').prop('update')('desc');
      expect(fn).toHaveBeenLastCalledWith([{ desc: 'desc', key: '', keyDesc: '', value: '' }]);
    });
    act(() => {
      wrapper.find('.key-desc-comp').prop('update')('key-desc');
      expect(fn).toHaveBeenLastCalledWith([{ desc: '', key: '', keyDesc: 'key-desc', value: '' }]);
    });
  });
});
