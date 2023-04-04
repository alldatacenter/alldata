/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Button, Row, Select, Space, Tabs, Transfer, Tree } from 'antd';
import { FilterConditionType } from 'app/constants';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import useMount from 'app/hooks/useMount';
import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import { RelationFilterValue } from 'app/types/ChartConfig';
import ChartDataView from 'app/types/ChartDataView';
import { getDistinctFields } from 'app/utils/fetch';
import { FilterSqlOperator } from 'globalConstants';
import {
  forwardRef,
  ForwardRefRenderFunction,
  useCallback,
  useImperativeHandle,
  useState,
} from 'react';
import styled from 'styled-components/macro';
import { SPACE_TIMES, SPACE_XS } from 'styles/StyleConstants';
import {
  isEmpty,
  isEmptyArray,
  isEmptyString,
  IsKeyIn,
  isTreeModel,
} from 'utils/object';
import { FilterOptionForwardRef } from '.';
import CategoryConditionEditableTable from './CategoryConditionEditableTable';
import CategoryConditionRelationSelector from './CategoryConditionRelationSelector';

const CategoryConditionConfiguration: ForwardRefRenderFunction<
  FilterOptionForwardRef,
  {
    colName: string;
    dataView?: ChartDataView;
    condition?: ChartFilterCondition;
    onChange: (condition: ChartFilterCondition) => void;
    fetchDataByField?: (fieldId) => Promise<string[]>;
  } & I18NComponentProps
