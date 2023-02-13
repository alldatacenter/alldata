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
import { TagsRow } from 'common';
import { IProps, TagItem } from 'common/components/tags-row';
import { shallow, mount } from 'enzyme';

const labels: IProps['labels'] = [
  { label: 'green label;green label', color: 'green' },
  { label: 'red label;red label', color: 'red' },
  { label: 'orange label;orange label', color: 'orange' },
  { label: 'purple label;purple label', color: 'purple' },
  { label: 'blue label;blue label', color: 'blue' },
  { label: 'cyan label;cyan label', color: 'cyan' },
  { label: 'gray label;gray label', color: 'gray' },
];
describe('TagsRow', () => {
  it('should render with default props', () => {
    const wrapper = shallow(<TagsRow labels={labels} />);
    expect(wrapper.find('.tags-box').children('TagItem')).toHaveLength(2);
    expect(wrapper.find('TagItem').at(0).prop('size')).toBe('small');
    expect(wrapper.find('.tags-box').children().last().children().text()).toContain('...');
    expect(wrapper.find('Tooltip')).toExist();
    // @ts-ignore
    expect(React.Children.count(wrapper.find('Tooltip').prop('title').props.children)).toBe(labels.length);
  });
  it('should render with customize props', () => {
    const addFn = jest.fn();
    const deleteFn = jest.fn();
    const wrapper = shallow(
      <TagsRow
        onDelete={deleteFn}
        onAdd={addFn}
        labels={labels}
        showCount={labels.length}
        size="default"
        containerClassName="containerClassName"
      />,
    );
    expect(wrapper).toHaveClassName('containerClassName');
    expect(wrapper.find('.tags-box').children('TagItem')).toHaveLength(labels.length);
    expect(wrapper.find('TagItem').at(0).prop('size')).toBe('default');
    expect(wrapper.find('Tooltip')).not.toExist();
    wrapper.find('.fake-link').simulate('click');
    expect(addFn).toHaveBeenCalled();
    wrapper.find('TagItem').at(0).simulate('delete');
    expect(deleteFn).toHaveBeenCalled();
  });
  it('should render with showGroup', () => {
    const groupLabel = labels.map((item, index) => {
      return {
        ...item,
        group: index % 2 === 0 ? 'groupA' : 'groupB',
      };
    });
    const wrapper = shallow(<TagsRow labels={groupLabel} />);
    expect(wrapper.find('TagItem')).toHaveLength(2);
  });
  it('TagItem should work well', () => {
    const data = [
      {
        label: 'a very long label, really very long',
        color: 'green',
      },
      {
        label: 'label text',
        color: 'yellow',
      },
      {
        label: 'label text',
      },
    ];
    const deleteFn = jest.fn();
    const wrapper = mount(
      <div>
        <TagItem label={data[0]} deleteConfirm={false} onDelete={deleteFn} withCut size="small" />
        <TagItem label={data[1]} size="default" />
        <TagItem label={data[2]} />
      </div>,
    );
    wrapper.find('.tag-close').at(0).simulate('click');
    expect(deleteFn).toHaveBeenLastCalledWith(data[0]);
    const tagItem = wrapper.find('.twt-tag-item');
    expect(tagItem.at(0)).toHaveClassName('small');
    expect(tagItem.at(0).prop('style').color).toBe('#34b37e');
    expect(tagItem.at(0).prop('style').backgroundColor).toBe('rgba(52,179,126,0.1)');
    expect(tagItem.at(1).children('Ellipsis').text()).toBe(data[1].label);
    expect(tagItem.at(1)).toHaveClassName('default');
    expect(tagItem.at(1).prop('style').color).toBe('yellow');
    expect(tagItem.at(1).prop('style').backgroundColor).toBe('yellow');
    expect(tagItem.at(2)).toHaveClassName('undefined');
  });
});
