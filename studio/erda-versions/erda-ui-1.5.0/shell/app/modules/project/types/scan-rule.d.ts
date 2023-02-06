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

declare namespace SCAN_RULE {
  interface IAppendedQuery {
    scopeId: string;
    scopeType: string;
  }

  interface IDeleteBody {
    id: number | string;
    scopeId: string;
    scopeType: string;
  }

  interface IBatchDeleteBody {
    scopeType: string;
    scopeId: string;
    ids: number[];
  }

  interface METRIC {
    description: string;
    metricKeyId: number;
    metricValue: string;
  }

  interface IBatchInsertBody {
    scopeType: string;
    scopeId: string;
    metrics: any[];
  }

  interface IUpdateBody {
    id: number | string;
    description: string;
    metricValue: string;
    scopeType: string;
    scopeId: string;
  }

  interface IOptionalQuery {
    scopeId: string;
    scopeType: string;
  }

  interface AppendedItem {
    id: number | string;
    description: string;
    scopeType: string;
    scopeId: string;
    metricKeyDesc: string;
    metricKey: string;
    metricKeyId: number;
    operational: string;
    decimalScale: string;
    metricValue: string;
    valueType: string;
  }
}
