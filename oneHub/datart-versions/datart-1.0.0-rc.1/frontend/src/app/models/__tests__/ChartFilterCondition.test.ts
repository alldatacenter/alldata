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
import { FilterSqlOperator } from 'globalConstants';
import ChartFilterCondition, {
  ConditionBuilder,
} from '../ChartFilterCondition';

describe('ChartFilterCondition Tests', () => {
  let initCondition;
  beforeEach(() => {
    initCondition = new ChartFilterCondition();
    initCondition.name = 'init-condition';
    initCondition.type = FilterConditionType.Filter;
    initCondition.visualType = 'STRING';
    initCondition.operator = FilterSqlOperator.In;
  });

  test('should init filter condition model', () => {
    const condition = new ChartFilterCondition();
    expect(condition).not.toBeNull();
    expect(condition.name).toEqual('');
    expect(condition.type).toEqual(FilterConditionType.Filter);
    expect(condition.value).toEqual(undefined);
    expect(condition.visualType).toEqual('');
    expect(condition.operator).toEqual(undefined);
    expect(condition.children).toEqual(undefined);
  });

  test('should init filter condition model with default filter type', () => {
    initCondition.type = undefined;
    const condition = new ChartFilterCondition(initCondition);
    expect(condition.type).toEqual(FilterConditionType.Filter);
  });

  test('should set condition model value', () => {
    const condition = new ChartFilterCondition();

    condition.setValue('value');
    expect(condition.value).toEqual('value');

    condition.setType(FilterConditionType.List);
    expect(condition.type).toEqual(FilterConditionType.List);

    condition.setOperator('IN');
    expect(condition.operator).toEqual('IN');
  });

  test('should init filter condition model by old condition', () => {
    const grandsonCondition = new ChartFilterCondition();
    grandsonCondition.name = 'grandson-condition';
    const childCondition = new ChartFilterCondition();
    childCondition.name = 'child-condition';
    childCondition.children = [grandsonCondition];
    const oldCondition = new ChartFilterCondition();
    oldCondition.name = 'condition';
    oldCondition.visualType = 'STRING';
    oldCondition.setValue('old');
    oldCondition.setType(FilterConditionType.Time);
    oldCondition.setOperator('EQ');
    oldCondition.children = [childCondition];

    const condition = new ChartFilterCondition(oldCondition);

    expect(condition.name).toEqual('condition');
    expect(condition.children?.[0].name).toEqual('child-condition');
    expect(condition.children?.[0].children?.[0].name).toEqual(
      'grandson-condition',
    );
  });

  test('should append into condition when condition is relation type and no child', () => {
    const condition = initCondition;
    condition.setType(FilterConditionType.Relation);

    condition.appendChild();

    expect(condition.name).toEqual('init-condition');
    expect(condition.type).toEqual(FilterConditionType.Relation);
    expect(condition.children?.length).toEqual(1);
    expect(condition.children?.[0].name).toEqual('init-condition');
    expect(condition.children?.[0].type).toEqual(FilterConditionType.Filter);
  });

  test('should append into condition when condition is relation type', () => {
    const childCondition = new ChartFilterCondition();
    childCondition.name = 'child-condition';
    const condition = initCondition;
    condition.setType(FilterConditionType.Relation);
    condition.children = [childCondition];

    condition.appendChild(1);

    expect(condition.name).toEqual('init-condition');
    expect(condition.type).toEqual(FilterConditionType.Relation);
    expect(condition.children?.length).toEqual(2);
    expect(condition.children?.[0].name).toEqual('child-condition');
    expect(condition.children?.[0].type).toEqual(FilterConditionType.Filter);
  });

  test('should expand filter into two children when condition is not relation type', () => {
    const condition = initCondition;
    condition.setType(FilterConditionType.List);
    condition.value = 'a';

    condition.appendChild();

    expect(condition.name).toEqual('init-condition');
    expect(condition.type).toEqual(FilterConditionType.Relation);
    expect(condition.value).toEqual(FilterRelationType.AND);
    expect(condition.children?.length).toEqual(2);
    expect(condition.children?.[0].name).toEqual('init-condition');
    expect(condition.children?.[0].type).toEqual(FilterConditionType.Filter);
    expect(condition.children?.[0].value).toEqual('a');
    expect(condition.children?.[0].operator).toEqual('IN');
    expect(condition.children?.[1].name).toEqual('init-condition');
    expect(condition.children?.[1].type).toEqual(FilterConditionType.Filter);
    expect(condition.children?.[1].value).toEqual('a');
    expect(condition.children?.[1].operator).toEqual('IN');
  });

  test('should not append into condition when index below zero', () => {
    const childCondition = new ChartFilterCondition();
    childCondition.name = 'child-condition';
    const condition = initCondition;
    condition.setType(FilterConditionType.Relation);
    condition.children = [childCondition];

    condition.appendChild(-1);

    expect(condition.name).toEqual('init-condition');
    expect(condition.type).toEqual(FilterConditionType.Relation);
    expect(condition.children?.length).toEqual(1);
    expect(condition.children?.[0].name).toEqual(childCondition.name);
    expect(condition.children?.[0].type).toEqual(childCondition.type);
  });

  test('should update condition child by index', () => {
    const childCondition = new ChartFilterCondition();
    childCondition.name = 'child-condition';
    const newChildCondition = new ChartFilterCondition();
    newChildCondition.name = 'new-child-condition';
    const condition = initCondition;
    condition.setType(FilterConditionType.Relation);
    condition.children = [childCondition, childCondition];

    condition.updateChild(1, newChildCondition);

    expect(condition.children?.length).toEqual(2);
    expect(condition.children?.[0].name).toEqual('child-condition');
    expect(condition.children?.[0].type).toEqual(FilterConditionType.Filter);
    expect(condition.children?.[1].name).toEqual('new-child-condition');
    expect(condition.children?.[0].type).toEqual(FilterConditionType.Filter);
  });

  test('should not update condition child when child is empty', () => {
    const newChildCondition = new ChartFilterCondition();
    newChildCondition.name = 'new-child-condition';
    const condition = initCondition;
    condition.setType(FilterConditionType.Relation);

    condition.updateChild(1, newChildCondition);

    expect(condition.children?.length).toEqual(undefined);
  });

  test('should replace current condition by child when only two children', () => {
    const childCondition = new ChartFilterCondition();
    childCondition.name = 'child-condition';
    const newChildCondition = new ChartFilterCondition();
    newChildCondition.name = 'new-child-condition';
    newChildCondition.operator = 'BETWEEN';
    newChildCondition.value = '1';
    const condition = initCondition;
    condition.setType(FilterConditionType.Relation);
    condition.children = [childCondition, newChildCondition];

    condition.removeChild(0);

    expect(condition.name).toEqual('init-condition');
    expect(condition.type).toEqual(FilterConditionType.Filter);
    expect(condition.operator).toEqual(newChildCondition.operator);
    expect(condition.value).toEqual(newChildCondition.value);
    expect(condition.children?.length).toEqual(0);
  });

  test('should replace current condition by child when children more than two', () => {
    const firstCondition = new ChartFilterCondition();
    firstCondition.name = 'first-condition';
    const secondCondition = new ChartFilterCondition();
    secondCondition.name = 'second-condition';
    const thirdCondition = new ChartFilterCondition();
    secondCondition.name = 'third-condition';
    const condition = initCondition;
    condition.setType(FilterConditionType.Relation);
    condition.children = [firstCondition, secondCondition, thirdCondition];

    condition.removeChild(0);

    expect(condition.name).toEqual('init-condition');
    expect(condition.type).toEqual(FilterConditionType.Relation);
    expect(condition.operator).toEqual(condition.operator);
    expect(condition.value).toEqual(condition.value);
    expect(condition.children?.length).toEqual(2);
    expect(condition.children?.[0]).toEqual(secondCondition);
    expect(condition.children?.[1]).toEqual(thirdCondition);
  });

  test('should not remove anything when child is less than two', () => {
    const firstCondition = new ChartFilterCondition();
    firstCondition.name = 'first-condition';
    const condition = initCondition;
    condition.setType(FilterConditionType.Relation);
    condition.children = [firstCondition];

    condition.removeChild(0);

    expect(condition.name).toEqual('init-condition');
    expect(condition.type).toEqual(FilterConditionType.Relation);
    expect(condition.operator).toEqual(condition.operator);
    expect(condition.value).toEqual(condition.value);
    expect(condition.children?.length).toEqual(1);
    expect(condition.children?.[0]).toEqual(firstCondition);
  });

  test('should not remove anything when child is empty', () => {
    const condition = initCondition;
    condition.setType(FilterConditionType.Relation);

    condition.removeChild(0);

    expect(condition.name).toEqual('init-condition');
    expect(condition.type).toEqual(FilterConditionType.Relation);
    expect(condition.operator).toEqual(condition.operator);
    expect(condition.value).toEqual(condition.value);
    expect(condition.children?.length).toEqual(undefined);
  });
});

