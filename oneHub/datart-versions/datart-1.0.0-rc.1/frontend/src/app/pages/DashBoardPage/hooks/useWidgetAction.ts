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
import { useCallback, useContext } from 'react';
import { WidgetActionContext } from '../components/ActionProvider/WidgetActionProvider';
import { Widget, widgetActionType } from '../types/widgetTypes';

export default function useWidgetAction() {
  const {
    onWidgetFullScreen,
    onWidgetGetData,
    onEditChartWidget,
    onEditMediaWidget,
    onEditContainerWidget,
    onEditControllerWidget,
    onEditWidgetLock,
    onEditWidgetUnLock,
    onEditDeleteActiveWidgets,
    onEditComposeGroup,
    onEditUnGroupAction,
  } = useContext(WidgetActionContext);

  const onWidgetEdit = useCallback(
    (widget: Widget) => {
      const type = widget.config.type;
      switch (type) {
        case 'chart':
          onEditChartWidget(widget);
          break;
        case 'controller':
          onEditControllerWidget(widget);
          break;
        case 'container':
          onEditContainerWidget(widget.id);
          break;
        case 'media':
          onEditMediaWidget(widget.id);
          break;
        default:
          break;
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );
  const widgetAction = useCallback((key: widgetActionType, widget: Widget) => {
    switch (key) {
      case 'delete':
        onEditDeleteActiveWidgets([widget.id]);
        break;
      case 'fullScreen':
        onWidgetFullScreen(widget.id);
        break;
      case 'refresh':
        onWidgetGetData(widget);
        break;
      case 'edit':
        onWidgetEdit(widget);
        break;
      case 'lock':
        onEditWidgetLock(widget.id);
        break;
      case 'unlock':
        onEditWidgetUnLock(widget.id);
        break;
      case 'group':
        onEditComposeGroup(widget.id);
        break;
      case 'unGroup':
        onEditUnGroupAction(widget.id);
        break;
      default:
        console.log('__ not found __ action', key);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return widgetAction;
}
