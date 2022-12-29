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
import { StoryPageConfig } from 'app/pages/StoryBoardPage/slice/types';
import { getInitStoryPageConfig } from 'app/pages/StoryBoardPage/utils';
import { setLatestVersion, versionCanDo } from '../utils';
import { APP_VERSION_BETA_2 } from './../constants';

export const parseStoryPageConfig = (storyConfig: string) => {
  try {
    let nextConfig: StoryPageConfig = JSON.parse(storyConfig);
    return nextConfig;
  } catch (error) {
    let nextConfig = getInitStoryPageConfig();
    return nextConfig;
  }
};

export const beta2 = (config: StoryPageConfig) => {
  const curVersion = APP_VERSION_BETA_2;
  if (!versionCanDo(curVersion, config.version)) return config;
  config.version = curVersion;
  return config;
};
export const migrateStoryPageConfig = (configStr: string) => {
  let config = parseStoryPageConfig(configStr);

  config = beta2(config);
  config = setLatestVersion(config);
  return config;
};
