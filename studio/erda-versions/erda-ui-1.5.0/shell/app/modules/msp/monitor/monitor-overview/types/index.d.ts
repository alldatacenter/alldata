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

declare namespace MONITOR_OVERVIEW {
  interface IChartQuery {
    avg?: string;
    sumCpm?: string;
    end: number;
    start: number;
    filter_target_terminus_key?: string;
    range?: string;
    ranges?: string;
    filter_tk?: string;
    rangeSize?: number;
    split?: number;
    cpm?: string;
  }

  interface IAIData {
    webAvg: string;
    webCpm: number;
  }
  interface IBIData {
    apdex: number;
    ajaxResp: string;
    ajaxErr?: number;
    ajaxCpm: number;
  }
}
