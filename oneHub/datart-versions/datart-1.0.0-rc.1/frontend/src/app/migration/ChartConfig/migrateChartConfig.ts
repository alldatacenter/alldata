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

import { APP_VERSION_RC_0 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';

export const RC0 = config => {
  if (!config) {
    return config;
  }

  try {
    if (config?.computedFields) {
      config.computedFields = config.computedFields.map(v => {
        if (!v.name) {
          return {
            ...v,
            name: v.id,
          };
        }
        return v;
      });
    }

    return config;
  } catch (error) {
    console.error('Migration config Errors | RC.0 | ', error);
    return config;
  }
};

const migrationViewConfig = (config: string): string => {
  if (!config) {
    return config;
  }
  const chartConfig = JSON.parse(config);
  const event2 = new MigrationEvent(APP_VERSION_RC_0, RC0);
  const dispatcher = new MigrationEventDispatcher(event2);
  const result = dispatcher.process(chartConfig);

  return JSON.stringify(result);
};

export default migrationViewConfig;