> = (
  {
    colName,
    i18nPrefix,
    condition,
    dataView,
    onChange: onConditionChange,
    fetchDataByField,
  },
  ref,
) => {
  const t = useI18NPrefix(i18nPrefix);
  const [curTab, setCurTab] = useState<FilterConditionType>(() => {
    if (
      [
        FilterConditionType.List,
        FilterConditionType.Condition,
        FilterConditionType.Customize,
      ].includes(condition?.type!)
    ) {
      return condition?.type!;
    }
    return FilterConditionType.List;
  });
  const [targetKeys, setTargetKeys] = useState<string[]>(() => {
    let values;
    if (condition?.operator === FilterSqlOperator.In) {
      values = condition?.value;
      if (Array.isArray(condition?.value)) {
        const firstValues =
          (condition?.value as [])?.filter(n => {
            if (IsKeyIn(n as RelationFilterValue, 'key')) {
              return (n as RelationFilterValue).isSelected;
            }
            return false;
          }) || [];
        values = firstValues?.map((n: RelationFilterValue) => n.key);
      }
    }
    return values || [];
  });
  const [selectedKeys, setSelectedKeys] = useState<string[]>([]);
  const [isTree] = useState(isTreeModel(condition?.value));
  const [treeOptions, setTreeOptions] = useState<string[]>([]);
  const [listDatas, setListDatas] = useState<RelationFilterValue[]>([]);
  const [treeDatas, setTreeDatas] = useState<RelationFilterValue[]>([]);

  useImperativeHandle(ref, () => ({
    onValidate: (args: ChartFilterCondition) => {
      if (isEmpty(args?.operator)) {
        return false;
      }
      if (args?.operator === FilterSqlOperator.In) {
        return !isEmptyArray(args?.value);
      } else if (
        [
          FilterSqlOperator.Contain,
          FilterSqlOperator.PrefixContain,
          FilterSqlOperator.SuffixContain,
          FilterSqlOperator.Equal,
          FilterSqlOperator.NotContain,
          FilterSqlOperator.NotPrefixContain,
          FilterSqlOperator.NotSuffixContain,
          FilterSqlOperator.NotEqual,
        ].includes(args?.operator as FilterSqlOperator)
      ) {
        return !isEmptyString(args?.value);
      } else if (
        [FilterSqlOperator.Null, FilterSqlOperator.NotNull].includes(
          args?.operator as FilterSqlOperator,
        )
      ) {
        return true;
      }
      return false;
    },
  }));

  useMount(() => {
    if (curTab === FilterConditionType.List) {
      handleFetchData();
    }
  });

  const getDataOptionFields = () => {
    return dataView?.meta || [];
  };

  const isChecked = (selectedKeys, eventKey) =>
    selectedKeys.indexOf(eventKey) !== -1;

  const fetchNewDataset = async (viewId, colName: string, dataView) => {
    const fieldDataset = await getDistinctFields(
      viewId,
      [colName],
      dataView,
      undefined,
    );

    return fieldDataset;
  };

  const setListSelectedState = (
    list?: RelationFilterValue[],
    keys?: string[],
  ) => {
    return (list || []).map(c =>
      Object.assign(c, { isSelected: isChecked(keys, c.key) }),
    );
  };

  const setTreeCheckableState = (
    treeList?: RelationFilterValue[],
    keys?: string[],
  ) => {
    return (treeList || []).map(c => {
      c.isSelected = isChecked(keys, c.key);
      c.children = setTreeCheckableState(c.children, keys);
      return c;
    });
  };

  const handleGeneralListChange = async selectedKeys => {
    const items = setListSelectedState(listDatas, selectedKeys);
    setTargetKeys(selectedKeys);
    setListDatas(items);

    const generalTypeItems = items?.filter(i => i.isSelected);
    const filter = new ConditionBuilder(condition)
      .setOperator(FilterSqlOperator.In)
      .setValue(generalTypeItems)
      .asGeneral();
    onConditionChange(filter);
  };

  const filterGeneralListOptions = useCallback(
    (inputValue, option) => option.label?.includes(inputValue) || false,
    [],
  );

  const handleGeneralTreeChange = async treeSelectedKeys => {
    const selectedKeys = treeSelectedKeys.checked;
    const treeItems = setTreeCheckableState(treeDatas, selectedKeys);
    setTargetKeys(selectedKeys);
    setTreeDatas(treeItems);
    const filter = new ConditionBuilder(condition)
      .setOperator(FilterSqlOperator.In)
      .setValue(treeItems)
      .asTree();
    onConditionChange(filter);
  };

  const onSelectChange = (
    sourceSelectedKeys: string[],
    targetSelectedKeys: string[],
  ) => {
    const newSelectedKeys = [...sourceSelectedKeys, ...targetSelectedKeys];
    setSelectedKeys(newSelectedKeys);
  };

  const handleTreeOptionChange = (
    associateField: string,
    labelField: string,
  ) => {
    setTreeOptions([associateField, labelField]);
  };

  const handleFetchData = () => {
    fetchNewDataset?.(dataView?.id, colName, dataView).then(dataset => {
      if (isTree) {
        // setTreeDatas(convertToTree(dataset?.columns, selectedKeys));
        // setListDatas(convertToList(dataset?.columns, selectedKeys));
      } else {
        setListDatas(convertToList(dataset?.rows, selectedKeys));
      }
    });
  };

  const convertToList = (collection, selectedKeys) => {
    const items: string[] = (collection || []).flatMap(c => c);
    const uniqueKeys = Array.from(new Set(items));
    return uniqueKeys.map(item => ({
      key: item,
      label: item,
      isSelected: selectedKeys.includes(item),
    }));
  };

  const handleTabChange = (activeKey: string) => {
    const conditionType = +activeKey;
    setCurTab(conditionType);
    const filter = new ConditionBuilder(condition)
      .setOperator(null!)
      .setValue(null)
      .asFilter(conditionType);
    setTreeDatas([]);
    setTargetKeys([]);
    setListDatas([]);
    onConditionChange(filter);
  };

  return (
    <StyledTabs activeKey={curTab.toString()} onChange={handleTabChange}>
      <Tabs.TabPane
        tab={t('general')}
        key={FilterConditionType.List.toString()}
      >
        <Row>
          <Space>
            <Button type="primary" onClick={handleFetchData}>
              {t('load')}
            </Button>
            {/* <Checkbox
                checked={isTree}
                disabled
                onChange={e => setIsTree(e.target.checked)}
              >
                {t('useTree')}
              </Checkbox> */}
          </Space>
        </Row>
        <Row>
          <Space>
            {isTree && (
              <>
                {t('associateField')}
                <Select
                  value={treeOptions?.[0]}
                  options={getDataOptionFields()?.map(f => ({
                    label: f.name,
                    value: f.name,
                  }))}
                  onChange={value =>
                    handleTreeOptionChange(value, treeOptions?.[1])
                  }
                />
                {t('labelField')}
                <Select
                  value={treeOptions?.[1]}
                  options={getDataOptionFields()?.map(f => ({
                    label: f.name,
                    value: f.name,
                  }))}
                  onChange={value =>
                    handleTreeOptionChange(treeOptions?.[0], value)
                  }
                />
              </>
            )}
          </Space>
        </Row>
        {isTree && (
          <Tree
            blockNode
            checkable
            checkStrictly
            defaultExpandAll
            checkedKeys={targetKeys}
            treeData={treeDatas}
            onCheck={handleGeneralTreeChange}
            onSelect={handleGeneralTreeChange}
          />
        )}
        {!isTree && (
          <Transfer
            operations={[t('moveToRight'), t('moveToLeft')]}
            dataSource={listDatas}
            titles={[`${t('sourceList')}`, `${t('targetList')}`]}
            targetKeys={targetKeys}
            selectedKeys={selectedKeys}
            onChange={handleGeneralListChange}
            onSelectChange={onSelectChange}
            render={item => item.label}
            filterOption={filterGeneralListOptions}
            showSearch
            pagination
          />
        )}
      </Tabs.TabPane>
      <Tabs.TabPane
        tab={t('customize')}
        key={FilterConditionType.Customize.toString()}
      >
        <CategoryConditionEditableTable
          dataView={dataView}
          i18nPrefix={i18nPrefix}
          condition={condition}
          onConditionChange={onConditionChange}
          fetchDataByField={fetchDataByField}
        />
      </Tabs.TabPane>
      <Tabs.TabPane
        tab={t('condition')}
        key={FilterConditionType.Condition.toString()}
      >
        <CategoryConditionRelationSelector
          condition={condition}
          onConditionChange={onConditionChange}
        />
      </Tabs.TabPane>
    </StyledTabs>
  );
};

export default forwardRef(CategoryConditionConfiguration);

const StyledTabs = styled(Tabs)`
  & .ant-tabs-content-holder {
    max-height: 600px;
    margin-top: 10px;
    overflow-y: auto;
  }

  & .ant-form-item-explain {
    align-self: end;
  }

  .ant-transfer {
    margin: ${SPACE_XS} 0;

    /* 
     * will be solved by upgrading antd to version a 4.17.x+
     * https://github.com/ant-design/ant-design/pull/31809 
     */
    .ant-transfer-list {
      width: ${SPACE_TIMES(56)};
      height: ${SPACE_TIMES(80)};

      .ant-pagination input {
        width: 48px;
      }
    }
  }

  .ant-select {
    width: 200px;
  }
`;
