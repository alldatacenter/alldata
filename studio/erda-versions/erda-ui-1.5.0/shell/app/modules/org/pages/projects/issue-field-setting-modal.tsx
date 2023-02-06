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

import i18n from 'i18n';
import React from 'react';
import { useUpdate } from 'common/use-hooks';
import issueFieldStore from 'org/stores/issue-field';
import orgStore from 'app/org-home/stores/org';
import { FIELD_TYPE_ICON_MAP, DEFAULT_ISSUE_FIELDS_MAP } from 'org/common/config';
import { getFieldsByIssue as getFieldOptions } from 'org/services/issue-field';
import { map, isEmpty, find } from 'lodash';
import { Modal, Select, Button, Popconfirm } from 'antd';
import './issue-field-setting-modal.scss';
import { produce } from 'immer';
import { useEffectOnce } from 'react-use';
import { IssueIcon } from 'org/common/issue-field-icon';

const { Option } = Select;

interface IProps {
  visible: boolean;
  issueType: ISSUE_FIELD.IIssueType;
  closeModal: () => void;
}

export const IssueFieldSettingModal = ({ visible, issueType = 'EPIC', closeModal }: IProps) => {
  const { addFieldItem, batchUpdateFieldsOrder, deleteFieldItem, getFieldsByIssue } = issueFieldStore.effects;
  const [fieldList] = issueFieldStore.useStore((s) => [s.fieldList]);
  const { clearFieldList } = issueFieldStore.reducers;
  const { id: orgID } = orgStore.useStore((s) => s.currentOrg);

  const [{ selectedField, filedOptions }, updater, update] = useUpdate({
    selectedField: {} as ISSUE_FIELD.IFiledItem,
    filedOptions: [] as ISSUE_FIELD.IFiledItem[],
  });

  useEffectOnce(() => {
    getFieldOptions({ propertyIssueType: 'COMMON', orgID }).then(({ data }) => {
      updater.filedOptions(data || []);
    });
  });

  const onAddField = React.useCallback(async () => {
    const params = {
      ...selectedField,
      propertyIssueType: issueType,
      relation: selectedField.propertyID,
    } as Omit<ISSUE_FIELD.IFiledItem, 'propertyID' | 'index'>;

    await addFieldItem(params);
    update({ selectedField: undefined });
    getFieldsByIssue({ propertyIssueType: issueType, orgID });
  }, [addFieldItem, getFieldsByIssue, issueType, orgID, selectedField, update]);

  const onCloseModal = () => {
    closeModal();
    update({ selectedField: undefined });
    clearFieldList();
  };

  const changePos = React.useCallback(
    async (index: number, direction: number) => {
      if (fieldList.length > 1) {
        const tempList = produce(fieldList, (draft) => {
          if (direction < 0 && index > 0) {
            draft[index - 1].index = index;
            draft[index].index = index - 1;
          } else {
            draft[index].index = index + 1;
            draft[index + 1].index = index;
          }
        });

        await batchUpdateFieldsOrder(tempList);
        getFieldsByIssue({ propertyIssueType: issueType, orgID });
      }
    },
    [batchUpdateFieldsOrder, fieldList, getFieldsByIssue, issueType, orgID],
  );

  const onDelete = React.useCallback(
    async (propertyID) => {
      await deleteFieldItem({ propertyID });
      getFieldsByIssue({ propertyIssueType: issueType, orgID });
    },
    [deleteFieldItem, getFieldsByIssue, issueType, orgID],
  );

  const renderFieldItem = ({ displayName, propertyType }: { displayName: string; propertyType: string }) => (
    <>
      <div className="nowrap field-label">{displayName}</div>
      <div className="">
        <IssueIcon type={propertyType} withName />
      </div>
    </>
  );

  const renderDefaultContent = React.useMemo(
    () =>
      map(DEFAULT_ISSUE_FIELDS_MAP[issueType], ({ propertyName, displayName, propertyType }) => {
        return <div key={propertyName}>{renderFieldItem({ displayName, propertyType })}</div>;
      }),
    [issueType],
  );

  const renderCustomFields = React.useCallback(
    () =>
      map(fieldList, ({ propertyName, propertyID, propertyType, displayName }, index) => {
        const isFirst = index === 0;
        const isLast = index === fieldList.length - 1;
        return (
          <div className="panel" key={propertyName}>
            <div className="common-list-item">
              <div className="list-item">
                <div className="flex justify-between items-center">
                  <div className="nowrap flex items-center justify-start">
                    {renderFieldItem({ displayName, propertyType })}
                  </div>
                  <div className="table-operations">
                    <Popconfirm
                      title={`${i18n.t('dop:confirm to remove the quote?')}`}
                      onConfirm={() => {
                        onDelete(propertyID);
                      }}
                    >
                      <span className="table-operations-btn">{i18n.t('remove')}</span>
                    </Popconfirm>
                    <span
                      className={`table-operations-btn ${isFirst ? 'disabled' : ''}`}
                      onClick={() => !isFirst && changePos(index, -1)}
                    >
                      {i18n.t('move up')}
                    </span>
                    <span
                      className={`table-operations-btn ${isLast ? 'disabled' : ''}`}
                      onClick={() => !isLast && changePos(index, 1)}
                    >
                      {i18n.t('move down')}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        );
      }),
    [changePos, fieldList, onDelete],
  );

  return (
    <Modal
      title={i18n.t('edit') + FIELD_TYPE_ICON_MAP[issueType]?.name + i18n.t('field')}
      visible={visible}
      onOk={onCloseModal}
      width="660px"
      onCancel={onCloseModal}
      destroyOnClose
      maskClosable={false}
      footer={[
        <Button type="primary" key="back" onClick={onCloseModal}>
          {i18n.t('close')}
        </Button>,
      ]}
    >
      <div className="issue-field-layout">
        <div className="default-field-panel">
          <div className="name">{i18n.t('dop:default field')}</div>
          <div className="field-grid mb-4 pl-2">{renderDefaultContent}</div>
        </div>
        <div className="custom-field-panel">
          <div className="name">{i18n.t('dop:custom fields')}</div>
          <div className="custom-field-list">{renderCustomFields()}</div>
          <div className="create-field-form mt-3">
            <div className="flex justify-between items-center">
              <Select
                className="flex-1 mr-2"
                value={selectedField?.propertyID}
                placeholder={i18n.t('please choose {name}', { name: i18n.t('dop:custom fields') })}
                onChange={(e: any) => {
                  const selectedFieldItem = find(filedOptions, (t) => t.propertyID === e) as ISSUE_FIELD.IFiledItem;
                  updater.selectedField(selectedFieldItem);
                }}
              >
                {map(filedOptions, ({ propertyID, propertyName }) => {
                  return (
                    <Option value={propertyID} key={propertyID}>
                      {propertyName}
                    </Option>
                  );
                })}
              </Select>
              <div>
                <Button type="primary" disabled={isEmpty(selectedField)} className="mr-2" onClick={onAddField}>
                  {i18n.t('dop:reference')}
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
};
