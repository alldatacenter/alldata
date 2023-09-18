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
import { memo, useContext } from 'react';
import { ORIGINAL_TYPE_MAP } from '../../constants';
import { WidgetDataProvider } from '../WidgetProvider/WidgetDataProvider';
import { WidgetContext } from '../WidgetProvider/WidgetProvider';
import { ControllerWidget } from '../Widgets/ControllerWidget/ControllerWidget';
import { DataChartWidget } from '../Widgets/DataChartWidget/DataChartWidget';
import { GroupWidget } from '../Widgets/GroupWidget';
import { IframeWidget } from '../Widgets/IframeWidget/IframeWidget';
import { ImageWidget } from '../Widgets/ImageWidget/ImageWidget';
import { QueryBtnWidget } from '../Widgets/QueryBtnWidget/QueryBtnWidget';
import { ResetBtnWidget } from '../Widgets/ResetBtnWidget/ResetBtnWidget';
import { RichTextWidget } from '../Widgets/RichTextWidget/RichTextWidget';
import { TabWidget } from '../Widgets/TabWidget/TabWidget';
import { TimerWidget } from '../Widgets/TimerWidget/TimerWidget';
import { VideoWidget } from '../Widgets/VideoWidget/VideoWidget';

export const WidgetMapper: React.FC<{
  boardEditing: boolean;
  hideTitle: boolean;
}> = memo(({ boardEditing, hideTitle }) => {
  const widget = useContext(WidgetContext);
  const originalType = widget.config.originalType;
  switch (originalType) {
    case ORIGINAL_TYPE_MAP.group:
      return <GroupWidget />;
    // chart
    case ORIGINAL_TYPE_MAP.linkedChart:
    case ORIGINAL_TYPE_MAP.ownedChart:
      return (
        <WidgetDataProvider
          widgetId={widget.id}
          boardId={widget.dashboardId}
          boardEditing={boardEditing}
        >
          <DataChartWidget hideTitle={hideTitle} />
        </WidgetDataProvider>
      );
    // media
    case ORIGINAL_TYPE_MAP.richText:
      return <RichTextWidget hideTitle={hideTitle} />;
    case ORIGINAL_TYPE_MAP.image:
      return <ImageWidget hideTitle={hideTitle} />;
    case ORIGINAL_TYPE_MAP.video:
      return <VideoWidget hideTitle={hideTitle} />;
    case ORIGINAL_TYPE_MAP.iframe:
      return <IframeWidget hideTitle={hideTitle} />;
    case ORIGINAL_TYPE_MAP.timer:
      return <TimerWidget hideTitle={hideTitle} />;

    // tab
    case ORIGINAL_TYPE_MAP.tab:
      return <TabWidget hideTitle={hideTitle} />;

    // btn
    case ORIGINAL_TYPE_MAP.queryBtn:
      return <QueryBtnWidget />;
    case ORIGINAL_TYPE_MAP.resetBtn:
      return <ResetBtnWidget />;
    // controller
    case ORIGINAL_TYPE_MAP.dropdownList:
    case ORIGINAL_TYPE_MAP.multiDropdownList:
    case ORIGINAL_TYPE_MAP.checkboxGroup:
    case ORIGINAL_TYPE_MAP.radioGroup:
    case ORIGINAL_TYPE_MAP.text:
    case ORIGINAL_TYPE_MAP.time:
    case ORIGINAL_TYPE_MAP.rangeTime:
    case ORIGINAL_TYPE_MAP.rangeValue:
    case ORIGINAL_TYPE_MAP.value:
    case ORIGINAL_TYPE_MAP.slider:
    case ORIGINAL_TYPE_MAP.dropDownTree:
      return (
        <WidgetDataProvider
          widgetId={widget.id}
          boardId={widget.dashboardId}
          boardEditing={boardEditing}
        >
          <ControllerWidget />
        </WidgetDataProvider>
      );
    //RangeSlider
    //Tree
    // unknown
    default:
      return <div> unknown widget ?{originalType} </div>;
  }
});
