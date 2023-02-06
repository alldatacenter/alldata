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

import './index.scss';

interface IProps {
  title: string;
  children: JSX.Element;
  onChange?: (value: any) => void;
}

// TODO: remove this component from common
const CompactSelect = ({ title, children, ...rest }: IProps) => {
  return (
    <div className="compact-select whitespace-nowrap">
      <span className="select-addon-before">{title}</span>
      {React.cloneElement(children, rest)}
    </div>
  );
};

export default CompactSelect;
