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
import { useCacheWidthHeight } from 'app/hooks/useCacheWidthHeight';
import { useCallback, useContext, useEffect } from 'react';
import { WidgetActionContext } from '../components/ActionProvider/WidgetActionProvider';
import { BoardConfigValContext } from '../components/BoardProvider/BoardConfigProvider';
import { BoardType, VizRenderMode } from '../pages/Board/slice/types';
import { boardScroll } from '../pages/BoardEditor/slice/events';
import { Widget } from '../types/widgetTypes';
import { isElView } from '../utils/board';

export default function useRenderWidget(
  widget: Widget,
  renderMode: VizRenderMode,
  boardType: BoardType,
  rendered: boolean,
) {
  const { initialQuery } = useContext(BoardConfigValContext);
  const { onRenderedWidgetById } = useContext(WidgetActionContext);
  const { cacheWhRef, cacheW } = useCacheWidthHeight();

  const renderWidget = useCallback(() => {
    const canView = isElView(cacheWhRef.current);
    if (canView) {
      onRenderedWidgetById(widget.id);
    }
  }, [cacheWhRef, onRenderedWidgetById, widget.id]);

  //监听board滚动
  useEffect(() => {
    boardScroll.off(widget.dashboardId, renderWidget);
    if (!rendered && boardType === 'auto') {
      boardScroll.on(widget.dashboardId, renderWidget);
    }
    return () => {
      boardScroll.off(widget.dashboardId, renderWidget);
    };
  }, [renderWidget, widget.dashboardId, rendered, boardType]);

  //定时任务中 或者 后端截图 直接fetch
  useEffect(() => {
    if (renderMode === 'schedule') {
      onRenderedWidgetById(widget.id);
    }
  }, [onRenderedWidgetById, renderMode, widget.id]);
  //初始化查询
  useEffect(() => {
    const canRender =
      initialQuery && renderMode !== 'schedule' && !rendered && cacheW > 1;
    if (canRender) {
      renderWidget();
    }
  }, [initialQuery, renderMode, rendered, renderWidget, cacheW]);
  return {
    cacheWhRef,
    cacheW,
  };
}
