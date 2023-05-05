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
import { isEmpty } from 'lodash';
import { useComponentWidth } from '../../use-hooks';
import { IF } from 'common';
import './index.scss';

interface IProps {
  dataList?: any[];
  contentRender: (content: any) => any;
}

const getCardGridClass = (compWidth: number) => {
  let gridClass = 'card-list-container-g1';
  if (compWidth > 1920) {
    gridClass = 'card-list-container-g6';
  } else if (compWidth > 1680) {
    gridClass = 'card-list-container-g5';
  } else if (compWidth > 1440) {
    gridClass = 'card-list-container-g4';
  } else if (compWidth > 1024) {
    gridClass = 'card-list-container-g3';
  } else if (compWidth > 600) {
    gridClass = 'card-list-container-g2';
  }
  return gridClass;
};

const CardsLayout = (props: IProps) => {
  const { dataList, contentRender } = props;
  const [gridClass, setGridClass] = React.useState('');
  const [widthHolder, width] = useComponentWidth();

  React.useEffect(() => {
    if (width !== Infinity) {
      setGridClass(getCardGridClass(width as number));
    }
  }, [width]);

  return (
    <>
      {widthHolder}
      <IF check={!isEmpty(dataList) && gridClass}>
        <ul className={`card-list-container ${gridClass}`}>
          {dataList && dataList.map((content) => contentRender(content))}
        </ul>
      </IF>
    </>
  );
};

export default CardsLayout;
