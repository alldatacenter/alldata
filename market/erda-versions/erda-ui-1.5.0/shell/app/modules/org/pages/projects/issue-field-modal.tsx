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
import { RadioChangeEvent } from 'core/common/interface';
import { FormModal } from 'common';
import { useUpdate } from 'common/use-hooks';
import { insertWhen } from 'common/utils';
import { getFieldTypeOption } from 'org/common/issue-field-icon';
import issueFieldStore from 'org/stores/issue-field';
import orgStore from 'app/org-home/stores/org';
import { FIELD_WITH_OPTION, TASK_SP_FIELD, BUG_SP_FIELD } from 'org/common/config';
import FieldOptionsSetting from 'org/common/field-options-setting';
import { isEmpty } from 'lodash';
import { message } from 'antd';

interface IProps {
  visible: boolean;
  formData: ISSUE_FIELD.IFiledItem;
  closeModal: () => void;
  onOk: () => void;
}

const specialFieldNameList = [TASK_SP_FIELD, BUG_SP_FIELD].map((item) => item.propertyName);
interface IFieldForm {
  displayName: string;
  propertyName: string;
  required: boolean;
  propertyType: ISSUE_FIELD.IPropertyType;
  enumeratedValues: ISSUE_FIELD.IEnumData;
}
export const IssueFieldModal = ({ visible, closeModal, formData, onOk }: IProps) => {
  const { addFieldItem, updateFieldItem, updateSpecialFieldOptions } = issueFieldStore.effects;
  const { id: orgID } = orgStore.useStore((s) => s.currentOrg);

  const [{ selectedRequired, optionListVisible }, updater, update] = useUpdate({
    selectedRequired: 'false',
    optionListVisible: false,
  });

  React.useEffect(() => {
    const { propertyType } = formData;
    if (FIELD_WITH_OPTION[propertyType]) {
      update({
        optionListVisible: true,
      });
    }
  }, [formData, update, updater, visible]);

  const handleSubmit = async (values: IFieldForm) => {
    const { enumeratedValues, propertyName } = values;
    if (specialFieldNameList.includes(propertyName) && isEmpty(formData)) {
      message.warning(i18n.t('the same {key} exists', { key: i18n.t('dop:field name') }));
      return;
    }
    const enumeratedList = isEmpty(enumeratedValues) ? [] : enumeratedValues.slice(0, enumeratedValues.length - 1);

    if (formData.isSpecialField) {
      await updateSpecialFieldOptions({
        orgID,
        issueType: formData.propertyIssueType,
        list: enumeratedList,
      });
    } else {
      const params = {
        ...values,
        orgID,
        scopeID: orgID,
        scopeType: 'org',
        propertyIssueType: 'COMMON' as ISSUE_FIELD.IIssueType,
        relation: 0,
        required: selectedRequired === 'true',
        enumeratedValues: enumeratedList,
      };

      if (isEmpty(formData)) {
        await addFieldItem(params);
      } else {
        await updateFieldItem(params);
      }
    }
    updater.optionListVisible(false);
    onOk();
  };

  const fieldList = () => [
    {
      name: 'propertyID',
      required: false,
      itemProps: {
        type: 'hidden',
      },
    },
    {
      label: i18n.t('dop:field name'),
      name: 'propertyName',
      itemProps: {
        disabled: formData?.isSpecialField,
        placeholder: i18n.t('please enter {name}', { name: i18n.t('dop:field name') }),
      },
    },
    {
      label: i18n.t('is it required'),
      name: 'required',
      type: 'radioGroup',
      initialValue: 'false',
      options: [
        { name: i18n.t('common:yes'), value: 'true' },
        { name: i18n.t('common:no'), value: 'false' },
      ],
      itemProps: {
        disabled: formData?.isSpecialField,
        onChange: (e: RadioChangeEvent) => {
          updater.selectedRequired(e.target.value);
        },
      },
    },
    {
      label: i18n.t('type'),
      name: 'propertyType',
      type: 'select',
      options: getFieldTypeOption,
      itemProps: {
        disabled: formData?.isSpecialField,
        placeholder: i18n.t('please select {name}', { name: i18n.t('type') }),
        onChange: (e: ISSUE_FIELD.IPropertyType) => {
          if (FIELD_WITH_OPTION[e]) {
            update({
              optionListVisible: true,
            });
          } else {
            updater.optionListVisible(false);
          }
        },
      },
    },
    ...insertWhen(optionListVisible, [
      {
        label: i18n.t('dop:enumerated value'),
        name: 'enumeratedValues',
        required: true,
        type: 'custom',
        getComp: () => <FieldOptionsSetting />,
        rules: [
          {
            validator: (_rule: any, values: ISSUE_FIELD.IEnumData[], callback: Function) => {
              const list = isEmpty(values) ? [] : values.slice(0, values.length - 1);
              const sameNameDic = {};
              let isSameName = false;

              const valid = list.every(({ name }) => {
                if (!sameNameDic[name]) {
                  sameNameDic[name] = true;
                } else {
                  isSameName = true;
                }

                return name;
              });

              if (!list.length || !valid) {
                return callback(i18n.t('dop:enumerated value can not be empty'));
              }
              if (isSameName) {
                return callback(i18n.t('dop:enumerated value can not be the same'));
              }
              callback();
            },
          },
        ],
      },
    ]),
  ];

  const onCloseModal = () => {
    updater.optionListVisible(false);
    closeModal();
  };

  return (
    <FormModal
      name={i18n.t('dop:issue field')}
      fieldsList={fieldList}
      visible={visible}
      onOk={handleSubmit}
      formData={formData}
      onCancel={onCloseModal}
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
      }}
    />
  );
};
