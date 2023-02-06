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

import './box-ele.scss';

interface IBoxEle {
  pos: {
    startX: number;
    endX: number;
    startY: number;
    endY: number;
  };
  name: string;
}

const BoxEle = ({ name, pos }: IBoxEle) => {
  const { startX, endX, startY, endY } = pos;
  const style = {
    width: endX - startX,
    height: endY - startY,
  };
  return (
    <div className="topology-box-ele" style={style}>
      <span>{name}</span>
    </div>
  );
};

export default BoxEle;
