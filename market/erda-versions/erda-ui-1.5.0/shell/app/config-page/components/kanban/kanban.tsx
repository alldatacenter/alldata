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
import { map, find, without, get } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import { Card } from '../card/card';
import { Input, Button, Popconfirm, Tooltip } from 'antd';
import { EmptyHolder, ErdaIcon } from 'common';
import { notify } from 'common/utils';
import { WithAuth } from 'user/common';
import { useDrop, DragObjectWithType } from 'react-dnd';
import { useUpdateEffect } from 'react-use';
import i18n from 'i18n';
import classnames from 'classnames';
import { getClass } from 'config-page/utils';
import createScrollingComponent from 'common/utils/create-scroll-component';
import produce from 'immer';
import './kanban.scss';

interface IState {
  showAdd: boolean;
  addValue: string;
  boards: CP_KANBAN.IBoard[];
  isDrag: boolean;
  currentCard: null | CP_KANBAN.ICard;
}

const ScrollingComponent = createScrollingComponent('div');
const Kanban = (props: CP_KANBAN.Props) => {
  const { data, props: configProps, execOperation, customOp } = props || {};

  const { CardRender, className = '' } = configProps || {};

  const { operations, boards: dataBoards } = data || {};
  const [{ showAdd, addValue, isDrag, currentCard, boards }, updater, update] = useUpdate<IState>({
    showAdd: false,
    addValue: '',
    boards: dataBoards || [],
    isDrag: false,
    currentCard: null,
  });

  React.useEffect(() => {
    const isLoadMore = dataBoards?.length === 1 && find(dataBoards, (item) => item.pageNo > 1);
    if (isLoadMore) {
      updater.boards((prev) =>
        map(prev, (item) => {
          const curNewData = find(dataBoards, (newItem) => newItem.id === item.id);
          return curNewData || item;
        }),
      );
    } else {
      updater.boards(dataBoards || []);
    }
  }, [data, updater]);

  const hideAdd = () => {
    update({ addValue: '', showAdd: false });
  };
  const doAdd = () => {
    const existTitle = find(boards || [], { title: addValue });
    if (existTitle) {
      notify('error', i18n.t('{name} already exists', { name: addValue }));
      return;
    }
    execOperation({ key: 'boardCreate', reload: true, ...operations?.boardCreate, clientData: { title: addValue } });
    hideAdd();
  };

  return (
    <ScrollingComponent className={`dice-cp cp-kanban ${className} ${getClass(configProps)}`}>
      {map(boards || [], (item) => {
        return item ? (
          <PureKanban
            setBoard={updater.boards}
            boards={boards}
            data={item}
            key={item.id || item.title}
            setIsDrag={updater.isDrag}
            isDrag={isDrag}
            CardRender={CardRender}
            isLoadMore
            setCurrentCard={updater.currentCard}
            currentCard={currentCard}
            execOperation={execOperation}
            customOp={customOp}
          />
        ) : null;
      })}
      {operations?.boardCreate ? (
        <div className="cp-kanban-col add-item">
          {showAdd ? (
            <div className="mt-5">
              <Input
                value={addValue}
                className="mb-2"
                onChange={(e) => updater.addValue(e.target.value)}
                placeholder={i18n.t('dop:input custom boards name')}
                onPressEnter={doAdd}
              />
              <div className="flex justify-between items-center">
                <Button onClick={hideAdd} className="mr-2">
                  {i18n.t('cancel')}
                </Button>
                <Button onClick={doAdd} type="primary">
                  {i18n.t('ok')}
                </Button>
              </div>
            </div>
          ) : operations.boardCreate.disabled ? (
            <Tooltip title={operations.boardCreate.disabledTip}>
              <ErdaIcon type="plus" className="cursor-pointer add-icon not-allowed" />
            </Tooltip>
          ) : (
            <ErdaIcon type="plus" className="cursor-pointer add-icon" onClick={() => updater.showAdd(true)} />
          )}
        </div>
      ) : null}
      {!dataBoards?.length ? <EmptyHolder relative className="w-full" /> : null}
    </ScrollingComponent>
  );
};

