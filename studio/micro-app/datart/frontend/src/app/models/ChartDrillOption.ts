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

import { ChartDataSectionField, FilterCondition } from 'app/types/ChartConfig';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { FilterSqlOperator } from 'globalConstants';
import { isEmptyArray } from 'utils/object';
import { ConditionBuilder } from './ChartFilterCondition';

export enum DrillMode {
  Normal = 'normal',
  Drill = 'drill',
  Expand = 'expand',
}

export class ChartDrillOption implements IChartDrillOption {
  private cursor: number = -1;
  private isSelected = false;
  private drillFields: ChartDataSectionField[] = [];
  private drillDownFields: Array<{
    field: ChartDataSectionField;
    condition?: FilterCondition;
  }> = [];
  private expandDownFields: Array<{
    field: ChartDataSectionField;
    condition?: FilterCondition;
  }> = [];

  constructor(fields: ChartDataSectionField[]) {
    this.drillFields = fields;
    this.clearAll();
  }

  public get mode() {
    if (!isEmptyArray(this.drillDownFields)) {
      return DrillMode.Drill;
    } else if (!isEmptyArray(this.expandDownFields)) {
      return DrillMode.Expand;
    }
    return DrillMode.Normal;
  }

  public get isSelectedDrill() {
    return this.isSelected;
  }

  public get isBottomLevel() {
    return this.cursor + 2 === this.drillFields.length;
  }

  public get isDrillable() {
    return this.drillFields.length > 1;
  }

  public get canSelect() {
    return isEmptyArray(this.expandDownFields);
  }

  public toggleSelectedDrill(enable?: boolean) {
    if (enable !== undefined) {
      this.isSelected = Boolean(enable);
    } else {
      this.isSelected = !this.isSelected;
    }
  }

  public getAllFields() {
    return this.drillFields;
  }

  public getAllDrillDownFields() {
    return this.drillDownFields;
  }

  public getDrilledFields() {
    if (this.mode === DrillMode.Normal) {
      return [];
    }
    return this.drillFields.slice(0, this.cursor + 2);
  }

  public getCurrentFields(): ChartDataSectionField[] | undefined {
    return this.cursor === -1
      ? undefined
      : this.mode === DrillMode.Drill
      ? [this.drillFields?.[this.cursor + 1]]
      : this.drillFields.slice(0, this.cursor + 2);
  }

  public getCurrentDrillLevel(): number {
    return this.cursor + 1;
  }

  public drillDown(filterData?: { [key in string]: any }) {
    if (this.drillFields.length === this.cursor + 2) {
      return;
    }
    this.cursor++;
    const currentField = this.drillFields[this.cursor];
    let cond;
    if (currentField && filterData) {
      cond = new ConditionBuilder()
        .setName(currentField.colName)
        .setOperator(FilterSqlOperator.Equal)
        .setValue(filterData[currentField.colName])
        .asFilter();
    }
    this.drillDownFields.push({
      field: currentField,
      condition: cond,
    });
  }

  public expandDown() {
    this.isSelected = false;
    if (this.drillFields.length === this.cursor + 2) {
      return;
    }
    this.cursor++;
    const currentField = this.drillFields[this.cursor];
    this.expandDownFields.push({
      field: currentField,
    });
  }

  public drillUp(field?: ChartDataSectionField) {
    if (this.cursor === -1) {
      return;
    }
    if (field) {
      const fieldIndex = this.drillDownFields.findIndex(
        d => d.field.uid === field.uid,
      );
      if (fieldIndex === 0) {
        this.clearAll();
      } else if (fieldIndex >= 1) {
        this.drillDownFields = this.drillDownFields.slice(0, fieldIndex);
        this.cursor = fieldIndex - 1;
      }
    } else {
      this.cursor--;
      this.drillDownFields.pop();
      if (isEmptyArray(this.drillDownFields)) {
        this.clearAll();
      }
    }
  }

  public expandUp(field?: ChartDataSectionField) {
    if (this.cursor === -1) {
      return;
    }
    if (field) {
      const fieldIndex = this.expandDownFields.findIndex(
        d => d.field.uid === field.uid,
      );
      if (fieldIndex === 0) {
        this.clearAll();
      } else if (fieldIndex >= 1) {
        this.expandDownFields = this.expandDownFields.slice(0, fieldIndex);
        this.cursor = fieldIndex - 1;
      }
    } else {
      this.cursor--;
      this.expandDownFields.pop();
      if (isEmptyArray(this.expandDownFields)) {
        this.clearAll();
      }
    }
  }

  public rollUp() {
    if (this.mode === DrillMode.Drill) {
      return this.drillUp();
    } else if (this.mode === DrillMode.Expand) {
      return this.expandUp();
    }
  }

  public clearAll() {
    this.cursor = -1;
    this.drillDownFields = [];
    this.expandDownFields = [];
  }
}
