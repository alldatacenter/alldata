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

declare namespace COMMON {
  interface LogItem {
    source: string;
    id: string;
    stream: string;
    timeBucket: string;
    timestamp: DOMHighResTimeStamp;
    offset: string;
    content: string;
    level: string;
    requestId: string;
  }

  interface LOG {
    content: COMMON.LogItem[]; // 内容
    emptyTimes: number; // 为空的次数
    fetchPeriod: number; // 拉取间隔
  }

  interface SlideComp {
    getTitle(): string | JSX.Element;
    getComp(): React.ElementType | JSX.Element | null;
  }
}
