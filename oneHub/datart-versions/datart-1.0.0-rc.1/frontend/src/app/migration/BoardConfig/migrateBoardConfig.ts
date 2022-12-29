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
import { MIN_MARGIN, MIN_PADDING } from 'app/pages/DashBoardPage/constants';
import { BoardTypes } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { BoardConfig } from 'app/pages/DashBoardPage/types/boardTypes';
import {
  getInitBoardConfig,
  getInitBoardConfigBeta3,
} from 'app/pages/DashBoardPage/utils/board';
import {
  APP_VERSION_BETA_0,
  APP_VERSION_BETA_2,
  APP_VERSION_BETA_4,
} from '../constants';
import { setLatestVersion, versionCanDo } from '../utils';

export const parseBoardConfig = (boardConfig: string) => {
  try {
    let nextConfig = JSON.parse(boardConfig);
    if (!BoardTypes.includes(nextConfig?.type)) {
      return getInitBoardConfigBeta3('auto');
    }
    return nextConfig;
  } catch (error) {
    console.log('解析 config 出错');
    let nextConfig = getInitBoardConfigBeta3('auto');
    return nextConfig;
  }
};

export const beta0 = config => {
  if (!versionCanDo(APP_VERSION_BETA_0, config.version)) return config;
  // 1. initialQuery 新增属性 检测没有这个属性就设置为 true,如果已经设置为false，则保持false
  if (!config.hasOwnProperty('initialQuery')) {
    config.initialQuery = true;
  }

  // 2.1 新增移动端属性 mobileMargin
  if (!config?.mobileMargin) {
    config.mobileMargin = [MIN_MARGIN, MIN_MARGIN];
  }
  // 2.2 新增移动端属性 mobileContainerPadding
  if (!config?.mobileContainerPadding) {
    config.mobileContainerPadding = [MIN_PADDING, MIN_PADDING];
  }
  // 3 QueryButton and ResetButton
  config.hasQueryControl = Boolean(config.hasQueryControl);
  config.hasResetControl = Boolean(config.hasQueryControl);

  // reset config.version
  config.version = APP_VERSION_BETA_0;
  return config;
};

export const beta2 = config => {
  if (!versionCanDo(APP_VERSION_BETA_2, config.version)) return config;
  if (!config.allowOverlap) {
    config.allowOverlap = false;
  }
  config.version = APP_VERSION_BETA_2;
  return config;
};

export const beta4 = (config: any) => {
  if (!versionCanDo(APP_VERSION_BETA_4, config.version)) return config;

  if (config.type === 'auto') {
    let newConfig: BoardConfig = config.jsonConfig
      ? config
      : getInitBoardConfig('auto');
    if (config.background || config.initialQuery) {
      newConfig.jsonConfig.props.forEach(item => {
        if (item.key === 'basic') {
          item!.rows!.forEach(row => {
            if (row.key === 'initialQuery') {
              row.value = config.initialQuery;
            }
            if (row.key === 'allowOverlap') {
              row.value = config.allowOverlap;
            }
          });
        }
        if (item.key === 'background') {
          if (item?.rows?.[0]?.default) {
            item.rows[0].value = config.background;
          }
        }
        if (item.key === 'space') {
          item!.rows!.forEach(row => {
            if (row.key === 'paddingTB') {
              row.value = config.containerPadding[1];
            }
            if (row.key === 'paddingLR') {
              row.value = config.containerPadding[0];
            }
            if (row.key === 'marginTB') {
              row.value = config.margin[1];
            }
            if (row.key === 'marginLR') {
              row.value = config.margin[0];
            }
          });
        }
        if (item.key === 'mSpace') {
          item!.rows!.forEach(row => {
            if (row.key === 'paddingTB') {
              row.value = config.mobileContainerPadding[0];
            }
            if (row.key === 'paddingLR') {
              row.value = config.mobileContainerPadding[1];
            }
            if (row.key === 'marginTB') {
              row.value = config.mobileMargin[0];
            }
            if (row.key === 'marginLR') {
              row.value = config.mobileMargin[1];
            }
          });
        }
      });
    }
    return newConfig;
  } else {
    let newConfig: BoardConfig = config.jsonConfig
      ? config
      : getInitBoardConfig('free');
    if (config.background || config.initialQuery) {
      newConfig.jsonConfig.props.forEach(item => {
        if (item.key === 'basic') {
          item!.rows!.forEach(row => {
            if (row.key === 'initialQuery') {
              row.value = config.initialQuery;
            }
            if (row.key === 'scaleMode') {
              row.value = config.scaleMode;
            }
          });
        }
        if (item.key === 'size') {
          item!.rows!.forEach(row => {
            if (row.key === 'width') {
              row.value = config.width;
            }
            if (row.key === 'height') {
              row.value = config.height;
            }
          });
        }
        if (item.key === 'background') {
          if (item?.rows?.[0]?.default) {
            item.rows[0].value = config.background;
          }
        }
      });
    }
    return newConfig;
  }
};
export const migrateBoardConfig = (boardConfig: string) => {
  let config = parseBoardConfig(boardConfig);
  config = beta0(config);
  config = beta2(config);
  config = beta4(config);
  config = setLatestVersion(config);
  return config as BoardConfig;
};
