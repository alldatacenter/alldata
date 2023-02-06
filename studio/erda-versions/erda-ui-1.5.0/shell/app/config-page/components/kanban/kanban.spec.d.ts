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

declare namespace CP_KANBAN {
  interface Spec {
    type: 'Kanban';
    props: IProps;
    data: IData;
  }

  interface IKanbanOp {
    boardCreate?: CP_COMMON.Operation;
  }

  interface IData {
    boards: IBoard[];
    operations?: IKanbanOp;
  }

  interface IBoard {
    id: string;
    title: string;
    cards: ICard[];
    pageNo: number;
    pageSize: number;
    total: number;
    operations?: IBoardOp;
  }

  interface IBoardOp {
    boardLoadMore?: CP_COMMON.Operation;
    boardUpdate?: CP_COMMON.Operation;
    boardDelete?: CP_COMMON.Operation;
  }

  interface ICard {
    id: string;
    title: string;
    operations: ICardOp;
    extra: Obj;
  }

  interface ICardOp {
    cardMoveTo?: IMoveToOP;
  }

  type IMoveToOP = Merge<
    CP_COMMON.Operation,
    {
      serviceData?: {
        extra: {
          allowedMoveToTargetBoardIDs: string[];
        };
      };
    }
  >;

  interface IProps {
    CardRender?: React.FC<{ data: Obj }>;
  }

  type Props = MakeProps<Spec>;
}
