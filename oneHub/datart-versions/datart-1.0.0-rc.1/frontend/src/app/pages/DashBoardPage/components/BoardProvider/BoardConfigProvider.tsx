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

import { createContext, FC, memo } from 'react';
import { ScaleModeType } from '../../constants';
import { BackgroundConfig, BoardType } from '../../pages/Board/slice/types';
import { BoardConfig } from '../../types/boardTypes';
import { getJsonConfigs } from '../../utils';
export interface BoardConfigValue {
  boardType: BoardType;
  initialQuery: boolean;
  background: BackgroundConfig;
  // free
  width: number;
  height: number;
  scaleMode: ScaleModeType;
  // auto
  allowOverlap: boolean;
  margin: [number, number];
  padding: [number, number];
  mMargin: [number, number];
  mPadding: [number, number];
}
export const BoardConfigContext = createContext<BoardConfig>({} as BoardConfig);
export const BoardConfigValContext = createContext<BoardConfigValue>(
  {} as BoardConfigValue,
);

export const BoardConfigProvider: FC<{
  config: BoardConfig;
  boardId: string;
}> = memo(({ config, boardId, children }) => {
  const props = config.jsonConfig.props;
  const [initialQuery, allowOverlap, scaleMode] = getJsonConfigs(
    props,
    ['basic'],
    ['initialQuery', 'allowOverlap', 'scaleMode'],
  );
  const [background] = getJsonConfigs(props, ['background'], ['background']);
  const [width, height] = getJsonConfigs(props, ['size'], ['width', 'height']);
  const [paddingTB, paddingLR, marginTB, marginLR] = getJsonConfigs(
    props,
    ['space'],
    ['paddingTB', 'paddingLR', 'marginTB', 'marginLR'],
  );
  const [mPaddingTB, mPaddingLR, mMarginTB, mMarginLR] = getJsonConfigs(
    props,
    ['space'],
    ['paddingTB', 'paddingLR', 'marginTB', 'marginLR'],
  );
  const configVal: BoardConfigValue = {
    boardType: config.type,
    initialQuery,
    background,
    width,
    height,
    scaleMode,
    allowOverlap,
    margin: [marginLR, marginTB],
    padding: [paddingLR, paddingTB],
    mMargin: [mMarginLR, mMarginTB],
    mPadding: [mPaddingLR, mPaddingTB],
  };
  return (
    <BoardConfigContext.Provider value={config}>
      <BoardConfigValContext.Provider value={configVal}>
        {children}
      </BoardConfigValContext.Provider>
    </BoardConfigContext.Provider>
  );
});
