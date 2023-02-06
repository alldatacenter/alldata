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
import { Button, Table, Spin } from 'antd';
import FormModal from '../form-modal';
import CustomFilter from '../custom-filter';
import IF from '../if';
import { IFormItem } from 'common';
import { useUpdate, useFilter } from 'common/use-hooks';
import { isEmpty, reduce } from 'lodash';
import { isPromise } from 'common/utils';
import i18n from 'i18n';
import { useEffectOnce } from 'react-use';
import { FormInstance, ColumnProps } from 'core/common/interface';
import { ICRUDStore } from 'common/stores/_crud_module';
import { useLoading } from 'core/stores/loading';
import { WithAuth } from 'user/common';

export interface ITableProps<P> {
  isFetching: boolean;
  name?: string;
  rowKey?: string | ((r: P) => string);
  formTitle?: string;
  extraQuery?: any;
  showTopAdd?: boolean;
  hasAddAuth?: boolean;
  addAuthTooltipTitle?: JSX.Element | string;
  list: P[];
  paging?: IPaging;
  filterConfig?: FilterItemConfig[];
  showSearchButton?: boolean;
  tableProps?: any;
  extraOperation?: React.ReactNode | (() => React.ReactNode);
  getList: (arg: any) => Promise<any>;
  clearList?: (arg?: any) => Promise<any> | void;
  handleFormSubmit?: (data: any, isEdit: boolean) => Promise<any> | void;
  getFieldsList?: (form: FormInstance, isEdit: boolean) => IFormItem[];
  getColumns: ({ onEdit, reloadList }: { onEdit: (arg: P) => void; reloadList: () => void }) => Array<ColumnProps<P>>;
  onModalClose?: (vis: boolean) => void;
}

const emptyArr = [] as any[];
function CRUDTable<P>(props: ITableProps<P>) {
  const {
    isFetching,
    getList,
    clearList,
    list,
    paging,
    rowKey,
    getFieldsList,
    extraQuery = {},
    showTopAdd,
    extraOperation,
    hasAddAuth = true,
    addAuthTooltipTitle = '',
    name = '',
    formTitle = '',
    filterConfig = emptyArr,
    handleFormSubmit,
    getColumns,
    tableProps = {},
    onModalClose,
    showSearchButton,
  } = props;
  const hasForm = !!getFieldsList;
  const [editData, setEditData] = React.useState(null as P | null);
  const [{ visible }, updater] = useUpdate({
    visible: false,
  });

  const { onSubmit, onReset, fetchDataWithQuery, autoPagination } = useFilter({
    getData: getList,
    extraQuery,
    debounceGap: 500,
    initQuery: reduce(
      filterConfig,
      (acc, config) => {
        if (config.initialValue) {
          return { ...acc, [config.name]: config.initialValue };
        }
        return acc;
      },
      {},
    ),
  });

  useEffectOnce(() => {
    return () => {
      clearList && clearList();
    };
  });

  const handelSubmit = (data: P) => {
    if (handleFormSubmit) {
      const res = handleFormSubmit(data, !isEmpty(editData));
      if (res && isPromise(res)) {
        return res.then(() => {
          onCancel();
          fetchDataWithQuery();
        });
      } else {
        return onCancel();
      }
    }
  };

  const editItem = (item: P) => {
    openModal(item);
  };

  const onCancel = () => {
    setEditData(null);
    updater.visible(false);
    onModalClose && onModalClose(false);
  };

  const openModal = (curData?: P) => {
    if (hasForm) {
      setEditData(curData || null);
      updater.visible(true);
    }
  };

  const TableComp = (
    <Spin spinning={isFetching}>
      <Table
        rowKey={rowKey || 'id'}
        columns={getColumns({ onEdit: editItem, reloadList: fetchDataWithQuery })}
        dataSource={list}
        pagination={paging ? autoPagination(paging) : false}
        scroll={{ x: '100%' }}
        {...tableProps}
      />
    </Spin>
  );
  return (
    <div className="crud-table">
      {!isEmpty(filterConfig) ? (
        <CustomFilter
          showButton={!!showSearchButton}
          onSubmit={onSubmit}
          onReset={onReset}
          config={filterConfig}
          isConnectQuery
        />
      ) : null}
      <div className={showTopAdd ? 'top-button-group' : ''}>
        {typeof extraOperation === 'function' ? extraOperation() : extraOperation}
        <IF check={hasForm}>
          <WithAuth pass={hasAddAuth} noAuthTip={addAuthTooltipTitle}>
            <Button type="primary" onClick={() => openModal()} className="mb-2">
              {i18n.t('add {name}', { name })}
            </Button>
          </WithAuth>
        </IF>
      </div>
      {hasForm ? (
        <FormModal
          title={formTitle}
          name={name}
          fieldsList={getFieldsList}
          visible={visible}
          onOk={handelSubmit}
          onCancel={onCancel}
          formData={editData}
          modalProps={{
            maskClosable: false,
            destroyOnClose: true,
          }}
        />
      ) : null}
      {TableComp}
    </div>
  );
}

export interface ICRUDStoreProps<P> {
  name?: string;
  formTitle?: string;
  rowKey?: string | ((r: P) => string);
  filterConfig?: FilterItemConfig[];
  store: ICRUDStore;
  extraQuery?: any;
  tableProps?: any;
  showTopAdd?: boolean;
  hasAddAuth?: boolean;
  addAuthTooltipTitle?: JSX.Element | string;
  getFieldsList?: (form: FormInstance, isEdit: boolean) => IFormItem[];
  handleFormSubmit?: (
    data: P,
    {
      addItem,
      updateItem,
      isEdit,
    }: {
      addItem: (arg: any) => Promise<any>;
      updateItem: (arg: any) => Promise<any>;
      isEdit: boolean;
    },
  ) => Promise<any>;
  getColumns: (
    effects: any,
    { onEdit, reloadList }: { onEdit: (arg: any) => void; reloadList: () => void },
  ) => Array<ColumnProps<P>>;
  onModalClose?: (vis: boolean) => void;
}
function CRUDStoreTable<P>(props: ICRUDStoreProps<P>) {
  const { store, getColumns, handleFormSubmit: handleSubmit, ...rest } = props;
  const [loadingList] = useLoading(store, ['getList']);
  const { getList, addItem, updateItem } = store.effects;
  const { clearList } = store.reducers;
  const [list, paging] = store.useStore((s) => [s.list, s.paging]);
  const handleFormSubmit = (data: P, isEdit: boolean) => {
    if (typeof handleSubmit === 'function') {
      return handleSubmit(data, { isEdit, updateItem, addItem });
    } else if (isEdit) {
      return updateItem(data);
    } else {
      return addItem(data);
    }
  };
  return (
    <CRUDTable<P>
      isFetching={loadingList}
      getList={getList}
      clearList={clearList}
      getColumns={({ onEdit, reloadList }) => getColumns(store.effects, { onEdit, reloadList })}
      handleFormSubmit={handleFormSubmit}
      list={list as P[]}
      paging={paging}
      {...rest}
    />
  );
}

CRUDTable.StoreTable = CRUDStoreTable;
export default CRUDTable;
