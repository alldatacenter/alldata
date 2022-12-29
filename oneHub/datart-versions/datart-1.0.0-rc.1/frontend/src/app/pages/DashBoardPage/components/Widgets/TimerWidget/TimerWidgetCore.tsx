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

import { TIME_FORMATTER } from 'globalConstants';
import moment from 'moment';
import { memo, useContext, useEffect, useState } from 'react';
import styled from 'styled-components/macro';
import { IFontDefault } from '../../../../../../types';
import { WidgetContext } from '../../WidgetProvider/WidgetProvider';
import { timerWidgetToolkit } from './timerConfig';

export const TimerWidgetCore: React.FC = memo(() => {
  const widget = useContext(WidgetContext);
  const { time, font } = timerWidgetToolkit.getTimer(
    widget.config.customConfig.props,
  );
  const [currentTime, setCurrentTime] = useState(
    moment().format(time?.format || TIME_FORMATTER),
  );

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(moment().format(time?.format || TIME_FORMATTER));
    }, time?.duration);
    return () => {
      clearInterval(timer);
    };
  }, [time]);

  return (
    <Wrapper {...font}>
      <div className="time-text">{currentTime}</div>
    </Wrapper>
  );
});

const Wrapper = styled.div<IFontDefault>`
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  width: 100%;
  overflow-y: hidden;
  .time-text {
    display: flex;
    flex: 1;
    justify-content: center;

    font-family: ${p => p?.fontFamily};
    font-size: ${p => p?.fontSize}px;
    font-style: ${p => p?.fontStyle};
    font-weight: ${p => p?.fontWeight};
    color: ${p => p?.color};
    text-align: center;
  }
`;
