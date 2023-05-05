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
import { isNumber, filter, map, sortBy } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import { OperationAction } from 'config-page/utils';
import { containerMap } from '../../components';
import ErdaList from 'common/components/list';

import './list-new.scss';

const emptyArr = [] as ERDA_LIST.IListData[];
const List = (props: CP_LIST_NEW.Props) => {
  const { customOp, execOperation, operations, props: configProps, data, state: propsState } = props;
  const { list = emptyArr } = data || {};
  const [state, updater, update] = useUpdate({
    ...propsState,
    combineList: list,
  });
  const { total = 0, pageSize, pageNo = 1 } = state || {};

  const {
    isLoadMore = false,
    visible = true,
    size = 'middle',
    rowKey,
    alignCenter = false,
    noBorder = false,
    pageSizeOptions,
  } = configProps || {};
  const currentList = React.useMemo(
    () =>
      (isLoadMore ? state.combineList : list).map((item: CP_LIST_NEW.IListData) => {
        let extraInfos = [] as ERDA_LIST.IIconInfo[];
        if (item.extraInfos) {
          extraInfos = item.extraInfos.map((info: CP_LIST_NEW.IIconInfo) => {
            const extraProps = {} as ERDA_LIST.IExtraProps;
            if (info.operations?.click) {
              extraProps.onClick = (e: React.MouseEvent<HTMLElement, MouseEvent>) => {
                e && e.stopPropagation();
                const curOp = (info.operations as Obj<CP_COMMON.Operation>).click;
                execOperation(curOp, data);
                if (customOp && customOp[curOp.key]) {
                  customOp[curOp.key](curOp, data);
                }
              };
            }

            return { ...info, extraProps };
          });
        }

        let extraContent = null;
        if (item.extraContent && item.extraContent.type) {
          const { type } = item.extraContent;
          const { extraContent: extraContentProps = {} } = configProps;
          const Comp = containerMap[type];

          extraContent = (
            <div className="flex h-16 justify-between">
              {item.extraContent.data?.map((dataItem: CP_LIST_NEW.IPieChart) => {
                const infoItem = dataItem.info.map((info: CP_LIST_NEW.IChartInfo) => ({
                  ...info,
                  className: 'flex-1',
                }));
                return (
                  <div className="mx-2 flex-1">
                    <Comp data={{ data: [{ ...dataItem, info: infoItem }] }} props={extraContentProps} />
                  </div>
                );
              })}
            </div>
          );
        }

        return {
          ...item,
          extraInfos,
          extraContent,
        };
      }) || [],
    [customOp, data, execOperation, isLoadMore, list, state.combineList, configProps],
  );

  // 将接口返回的list和之前的list进行拼接
  React.useEffect(() => {
    // if isLoadMore is true, the data will be set undefined, combineList don't need to do anything
    if (data === undefined) {
      return;
    }
    update((pre) => {
      const newState = {
        ...pre,
        ...propsState,
      };
      return {
        ...newState,
        combineList: newState.pageNo === 1 ? list : (newState.combineList || []).concat(list),
      };
    });
  }, [propsState, list, update, data]);

  const pagination = React.useMemo(() => {
    return isNumber(pageNo)
      ? {
          total: total || list?.length,
          current: pageNo || 1,
          pageSize: pageSize || 20,
          onChange: (_no: number, _size: number) => changePage(_no, _size),
          ...(pageSizeOptions
            ? {
                showSizeChanger: true,
                pageSizeOptions,
              }
            : {}),
        }
      : undefined;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageNo, pageSize, total]);

  if (!visible) return null;

  const changePage = (pNo: number, pSize: number) => {
    operations?.changePageNo && execOperation(operations.changePageNo, { pageNo: pNo, pageSize: pSize });
  };

  const loadMore = () => {
    operations?.changePageNo && execOperation(operations.changePageNo, { pageNo: +pageNo + 1 });
  };

  const getKey = (item: ERDA_LIST.IListData, idx: number) => {
    return rowKey ? item[rowKey] : `${idx}-${item.title}`;
  };

  const onRow = {
    onClick: (record: ERDA_LIST.IListData) => {
      const { click } = record.operations || {};
      if (click) {
        execOperation(click, record);
      }
      if (customOp?.clickItem) {
        customOp.clickItem(click, record);
      }
    },
  };

  const itemOperations = (record: CP_LIST_NEW.IListData) => {
    const { click, ...restOp } = record.operations || {};
    const actions = sortBy(
      filter(map(restOp) || [], (item) => item.show !== false),
      'showIndex',
    );

    return actions.map((action) => ({
      title: (
        <OperationAction
          tipProps={{ placement: 'left' }}
          operation={action}
          onClick={() => {
            execOperation(action);
            if (customOp && customOp[action.key]) {
              customOp[action.key](action, data);
            }
          }}
        >
          <div>{action.text}</div>
        </OperationAction>
      ),
      key: action.key,
    }));
  };

  return (
    <ErdaList
      dataSource={currentList}
      pagination={pagination}
      isLoadMore={isLoadMore}
      onLoadMore={loadMore}
      onRow={onRow}
      getKey={getKey}
      operations={itemOperations}
      size={size}
      noBorder={noBorder}
      alignCenter={alignCenter}
    />
  );
};

export default List;
