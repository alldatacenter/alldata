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

import { FilterConditionType, FilterRelationType } from 'app/constants';
import { FilterCondition } from 'app/types/ChartConfig';
import { FilterSqlOperator } from 'globalConstants';

class ChartFilterCondition implements FilterCondition {
  name = '';
  type = FilterConditionType.Filter;
  value?;
  visualType = '';
  operator?: any;
  children?: ChartFilterCondition[];

  constructor(condition?: FilterCondition) {
    if (condition) {
      this.name = condition.name;
      this.type = condition.type || this.type;
      this.value = condition.value;
      this.visualType = condition.visualType;
      this.operator = condition.operator;
      this.children = (condition.children || []).map(
        child => new ChartFilterCondition(child),
      );
    }
  }

  setValue(value) {
    this.value = value;
  }

  setType(type) {
    this.type = type;
  }

  setOperator(operator) {
    this.operator = operator;
  }

  removeChild(index) {
    if (this.children && this.children.length > 2) {
      this.children?.splice(index, 1);
    } else if (this.children && this.children.length === 2) {
      this.children.splice(index, 1);
      const child = this.children[0];
      this.type = FilterConditionType.Filter;
      this.operator = child.operator;
      this.value = child.value;
      this.children = [];
    }
  }

  updateChild(index, child) {
    if (this.children && this.children.length >= index + 1) {
      this.children[index] = child;
    }
  }

  appendChild(index?: number) {
    if (this.type === FilterConditionType.Relation) {
      if (index === null || index === undefined) {
        this.children = (this.children || []).concat(
          new ConditionBuilder(this).asFilter(),
        );
      } else if (index >= 0) {
        this.children?.splice(index, 0, new ConditionBuilder(this).asFilter());
      }
    } else {
      this.children = [
        new ConditionBuilder(this).asFilter(),
        new ConditionBuilder(this).asFilter(),
      ];
      this.type = FilterConditionType.Relation;
      this.value = FilterRelationType.AND;
      this.operator = undefined;
    }
  }
}

export class ConditionBuilder {
  condition: ChartFilterCondition;

  constructor(condition?: FilterCondition) {
    this.condition = new ChartFilterCondition();
    if (condition) {
      this.condition.name = condition.name;
      this.condition.visualType = condition.visualType;
      this.condition.type = condition.type;
      this.condition.value = condition.value;
      this.condition.operator = condition.operator;
    }
    if (Array.isArray(condition?.children)) {
      this.condition.children = condition?.children?.map(child =>
        new ConditionBuilder(child).asSelf(),
      );
    }
  }

  setName(name: string) {
    this.condition.name = name;
    return this;
  }

  setSqlType(sqlType: string) {
    this.condition.visualType = sqlType;
    return this;
  }

  setValue(value?: any) {
    this.condition.value = value;
    return this;
  }

  setOperator(operator?: string) {
    this.condition.operator = operator;
    return this;
  }

  asSelf() {
    return this.condition;
  }

  asFilter(conditionType?: FilterConditionType) {
    this.condition.type = conditionType || FilterConditionType.Filter;
    return this.condition;
  }

  asRelation(value?: any, children?: ChartFilterCondition[]) {
    this.condition.type = FilterConditionType.Relation;
    this.condition.value = value || this.condition.value;
    this.condition.children = children || this.condition.children;
    return this.condition;
  }

  asRangeTime(name?, sqlType?) {
    this.condition.type = FilterConditionType.RangeTime;
    this.condition.operator = FilterSqlOperator.Between;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asRecommendTime(name?, sqlType?) {
    this.condition.type = FilterConditionType.RecommendTime;
    this.condition.operator = FilterSqlOperator.Between;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asGeneral(name?, sqlType?) {
    this.condition.type = FilterConditionType.List;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asCustomize(name?, sqlType?) {
    this.condition.type = FilterConditionType.Customize;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asCondition(name?, sqlType?) {
    this.condition.type = FilterConditionType.Condition;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asSingleOrRangeValue(name?, sqlType?) {
    this.condition.type = [
      FilterSqlOperator.Between,
      FilterSqlOperator.NotBetween,
    ].includes(this.condition.operator as FilterSqlOperator)
      ? FilterConditionType.RangeValue
      : FilterConditionType.Value;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }

  asTree(name?, sqlType?) {
    this.condition.type = FilterConditionType.Tree;
    this.condition.name = name || this.condition.name;
    this.condition.visualType = sqlType || this.condition.visualType;
    return this.condition;
  }
}

export default ChartFilterCondition;
