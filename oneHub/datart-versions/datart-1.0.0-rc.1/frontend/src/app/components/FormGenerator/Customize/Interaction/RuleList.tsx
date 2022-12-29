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

import { Button, Input, Select, Table } from 'antd';
import { ColumnsType } from 'antd/lib/table';
import ChartDataView from 'app/types/ChartDataView';
import { FC } from 'react';
import styled from 'styled-components/macro';
import {
  InteractionAction,
  InteractionCategory,
  InteractionMouseEvent,
} from '../../constants';
import JumpToChart from './JumpToChart';
import JumpToDashboard from './JumpToDashboard';
import JumpToUrl from './JumpToUrl';
import { I18nTranslator, InteractionRule, VizType } from './types';

const RuleList: FC<
  {
    rules?: InteractionRule[];
    vizs?: VizType[];
    dataview?: ChartDataView;
    onRuleChange: (id, prop, value) => void;
    onDeleteRule: (id) => void;
  } & I18nTranslator
> = ({ rules, vizs, dataview, onRuleChange, onDeleteRule, translate: t }) => {
  const tableColumnStyle = { width: '150px' };

  const columns: ColumnsType<InteractionRule> = [
    {
      title: t('drillThrough.rule.header.name'),
      dataIndex: 'name',
      key: 'name',
      render: (value, record) => (
        <Input
          style={tableColumnStyle}
          value={value}
          maxLength={100}
          onChange={e => onRuleChange(record.id, 'name', e.target.value)}
        />
      ),
    },
    {
      title: t('drillThrough.rule.header.category'),
      dataIndex: 'category',
      key: 'category',
      render: (value, record) => (
        <Select
          style={tableColumnStyle}
          value={value}
          onChange={value => onRuleChange(record.id, 'category', value)}
        >
          <Select.Option value={InteractionCategory.JumpToChart}>
            {t('drillThrough.rule.category.jumpToChart')}
          </Select.Option>
          <Select.Option value={InteractionCategory.JumpToDashboard}>
            {t('drillThrough.rule.category.jumpToDashboard')}
          </Select.Option>
          <Select.Option value={InteractionCategory.JumpToUrl}>
            {t('drillThrough.rule.category.jumpToUrl')}
          </Select.Option>
        </Select>
      ),
    },
    {
      title: t('drillThrough.rule.header.event'),
      dataIndex: 'event',
      key: 'event',
      render: (value, record) => (
        <Select
          style={tableColumnStyle}
          value={value}
          onChange={value => onRuleChange(record.id, 'event', value)}
        >
          <Select.Option value={InteractionMouseEvent.Left}>
            {t('drillThrough.rule.event.left')}
          </Select.Option>
          <Select.Option value={InteractionMouseEvent.Right}>
            {t('drillThrough.rule.event.right')}
          </Select.Option>
        </Select>
      ),
    },
    {
      title: t('drillThrough.rule.header.open'),
      dataIndex: 'action',
      key: 'action',
      render: (value, record) => (
        <Select
          style={tableColumnStyle}
          value={value}
          onChange={value => onRuleChange(record.id, 'action', value)}
        >
          <Select.Option value={InteractionAction.Redirect}>
            {t('drillThrough.rule.action.redirect')}
          </Select.Option>
          <Select.Option value={InteractionAction.Window}>
            {t('drillThrough.rule.action.window')}
          </Select.Option>
          <Select.Option value={InteractionAction.Dialog}>
            {t('drillThrough.rule.action.dialog')}
          </Select.Option>
        </Select>
      ),
    },
    {
      title: t('drillThrough.rule.header.relation'),
      dataIndex: 'relation',
      key: 'relation',
      render: (_, record) => {
        if (!record.category) {
          return <></>;
        }
        const props = {
          translate: t,
          vizs: vizs,
          dataview: dataview,
          value: record?.[record.category] as any,
          onValueChange: value =>
            onRuleChange(record.id, record.category, value),
        };
        switch (record.category) {
          case InteractionCategory.JumpToChart:
            return <JumpToChart {...props} />;
          case InteractionCategory.JumpToDashboard:
            return <JumpToDashboard {...props} />;
          case InteractionCategory.JumpToUrl:
            return <JumpToUrl {...props} />;
          default:
            return <></>;
        }
      },
    },
    {
      title: t('drillThrough.rule.header.operation'),
      key: 'operation',
      width: 50,
      fixed: 'right',
      render: (_, record) => (
        <Button type="link" onClick={() => onDeleteRule(record.id)}>
          {t('drillThrough.rule.operation.delete')}
        </Button>
      ),
    },
  ];

  return (
    <StyledRuleListContainer>
      <Table
        bordered
        style={{ height: 400, overflow: 'auto' }}
        rowKey="id"
        columns={columns}
        dataSource={rules}
        pagination={{ hideOnSinglePage: true, pageSize: 5 }}
      />
    </StyledRuleListContainer>
  );
};

export default RuleList;

const StyledRuleListContainer = styled.div`
  & thead .ant-table-cell {
    word-break: keep-all;
  }
`;
