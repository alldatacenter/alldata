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
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';
import { SPACE_SM } from 'styles/StyleConstants';
import { CloneValueDeep } from 'utils/object';
import { uuidv4 } from 'utils/utils';
import { ItemLayoutProps } from '../../types';
import { itemLayoutComparer } from '../../utils';
import AddModal from './add';
import { ConditionalStyleFormValues } from './types';

const ConditionalStyle: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({
    ancestors,
    translate: t = title => title,
    data,
    onChange,
    dataConfigs,
    context,
  }) => {
    const [myData] = useState(() => CloneValueDeep(data));
    const [visible, setVisible] = useState<boolean>(false);
    const [dataSource, setDataSource] = useState<ConditionalStyleFormValues[]>(
      myData.value || [],
    );

    const [currentItem, setCurrentItem] = useState<ConditionalStyleFormValues>(
      {} as ConditionalStyleFormValues,
    );
    const onEditItem = (values: ConditionalStyleFormValues) => {
      setCurrentItem(CloneValueDeep(values));
      openConditionalStyle();
    };
    const onRemoveItem = (values: ConditionalStyleFormValues) => {
      const result: ConditionalStyleFormValues[] = dataSource.filter(
        item => item.uid !== values.uid,
      );

      setDataSource(result);
      onChange?.(ancestors, {
        ...myData,
        value: result,
      });
    };

    const tableColumnsSettings: ColumnsType<ConditionalStyleFormValues> = [
      {
        title: t('conditionalStyleTable.header.range.title'),
        dataIndex: 'range',
        width: 100,
        render: (_, { range }) => (
          <Tag>{t(`conditionalStyleTable.header.range.${range}`)}</Tag>
        ),
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
            <Tag color={color.background}>
              {t('conditionalStyleTable.header.color.background')}
            </Tag>
            <Tag color={color.textColor}>
              {t('conditionalStyleTable.header.color.text')}
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
      setCurrentItem({} as ConditionalStyleFormValues);
    };
    const submitConditionalStyleModal = (
      values: ConditionalStyleFormValues,
    ) => {
      let result: ConditionalStyleFormValues[] = [];

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
      <StyledConditionalStylePanel direction="vertical">
        <Button type="primary" onClick={openConditionalStyle}>
          {t('conditionalStyleTable.btn.add')}
        </Button>
        <Row gutter={24}>
          <Col span={24}>
            <Table<ConditionalStyleFormValues>
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
          onOk={submitConditionalStyleModal}
          onCancel={closeConditionalStyleModal}
        />
      </StyledConditionalStylePanel>
    );
  },
  itemLayoutComparer,
);

const StyledConditionalStylePanel = styled(Space)`
  width: 100%;
  margin-top: ${SPACE_SM};
`;

export default ConditionalStyle;
