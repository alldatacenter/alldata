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

/**
 * Created by 含光<jiankang.pjk@alibaba-inc.com> on 2021/2/4 14:57.
 */
declare namespace CP_SELECT_PRO {
  interface Spec {
    type: 'SelectPro';
    operations: Obj<CP_COMMON.Operation>;
    props: IProps;
    data: IData;
    state?: IState;
  }

  interface IData {
    list: Obj[];
  }

  interface IState {
    value?: string;
  }

  interface IProps {
    renderType: string;
    showSearch: boolean;
    optionLabelProp?: string;
    placeholder?: string;
    allowClear?: boolean;
  }

  type Props = MakeProps<Spec>;
}
