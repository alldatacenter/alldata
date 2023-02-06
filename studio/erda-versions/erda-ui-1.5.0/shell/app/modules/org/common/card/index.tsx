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
  header: React.ReactNode;
  actions: React.ReactNode;
  children: React.ReactNode;
}

const Card = ({ header, actions, children }: IProps) => {
  return (
    <div className="erda-card w-full mb-4">
      {(header || actions) && (
        <div className="erda-card-header flex justify-between p-4">
          <div>{header}</div>
          <div>{actions}</div>
        </div>
      )}

      <div className="erda-card-body px-4 py-3">{children}</div>
    </div>
  );
};

export default Card;
