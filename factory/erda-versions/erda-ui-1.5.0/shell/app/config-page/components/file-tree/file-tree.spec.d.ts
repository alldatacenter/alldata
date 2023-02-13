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

declare namespace CP_FILE_TREE {
  interface Spec {
    type: 'FileTree';
    data: IData;
    props: IProps;
  }

  interface INode {
    key: string;
    title: string | JSX.Element;
    icon?: string | JSX.Element;
    isColorIcon?: boolean;
    children?: INode[];
    selectable?: boolean;
    clickToExpand?: boolean;
    isLeaf?: boolean;
    operations?: Obj;
    _dataType?: string;
  }

  interface Field {
    label?: string;
    valueKey?: any;
  }

  interface IProps {
    searchable?: boolean;
    draggable?: boolean;
    multiple?: boolean;
  }
  interface IData {
    treeData: INode[];
  }

  type Props = MakeProps<Spec>;
}
