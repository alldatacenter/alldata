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
import { EmptyHolder } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Table, Modal, Collapse, Button, Spin, Tooltip } from 'antd';
import i18n from 'i18n';
import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { every, isEmpty, map, values, forEach } from 'lodash';
import routeInfoStore from 'core/stores/route';
import { API_FORM_KEY, API_PROPERTY_REQUIRED } from 'app/modules/apiManagePlatform/configs.ts';
import { useLoading } from 'core/stores/loading';
import { produce } from 'immer';
import './api-params-modal.scss';

const { Panel } = Collapse;

const SAME_TIP = i18n.t('dop:same field exists');
const EMPTY_TIP = i18n.t('dop:please select the parameters to be imported');

interface IProps {
  visible: boolean;
  paramList: Obj[];
  onClose: () => void;
  onImport: (e: Obj[]) => void;
}

const ApiParamsModal = (props: IProps) => {
  const { visible, onClose, onImport, paramList } = props;
  const [{ selectedParams, isValidData, selectedParamsMap, disabledTip }, updater, update] = useUpdate({
    selectedParams: [],
    isValidData: false,
    selectedParamsMap: {},
    disabledTip: EMPTY_TIP,
  });

  const { inode } = routeInfoStore.useStore((s) => s.query);

  const schemaParams = apiDesignStore.useStore((s) => s.schemaParams);
  const { getSchemaParams, clearSchemaParams } = apiDesignStore;

  const [isFetching] = useLoading(apiDesignStore, ['getSchemaParams']);

  React.useEffect(() => {
    if (visible) {
      getSchemaParams({ inode });
    } else {
      update({
        selectedParams: [],
        isValidData: false,
        disabledTip: EMPTY_TIP,
      });
      clearSchemaParams();
    }
  }, [clearSchemaParams, getSchemaParams, inode, update, visible]);

  React.useEffect(() => {
    const tempData = {};
    forEach(schemaParams, ({ tableName }) => {
      tempData[tableName] = [];
    });
    updater.selectedParamsMap(tempData);
  }, [getSchemaParams, inode, schemaParams, update, updater, visible]);

  const columns = [
    {
      title: i18n.t('application'),
      dataIndex: API_FORM_KEY,
    },
    {
      title: i18n.t('type'),
      dataIndex: 'type',
    },
  ];

  const onCancel = () => {
    onClose();
  };

  const onOk = () => {
    onImport(selectedParams);
  };

  const handleSelect = React.useCallback(
    (selectedRows: any[], tableName: string) => {
      const existNames = map(paramList, (item) => item[API_FORM_KEY]);

      const _tempData = produce(selectedParamsMap, (draft) => {
        draft[tableName] = selectedRows;
      });
      updater.selectedParamsMap(_tempData);

      const isNoSame = every(_tempData[tableName], (item) => !existNames.includes(item[API_FORM_KEY]));
      if (!isNoSame) {
        update({
          isValidData: false,
          disabledTip: SAME_TIP,
        });
      } else {
        const paramsMap = {};
        let isValid = true;

        forEach(values(_tempData), (pList) => {
          forEach(pList, (item) => {
            if (!paramsMap[item[API_FORM_KEY]]) {
              const tempItem = { ...item };
              tempItem[API_PROPERTY_REQUIRED] = true;
              paramsMap[tempItem[API_FORM_KEY]] = tempItem;
            } else {
              isValid = false;
            }
          });
        });

        if (!isValid) {
          update({
            isValidData: false,
            disabledTip: SAME_TIP,
          });
        } else {
          const validParams = values(paramsMap);
          const isEmptySelected = isEmpty(validParams);
          update({
            selectedParams: validParams,
            isValidData: !isEmptySelected,
          });

          if (isEmptySelected) {
            updater.disabledTip(EMPTY_TIP);
          }
        }
      }
    },
    [paramList, selectedParamsMap, update, updater],
  );

  return (
    <Modal
      title={i18n.t('dop:import parameters')}
      visible={visible}
      onCancel={onCancel}
      destroyOnClose
      maskClosable={false}
      className="api-params-modal"
      footer={
        schemaParams?.length
          ? [
              <Tooltip title={!isValidData ? disabledTip : undefined}>
                <Button type="primary" disabled={!isValidData} onClick={onOk}>
                  {i18n.t('ok')}
                </Button>
              </Tooltip>,
            ]
          : []
      }
    >
      <Spin spinning={isFetching}>
        {!schemaParams?.length ? (
          <EmptyHolder relative />
        ) : (
          <div className="schema-params-list">
            {map(schemaParams, ({ tableName, list }) => {
              return (
                <Collapse style={{ border: 'none' }} key={tableName}>
                  <Panel header={tableName} key={tableName}>
                    <Table
                      rowKey={API_FORM_KEY}
                      columns={columns}
                      dataSource={list}
                      rowSelection={{
                        hideDefaultSelections: true,
                        onChange: (_selectedKeys: string[], selectedRows: any[]) => {
                          handleSelect(selectedRows, tableName);
                        },
                      }}
                      pagination={false}
                      scroll={{ x: '100%' }}
                    />
                  </Panel>
                </Collapse>
              );
            })}
          </div>
        )}
      </Spin>
    </Modal>
  );
};

export default ApiParamsModal;
