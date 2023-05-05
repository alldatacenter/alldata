// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { PureLogRoller } from 'common';
import { useUpdate } from 'common/use-hooks';
import { regLog } from 'common/components/pure-log-roller/log-util';
import { transformLog } from 'common/utils';
import commonStore from 'common/stores/common';

const noop = () => {};

export const LogItem = ({ log }: { log: COMMON.LogItem }) => {
  const { content } = log;
  let time = '';
  let level = '';
  let showContent = content;
  if (regLog.LOGSTART.test(content)) {
    const [parent, _time, _level, _params] = regLog.LOGSTART.exec(content);
    const [serviceName] = (_params || '').split(',');
    time = _time;
    level = _level;
    showContent = `[${serviceName}] --- ${content.split(parent).join('')}`;
  }

  const reContent = transformLog(showContent);
  return (
    <div className="log-insight-item">
      <span className="log-item-logtime">{time}</span>
      <span className={`log-item-level ${level.toLowerCase()}`}>{level}</span>
      <pre className="log-item-content" dangerouslySetInnerHTML={{ __html: reContent }} />
    </div>
  );
};

interface IProps {
  logKey: string;
  query: {
    requestId?: string;
    applicationId?: string | number;
  };
  style: Obj;
  [k: string]: any;
}
const SimpleLogRoller = ({ logKey, query = {}, style = {}, ...otherProps }: IProps) => {
  const logsMap = commonStore.useStore((s) => s.logsMap);
  const { fetchLog } = commonStore.effects;
  const { clearLog } = commonStore.reducers;
  const { content } = logsMap[logKey] || { content: [] };

  const [state, updater] = useUpdate({
    rolling: false,
  });

  const logRoller = React.useRef();
  const scrollTo = (to: number) => {
    if (logRoller.current) {
      logRoller.current.scrollTop = to;
    }
  };

  const scrollToTop = () => scrollTo(0);

  const scrollToBottom = React.useCallback(() => scrollTo(999999999), []); // safari下设置过大的数值无效，所以给一个理论上足够大的值

  React.useEffect(() => {
    const { requestId, applicationId } = query;
    let param = {};
    if (requestId && applicationId) {
      param = { requestId, applicationId };
    } else if (requestId) {
      param = { requestId };
    }

    if (param) {
      updater.rolling(true);
      fetchLog({ ...param, logKey }).then(() => {
        if (logRoller.current) {
          scrollToBottom();
          updater.rolling(false);
        }
      });
    }
    return () => clearLog();
  }, [clearLog, fetchLog, logKey, query, scrollToBottom, updater]);

  return (
    <div className="log-viewer" style={style}>
      <PureLogRoller
        ref={logRoller}
        content={content}
        onStartRolling={noop}
        onCancelRolling={noop}
        onGoToTop={scrollToTop}
        onGoToBottom={scrollToBottom}
        rolling={state.rolling}
        searchOnce
        CustomLogItem={LogItem}
        {...otherProps}
      />
    </div>
  );
};

export default SimpleLogRoller;
