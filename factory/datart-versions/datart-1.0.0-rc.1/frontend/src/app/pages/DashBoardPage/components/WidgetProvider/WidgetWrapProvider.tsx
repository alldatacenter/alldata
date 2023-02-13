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

import { FC, memo } from 'react';
import { WidgetChartProvider } from './WidgetChartProvider';
import { WidgetInfoProvider } from './WidgetInfoProvider';
import { WidgetProvider } from './WidgetProvider';
import { WidgetSelectionProvider } from './WidgetSelectionProvider';

export const WidgetWrapProvider: FC<{
  id: string;
  boardId: string;
  boardEditing: boolean;
}> = memo(({ id, boardEditing, boardId, children }) => {
  return (
    <WidgetProvider boardId={boardId} boardEditing={boardEditing} widgetId={id}>
      <WidgetInfoProvider
        boardId={boardId}
        boardEditing={boardEditing}
        widgetId={id}
      >
        <WidgetSelectionProvider boardEditing={boardEditing} widgetId={id}>
          <WidgetChartProvider boardEditing={boardEditing} widgetId={id}>
            {children}
          </WidgetChartProvider>
        </WidgetSelectionProvider>
      </WidgetInfoProvider>
    </WidgetProvider>
  );
});
