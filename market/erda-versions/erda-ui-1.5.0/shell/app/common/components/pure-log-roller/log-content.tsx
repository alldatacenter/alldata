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

import { map } from 'lodash';
import React from 'react';
import moment from 'moment';
import { transformLog } from 'common/utils';
import './log-content.scss';

interface ILogItem {
  content: string;
  timestamp: DOMHighResTimeStamp;
}

interface IItemProps {
  log: ILogItem;
  transformContent?: (content: string) => { content: string; suffix: any };
}
const DefaultLogItem = ({ log, transformContent }: IItemProps) => {
  const { content, timestamp } = log;
  let reContent = transformLog(content);
  let suffix = null;
  if (typeof transformContent === 'function') {
    const result = transformContent(reContent);
    reContent = result.content;
    suffix = result.suffix;
  }
  return (
    <div className="log-item">
      <span className="log-item-logtime">
        {moment(`${timestamp}`.length < 13 ? timestamp * 1000 : timestamp / 1000000).format('YYYY-MM-DD HH:mm:ss')}
      </span>
      <pre className="log-item-content" dangerouslySetInnerHTML={{ __html: reContent }} />
      {suffix || null}
    </div>
  );
};

interface IProps {
  logs: ILogItem[];
  CustomLogItem?: (content: string) => string;
}
const LogContent = ({ logs, CustomLogItem, ...rest }: IProps) => {
  const ItemRender: Function = CustomLogItem || DefaultLogItem;
  return (
    <div className="log-list-box">
      {map(logs, (log, i) => (
        <ItemRender key={i} log={log} {...rest} />
      ))}
    </div>
  );
};
export default LogContent;
