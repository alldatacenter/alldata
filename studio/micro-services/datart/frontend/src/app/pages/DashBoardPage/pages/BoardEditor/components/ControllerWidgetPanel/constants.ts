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

import { ControllerFacadeTypes } from 'app/constants';

export const FixedSqlOperatorTypes = [
  ControllerFacadeTypes.RangeTime,
  ControllerFacadeTypes.RangeSlider,
  ControllerFacadeTypes.RangeValue,
];
export const ConventionalControllerTypes = [
  ControllerFacadeTypes.DropdownList,
  ControllerFacadeTypes.MultiDropdownList,
  ControllerFacadeTypes.RadioGroup,
  ControllerFacadeTypes.Text,
];
export const NumericalControllerTypes = [
  ControllerFacadeTypes.RangeValue,
  ControllerFacadeTypes.Value,
  ControllerFacadeTypes.Slider,
  ControllerFacadeTypes.RangeSlider,
];
export const DateControllerTypes = [
  ControllerFacadeTypes.RangeTime,
  ControllerFacadeTypes.Time,
];

export const RangeControlTypes = [
  ControllerFacadeTypes.RangeTime,
  ControllerFacadeTypes.RangeSlider,
  ControllerFacadeTypes.RangeValue,
];
export const StrControlTypes = [
  ControllerFacadeTypes.DropdownList,
  ControllerFacadeTypes.MultiDropdownList,
  ControllerFacadeTypes.RadioGroup,
  ControllerFacadeTypes.Text,
];

export const HasOptionsControlTypes = [
  ControllerFacadeTypes.DropdownList,
  ControllerFacadeTypes.MultiDropdownList,
  ControllerFacadeTypes.RadioGroup,
  ControllerFacadeTypes.CheckboxGroup,
  ControllerFacadeTypes.DropDownTree,
];
