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

declare namespace CP_TAGS {
  interface ILabel {
    label: string;
    group?: string;
    color?: string;
  }

  interface IProps {
    visible?: boolean;
    showCount?: number;
    size: 'small' | 'default';
    onAdd?: () => void;
  }

  interface Spec {
    type: 'Tags';
    props: IProps;
    data: {
      labels: ILabel[] | ILabel;
    };
    operations?: CP_COMMON.Operation;
  }

  type Props = MakeProps<Spec>;
}
