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
import intro from 'app/images/intro.png';
import { Button } from 'antd';

interface IProps {
  introImg?: string;
  content: string;
  action?: string;
  onAction?: () => void;
}

const Intro = ({ introImg, content, action, onAction }: IProps) => {
  return (
    <div className="h-full flex justify-center items-center">
      <div className="flex items-center flex-col">
        <img className="block" src={introImg ?? intro} alt="intro img" width={320} />
        <span className="text-base mt-6 mb-4 font-semibold text-center">{content}</span>
        {action && onAction ? (
          <Button onClick={onAction} type="primary">
            {action}
          </Button>
        ) : null}
      </div>
    </div>
  );
};

export default Intro;
