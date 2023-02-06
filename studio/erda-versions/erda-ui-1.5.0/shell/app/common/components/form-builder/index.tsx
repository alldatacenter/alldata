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

import { FormBuilder, PureFormBuilder, IFormExtendType } from './form-builder';
import { Fields, IFieldType } from './fields';
import SearchForm from './search-form';

type IFormBuilder = typeof FormBuilder;

interface IRealFormInterface extends IFormBuilder {
  Fields: typeof Fields;
  PureFormBuilder: typeof PureFormBuilder;
  SearchForm: typeof SearchForm;
}

const RealFormBuilder = FormBuilder as IRealFormInterface;

RealFormBuilder.PureFormBuilder = PureFormBuilder;
RealFormBuilder.Fields = Fields;
RealFormBuilder.SearchForm = SearchForm;

export default RealFormBuilder;

export type { IFormExtendType, IFieldType };
