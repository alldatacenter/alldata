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

import {
  InteractionAction,
  InteractionCategory,
  InteractionFieldMapper,
  InteractionFieldRelation,
  InteractionMouseEvent,
  InteractionRelationType,
} from '../../constants';

export type VizType = {
  relId: string;
  name: string;
  relType: string;
};

export type I18nTranslator = {
  translate: (title: string, disablePrefix?: boolean, options?: any) => string;
};

export type CustomizeRelation = {
  id: string;
  type: InteractionRelationType;
  source?: string;
  target?: string;
};

export type JumpToChartRule = {
  relId: string;
  relation: InteractionFieldRelation;
  [InteractionFieldRelation.Customize]: CustomizeRelation[];
};

export type JumpToDashboardRule = {
  relId: string;
  [InteractionFieldRelation.Customize]: CustomizeRelation[];
};

export type JumpToUrlRule = {
  relId: string;
  url: string;
  [InteractionFieldRelation.Customize]: CustomizeRelation[];
};

export type InteractionRule = {
  id: string;
  name?: string;
  event?: InteractionMouseEvent;
  category?: InteractionCategory;
  action?: InteractionAction;
  [InteractionCategory.JumpToChart]?: JumpToChartRule;
  [InteractionCategory.JumpToDashboard]?: JumpToDashboardRule;
  [InteractionCategory.JumpToUrl]?: JumpToUrlRule;
};

export type CrossFilteringInteractionRule = {
  id: string;
  relId?: string;
  enable?: boolean;
  relation?: InteractionFieldRelation;
  [InteractionFieldRelation.Customize]: CustomizeRelation[];
};

export type DrillThroughSetting = {
  rules?: InteractionRule[];
};

export type CrossFilteringSetting = {
  event?: InteractionMouseEvent;
  rules?: CrossFilteringInteractionRule[];
};

export type ViewDetailSetting = {
  event: InteractionMouseEvent;
  mapper?: InteractionFieldMapper;
  [InteractionFieldMapper.Customize]?: any[];
};
