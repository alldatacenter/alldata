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
import { MarkdownEditor, Icon as CustomIcon, FormBuilder, IFormExtendType } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Input, Menu, Dropdown } from 'antd';
import i18n from 'i18n';
import { set, keys, map, get, filter, every, forEach } from 'lodash';
import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { DEFAULT_TAG, INPUT_MAX_LENGTH, TEXTAREA_MAX_LENGTH } from 'app/modules/apiManagePlatform/configs.ts';
import { produce } from 'immer';

const { Fields } = FormBuilder;

interface IProps {
  formData: Obj;
  isEditMode?: boolean;
  onChange: (key: string, formData: Obj) => void;
}

interface ITagSelect {
  options: Obj[];
  value: string;
  disabled: boolean;
  onChange: (e: string) => void;
  onDelete: (e: string) => void;
  onSubmit: () => void;
  onInput: (e: string) => void;
}

const TagSelect = React.forwardRef((tagProps: ITagSelect) => {
  const { options, onChange, onDelete, value, onInput, onSubmit, disabled } = tagProps;

  const content = (
    <Menu className="api-resource-tag-select">
      {map(options, ({ name }) => {
        return (
          <Menu.Item key={name} onClick={(e) => onChange(e.key)}>
            <div className="flex justify-between items-center tag-option">
              <span>{name}</span>
              {name !== 'other' && (
                <CustomIcon
                  type="shanchu"
                  className="tag-option-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete(name);
                  }}
                />
              )}
            </div>
          </Menu.Item>
        );
      })}
    </Menu>
  );

  return (
    <div className="api-tag-select">
      {disabled ? (
        <Input
          placeholder={i18n.t('dop:please select or enter the tag')}
          value={value}
          maxLength={INPUT_MAX_LENGTH}
          disabled={disabled}
        />
      ) : (
        <Dropdown overlay={content} trigger={['click']}>
          <Input
            maxLength={INPUT_MAX_LENGTH}
            placeholder={i18n.t('dop:please select or enter the tag')}
            value={value}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
              onInput(e.target.value);
            }}
            onBlur={onSubmit}
          />
        </Dropdown>
      )}
    </div>
  );
});

const ResourceSummary = React.memo((props: IProps) => {
  const [{ tagList, operationIdList }, updater] = useUpdate({
    tagList: [],
    operationIdList: [],
  });
  const { updateOpenApiDoc } = apiDesignStore;
  const [openApiDoc] = apiDesignStore.useStore((s) => [s.openApiDoc]);

  const { formData, onChange, isEditMode = true } = props;
  const formRef = React.useRef<IFormExtendType>(null);

  React.useEffect(() => {
    setTimeout(() => {
      formRef.current!.setFieldsValue({
        operationId: formData?.operationId,
        description: formData?.description || '',
        tags: get(formData, 'tags.0'),
      });
    });
  }, [formData]);

  const totalOperationIdList = React.useMemo(() => {
    const _operationIdList: string[] = [];
    const { paths } = openApiDoc;
    forEach(keys(paths), (pathName: string) => {
      const methodData = paths[pathName];
      forEach(keys(methodData), (item) => {
        methodData[item]?.operationId && _operationIdList.push(methodData[item]?.operationId);
      });
    });
    return _operationIdList;
  }, [openApiDoc]);

  React.useEffect(() => {
    const _operationIdList: string[] = totalOperationIdList;
    const tags = openApiDoc.tags || [];
    updater.tagList([...tags]);
    updater.operationIdList(_operationIdList);
  }, [openApiDoc, totalOperationIdList, updater]);

  const onCreateTag = React.useCallback(() => {
    const { tags } = formRef.current!.getFieldsValue();
    const newTagName = Array.isArray(tags) ? tags[0] : tags;
    const isNew = tagList?.length ? every(tagList, (item) => item.name !== newTagName) : true;
    if (isNew && newTagName && newTagName !== 'other') {
      const newTags = [...tagList, { name: newTagName }];
      onChange('summary', { propertyName: 'tags', propertyData: [newTagName], newTags });
    }
  }, [onChange, tagList]);

  const onDeleteTag = React.useCallback(
    (tagName: string) => {
      const newList = filter(tagList, (item) => item?.name !== tagName);
      const tempDocDetail = produce(openApiDoc, (draft) => {
        set(draft, 'tags', newList);
        forEach(keys(draft.paths), (apiName: string) => {
          const apiData = draft.paths[apiName];

          forEach(keys(apiData), (method) => {
            if (get(draft, ['paths', apiName, method, 'tags', '0']) === tagName) {
              draft.paths[apiName][method].tags = [DEFAULT_TAG];
            }
          });
        });
      });
      updateOpenApiDoc(tempDocDetail);
      const curTag = formRef.current!.getFieldsValue()?.tags;
      if (curTag === tagName) {
        formRef.current!.setFieldsValue({ tags: undefined });
      }
    },
    [openApiDoc, tagList, updateOpenApiDoc],
  );

  const setField = React.useCallback(
    (propertyName: string, val: string | string[]) => {
      const curFormData = formRef.current!.getFieldsValue();
      if (propertyName !== 'operationId' && !curFormData?.operationId) {
        let _operationId = 'operationId';
        const _operationIdList: string[] = totalOperationIdList;
        while (_operationIdList.includes(_operationId)) {
          _operationId += '1';
        }
        formRef.current!.setFieldsValue({ operationId: _operationId });
      }
      if (propertyName !== 'tags' && !curFormData?.tags) {
        formRef.current!.setFieldsValue({ tags: ['other'] });
      }
      onChange('summary', { propertyName, propertyData: val });
    },
    [onChange, totalOperationIdList],
  );

  const fieldList = React.useMemo(
    () => [
      {
        type: Input,
        label: i18n.t('name'),
        name: 'operationId',
        colSpan: 24,
        required: false,
        customProps: {
          maxLength: INPUT_MAX_LENGTH,
          disabled: !isEditMode,
        },
        rules: [
          {
            validator: (_rule: any, value: string, callback: (msg?: string) => void) => {
              if (operationIdList.includes(value)) {
                callback(i18n.t('the same {key} exists', { key: i18n.t('name') }));
              } else {
                setField('operationId', value);
                callback();
              }
            },
          },
        ],
      },
      {
        name: 'tags',
        label: i18n.t('dop:grouping'),
        colSpan: 24,
        type: TagSelect,
        customProps: {
          disabled: !isEditMode,
          options: tagList,
          onChange: (e: string) => {
            setField('tags', [e]);
          },
          onDelete: onDeleteTag,
          onSubmit: onCreateTag,
          onInput: (e: string) => {
            formRef.current!.setFieldsValue({ tags: e });
          },
        },
      },
      {
        type: MarkdownEditor,
        label: i18n.t('description'),
        name: 'description',
        colSpan: 24,
        required: false,
        customProps: {
          defaultMode: !isEditMode ? 'html' : 'md',
          readOnly: !isEditMode,
          maxLength: TEXTAREA_MAX_LENGTH,
          onChange: (val: string) => setField('description', val),
        },
      },
    ],
    [isEditMode, onCreateTag, onDeleteTag, operationIdList, setField, tagList],
  );

  return (
    <FormBuilder isMultiColumn ref={formRef}>
      <Fields fields={fieldList} fid="summaryFields" />
    </FormBuilder>
  );
});

export default ResourceSummary;
