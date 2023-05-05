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

declare namespace MONITOR_COMMON_METADATA {
  interface CommonMetaQuery {
    scope: string;
    scopeId: string;
    mode: string;
    appendTags?: 'true' | 'false';
    format?: 'influx';
  }

  interface MetaConstantMap {
    types: {
      [type: string]: {
        aggregations: Array<{
          aggregation: string;
          name: string;
          result_type: string;
        }>;
        operations: Array<{
          operation: string;
          name: string;
        }>;
      };
    };
    filters: Array<{
      operation: string;
      name: string;
    }>;
  }

  type MetaMetrics = Array<{
    metric: string;
    name: string;
    filters: Array<{
      tag: string;
      op: string;
      value: string;
    }>;
    fields: Array<{
      key: string;
      name: string;
      type: string;
      unit: string;
    }>;
    tags: Array<{
      key: string;
      name: string;
      values?: Array<{
        value: string;
        name: string;
      }>;
    }>;
  }>;

  interface MetaData {
    meta: MetaConstantMap;
    id: string;
    metrics: MetaMetrics;
  }
}
