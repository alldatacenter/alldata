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

import { Button, Input, Radio, Select, Table } from 'antd';
import { ColumnsType } from 'antd/lib/table';
import { handleDateLevelsName } from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/utils';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { updateBy } from 'app/utils/mutation';
import { FC } from 'react';
import styled from 'styled-components/macro';
import { uuidv4 } from 'utils/utils';
import { InteractionRelationType } from '../../constants';
import { CustomizeRelation, I18nTranslator } from './types';

const UrlParamList: FC<
  {
    targetRelId?: string;
    relations?: CustomizeRelation[];
    sourceFields?: ChartDataViewMeta[];
    sourceVariables?: Array<{ id: string; name: string }>;
    onRelationChange: (relations?: CustomizeRelation[]) => void;
  } & I18nTranslator
> = ({
  targetRelId,
  relations,
  sourceFields,
  sourceVariables,
  onRelationChange,
  translate: t,
}) => {
  const handleAddRelation = () => {
    onRelationChange(
      relations?.concat({ id: uuidv4(), type: InteractionRelationType.Field }),
    );
  };

  const handleDeleteRelation = id => {
    if (id) {
      const newRelations = updateBy(relations, draft => {
        const index = draft!.findIndex(v => v.id === id);
        if (index > -1) {
          draft?.splice(index, 1);
        }
      });
      onRelationChange(newRelations);
    }
  };

  const handleRelationChange = (id, key, value) => {
    if (id) {
      const newRelations = updateBy(relations, draft => {
        const config = draft!.find(v => v.id === id);
        config && (config[key] = value);
      });
      onRelationChange(newRelations);
    }
  };

  const handleRelationTypeChange = (id, value) => {
    if (id) {
      const newRelations = updateBy(relations, draft => {
        const index = draft!.findIndex(v => v.id === id);
        if (index > -1) {
          draft![index] = {
            id: uuidv4(),
            type: value,
          };
        }
      });
      onRelationChange(newRelations);
    }
  };

  const isFieldType = (relation: CustomizeRelation) => {
    return relation?.type === InteractionRelationType.Field;
  };

  const columns: ColumnsType<CustomizeRelation> = [
    {
      title: t('drillThrough.rule.relation.type'),
      dataIndex: 'type',
      key: 'type',
      render: (value, record) => (
        <Radio.Group
          size="small"
          style={{ width: '100px' }}
          value={value}
          onChange={e => handleRelationTypeChange(record.id, e.target.value)}
        >
          <Radio value={InteractionRelationType.Field}>
            {t('drillThrough.rule.relation.field')}
          </Radio>
          <Radio value={InteractionRelationType.Variable}>
            {t('drillThrough.rule.relation.variable')}
          </Radio>
        </Radio.Group>
      ),
    },
    {
      title: t('drillThrough.rule.relation.source'),
      dataIndex: 'source',
      key: 'source',
      render: (value, record) => (
        <Select
          style={{ width: '150px' }}
          value={value}
          onChange={value => handleRelationChange(record.id, 'source', value)}
          dropdownMatchSelectWidth={false}
        >
          {(isFieldType(record) ? sourceFields : sourceVariables)?.map(sf => {
            return (
              <Select.Option value={sf?.name}>
                {handleDateLevelsName(sf)}
              </Select.Option>
            );
          })}
        </Select>
      ),
    },
    {
      title: t('drillThrough.rule.relation.target'),
      dataIndex: 'target',
      key: 'target',
      render: (value, record) => (
        <Input
          value={value}
          onChange={e =>
            handleRelationChange(record.id, 'target', e.target.value)
          }
        />
      ),
    },
    {
      key: 'operation',
      width: 50,
      render: (_, record) => (
        <Button type="link" onClick={() => handleDeleteRelation(record.id)}>
          {t('drillThrough.rule.operation.delete')}
        </Button>
      ),
    },
  ];

  return (
    <StyledRelationList>
      <Button type="link" onClick={handleAddRelation}>
        {t('drillThrough.rule.relation.addRelation')}
      </Button>
      <Table
        size="small"
        style={{ overflow: 'auto' }}
        rowKey={'id'}
        columns={columns}
        dataSource={relations}
        pagination={{ hideOnSinglePage: true, pageSize: 3 }}
      />
    </StyledRelationList>
  );
};

export default UrlParamList;

const StyledRelationList = styled.div`
  background: ${p => p.theme.emphasisBackground};
`;
