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

declare namespace CP_TABS {
  interface ITabMenu {
    [pro: string]: any;
    key: string;
    name: string;
    operations?: Obj<CP_COMMON.Operation>;
  }

  interface Spec {
    type: 'Tabs';
    state?: IState;
    props: IProps;
    operations?: Obj<CP_COMMON.Operation>;
  }

  interface IProps {
    tabMenu: ITabMenu[];
    visible?: boolean;
  }

  interface IState {
    activeKey: string;
  }

  type Props = MakeProps<Spec> & {
    tabBarExtraContent?: React.ReactElement;
    children?: React.ReactElement[];
  };
}
