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

declare namespace CP_GANTT {
  interface Spec {
    type: 'Gantt';
    props: IProps;
    data: {
      expandList: Obj<IData[]>;
      updateList: IData[];
      refresh?: boolean;
    };
  }

  interface IData {
    key: string;
    title: string;
    start: number;
    end: number;
    isLeaf?: boolean;
    hideChildren?: boolean;
    extra?: Obj;
  }

  type TaskType = 'task' | 'milestone' | 'project';
  interface IGanttData {
    id: string;
    type: TaskType;
    name: string;
    start: Date;
    end: Date;
    progress: number;
    extra?: Obj;
    level: number;
    isLeaf?: boolean;
    styles?: {
      backgroundColor?: string;
      backgroundSelectedColor?: string;
      progressColor?: string;
      progressSelectedColor?: string;
    };
    isDisabled?: boolean;
    project?: string;
    dependencies?: string[];
    hideChildren?: boolean;
  }

  interface IProps extends CustomProps {
    rowHeight?: number;
    listCellWidth?: string;
  }

  interface CustomProps {
    BarContentRender: React.FC<{ tasks: IGanttData[]; rowHeight: number; onExpanderClick: (task: IGanttData) => void }>;
    TreeNodeRender: React.FC<{ node: IGanttData; nodeList: IGanttData[] }>;
    TaskListHeader?: React.FC<{
      headerHeight: number;
      rowWidth: string;
      fontFamily: string;
      fontSize: string;
    }>;
    rootWrapper: React.ReactElement;
    onScreenChange: () => void,
  }

  type Props = MakeProps<Spec> & {};
}