interface IKanbanProps {
  data: CP_KANBAN.IBoard;
  boards: CP_KANBAN.IBoard[];
  setBoard: (boards: CP_KANBAN.IBoard[]) => void;
  CardRender?: React.FC<{ data: Obj }>;
  isDrag: boolean;
  setIsDrag: (isDrag: boolean) => void;
  currentCard: CP_KANBAN.ICard | null;
  setCurrentCard: (value: CP_KANBAN.ICard | null) => void;
  execOperation: (opObj: CP_COMMON.Operation, extraUpdateInfo?: Obj) => void;
  customOp?: Obj;
}

interface ICardData extends Omit<CP_KANBAN.ICard, 'operations'> {
  boardId: string;
  operations: {
    cardMoveTo: CP_KANBAN.IMoveToOP;
  };
}
const PureKanban = (props: IKanbanProps) => {
  const { data, boards, execOperation, setBoard, setIsDrag, isDrag, setCurrentCard, currentCard, CardRender, ...rest } =
    props;
  const {
    id: boardId,
    title: boardTitle,
    cards: boardCards,
    pageNo: propsPageNo,
    operations,
    total = 0,
    pageSize = 20,
    ...dataRest
  } = data || {};
  const titleArr = map(boards, 'title');
  const otherTitle = without(titleArr, boardTitle);
  const [pageNo, setPageNo] = React.useState(propsPageNo);
  const [cards, setCards] = React.useState(boardCards || []);
  const [title, setTitle] = React.useState(boardTitle);
  const [showShadow, setShowShadow] = React.useState(false);
  const cardType = 'kanban-info-card';

  const boardDataRef = {
    id: boardId,
    title: boardTitle,
    pageNo: propsPageNo,
    operations,
    total,
    pageSize,
    ...dataRest,
  };

  useUpdateEffect(() => {
    if (propsPageNo > pageNo) {
      boardCards && setCards((prev) => prev.concat(boardCards));
    } else {
      setCards(boardCards || []);
    }
    setPageNo(propsPageNo);
  }, [boardCards, propsPageNo]);

  const boardLoadMoreOp = operations?.boardLoadMore;
  const hasMore = boardLoadMoreOp && total > cards.length;

  const [{ isOver, isAllowDrop }, drop] = useDrop({
    accept: cardType,
    drop: (item: Merge<DragObjectWithType, { data: ICardData }>) => {
      const { cardMoveTo } = item.data.operations;
      const targetKeys = cardMoveTo.serverData?.extra?.allowedTargetBoardIDs || [];
      if (!targetKeys?.includes(boardId)) {
        setCurrentCard(null);
        return;
      }
      setCurrentCard(item.data);
      const dragColKey = item.data.boardId;
      const dropColKey = boardId;
      let newTargetKeys = [...targetKeys];
      if (!newTargetKeys.includes(dragColKey)) {
        newTargetKeys.push(dragColKey);
      }
      newTargetKeys = newTargetKeys.filter((t) => t !== dropColKey);
      const newItem = produce(item, (draft: { data: Obj }) => {
        draft.data.operations.cardMoveTo.serverData.extra.allowedTargetBoardIDs = newTargetKeys;
      });
      setBoard((prev) => {
        return prev.map((col) => {
          if (col.id === dropColKey) {
            return {
              ...col,
              cards: col.cards ? [newItem.data, ...col.cards] : [newItem.data],
              total: +(col.total || 0) + 1,
            };
          } else if (col.id === dragColKey) {
            return {
              ...col,
              cards: col.cards?.filter((a) => a.id !== newItem.data.id),
              total: Math.max((col.total || 0) - 1, 0),
            };
          }
          return col;
        });
      });
      execOperation({
        reload: true,
        ...cardMoveTo,
        clientData: { targetBoardID: boardId, dataRef: item.data.dataRef },
      });
    },
    collect: (monitor) => {
      const item = monitor?.getItem && monitor?.getItem();
      const targetKeys = get(item, 'data.operations.cardMoveTo.serverData.extra.allowedTargetBoardIDs') || [];
      let _isAllowDrop = true;
      if (!targetKeys?.length || !targetKeys.includes(boardId)) {
        _isAllowDrop = false;
      }
      return {
        isOver: monitor.isOver(),
        isAllowDrop: _isAllowDrop,
      };
    },
  });

  const changeData = (item: CP_KANBAN.ICard) => {
    const { operations: cardOp } = item;
    return {
      ...item,
      boardId,
      operations: {
        ...(cardOp?.cardMoveTo ? { cardMoveTo: { key: 'cardMoveTo', ...cardOp.cardMoveTo } } : {}),
      },
      dataRef: item,
    };
  };

  let cls = isOver ? 'drag-over' : '';
  cls = isDrag && !isAllowDrop ? `drop-disable ${cls}` : cls;
  cls = isDrag && !isOver ? `not-drag ${cls}` : cls;
  const deleteBoardOp = operations?.boardDelete;
  const deleteAuth = deleteBoardOp?.disabled !== true;
  const updateBoardOp = operations?.boardUpdate;
  const updateAuth = updateBoardOp?.disabled !== true;

  const doUpdate = () => {
    if (title === boardTitle) return;
    if (!title) {
      setTitle(boardTitle);
      return notify('error', i18n.t('can not be empty'));
    }
    if (otherTitle.includes(title)) {
      setTitle(boardTitle);
      return notify('error', i18n.t('{name} already exists', { name: boardTitle }));
    }
    execOperation({
      key: 'boardUpdate',
      reload: true,
      ...operations?.boardUpdate,
      clientData: { dataRef: data, title },
    });
  };

  const handleScroll = (e: any) => {
    setShowShadow(e.target.scrollTop !== 0);
  };

  const loadMore = () => {
    execOperation({
      key: 'boardLoadMore',
      reload: true,
      ...boardLoadMoreOp,
      clientData: {
        pageNo: pageNo + 1,
        pageSize,
        dataRef: boardDataRef,
      },
    } as CP_COMMON.Operation);
  };

  return (
    <div className={classnames(`cp-kanban-col ${cls}`, { 'cp-kanban-col-special-pdd': updateBoardOp })} ref={drop}>
      <div
        className={`flex justify-between items-center cp-kanban-col-header ${showShadow ? 'shadow' : ''} ${
          updateBoardOp ? 'inp' : ''
        }`}
      >
        <div className="text-base font-medium text-default-8 flex-1 flex items-center ">
          {updateBoardOp ? (
            updateAuth ? (
              <Input
                className="text-base font-medium cp-kanban-label-input"
                value={title}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setTitle(e.target.value)}
                onPressEnter={doUpdate}
                onBlur={doUpdate}
              />
            ) : (
              <Tooltip title={updateBoardOp.disabledTip || i18n.t('common:no permission to operate')}>
                <Input className="text-base font-medium cp-kanban-label-input update-disabled" readOnly value={title} />
              </Tooltip>
            )
          ) : (
            title
          )}
          <div className="text-default-8 ml-1 text-sm px-2.5 rounded-lg bg-default-06">{total}</div>
        </div>
        {deleteBoardOp ? (
          deleteBoardOp.confirm ? (
            <WithAuth pass={deleteAuth} noAuthTip={deleteBoardOp.disabledTip}>
              <Popconfirm
                title={deleteBoardOp.confirm}
                onConfirm={() =>
                  execOperation({
                    key: 'boardDelete',
                    reload: true,
                    ...deleteBoardOp,
                    clientData: { dataRef: boardDataRef },
                  })
                }
              >
                <ErdaIcon type="delete1" className="ml-3 cursor-pointer" />
              </Popconfirm>
            </WithAuth>
          ) : (
            <WithAuth pass={deleteAuth} noAuthTip={deleteBoardOp.disabledTip}>
              <ErdaIcon
                type="delete1"
                className="ml-3 cursor-pointer"
                onClick={() =>
                  execOperation({
                    key: 'boardDelete',
                    reload: true,
                    ...deleteBoardOp,
                    clientData: { dataRef: boardDataRef },
                  })
                }
              />
            </WithAuth>
          )
        ) : null}
      </div>
      <div className="cp-kanban-col-content" onScroll={handleScroll}>
        {map(cards, (item) => {
          return (
            <Card
              key={item.id}
              execOperation={execOperation}
              props={{
                CardRender,
                cardType,
                className: `${isDrag ? 'hidden' : ''} list-item ${currentCard?.id === item.id ? 'dragged-card' : ''}`,
                data: changeData(item),
                setIsDrag,
              }}
              customOp={rest.customOp}
            />
          );
        })}
        {hasMore && !isDrag ? (
          <div className="hover-active py-1 text-center load-more" onClick={() => loadMore()}>
            {i18n.t('load more')}
          </div>
        ) : null}
      </div>
    </div>
  );
};

export default Kanban;
