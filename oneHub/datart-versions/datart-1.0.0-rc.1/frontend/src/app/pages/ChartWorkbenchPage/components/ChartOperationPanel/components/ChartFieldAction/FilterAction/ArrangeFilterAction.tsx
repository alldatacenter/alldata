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
 * distributed under the License is distributed on an "AS I\S" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import { ChartDataConfig } from 'app/types/ChartConfig';
import { FC, memo, useState } from 'react';
import RelationTypeFilter from './RelationTypeFilter';

const ArrangeFilterAction: FC<{
  config: ChartDataConfig;
  onConfigChange: (config: ChartDataConfig) => void;
}> = memo(({ config, onConfigChange }) => {
  const [fields] = useState(config?.rows?.filter(c => Boolean(c.filter)) || []);
  const [relation, setRelation] = useState<ChartFilterCondition>(() => {
    if (config?.fieldRelation) {
      const condition = config.fieldRelation;
      return new ConditionBuilder(condition).asRelation(
        condition.value,
        condition.children as ChartFilterCondition[],
      );
    }
    return new ConditionBuilder().asRelation(null, []);
  });

  const handleRelationChange = relation => {
    const relationCondition = new ConditionBuilder(relation).asRelation(
      relation.value,
      relation.children,
    );
    setRelation(relationCondition);
    config.fieldRelation = relationCondition;
    onConfigChange(config);
  };

  return (
    <RelationTypeFilter
      condition={relation}
      fields={fields}
      onRelationChange={handleRelationChange}
    />
  );
});

export default ArrangeFilterAction;
