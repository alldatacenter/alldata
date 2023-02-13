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

package node

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
	cpu      string = "cpu"
	memory   string = "memory"
	load     string = "load"
	podCount string = "podCount"
	disk     string = "disk"
	network  string = "network"
)

type provider struct {
	impl.DefaultLineGraph
	custom.ServiceInParams
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

func (p *provider) getCpuLineGraph(ctx context.Context, startTime, endTime int64, hostIp string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(cpu_usage_active::field, 2) " +
		"FROM host_summary " +
		"WHERE host_ip::tag=$host_ip " +
		"GROUP BY time()")
	queryParams := map[string]*structpb.Value{"host_ip": structpb.NewStringValue(hostIp)}

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
		dimension := "usage rate"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(value, 2),
			Dimension: dimension,
		})
	}
	return metadata, nil
}

func (p *provider) getMemoryLineGraph(ctx context.Context, startTime, endTime int64, hostIp string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(mem_used_percent::field, 2) " +
		"FROM host_summary " +
		"WHERE host_ip::tag=$host_ip " +
		"GROUP BY time()")
	queryParams := map[string]*structpb.Value{"host_ip": structpb.NewStringValue(hostIp)}

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
		dimension := "usage rate"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(value, 2),
			Dimension: dimension,
		})
	}
	return metadata, nil
}

func (p *provider) getLoadLineGraph(ctx context.Context, startTime, endTime int64, hostIp string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT avg(load1::field),avg(load5::field),avg(load15::field) " +
		"FROM host_summary " +
		"WHERE host_ip::tag=$host_ip " +
		"GROUP BY time()")
	queryParams := map[string]*structpb.Value{"host_ip": structpb.NewStringValue(hostIp)}

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
		load1Value := math.DecimalPlacesWithDigitsNumber(row.Values[1].GetNumberValue(), 2)
		load5Value := math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue(), 2)
		load15Value := math.DecimalPlacesWithDigitsNumber(row.Values[3].GetNumberValue(), 2)
		load1Dimension := "load1"
		load5Dimension := "load5"
		load15Dimension := "load15"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     load1Value,
			Dimension: load1Dimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     load5Value,
			Dimension: load5Dimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     load15Value,
			Dimension: load15Dimension,
		})
	}
	return metadata, nil
}

func (p *provider) getPodCountLineGraph(ctx context.Context, startTime, endTime int64, hostIp string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT max(task_containers::field) " +
		"FROM host_summary " +
		"WHERE host_ip::tag=$host_ip " +
		"GROUP BY time()")
	queryParams := map[string]*structpb.Value{"host_ip": structpb.NewStringValue(hostIp)}

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
		value := math.DecimalPlacesWithDigitsNumber(row.Values[1].GetNumberValue(), 2)
		dimension := "pod count"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     value,
			Dimension: dimension,
		})
	}
	return metadata, nil
}

func (p *provider) getDiskIoLineGraph(ctx context.Context, startTime, endTime int64, hostIp string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(write_rate::field, 2),round_float(read_rate::field, 2) " +
		"FROM diskio " +
		"WHERE host_ip::tag=$host_ip " +
		"GROUP BY time()")
	queryParams := map[string]*structpb.Value{"host_ip": structpb.NewStringValue(hostIp)}

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
		writeValue := row.Values[1].GetNumberValue()
		readValue := row.Values[2].GetNumberValue()
		writeDimension := "write"
		readDimension := "read"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(writeValue/1024, 2),
			Dimension: writeDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(readValue/1024, 2),
			Dimension: readDimension,
		})
	}
	return metadata, nil
}

func (p *provider) getNetworkLineGraph(ctx context.Context, startTime, endTime int64, hostIp string) ([]*model.LineGraphMetaData, error) {
	statement := fmt.Sprintf("SELECT round_float(send_rate::field, 2),round_float(recv_rate::field, 2) " +
		"FROM net " +
		"WHERE host_ip::tag=$host_ip AND interface::tag='eth0' " +
		"GROUP BY time()")
	queryParams := map[string]*structpb.Value{"host_ip": structpb.NewStringValue(hostIp)}

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
		sendValue := row.Values[1].GetNumberValue()
		recvValue := row.Values[2].GetNumberValue()
		sendDimension := "send"
		recvDimension := "recv"
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(sendValue/1024, 2),
			Dimension: sendDimension,
		})
		metadata = append(metadata, &model.LineGraphMetaData{
			Time:      timeFormat,
			Value:     math.DecimalPlacesWithDigitsNumber(recvValue/1024, 2),
			Dimension: recvDimension,
		})
	}
	return metadata, nil
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		startTime := p.ServiceInParams.InParamsPtr.StartTime
		endTime := p.ServiceInParams.InParamsPtr.EndTime
		hostIp := p.ServiceInParams.InParamsPtr.HostIp
		switch sdk.Comp.Name {
		case cpu:
			graph, err := p.getCpuLineGraph(sdk.Ctx, startTime, endTime, hostIp)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, cpu, structure.String, "rateUnit", graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case memory:
			graph, err := p.getMemoryLineGraph(sdk.Ctx, startTime, endTime, hostIp)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, memory, structure.String, "rateUnit", graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case load:
			graph, err := p.getLoadLineGraph(sdk.Ctx, startTime, endTime, hostIp)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, load, structure.String, "", graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case podCount:
			graph, err := p.getPodCountLineGraph(sdk.Ctx, startTime, endTime, hostIp)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, podCount, structure.String, "pcsUnit", graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case disk:
			graph, err := p.getDiskIoLineGraph(sdk.Ctx, startTime, endTime, hostIp)
			if err != nil {
				return nil
			}
			line := model.HandleLineGraphMetaData(sdk.Lang, p.I18n, disk, structure.TrafficRate, structure.KBSlashS, graph)
			return &impl.StdStructuredPtr{StdDataPtr: line}
		case network:
			graph, err := p.getNetworkLineGraph(sdk.Ctx, startTime, endTime, hostIp)
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
	cpregister.RegisterProviderComponent("resources-node-monitor", "node", &provider{})
}
