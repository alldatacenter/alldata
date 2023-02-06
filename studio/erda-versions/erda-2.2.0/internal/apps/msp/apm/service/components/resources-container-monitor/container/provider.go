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

package container

import (
	"context"
	"fmt"
	"strconv"
	"time"

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
	cpu     string = "cpuUsage"
	memory  string = "memoryUsage"
	diskIO  string = "diskioUsage"
	network string = "networkUsage"
)

type provider struct {
	impl.DefaultLineGraph
	custom.ServiceInParams
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

func (p *provider) getCpuLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT avg(cpu_usage_percent::field),tostring('usage rate') " +
		"FROM docker_container_summary " +
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
		value := row.Values[1].GetNumberValue()
		dimension := row.Values[2].GetStringValue()
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     value,
			Dimension: dimension,
		})
	}
	return metadata, nil
}

func (p *provider) getMemoryLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(avg(mem_limit::field), 2),round_float(avg(mem_usage::field), 2) " +
		"FROM docker_container_summary " +
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
		maxValue := row.Values[1].GetNumberValue()
		usedValue := row.Values[2].GetNumberValue()
		maxDimension := "max"
		usedDimension := "used"
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

func (p *provider) getDiskIoLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(diffps(blk_read_bytes::field), 2),round_float(diffps(blk_write_bytes::field), 2) " +
		"FROM docker_container_summary " +
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
	for i, row := range rows {
		timeFormat := ""
		if i == 0 {
			timeFormat = row.Values[0].GetStringValue()
		} else {
			timestampNano := row.Values[0].GetNumberValue()
			timeFormat = time.Unix(0, int64(timestampNano)).Format("2006-01-02T15:04:05Z")
		}

		readValue := row.Values[1].GetNumberValue()
		writeValue := row.Values[2].GetNumberValue()
		readDimension := "read"
		writeDimension := "write"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(readValue/1024, 2),
			Dimension: readDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(writeValue/1024, 2),
			Dimension: writeDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getNetworkLineGraph(ctx context.Context, startTime, endTime int64, tenantId, instanceId, serviceId string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(diffps(rx_bytes::field), 2),round_float(diffps(tx_bytes::field), 2) " +
		"FROM docker_container_summary " +
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
	for i, row := range rows {
		timeFormat := ""
		if i == 0 {
			timeFormat = row.Values[0].GetStringValue()
		} else {
			timestampNano := row.Values[0].GetNumberValue()
			timeFormat = time.Unix(0, int64(timestampNano)).Format("2006-01-02T15:04:05Z")
		}
		readValue := row.Values[1].GetNumberValue()
		writeValue := row.Values[2].GetNumberValue()
		readDimension := "accept"
		writeDimension := "send"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(readValue/1024, 2),
			Dimension: readDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(writeValue/1024, 2),
			Dimension: writeDimension,
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
		case cpu:
			graph, err := p.getCpuLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, cpu, structure.String, "rateUnit", graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case memory:
			graph, err := p.getMemoryLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, memory, structure.Storage, structure.KB, graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case diskIO:
			graph, err := p.getDiskIoLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, diskIO, structure.TrafficRate, structure.KBSlashS, graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case network:
			graph, err := p.getNetworkLineGraph(sdk.Ctx, startTime, endTime, tenantId, instanceId, serviceId)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, network, structure.TrafficRate, structure.KBSlashS, graph)
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
	cpregister.RegisterProviderComponent("resources-container-monitor", "container", &provider{})
}
