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

	"google.golang.org/protobuf/types/known/structpb"

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
	jvmMemoryHeap          string = "jvm_memory_heap"
	jvmMemoryNonHeap       string = "jvm_memory_non_heap"
	jvmMemoryEdenSpace     string = "jvm_memory_eden_space"
	jvmMemorySurvivorSpace string = "jvm_memory_survivor_space"
	jvmMemoryOldGen        string = "jvm_memory_old_gen"
	jvmGcCount             string = "jvm_gc_count"
	jvmGcTime              string = "jvm_gc_time"
	jvmClassLoader         string = "jvm_class_loader"
	jvmThread              string = "jvm_thread"
)

type provider struct {
	impl.DefaultLineGraph
	custom.ServiceInParams
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

func (p *provider) getMemoryHeapLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(avg(committed::field), 2),round_float(avg(init::field), 2),round_float(max(max::field), 2),round_float(avg(used::field), 2) " +
		"FROM jvm_memory " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"AND name::tag=$name " +
		"GROUP BY time()")
	queryParams := model.ToQueryParams(tenantId, serviceId, instanceId)
	queryParams["name"] = structpb.NewStringValue("heap_memory")

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
		committedValue := row.Values[1].GetNumberValue()
		initValue := row.Values[2].GetNumberValue()
		maxValue := row.Values[3].GetNumberValue()
		usedValue := row.Values[4].GetNumberValue()

		committedDimension := "committed"
		initDimension := "init"
		maxDimension := "max"
		usedDimension := "used"

		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(committedValue/1024, 0),
			Dimension: committedDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(initValue/1024, 0),
			Dimension: initDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(maxValue/1024, 0),
			Dimension: maxDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(usedValue/1024, 0),
			Dimension: usedDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getMemoryNonHeapLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(avg(committed::field), 2),round_float(avg(init::field), 2),round_float(avg(used::field), 2) " +
		"FROM jvm_memory " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"AND name::tag=$name " +
		"GROUP BY time()")
	queryParams := model.ToQueryParams(tenantId, serviceId, instanceId)
	queryParams["name"] = structpb.NewStringValue("non_heap_memory")

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
		committedValue := row.Values[1].GetNumberValue()
		initValue := row.Values[2].GetNumberValue()
		usedValue := row.Values[3].GetNumberValue()

		committedDimension := "committed"
		initDimension := "init"
		usedDimension := "used"

		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(committedValue/1024, 0),
			Dimension: committedDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(initValue/1024, 0),
			Dimension: initDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(usedValue/1024, 0),
			Dimension: usedDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getMemoryEdenSpaceLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(avg(committed::field), 2),round_float(avg(init::field), 2),round_float(max(max::field), 2),round_float(avg(used::field), 2) " +
		"FROM jvm_memory " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"AND name::tag=~/.*_eden_space/ " +
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
		committedValue := row.Values[1].GetNumberValue()
		initValue := row.Values[2].GetNumberValue()
		maxValue := row.Values[3].GetNumberValue()
		usedValue := row.Values[4].GetNumberValue()

		committedDimension := "committed"
		initDimension := "init"
		maxDimension := "max"
		usedDimension := "used"

		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(committedValue/1024, 0),
			Dimension: committedDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(initValue/1024, 0),
			Dimension: initDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(maxValue/1024, 0),
			Dimension: maxDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(usedValue/1024, 0),
			Dimension: usedDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getMemorySurvivorSpaceLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(avg(committed::field), 2),round_float(avg(init::field), 2),round_float(max(max::field), 2),round_float(avg(used::field), 2) " +
		"FROM jvm_memory " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"AND name::tag=~/.*_survivor_space/ " +
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
		committedValue := row.Values[1].GetNumberValue()
		initValue := row.Values[2].GetNumberValue()
		maxValue := row.Values[3].GetNumberValue()
		usedValue := row.Values[4].GetNumberValue()

		committedDimension := "committed"
		initDimension := "init"
		maxDimension := "max"
		usedDimension := "used"

		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(committedValue/1024, 0),
			Dimension: committedDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(initValue/1024, 0),
			Dimension: initDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(maxValue/1024, 0),
			Dimension: maxDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(usedValue/1024, 0),
			Dimension: usedDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getMemoryOldGenLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(avg(committed::field), 2),round_float(avg(init::field), 2),round_float(max(max::field), 2),round_float(avg(used::field), 2) " +
		"FROM jvm_memory " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"AND name::tag=~/.*_old_gen/ " +
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
		committedValue := row.Values[1].GetNumberValue()
		initValue := row.Values[2].GetNumberValue()
		maxValue := row.Values[3].GetNumberValue()
		usedValue := row.Values[4].GetNumberValue()

		committedDimension := "committed"
		initDimension := "init"
		maxDimension := "max"
		usedDimension := "used"

		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(committedValue/1024, 0),
			Dimension: committedDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(initValue/1024, 0),
			Dimension: initDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(maxValue/1024, 0),
			Dimension: maxDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(usedValue/1024, 0),
			Dimension: usedDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getGCCountLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT name::tag,round_float(sum(count::field), 2) " +
		"FROM jvm_gc " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"GROUP BY time(),name::tag")
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
		dimension := row.Values[1].GetStringValue()
		value := row.Values[2].GetNumberValue()
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     value,
			Dimension: dimension,
		})
	}
	return metadata, nil
}

