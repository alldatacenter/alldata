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

import { DeleteOutlined } from '@ant-design/icons';
import { Col, Divider, List, Row, Select, Typography } from 'antd';
import {
  ChartStyleConfig,
  ChartStyleSelectorItem,
} from 'app/types/ChartConfig';
import { updateBy, updateByAction } from 'app/utils/mutation';
import { FC, memo, useRef, useState } from 'react';
import { AssignDeep, CloneValueDeep, isEmpty } from 'utils/object';
import { FormGroupLayoutMode } from '../constants';
import GroupLayout from '../Layout/GroupLayout';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';

const { Text } = Typography;

const ListTemplatePanel: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({
    ancestors,
    translate: t = title => title,
    data,
    onChange,
    dataConfigs,
  }) => {
    const myDataRef = useRef(data);
    const [rowSettings, setRowSettings] = useState(
      () => myDataRef?.current.rows || [],
    );
    const [allItems] = useState(() => {
      let results: ChartStyleSelectorItem[] = [];
      try {
        results =
          typeof myDataRef.current?.options?.getItems === 'function'
            ? myDataRef.current?.options?.getItems.call(
                null,
                dataConfigs?.map(col => AssignDeep(col)),
              ) || []
            : [];
      } catch (error) {
        console.error(`ListTemplatePanel | invoke action error ---> `, error);
      }
      return results;
    });
    const [selectedItems, setSelectedItems] = useState<any[]>(() => {
      if (rowSettings?.length! > 0) {
        const ItemsKeys = rowSettings?.map(r => r.key) || [];
        return allItems.filter(item => ItemsKeys.includes(item.key));
      }
      return [];
    });
    const [unSelectedItems, setUnSelectedItems] = useState(() => {
      const selectedItemsKeys = selectedItems.map(item => item.key);
      return allItems.filter(item => !selectedItemsKeys.includes(item.key));
    });
    const [currentSelectedItem, setCurrentSelectedItem] = useState<any>(() => {
      if (!!selectedItems.length) {
        return selectedItems[0];
      }
    });

    const handleRowSettingChange = listItem => {
      setCurrentSelectedItem(listItem);
    };

    const handleDeleteItem = item => {
      if (item) {
        setUnSelectedItems(unSelectedItems.concat([item]));
        setSelectedItems(selectedItems.filter(so => so.key !== item.key));
        setCurrentSelectedItem(null);
        const newMyData = updateBy(myDataRef.current, draft => {
          draft.rows = draft?.rows?.filter(r => r?.key !== item.key);
          return draft;
        });
        handleDataChange(newMyData);
      }
    };

    const handleColumnChange = key => {
      const item = unSelectedItems.find(so => so.key === key);
      if (item) {
        setUnSelectedItems(unSelectedItems.filter(so => so.key !== key));
        setSelectedItems(selectedItems.concat([item]));
        setCurrentSelectedItem(item);
        const newRowSettings = rowSettings?.concat([
          createNewRowByTemplate(key),
        ]);
        const newMyData = updateBy(myDataRef.current, draft => {
          draft.rows = newRowSettings;
          return draft;
        });
        handleDataChange(newMyData);
      }
    };

    const handleChildComponentUpdate = key => (ancestors, row) => {
      const index = rowSettings.findIndex(r => r.key === key);
      const newMyData = updateByAction(myDataRef.current, {
        ancestors: [index].concat(ancestors),
        value: row,
      });
      handleDataChange(newMyData);
    };

    const handleDataChange = newData => {
      myDataRef.current = newData as any;
      setRowSettings([...newData.rows]);
      onChange?.(ancestors, newData);
    };

    const createNewRowByTemplate = key => {
      const template = CloneValueDeep(myDataRef.current?.template!);
      return { ...template, key };
    };

    const renderRowSettings = r => {
      return (
        <GroupLayout
          ancestors={[]}
          key={r.key}
          mode={FormGroupLayoutMode.INNER}
          data={r}
          translate={t}
          onChange={handleChildComponentUpdate(r.key)}
          context={currentSelectedItem}
        />
      );
    };

    return (
      <>
        <Row>
          <Col span={8}>
            <Select
              style={{ width: '100%' }}
              dropdownMatchSelectWidth
              placeholder={t('select')}
              value={currentSelectedItem?.label}
              onChange={handleColumnChange}
            >
              {unSelectedItems?.map((o, index) => {
                const label = isEmpty(o['label']) ? o : o.label;
                const key = isEmpty(o['key']) ? index : o.key;
                return (
                  <Select.Option key={key} value={key}>
                    {label}
                  </Select.Option>
                );
              })}
            </Select>
          </Col>
        </Row>
        <Divider />
        <Row>
          <Col span={8}>
            <List
              bordered
              dataSource={selectedItems}
              renderItem={item => (
                <List.Item
                  key={item.label}
                  onClick={() => handleRowSettingChange(item)}
                  actions={[
                    <DeleteOutlined onClick={() => handleDeleteItem(item)} />,
                  ]}
                  style={{
                    backgroundColor:
                      item.label === currentSelectedItem?.label
                        ? '#1890ff'
                        : 'white',
                  }}
                >
                  <Text
                    // @FIX 48px icon的marginLeft 22px icon的宽度
                    style={{ width: 'calc(100% - 48px - 22px)' }}
                    ellipsis={{ tooltip: item.label }}
                  >
                    {item.label}
                  </Text>
                </List.Item>
              )}
            />
          </Col>
          <Col span={16}>
            {rowSettings
              ?.filter(r => currentSelectedItem?.key === r?.key)
              .map(renderRowSettings)}
          </Col>
        </Row>
      </>
    );
  },
  itemLayoutComparer,
);

export default ListTemplatePanel;
