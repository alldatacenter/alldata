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

declare namespace CP_DATE_PICKER {
  interface Spec {
    type: 'DatePicker';
    props: IProps;
    state: IState;
    cId: string;
  }

  type Moment = import('moment').Moment;

  interface IState {
    value: number | number[];
  }

  interface IPureDatePickerProps {
    allowClear?: boolean;
    bordered?: boolean;
    className?: string;
    showTime?: boolean;
    disabled?: boolean;
    mode?: 'time' | 'date' | 'month' | 'year' | 'decade';
    picker?: 'date' | 'week' | 'month' | 'quarter' | 'year';
    placeholder?: string;
    size?: 'large' | 'middle' | 'small';
    defaultPickerValue?: number | number[];
    ranges?: Obj<number[]>;
    defaultValue?: number | number[];
    format?: string;
  }

  interface IProps extends IPureDatePickerProps {
    visible?: boolean;
    type?: 'dateRange';
    borderTime?: boolean;
    disabledFuture?: boolean;
  }

  type Props = MakeProps<Spec> & {
    extraFooter?: React.ReactElement;
  };
}
