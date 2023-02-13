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
import { FormBuilder, IFormExtendType } from 'common';
import { useUpdate } from 'common/use-hooks';
import { produce } from 'immer';
import { keys, set, get, unset, filter, omit, values, forEach } from 'lodash';
import { PropertyItemForm } from 'apiManagePlatform/pages/api-market/design/basic-params-config';
import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { API_FORM_KEY, QUOTE_PREFIX, QUOTE_PREFIX_NO_EXTENDED } from 'app/modules/apiManagePlatform/configs.ts';
import { useLatest } from 'react-use';

interface IProps {
  formData: Obj;
  dataType: string;
  quotePathMap: {
    [prop: string]: string[][];
  };
  dataTypeNameMap: string[];
  isEditMode: boolean;
  onQuoteNameChange: (e: any) => void;
  onDataTypeNameChange: (e: any) => void;
}

const DataTypeConfig = (props: IProps) => {
  const [{ propertyFormData }, updater] = useUpdate({
    propertyFormData: {} as Obj,
  });
  const { formData, onQuoteNameChange, quotePathMap, isEditMode, dataType, onDataTypeNameChange, dataTypeNameMap } =
    props;
  const { updateOpenApiDoc, updateFormErrorNum } = apiDesignStore;
  const [openApiDoc] = apiDesignStore.useStore((s) => [s.openApiDoc]);

  const openApiDocRef = useLatest(openApiDoc);
  const quotePathMapRef = useLatest(quotePathMap);
  const formRef = React.useRef<IFormExtendType>(null);

  React.useEffect(() => {
    const newFormData = { ...omit(formData, 'name') };
    newFormData[API_FORM_KEY] = formData[API_FORM_KEY] || formData.name;
    updater.propertyFormData(newFormData);
  }, [updater, formData]);

  const extraDataTypes = React.useMemo(() => {
    const tempTypes = get(openApiDoc, 'components.schemas') || {};
    const types = {};
    const forbiddenTypes = get(tempTypes, [dataType, 'x-dice-forbidden-types']) || [dataType];
    forEach(keys(tempTypes), (typeName) => {
      if (!forbiddenTypes?.includes(typeName)) {
        types[typeName] = tempTypes[typeName];
      }
    });
    return types;
  }, [dataType, openApiDoc]);

  const allExtraDataTypes = React.useMemo(() => {
    return get(openApiDoc, 'components.schemas') || {};
  }, [openApiDoc]);

  const getForbiddenTypes = React.useCallback((data: Obj, storage: string[]) => {
    if (data?.properties) {
      forEach(values(data.properties), (item) => {
        const refTypePath = get(item, [QUOTE_PREFIX, 0, '$ref']) || data[QUOTE_PREFIX_NO_EXTENDED];
        if (refTypePath) {
          const tempQuoteName = refTypePath.split('#/components/schemas/')[1];
          if (!storage.includes(tempQuoteName)) {
            storage.push(tempQuoteName);
          }
        }
        getForbiddenTypes(item, storage);
      });
    } else if (data?.items) {
      const itemsData = data.items;
      const refTypePath = get(itemsData, [QUOTE_PREFIX, 0, '$ref']) || itemsData[QUOTE_PREFIX_NO_EXTENDED];
      if (refTypePath) {
        const tempQuoteName = refTypePath.split('#/components/schemas/')[1];
        if (!storage.includes(tempQuoteName)) {
          storage.push(tempQuoteName);
        }
        getForbiddenTypes(itemsData, storage);
      }
    }
  }, []);

  const onFormChange = React.useCallback(
    (_formKey: string, _formData: any, extraProps?: Obj) => {
      const quotedType = extraProps?.quoteTypeName;

      const nName: string = _formData[API_FORM_KEY]; // 当前正在编辑的类型的新name
      const tempMap = {};
      const originalDataTypeMap = get(openApiDocRef.current, ['components', 'schemas']);

      const usedCustomTypes: string[] = []; // 获取所有引用当前类型的其他类型
      const refTypePath = get(_formData, [QUOTE_PREFIX, 0, '$ref']) || _formData[QUOTE_PREFIX_NO_EXTENDED];
      if (refTypePath) {
        const tempQuoteName = refTypePath.split('#/components/schemas/')[1];
        usedCustomTypes.push(tempQuoteName);
      }

      getForbiddenTypes(_formData, usedCustomTypes);

      forEach(keys(originalDataTypeMap), (typeKey: string) => {
        if (typeKey !== dataType) {
          const isSelfUsed = usedCustomTypes.includes(typeKey); // 是否引用了当前正在编辑的类型
          const _originalForbiddenTypes = filter(
            get(originalDataTypeMap, [typeKey, 'x-dice-forbidden-types']) || [],
            (item) => item !== dataType,
          );

          if (!_originalForbiddenTypes.includes(typeKey)) {
            // 每个类型禁止引用自己
            _originalForbiddenTypes.push(typeKey);
          }

          // 若当前编辑的类型引用了该类型，则禁该止引用当前正在编辑的类型
          if (isSelfUsed || quotedType === typeKey) {
            _originalForbiddenTypes.push(nName);
          }

          tempMap[typeKey] = {
            ...originalDataTypeMap[typeKey],
            'x-dice-forbidden-types': _originalForbiddenTypes,
          };
        }
      });

      let originalForbiddenTypes: string[] = _formData['x-dice-forbidden-types'] || [dataType];
      if (dataType !== nName) {
        originalForbiddenTypes = originalForbiddenTypes.filter(
          (item) => item !== dataType && originalDataTypeMap[item],
        );
        originalForbiddenTypes.push(nName);
      }
      tempMap[nName] = {
        ..._formData,
        'x-dice-forbidden-types': originalForbiddenTypes,
      };

      if (dataType !== nName) {
        onDataTypeNameChange(nName);

        const newPathMap = produce(quotePathMapRef.current, (draft) => {
          draft[nName] = filter(draft[dataType], (itemPath) => get(openApiDocRef.current, itemPath));
          unset(draft, dataType);
        });

        const newHref =
          _formData.type === 'object' ? [{ $ref: `#/components/schemas/${nName}` }] : `#/components/schemas/${nName}`;
        const quotePrefix = _formData.type === 'object' ? QUOTE_PREFIX : QUOTE_PREFIX_NO_EXTENDED;

        const _tempDocDetail = produce(openApiDocRef.current, (draft) => {
          set(draft, 'components.schemas', tempMap);
        });

        const tempDocDetail = produce(_tempDocDetail, (draft) => {
          forEach(quotePathMapRef.current[dataType], (curTypeQuotePath) => {
            if (
              get(draft, [...curTypeQuotePath, QUOTE_PREFIX]) ||
              get(draft, [...curTypeQuotePath, QUOTE_PREFIX_NO_EXTENDED])
            ) {
              unset(draft, [...curTypeQuotePath, QUOTE_PREFIX]);
              unset(draft, [...curTypeQuotePath, QUOTE_PREFIX_NO_EXTENDED]);
              set(draft, [...curTypeQuotePath, quotePrefix], newHref);
            }
          });
        });
        updateOpenApiDoc(tempDocDetail);
        onQuoteNameChange(newPathMap);
      } else {
        if (quotedType && extraProps?.typeQuotePath) {
          const prefixPath = `components.schemas.${extraProps.typeQuotePath}`;

          const isQuotedBefore =
            get(openApiDocRef.current, `${prefixPath}.${QUOTE_PREFIX}.0.$ref`) ||
            get(openApiDocRef.current, `${prefixPath}.${QUOTE_PREFIX_NO_EXTENDED}`);
          const oldQuotedType = isQuotedBefore ? isQuotedBefore.split('/').slice(-1)[0] : '';

          const newPathMap = produce(quotePathMapRef.current, (draft) => {
            draft[quotedType] = draft[quotedType] || [];
            draft[quotedType].push(prefixPath.split('.'));
            if (oldQuotedType) {
              draft[oldQuotedType] = filter(draft[oldQuotedType], (item) => item.join('.') !== prefixPath);
            }
          });
          onQuoteNameChange(newPathMap);
        }

        const tempDocDetail = produce(openApiDocRef.current, (draft) => {
          set(draft, 'components.schemas', tempMap);
        });
        updateOpenApiDoc(tempDocDetail);
      }
    },
    [
      dataType,
      getForbiddenTypes,
      onDataTypeNameChange,
      onQuoteNameChange,
      openApiDocRef,
      quotePathMapRef,
      updateOpenApiDoc,
    ],
  );

  return (
    <div className="basic-params-config">
      <FormBuilder isMultiColumn className="param-config-form" ref={formRef}>
        <PropertyItemForm
          onChange={onFormChange}
          onFormErrorNumChange={updateFormErrorNum}
          detailVisible
          formType="DataType"
          formData={propertyFormData}
          extraDataTypes={extraDataTypes}
          allExtraDataTypes={allExtraDataTypes}
          isEditMode={isEditMode}
          allDataTypes={dataTypeNameMap}
        />
      </FormBuilder>
    </div>
  );
};

export default DataTypeConfig;
