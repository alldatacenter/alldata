// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

declare namespace ISSUE_FIELD {
  type IIssueType = 'BUG' | 'TASK' | 'REQUIREMENT' | 'EPIC' | 'COMMON';
  type IPropertyType = 'Text' | 'Number' | 'Select' | 'MultiSelect' | 'Date' | 'Person' | 'URL' | 'Email' | 'Phone';

  interface IIssueTime {
    task: string;
    bug: string;
    requirement: string;
    epic: string;
  }

  interface IProjectIssueQuery {
    orgID: number;
  }

  interface ISpecialOption {
    id: number;
    name: string;
    value: string;
  }

  interface ISpecialFieldQuery {
    orgID: number;
    issueType: IIssueType;
    list?: ISpecialOption[];
  }

  interface IFieldsByIssueQuery extends IProjectIssueQuery {
    propertyIssueType: IIssueType;
  }

  interface IEnumData {
    name: any;
    id?: number;
    index?: number;
  }

  interface IFiledItem {
    propertyID?: number;
    required: boolean | 'true' | 'false';
    propertyName: string;
    displayName?: string;
    propertyType: IPropertyType;
    orgID: number;
    scopeType: string;
    scopeID: number;
    propertyIssueType: IIssueType;
    relation: number;
    index: number;
    enumeratedValues?: IEnumData[];
    values?: number[];
    arbitraryValue?: string;
    relatedIssue: string[];
    isSpecialField?: boolean;
  }
}
