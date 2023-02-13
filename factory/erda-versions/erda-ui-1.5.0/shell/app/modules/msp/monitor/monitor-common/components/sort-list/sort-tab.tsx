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
import './sort-tab.scss';

interface IProps {
  defaultChosen?: boolean;
  tabList: ITab[];
  onChange?: (args: any) => void;
}
interface IState {
  chosen: string;
}
interface ITab {
  [pro: string]: any;
  name: string;
  key: string;
}
const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;
class SortTab extends React.Component<IProps, IState> {
  constructor(props: IProps) {
    super(props);
    const { defaultChosen = true, tabList } = props;
    let chosen = '';
    if (defaultChosen) {
      chosen = tabList[0].key;
      if (this.props.onChange) this.props.onChange(chosen);
    }
    this.state = {
      chosen,
    };
  }

  setChosen = (chosen: string) => {
    this.setState({ chosen });
    if (this.props.onChange) this.props.onChange(chosen);
  };

  onChange = (e: any) => {
    const key = e.target.value;
    const { chosen } = this.state;
    if (key !== chosen) {
      this.setChosen(key);
    }
  };

  render() {
    const { tabList } = this.props;
    const { chosen } = this.state;
    return (
      <RadioGroup className="sort-tab" onChange={this.onChange} value={chosen} defaultValue={tabList[0].key}>
        {tabList.map((item) => (
          <RadioButton key={item.key} value={item.key}>
            {item.name}
          </RadioButton>
        ))}
      </RadioGroup>
    );
  }
}
export default SortTab;
