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

import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { RC2 } from '../ChartConfig/migrateChartConfig';
import { APP_VERSION_RC_2 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';

const migrateWidgetChartConfig = (widgets: Widget[]): Widget[] => {
  if (!Array.isArray(widgets)) {
    return [];
  }
  return widgets
    .map(widget => {
      if (widget?.config?.content?.dataChart?.config) {
        const event_rc_2 = new MigrationEvent(APP_VERSION_RC_2, RC2);
        const dispatcher_rc_2 = new MigrationEventDispatcher(event_rc_2);

        widget.config.content.dataChart.config = dispatcher_rc_2.process(
          widget.config.content.dataChart.config,
        );
      }

      return widget;
    })
    .filter(Boolean) as Widget[];
};

export default migrateWidgetChartConfig;
