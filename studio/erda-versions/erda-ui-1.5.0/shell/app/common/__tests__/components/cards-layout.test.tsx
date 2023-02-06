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
import { CardsLayout } from 'common';
import { mount } from 'enzyme';
import * as hooks from 'common/use-hooks';

const widths = [
  { width: 2000, className: 'card-list-container-g6' },
  { width: 1800, className: 'card-list-container-g5' },
  { width: 1600, className: 'card-list-container-g4' },
  { width: 1200, className: 'card-list-container-g3' },
  { width: 800, className: 'card-list-container-g2' },
  { width: 400, className: 'card-list-container-g1' },
];
const dataList = [{ id: 1 }, { id: 2 }, { id: 3 }];
const contentRender = (item) => {
  return (
    <div className="data_item" key={item.id}>
      {item.id}
    </div>
  );
};

describe('CardsLayout', () => {
  beforeAll(() => {
    jest.mock('common/components/use-hooks');
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  widths.forEach(({ width, className }) => {
    it(`should render well with ${width}`, () => {
      Object.defineProperty(hooks, 'useComponentWidth', {
        writable: true,
        value: () => {
          return [<div className={`holder-${width}`} />, width];
        },
      });
      const wrapper = mount(<CardsLayout dataList={dataList} contentRender={contentRender} />);
      expect(wrapper.find(`.${className}`)).toExist();
      expect(wrapper.find(`.holder-${width}`)).toExist();
      expect(wrapper.find('.data_item')).toHaveLength(dataList.length);
    });
  });
});