func (p *provider) getGCAvgDurationLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT name::tag,round_float(avg(time::field), 2) " +
		"FROM jvm_gc " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"GROUP BY time(),name::tag")
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
		dimension := row.Values[1].GetStringValue()
		value := row.Values[2].GetNumberValue()
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     value,
			Dimension: dimension,
		})
	}
	return metadata, nil
}

func (p *provider) getClassCountLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(max(loaded::field), 2),round_float(max(unloaded::field), 2) " +
		"FROM jvm_class_loader " +
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
		loadedValue := row.Values[1].GetNumberValue()
		unloadedValue := row.Values[2].GetNumberValue()

		loadedDimension := "loaded"
		unloadedDimension := "unloaded"

		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     loadedValue,
			Dimension: loadedDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     unloadedValue,
			Dimension: unloadedDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getThreadLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT name::tag,round_float(max(state::field), 2) " +
		"FROM jvm_thread " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"AND service_instance_id::tag=$instance_id " +
		"GROUP BY time(),name::tag")
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
		dimension := row.Values[1].GetStringValue()
		value := row.Values[2].GetNumberValue()
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     value,
			Dimension: dimension,
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
		case jvmMemoryHeap:
			graph, err := p.getMemoryHeapLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, jvmMemoryHeap, structure.Storage, structure.KB, graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case jvmMemoryNonHeap:
			graph, err := p.getMemoryNonHeapLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, jvmMemoryNonHeap, structure.Storage, structure.KB, graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case jvmMemoryEdenSpace:
			graph, err := p.getMemoryEdenSpaceLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, jvmMemoryEdenSpace, structure.Storage, structure.KB, graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case jvmMemorySurvivorSpace:
			graph, err := p.getMemorySurvivorSpaceLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, jvmMemorySurvivorSpace, structure.Storage, structure.KB, graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case jvmMemoryOldGen:
			graph, err := p.getMemoryOldGenLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, jvmMemoryOldGen, structure.Storage, structure.KB, graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case jvmGcCount:
			graph, err := p.getGCCountLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, jvmGcCount, structure.String, "countUnit", graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case jvmGcTime:
			graph, err := p.getGCAvgDurationLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, jvmGcTime, structure.Time, "ns", graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case jvmClassLoader:
			graph, err := p.getClassCountLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, jvmClassLoader, structure.String, "pcsUnit", graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case jvmThread:
			graph, err := p.getThreadLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, jvmThread, structure.String, "pcsUnit", graph)
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
	cpregister.RegisterProviderComponent("resources-runtime-monitor-java", "runtime", &provider{})
}
