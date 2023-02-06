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

package runtime

import (
	"context"
	"fmt"
	"strconv"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	structure "github.com/erda-project/erda-infra/providers/component-protocol/components/commodel/data-structure"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/linegraph/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/common/custom"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/common/model"
	"github.com/erda-project/erda/pkg/math"
)

const (
	nodejsMemoryHeap    string = "nodejs_memory_heap"
	nodejsMemoryNonHeap string = "nodejs_memory_non_heap"
	nodejsCluster       string = "nodejs_cluster"
	nodejsAsyncResource string = "nodejs_async_resource"
)

type provider struct {
	impl.DefaultLineGraph
	custom.ServiceInParams
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

func (p *provider) getMemoryHeapLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(avg(heap_total::field), 2),round_float(avg(heap_used::field), 2),round_float(max(max::field), 2) " +
		"FROM nodejs_memory " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"GROUP BY time()")
	queryParams := model.ToQueryParams(tenantId, serviceId, instanceId)
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(startTime, 10),
		End:       strconv.FormatInt(endTime, 10),
		Statement: statement,
		Params:    queryParams,
	}
	resp, err := p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, err
	}
	rows := resp.Results[0].Series[0].Rows
	var metadata []*model.LineGraphMetaData
	for _, row := range rows {
		timeFormat := row.Values[0].GetStringValue()
		totalValue := row.Values[1].GetNumberValue()
		usedValue := row.Values[2].GetNumberValue()
		maxValue := row.Values[3].GetNumberValue()
		totalDimension := "heap_total"
		usedDimension := "heap_used"
		maxDimension := "max"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(totalValue/1024, 0),
			Dimension: totalDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(usedValue/1024, 0),
			Dimension: usedDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(maxValue/1024, 0),
			Dimension: maxDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getMemoryNonHeapLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(avg(external::field), 2) " +
		"FROM nodejs_memory " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"GROUP BY time()")
	queryParams := model.ToQueryParams(tenantId, serviceId, instanceId)

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(startTime, 10),
		End:       strconv.FormatInt(endTime, 10),
		Statement: statement,
		Params:    queryParams,
	}
	resp, err := p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, err
	}
	rows := resp.Results[0].Series[0].Rows
	var metadata []*model.LineGraphMetaData
	for _, row := range rows {
		timeFormat := row.Values[0].GetStringValue()
		externalValue := row.Values[1].GetNumberValue()
		externalDimension := "external"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(externalValue/1024, 0),
			Dimension: externalDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getClusterCountLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(sum(count::field), 2) " +
		"FROM nodejs_cluster " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"GROUP BY time()")
	queryParams := model.ToQueryParams(tenantId, serviceId, instanceId)

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(startTime, 10),
		End:       strconv.FormatInt(endTime, 10),
		Statement: statement,
		Params:    queryParams,
	}
	resp, err := p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, err
	}
	rows := resp.Results[0].Series[0].Rows
	var metadata []*model.LineGraphMetaData
	for _, row := range rows {
		timeFormat := row.Values[0].GetStringValue()
		countValue := row.Values[1].GetNumberValue()
		countDimension := "count"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     countValue,
			Dimension: countDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getAsyncResourcesLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(sum(count::field), 2) " +
		"FROM nodejs_async_resource " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"GROUP BY time()")
	queryParams := model.ToQueryParams(tenantId, serviceId, instanceId)

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(startTime, 10),
		End:       strconv.FormatInt(endTime, 10),
		Statement: statement,
		Params:    queryParams,
	}
	resp, err := p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, err
	}
	rows := resp.Results[0].Series[0].Rows
	var metadata []*model.LineGraphMetaData
	for _, row := range rows {
		timeFormat := row.Values[0].GetStringValue()
		countValue := row.Values[1].GetNumberValue()
		countDimension := "count"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     countValue,
			Dimension: countDimension,
		})
	}
	return metadata, nil
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		startTime := p.ServiceInParams.InParamsPtr.StartTime
		endTime := p.ServiceInParams.InParamsPtr.EndTime
		tenantId := p.ServiceInParams.InParamsPtr.TenantId
		serviceId := p.ServiceInParams.InParamsPtr.ServiceId
		instanceId := p.ServiceInParams.InParamsPtr.InstanceId

		switch sdk.Comp.Name {
		case nodejsMemoryHeap:
			graph, err := p.getMemoryHeapLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, nodejsMemoryHeap, structure.Storage, structure.KB, graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case nodejsMemoryNonHeap:
			graph, err := p.getMemoryNonHeapLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, nodejsMemoryNonHeap, structure.Storage, structure.KB, graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case nodejsCluster:
			graph, err := p.getClusterCountLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, nodejsCluster, structure.String, "pcsUnit", graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case nodejsAsyncResource:
			graph, err := p.getAsyncResourcesLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, nodejsAsyncResource, structure.String, "pcsUnit", graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		}
		return &impl.StdStructuredPtr{}
	}
}

// RegisterRenderingOp .
func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

// Provide .
func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p
}

func init() {
	cpregister.RegisterProviderComponent("resources-runtime-monitor-nodejs", "runtime", &provider{})
}
