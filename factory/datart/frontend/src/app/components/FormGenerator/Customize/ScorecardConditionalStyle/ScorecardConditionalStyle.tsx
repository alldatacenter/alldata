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

import { Button, Col, Popconfirm, Row, Space, Table, Tag } from 'antd';
import { ColumnsType } from 'antd/lib/table';
import {
  ChartStyleConfig,
  ChartStyleSelectorItem,
} from 'app/types/ChartConfig';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';
import { AssignDeep, CloneValueDeep } from 'utils/object';
import { uuidv4 } from 'utils/utils';
import { ItemLayoutProps } from '../../types';
import { itemLayoutComparer } from '../../utils';
import AddModal from './add';
import { ScorecardConditionalStyleFormValues } from './types';

const ScorecardConditionalStyle: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({
    ancestors,
    translate: t = title => title,
    data,
    onChange,
    dataConfigs,
    context,
  }) => {
    const [myData] = useState(() => CloneValueDeep(data));
    const [allItems] = useState(() => {
      let results: ChartStyleSelectorItem[] = [];
      try {
        results =
          typeof myData?.options?.getItems === 'function'
            ? myData?.options?.getItems.call(
                null,
                dataConfigs?.map(col => AssignDeep(col)),
              ) || []
            : [];
      } catch (error) {
        console.error(`ListTemplatePanel | invoke action error ---> `, error);
      }
      return results;
    });

    const [visible, setVisible] = useState<boolean>(false);
    const [dataSource, setDataSource] = useState<
      ScorecardConditionalStyleFormValues[]
    >(
      myData?.value?.filter(item =>
        allItems.find(ac => ac.key === item.metricKey),
      ) || [],
    );

    const [currentItem, setCurrentItem] =
      useState<ScorecardConditionalStyleFormValues>(
        {} as ScorecardConditionalStyleFormValues,
      );
    const onEditItem = (values: ScorecardConditionalStyleFormValues) => {
      setCurrentItem(CloneValueDeep(values));
      openConditionalStyle();
    };
    const onRemoveItem = (values: ScorecardConditionalStyleFormValues) => {
      const result: ScorecardConditionalStyleFormValues[] = dataSource.filter(
        item => item.uid !== values.uid,
      );

      setDataSource(result);
      onChange?.(ancestors, {
        ...myData,
        value: result,
      });
    };

    const tableColumnsSettings: ColumnsType<ScorecardConditionalStyleFormValues> =
      [
        {
          title: t('viz.palette.data.metrics', true),
          dataIndex: 'metricKey',
          render: key => {
            return allItems.find(v => v.key === key)?.label;
          },
        },
        {
          title: t('conditionalStyleTable.header.operator'),
          dataIndex: 'operator',
        },
        {
          title: t('conditionalStyleTable.header.value'),
          dataIndex: 'value',
          render: (_, { value }) => <>{JSON.stringify(value)}</>,
        },
        {
          title: t('conditionalStyleTable.header.color.title'),
          dataIndex: 'value',
          render: (_, { color }) => (
            <>
              <Tag color={color.textColor}>
                {t('conditionalStyleTable.header.color.text')}
              </Tag>
              <Tag color={color.background}>
                {t('conditionalStyleTable.header.color.background')}
              </Tag>
            </>
          ),
        },
        {
          title: t('conditionalStyleTable.header.action'),
          dataIndex: 'action',
          width: 140,
          render: (_, record) => {
            return [
              <Button type="link" key="edit" onClick={() => onEditItem(record)}>
                {t('conditionalStyleTable.btn.edit')}
              </Button>,
              <Popconfirm
                key="remove"
                placement="topRight"
                title={t('conditionalStyleTable.btn.confirm')}
                onConfirm={() => onRemoveItem(record)}
              >
                <Button type="link" danger>
                  {t('conditionalStyleTable.btn.remove')}
                </Button>
              </Popconfirm>,
            ];
          },
        },
      ];

    const openConditionalStyle = () => {
      setVisible(true);
    };
    const closeConditionalStyleModal = () => {
      setVisible(false);
      setCurrentItem({} as ScorecardConditionalStyleFormValues);
    };
    const submitConditionalStyleModal = (
      values: ScorecardConditionalStyleFormValues,
    ) => {
      let result: ScorecardConditionalStyleFormValues[] = [];

      if (values.uid) {
        result = dataSource.map(item => {
          if (item.uid === values.uid) {
            return values;
          }
          return item;
        });
      } else {
        result = [...dataSource, { ...values, uid: uuidv4() }];
      }

      setDataSource(result);
      closeConditionalStyleModal();
      onChange?.(ancestors, {
        ...myData,
        value: result,
      });
    };

    return (
      <StyledScorecardConditionalStylePanel direction="vertical">
        <Button type="primary" onClick={openConditionalStyle}>
          {t('conditionalStyleTable.btn.add')}
        </Button>
        <Row gutter={24}>
          <Col span={24}>
            <Table<ScorecardConditionalStyleFormValues>
              bordered={true}
              size="small"
              pagination={false}
              rowKey={record => record.uid!}
              columns={tableColumnsSettings}
              dataSource={dataSource}
            />
          </Col>
        </Row>
        <AddModal
          context={context}
          visible={visible}
          translate={t}
          values={currentItem}
          allItems={allItems}
          onOk={submitConditionalStyleModal}
          onCancel={closeConditionalStyleModal}
        />
      </StyledScorecardConditionalStylePanel>
    );
  },
  itemLayoutComparer,
);

const StyledScorecardConditionalStylePanel = styled(Space)`
  width: 100%;
  margin-top: 10px;
  overflow: hidden;
`;

export default ScorecardConditionalStyle;
