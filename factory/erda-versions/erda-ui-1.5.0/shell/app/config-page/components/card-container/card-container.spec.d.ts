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

declare namespace CP_CARD_CONTAINER {
  interface Spec {
    type: 'ChartContainer' | 'CardContainer';
    props?: IProps;
  }

  interface IProps {
    visible?: boolean;
    title: string;
    tip?: string;
    holderWhen?: boolean;
    style?: React.CSSProperties;
  }

  type Props = MakeProps<Spec> & {
    title: string | React.ElementType;
    extraContent: React.ReactChild | React.ReactChild[];
    children: React.ReactChild | React.ReactChild[];
  };
}
