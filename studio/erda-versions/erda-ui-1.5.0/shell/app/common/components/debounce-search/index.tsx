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
import { Input } from 'antd';
import { useDebounce } from 'react-use';
import i18n from 'i18n';

const { Search } = Input;
interface IProps {
  placeholder?: string;
  className?: string;
  value?: string;
  onChange: (val: any) => void;
  [pro: string]: any;
}
const DebounceSearch = (props: IProps) => {
  const { onChange, value: pValue, className = '', placeholder = i18n.t('search by keywords'), ...rest } = props;
  const [value, setValue] = React.useState(undefined as string | undefined);

  React.useEffect(() => {
    setValue(pValue);
  }, [pValue]);

  useDebounce(
    () => {
      onChange && onChange(value);
    },
    600,
    [value],
  );

  return (
    <Search
      className={`search-input ${className}`}
      value={value}
      placeholder={placeholder}
      onChange={(e: any) => {
        setValue(e.target.value);
      }}
      {...rest}
    />
  );
};

export default DebounceSearch;
