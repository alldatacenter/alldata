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

interface IProps {
  value?: string;
  type?: string;
  onChange?: (v: string) => void;
  placeholder?: string;
  label: string;
  errorTip?: string;
  name?: string;
  labelExtra?: React.ReactNode;
}

const FormInput = (props: IProps) => {
  const { value, onChange, label, errorTip, labelExtra = null, ...rest } = props;

  const _onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange?.(e.target.value);
  };

  return (
    <div className="mt-8 relative">
      <div className="text-sm font-bold text-gray-700 tracking-wide flex justify-between items-center">
        {label}
        {labelExtra}
      </div>
      <input
        value={value}
        onChange={_onChange}
        className="w-full text-lg p-2 border-b border-gray-300 focus:outline-none focus:border-indigo-500"
        {...rest}
      />
      {errorTip ? <span className="text-red-500 -bottom-6 left-0 text-sm">{errorTip}</span> : null}
    </div>
  );
};

export default FormInput;