describe('ConditionBuilder Tests', () => {
  let initCondition;
  beforeEach(() => {
    initCondition = new ChartFilterCondition();
    initCondition.name = 'init-condition';
    initCondition.type = FilterConditionType.Filter;
    initCondition.visualType = 'STRING';
    initCondition.operator = FilterSqlOperator.In;
  });

  test('should init builder', () => {
    const builder = new ConditionBuilder();
    builder.setName('filter');
    builder.setSqlType('NUMERIC');
    builder.setValue(1);
    builder.setOperator('IN');

    const condition = builder.asSelf();

    expect(condition.name).toEqual('filter');
    expect(condition.visualType).toEqual('NUMERIC');
    expect(condition.value).toEqual(1);
    expect(condition.operator).toEqual('IN');
  });

  test('should init builder by condition filter', () => {
    const childCondition = new ChartFilterCondition();
    childCondition.name = 'child-condition';
    const condition = new ChartFilterCondition(initCondition);
    condition.setType(FilterConditionType.Relation);
    condition.children = [childCondition];

    const builder = new ConditionBuilder(condition);

    expect(builder.asSelf().name).toEqual(condition.name);
    expect(builder.asSelf().visualType).toEqual(condition.visualType);
    expect(builder.asSelf().type).toEqual(condition.type);
    expect(builder.asSelf().value).toEqual(condition.value);
    expect(builder.asSelf().operator).toEqual(condition.operator);
    expect(builder.asSelf().children).toEqual(condition.children);
  });

  test('should get condition from asFilter', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asFilter();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Filter);
  });

  test('should get condition from asFilter with condition type', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asFilter(FilterConditionType.RangeTime);

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.RangeTime);
  });

  test('should get condition from asRelation', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asRelation();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Relation);
  });

  test('should get condition from asRelation with value and children', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asRelation('value', [initCondition]);

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.value).toEqual('value');
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Relation);
    expect(newCondition.children).toEqual([initCondition]);
  });

  test('should get condition from asRangeTime', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asRangeTime();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.visualType).toEqual(initCondition.visualType);
    expect(newCondition.operator).toEqual(FilterSqlOperator.Between);
    expect(newCondition.type).toEqual(FilterConditionType.RangeTime);
  });

  test('should get condition from asRangeTime with name and sql type', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asRangeTime('newName', 'NUMERIC');

    expect(newCondition.name).toEqual('newName');
    expect(newCondition.visualType).toEqual('NUMERIC');
    expect(newCondition.operator).toEqual(FilterSqlOperator.Between);
    expect(newCondition.type).toEqual(FilterConditionType.RangeTime);
  });

  test('should get condition from asRecommendTime', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asRecommendTime();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.visualType).toEqual(initCondition.visualType);
    expect(newCondition.operator).toEqual(FilterSqlOperator.Between);
    expect(newCondition.type).toEqual(FilterConditionType.RecommendTime);
  });

  test('should get condition from asRecommendTime with name and sql type', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asRecommendTime('newName', 'NUMERIC');

    expect(newCondition.name).toEqual('newName');
    expect(newCondition.visualType).toEqual('NUMERIC');
    expect(newCondition.operator).toEqual(FilterSqlOperator.Between);
    expect(newCondition.type).toEqual(FilterConditionType.RecommendTime);
  });

  test('should get condition from asGeneral', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asGeneral();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.visualType).toEqual(initCondition.visualType);
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.List);
  });

  test('should get condition from asGeneral with name and sql type', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asGeneral('newName', 'NUMERIC');

    expect(newCondition.name).toEqual('newName');
    expect(newCondition.visualType).toEqual('NUMERIC');
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.List);
  });

  test('should get condition from asCustomize', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asCustomize();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.visualType).toEqual(initCondition.visualType);
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Customize);
  });

  test('should get condition from asCustomize with name and sql type', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asCustomize('newName', 'NUMERIC');

    expect(newCondition.name).toEqual('newName');
    expect(newCondition.visualType).toEqual('NUMERIC');
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Customize);
  });

  test('should get condition from asCondition', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asCondition();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.visualType).toEqual(initCondition.visualType);
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Condition);
  });

  test('should get condition from asCondition with name and sql type', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asCondition('newName', 'NUMERIC');

    expect(newCondition.name).toEqual('newName');
    expect(newCondition.visualType).toEqual('NUMERIC');
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Condition);
  });

  test('should get condition from asSingleOrRangeValue when init condition type is not between like', () => {
    initCondition.operator = FilterSqlOperator.Contain;
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asSingleOrRangeValue();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.visualType).toEqual(initCondition.visualType);
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Value);
  });

  test('should get condition from asSingleOrRangeValue when init condition type is BETWEEN', () => {
    initCondition.operator = FilterSqlOperator.Between;
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asSingleOrRangeValue();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.visualType).toEqual(initCondition.visualType);
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.RangeValue);
  });

  test('should get condition from asSingleOrRangeValue when init condition type is NOT_BETWEEN', () => {
    initCondition.operator = FilterSqlOperator.NotBetween;
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asSingleOrRangeValue();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.visualType).toEqual(initCondition.visualType);
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.RangeValue);
  });

  test('should get condition from asSingleOrRangeValue with name and sql type', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asSingleOrRangeValue('newName', 'NUMERIC');

    expect(newCondition.name).toEqual('newName');
    expect(newCondition.visualType).toEqual('NUMERIC');
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Value);
  });

  test('should get condition from asTree', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asTree();

    expect(newCondition.name).toEqual(initCondition.name);
    expect(newCondition.visualType).toEqual(initCondition.visualType);
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Tree);
  });

  test('should get condition from asTree with name and sql type', () => {
    const builder = new ConditionBuilder(initCondition);

    const newCondition = builder.asTree('newName', 'NUMERIC');

    expect(newCondition.name).toEqual('newName');
    expect(newCondition.visualType).toEqual('NUMERIC');
    expect(newCondition.operator).toEqual(initCondition.operator);
    expect(newCondition.type).toEqual(FilterConditionType.Tree);
  });
});
