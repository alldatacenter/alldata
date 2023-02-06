// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package table

import (
	"context"

	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
)

type Column struct {
	Key      string
	Name     string
	Sortable bool
}

type Cell struct {
	Key   string
	Value interface{}
}

type Row interface {
	GetCells() []*Cell
}

type Table struct {
	Total   float64
	Columns []*Column
	Rows    []Row
}

type Builder interface {
	GetTable(ctx context.Context) (*Table, error)
	GetBaseBuildParams() *BaseBuildParams
}

type BaseBuildParams struct {
	StartTime int64
	EndTime   int64
	TenantId  string
	ServiceId string
	Layer     common.TransactionLayerType
	LayerPath string
	FuzzyPath bool
	OrderBy   []*common.Sort
	PageSize  int
	PageNo    int
	Metric    metricpb.MetricServiceServer
}
