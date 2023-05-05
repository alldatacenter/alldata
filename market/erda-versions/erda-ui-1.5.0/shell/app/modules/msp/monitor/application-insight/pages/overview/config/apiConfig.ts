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

import { groupHandler, sortHandler } from 'common/utils/chart-utils';

export const ApiMap = {
  sortList: {
    fetchApi: 'ai_web_top10',
    query: { group: 'http_path', avg: 'elapsed_mean', limit: 10, sort: 'avg_elapsed_mean' },
    dataHandler: sortHandler('avg.elapsed_mean'),
  },
  overviewWeb: {
    fetchApi: 'ai_web_avg_time/histogram',
    query: { group: 'http_path', avg: 'elapsed_mean', sort: 'histogram_avg_elapsed_mean', limit: 5 },
    dataHandler: groupHandler('avg.elapsed_mean'),
  },
  overviewCpm: {
    fetchApi: 'ai_web_cpm/histogram',
    query: { group: 'http_path', sumCpm: 'elapsed_count', sort: 'histogram_sumCpm_elapsed_count', limit: 5 },
    dataHandler: groupHandler('sumCpm.elapsed_count'),
  },
};
