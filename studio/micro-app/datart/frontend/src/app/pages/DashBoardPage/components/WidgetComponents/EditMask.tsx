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
import widgetManagerInstance from '../WidgetManager';
import { WidgetInfoContext } from '../WidgetProvider/WidgetInfoProvider';
import { WidgetContext } from '../WidgetProvider/WidgetProvider';
import { BlockMaskLayer } from './BlockMaskLayer';
import { WidgetDndHandleMask } from './WidgetDndHandleMask';

export const EditMask: React.FC<{ group?: boolean }> = memo(({ group }) => {
  const widget = useContext(WidgetContext);
  const canWrapped = widgetManagerInstance.meta(
    widget.config.originalType,
  ).canWrapped;
  const widgetInfo = useContext(WidgetInfoContext);
  return (
    <>
      {!widgetInfo.editing && (
        <WidgetDndHandleMask widgetId={widget.id} canWrapped={canWrapped} />
      )}
      <BlockMaskLayer widget={widget} widgetInfo={widgetInfo} />
    </>
  );
});
