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
import { WidgetContext } from 'app/pages/DashBoardPage/components/WidgetProvider/WidgetProvider';
import useRenderWidget from 'app/pages/DashBoardPage/hooks/useRenderWidget';
import useWidgetAutoFetch from 'app/pages/DashBoardPage/hooks/useWidgetAutoFetch';
import { memo, useContext } from 'react';
import { BoardContext } from '../../BoardProvider/BoardProvider';
import { FlexStyle, ZIndexStyle } from '../../WidgetComponents/constants';
import { EditMask } from '../../WidgetComponents/EditMask';
import { WidgetTitle } from '../../WidgetComponents/WidgetTitle';
import { WidgetWrapper } from '../../WidgetComponents/WidgetWrapper';
import {
  getWidgetBaseStyle,
  getWidgetTitle,
} from '../../WidgetManager/utils/utils';
import { WidgetInfoContext } from '../../WidgetProvider/WidgetInfoProvider';
import { ToolBar } from './components/ToolBar';
import { DataChartWidgetCore } from './DataChartWidgetCore';

export const DataChartWidget: React.FC<{ hideTitle: boolean }> = memo(
  ({ hideTitle }) => {
    const widget = useContext(WidgetContext);
    const { rendered } = useContext(WidgetInfoContext);
    const { renderMode, boardType } = useContext(BoardContext);
    const { cacheWhRef } = useRenderWidget(
      widget,
      renderMode,
      boardType,
      rendered,
    );
    useWidgetAutoFetch(widget, renderMode, cacheWhRef, rendered);
    const title = getWidgetTitle(widget.config.customConfig.props);
    title.title = widget.config.name;
    const { background, border, padding } = getWidgetBaseStyle(
      widget.config.customConfig.props,
    );
    return (
      <WidgetWrapper background={background} border={border} padding={padding}>
        <div ref={cacheWhRef} style={ZIndexStyle}>
          {!hideTitle && <WidgetTitle title={title} />}

          <div style={FlexStyle}>
            <DataChartWidgetCore />
          </div>
        </div>
        {renderMode === 'edit' && <EditMask />}
        <ToolBar />
      </WidgetWrapper>
    );
  },
);
