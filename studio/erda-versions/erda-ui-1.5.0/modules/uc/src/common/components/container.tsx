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
import logo from 'src/images/logo.svg';
import login from 'src/images/login.svg';
import './container.css';

interface IProps {
  children: React.ReactNode;
}

const Container = (props: IProps) => {
  const { children } = props;
  return (
    <div className="lg:flex">
      <div className="lg:w-1/2 xl:max-w-screen-sm h-screen	overflow-auto">
        <div className="py-12 bg-indigo-100 lg:bg-white flex items-center justify-center lg:px-12">
          <div className="cursor-pointer flex items-center">
            <img src={logo} className="lg:w-80 md:w-auto" alt="logo" />
          </div>
        </div>
        <div className="mt-10 px-12 sm:px-24 md:px-48 lg:px-12 lg:mt-16 xl:px-24 xl:max-w-2xl">{children}</div>
      </div>
      <div className="hidden lg:flex items-center justify-center bg-indigo-100 flex-1 h-screen">
        <div className="max-w-xs transform duration-200 hover:scale-110 cursor-pointer">
          <img src={login} className="w-60" alt="login" />
        </div>
      </div>
    </div>
  );
};

export default Container;
