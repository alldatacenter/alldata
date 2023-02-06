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
import { Table, Popconfirm, Button, Input, Tooltip, message } from 'antd';
import { SectionInfoEdit } from 'project/common/components/section-info-edit';
import i18n from 'i18n';
import issueFieldStore from 'org/stores/issue-field';
import { WithAuth } from 'app/user/common';
import { Filter } from 'common';
import { useUpdate } from 'common/use-hooks';
import { IssueFieldModal } from './issue-field-modal';
import orgStore from 'app/org-home/stores/org';
import { IssueIcon } from 'org/common/issue-field-icon';
import { isEmpty, map } from 'lodash';
import { useUnmount, useMount } from 'react-use';
import { useLoading } from 'core/stores/loading';
import { TASK_SP_FIELD, BUG_SP_FIELD } from 'org/common/config';

const IssueFieldManage = () => {
  const { id: orgID } = orgStore.useStore((s) => s.currentOrg);
  const tableData = issueFieldStore.useStore((s) => s.fieldList);
  const { getFieldsByIssue, deleteFieldItem, getSpecialFieldOptions } = issueFieldStore.effects;
  const { clearFieldList } = issueFieldStore.reducers;
  const [{ filterData, modalVisible, formData, taskSpecialField, bugSpecialField }, updater, update] = useUpdate({
    filterData: {} as Obj,
    modalVisible: false,
    formData: {} as ISSUE_FIELD.IFiledItem,
    taskSpecialField: { ...TASK_SP_FIELD },
    bugSpecialField: { ...BUG_SP_FIELD },
  });

  const getSpecialField = React.useCallback(() => {
    getSpecialFieldOptions({ orgID, issueType: 'TASK' }).then((res) => {
      updater.taskSpecialField({ ...TASK_SP_FIELD, enumeratedValues: res });
    });
    getSpecialFieldOptions({ orgID, issueType: 'BUG' }).then((res) => {
      updater.bugSpecialField({ ...BUG_SP_FIELD, enumeratedValues: res });
    });
  }, [getSpecialFieldOptions, orgID, updater]);

  useMount(() => {
    getSpecialField();
  });

  const tableList = React.useMemo(() => {
    const tempList = [taskSpecialField, bugSpecialField]?.filter(({ propertyName }) => {
      return filterData.propertyName === undefined || propertyName?.indexOf(filterData?.propertyName) !== -1;
    });
    return [...tempList, ...tableData];
  }, [bugSpecialField, filterData, tableData, taskSpecialField]);

  const [isFetching] = useLoading(issueFieldStore, ['getFieldsByIssue']);
  useUnmount(() => {
    clearFieldList();
  });

  const fieldsList: object[] = React.useMemo(
    () => [
      {
        label: i18n.t('dop:field name'),
        name: 'propertyName',
      },
    ],
    [],
  );

  const onDeleteField = React.useCallback(
    async ({ propertyID, relatedIssue }) => {
      if (!isEmpty(relatedIssue)) {
        message.warning(
          i18n.t(
            'dop:This field has been referenced. If you want to delete it, please remove the reference in the corresponding issue type first.',
          ),
        );
        return;
      }

      await deleteFieldItem({ propertyID });
      getFieldsByIssue({ ...filterData, propertyIssueType: 'COMMON', orgID });
    },
    [deleteFieldItem, filterData, getFieldsByIssue, orgID],
  );

  const columns = React.useMemo(
    () => [
      {
        key: 'propertyName',
        title: i18n.t('dop:field name'),
        width: '200',
        dataIndex: 'propertyName',
      },
      {
        key: 'required',
        title: i18n.t('is it required'),
        dataIndex: 'required',
        render: (value: boolean) => (String(value) === 'true' ? i18n.t('common:yes') : i18n.t('common:no')),
      },
      {
        key: 'propertyType',
        title: i18n.t('type'),
        width: '200',
        dataIndex: 'propertyType',
        render: (t: string) => <IssueIcon type={t} withName />,
      },
      {
        key: 'relatedIssue',
        title: i18n.t('dop:related issue type'),
        width: '250',
        dataIndex: 'relatedIssue',
        render: (types: string[]) => {
          const fullTags = () =>
            map(types, (t) => (
              <span key={t} className="tag-default">
                {t}
              </span>
            ));
          return (
            <Tooltip title={fullTags()} placement="top" overlayClassName="tags-tooltip">
              {fullTags()}
            </Tooltip>
          );
        },
      },
      {
        title: i18n.t('operation'),
        width: 100,
        dataIndex: 'operation',
        className: 'operation',
        render: (_text: any, record: ISSUE_FIELD.IFiledItem) => {
          return (
            <div className="table-operations">
              <span
                className="table-operations-btn"
                onClick={() => {
                  const { enumeratedValues = [] } = record;
                  const optionList = enumeratedValues
                    ? [...enumeratedValues, { name: '', index: enumeratedValues.length }]
                    : [{ name: '', index: 0 }];
                  updater.formData({
                    ...record,
                    required: record.required ? 'true' : 'false',
                    enumeratedValues: optionList,
                  });
                  setTimeout(() => {
                    updater.modalVisible(true);
                  });
                }}
              >
                {i18n.t('edit')}
              </span>
              <WithAuth pass={!record?.isSpecialField}>
                <Popconfirm
                  title={`${i18n.t('common:confirm to delete')}?`}
                  onConfirm={() => {
                    onDeleteField(record);
                  }}
                >
                  <span className="table-operations-btn">{i18n.t('delete')}</span>
                </Popconfirm>
              </WithAuth>
            </div>
          );
        },
      },
    ],
    [onDeleteField, updater],
  );

  const filterField = [
    {
      type: Input,
      name: 'propertyName',
      customProps: {
        placeholder: i18n.t('filter by {name}', { name: i18n.t('dop:field name') }),
      },
    },
  ];

  const onFilter = (query: Obj) => {
    updater.filterData(query);
    getFieldsByIssue({ ...query, propertyIssueType: 'COMMON', orgID });
  };

  const onClose = React.useCallback(() => {
    update({
      formData: {} as ISSUE_FIELD.IFiledItem,
      modalVisible: false,
    });
  }, [update]);

  const onOk = React.useCallback(() => {
    formData?.isSpecialField && getSpecialField();

    onClose();
    getFieldsByIssue({ ...filterData, propertyIssueType: 'COMMON', orgID });
  }, [filterData, formData, getFieldsByIssue, getSpecialField, onClose, orgID]);

  const readonlyForm = (
    <div style={{ overflow: 'hidden' }}>
      <div>
        <WithAuth pass tipProps={{ placement: 'bottom' }}>
          <Button
            type="primary"
            onClick={() => {
              updater.modalVisible(true);
            }}
          >
            {i18n.t('add')}
          </Button>
        </WithAuth>
        <Filter config={filterField} onFilter={onFilter} connectUrlSearch />
      </div>
      <Table
        loading={isFetching}
        rowKey="propertyName"
        dataSource={tableList}
        columns={columns}
        pagination={false}
        scroll={{ x: '100%' }}
      />
      <IssueFieldModal visible={modalVisible} formData={formData} onOk={onOk} closeModal={onClose} />
    </div>
  );

  return (
    <SectionInfoEdit
      hasAuth={false}
      data={formData}
      readonlyForm={readonlyForm}
      fieldsList={fieldsList}
      updateInfo={getFieldsByIssue}
      name={i18n.t('dop:issue field')}
      desc={i18n.t('dop:Custom fields common to the whole organization to meet needs of more scenarios.')}
    />
  );
};

export default IssueFieldManage;
