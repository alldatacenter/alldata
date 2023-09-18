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

import { addPathToHierarchyStructureAndChangeName } from 'app/pages/MainPage/pages/ViewPage/utils';
import { CloneValueDeep } from 'utils/object';
import { APP_VERSION_BETA_2, APP_VERSION_BETA_4 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';
/**
 * Migrate @see View config in beta.2 version
 * Changes:
 * - migrate model to ...
 * - ....
 *
 * @param {object} [model]
 * @return {*}  {(object | undefined)}
 */
const beta2 = model => {
  const clonedModel = CloneValueDeep(model) || {};
  if (model) {
    Object.keys(clonedModel).forEach(name => {
      clonedModel[name] = { ...clonedModel[name], name };
    });
    model = {
      hierarchy: clonedModel,
      columns: clonedModel,
    };
  }
  return model;
};

const beta4 = view => {
  const { viewType, result } = view;
  try {
    result.hierarchy = addPathToHierarchyStructureAndChangeName(
      result.hierarchy,
      viewType,
    );
    return result;
  } catch (error) {
    console.error('Migration view Errors | beta.4 | ', error);
    return result;
  }
};

/**
 * main entry point of migration
 *
 * @param {string} model
 * @return {string}
 */
const beginViewModelMigration = (model: string, viewType): string => {
  if (!model?.trim().length) {
    return model;
  }
  const modelObj = JSON.parse(model);
  const event2 = new MigrationEvent(APP_VERSION_BETA_2, beta2);
  const event4 = new MigrationEvent(APP_VERSION_BETA_4, beta4);

  const dispatcher2 = new MigrationEventDispatcher(event2);
  const result2 = dispatcher2.process(modelObj);

  const dispatcher4 = new MigrationEventDispatcher(event4);

  const result4 = dispatcher4.process({
    result: result2,
    viewType,
  });

  return JSON.stringify(result4);
};

export default beginViewModelMigration;
