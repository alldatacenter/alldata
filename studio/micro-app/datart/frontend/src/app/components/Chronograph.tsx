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

import { Badge, BadgeProps } from 'antd';
import moment from 'moment';
import { useCallback, useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { FONT_SIZE_LABEL } from 'styles/StyleConstants';

interface ChronographProps {
  running: boolean;
  status: BadgeProps['status'];
}

export function Chronograph({ running, status }: ChronographProps) {
  const [label, setLabel] = useState('00:00:00.00');
  const intervalRef = useRef<ReturnType<typeof setInterval>>();

  const clear = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = void 0;
    }
  }, []);

  useEffect(() => {
    if (running) {
      const start = Number(new Date());
      intervalRef.current = setInterval(() => {
        const current = Number(new Date());
        setLabel(
          moment(current - start)
            .utc()
            .format('HH:mm:ss.SS'),
        );
      }, 10);
    } else {
      clear();
    }
    return clear;
  }, [running, clear]);

  return <StyledBadge status={status} text={label} />;
}

const StyledBadge = styled(Badge)`
  .ant-badge-status-text {
    font-size: ${FONT_SIZE_LABEL};
    color: ${p => p.theme.textColorLight};
  }
`;
