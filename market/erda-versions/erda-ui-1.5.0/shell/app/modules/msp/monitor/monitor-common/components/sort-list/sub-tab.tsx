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
import { Radio } from 'antd';
import { useMount } from 'react-use';
import './sub-tab.scss';

interface IProps {
  defaultChosen?: boolean;
  tabList: ITab[];
  onChange?: (args: any) => void;
}

interface ITab {
  [pro: string]: any;
  name: string;
  key: string;
}

const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;

const SubTab = (props: IProps) => {
  const { defaultChosen, tabList, onChange } = props;
  const [chosen, setChosen] = React.useState('');

  useMount(() => {
    defaultChosen && setChosen(defaultChosen === true ? tabList[0].key : defaultChosen);
  });

  React.useEffect(() => {
    onChange && onChange(chosen);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chosen]);

  const onClick = (e: any) => {
    const type = e.target.value;
    if (type === chosen) {
      setChosen('');
    }
  };

  return (
    <RadioGroup value={chosen} onChange={(e) => setChosen(e.target.value)}>
      {tabList &&
        tabList.map((tab) => (
          <RadioButton key={tab.key} value={tab.key} onClick={onClick}>
            {tab.name}
          </RadioButton>
        ))}
    </RadioGroup>
  );
};

export default SubTab;
