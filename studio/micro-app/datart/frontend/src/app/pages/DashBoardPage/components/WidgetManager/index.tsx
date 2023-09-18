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
import type { WidgetProto } from '../../types/widgetTypes';
import checkboxGroupProto from '../Widgets/ControllerWidget/config/checkboxGroup';
import dropdownListProto from '../Widgets/ControllerWidget/config/dropdownList';
import dropDownTree from '../Widgets/ControllerWidget/config/dropDownTree';
import multiDropdownListProto from '../Widgets/ControllerWidget/config/multiDropdownList';
import radioGroupProto from '../Widgets/ControllerWidget/config/radioGroup';
import rangeTimeProto from '../Widgets/ControllerWidget/config/rangeTimeProto';
import rangeValueProto from '../Widgets/ControllerWidget/config/rangeValueProto';
import sliderProto from '../Widgets/ControllerWidget/config/sliderProto';
import textProto from '../Widgets/ControllerWidget/config/textProto';
import timeProto from '../Widgets/ControllerWidget/config/timeProto';
import valueProto from '../Widgets/ControllerWidget/config/valueProto';
import linkedChartProto from '../Widgets/DataChartWidget/linkedChartConfig';
import ownedChartProto from '../Widgets/DataChartWidget/ownedChartConfig';
import groupProto from '../Widgets/GroupWidget/config';
import iframeProto from '../Widgets/IframeWidget/iframeConfig';
import imageProto from '../Widgets/ImageWidget/imageConfig';
import queryBtnProto from '../Widgets/QueryBtnWidget/queryBtnConfig';
import resetBtnProto from '../Widgets/ResetBtnWidget/resetBtnConfig';
import richTextProto from '../Widgets/RichTextWidget/richTextConfig';
import tabProto from '../Widgets/TabWidget/tabConfig';
import timerProto from '../Widgets/TimerWidget/timerConfig';
import videoProto from '../Widgets/VideoWidget/videoConfig';
import { widgetManagerInstance as widgetManager } from './WidgetManager';

const protoList: WidgetProto[] = [
  linkedChartProto, // chart
  ownedChartProto,
  tabProto, //  container
  imageProto, //   media
  videoProto,
  richTextProto,
  iframeProto,
  timerProto,
  queryBtnProto, //   button
  resetBtnProto,
  dropdownListProto, //controller
  multiDropdownListProto,
  checkboxGroupProto,
  radioGroupProto,
  textProto,
  timeProto,
  dropDownTree,
  rangeTimeProto,
  rangeValueProto,
  valueProto,
  sliderProto,
  groupProto, //   group
];

protoList.forEach(item => {
  widgetManager.register(item);
});

export default widgetManager;
