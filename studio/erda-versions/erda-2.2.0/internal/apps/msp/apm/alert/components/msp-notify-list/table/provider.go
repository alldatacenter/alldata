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
	"encoding/json"
	"math"
	"strconv"
	"strings"
	"time"

	"github.com/ahmetb/go-linq/v3"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/commodel"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-infra/providers/i18n"
	messenger "github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-notify-list/common"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

type provider struct {
	impl.DefaultTable
	Log  logs.Logger
	I18n i18n.Translator `autowired:"i18n" translator:"msp-alert-event-list"`

	Messenger messenger.NotifyServiceServer `autowired:"erda.core.messenger.notify.NotifyService"`
	Monitor   monitor.AlertServiceServer    `autowired:"erda.core.monitor.alert.AlertService"`
}

func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		filters := common.NewConfigurableFilterOptions().GetFromGlobalState(*sdk.GlobalState)
		pageNo, pagSize := common.GetPagingFromGlobalState(*sdk.GlobalState)
		operationData := sdk.Event.OperationData
		clientData, ok := operationData["clientData"]
		dataRef := &common.DataRef{}
		if ok {
			clientData, ok = clientData.(map[string]interface{})
			data, err := json.Marshal(clientData)
			if err != nil {
				p.Log.Error(err)
				(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
				return nil
			}
			err = json.Unmarshal(data, &dataRef)
			if err != nil {
				p.Log.Error(err)
				(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
				return nil
			}
		}
		context := utils.NewContextWithHeader(sdk.Ctx)
		inParams, err := common.ParseFromCpSdk(sdk)
		if err != nil {
			p.Log.Error(err)
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}
		sendTime := make([]string, 0)
		if len(filters.SendTime) > 0 {
			startTime := strconv.Itoa(int(filters.SendTime[0]))
			endTime := strconv.Itoa(int(filters.SendTime[1]))
			sendTime = []string{startTime, endTime}
		}
		notifyHistory := &messenger.QueryAlertNotifyHistoriesRequest{
			ScopeType:  inParams.Scope,
			ScopeID:    inParams.ScopeID,
			NotifyName: filters.NotifyName,
			Status:     filters.Status,
			Channel:    filters.Channel,
			AlertID:    filters.AlertId,
			SendTime:   sendTime,
			PageNo:     int64(pageNo),
			PageSize:   int64(pagSize),
		}
		if dataRef.DataRef != nil && dataRef.DataRef["fieldBindToOrder"].(string) == "SendTime" && dataRef.DataRef["ascOrder"] != nil {
			if dataRef.DataRef["ascOrder"].(bool) == true {
				notifyHistory.TimeOrder = true
			}
		}
		data, err := p.Messenger.QueryAlertNotifyHistories(context, notifyHistory)
		if err != nil {
			p.Log.Error(err)
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}
		alerts, err := p.getAlerts(sdk.Ctx, inParams)
		alertMap := make(map[int64]string)
		for _, v := range alerts {
			alertMap[int64(v.Id.(uint64))] = v.Name
		}
		t := &table.Table{
			Columns: table.ColumnsInfo{
				Merges: nil,
				Orders: []table.ColumnKey{"NotifyName", "Status", "Channel", "LinkedStrategy", "SendTime"},
				ColumnsMap: map[table.ColumnKey]table.Column{
					"NotifyName": {
						Title: sdk.I18n("NotifyName"),
					},
					"Status": {
						Title: sdk.I18n("Status"),
					},
					"Channel": {
						Title: sdk.I18n("Channel"),
					},
					"LinkedStrategy": {
						Title: sdk.I18n("LinkedStrategy"),
					},
					"SendTime": {
						Title:            sdk.I18n("SendTime"),
						EnableSort:       true,
						FieldBindToOrder: "SendTime",
					},
				},
			},
			PageNo:   uint64(pageNo),
			PageSize: uint64(pagSize),
			Total:    uint64(data.Data.Total),
		}
		notifyAttributes := common.NotifyAttributes{}
		for _, item := range data.Data.List {
			if item.Attributes != "" {
				err := json.Unmarshal([]byte(item.Attributes), &notifyAttributes)
				if err != nil {
					p.Log.Error(err)
					(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
					return nil
				}
			}
			sdk.I18n(item.Status)
			str := strings.TrimLeft(item.NotifyName, "【")
			str = strings.TrimRight(str, "】\n")
			status := "success"
			if item.Status == "failed" {
				status = "error"
			}
			sendTime := time.Unix(item.SendTime.GetSeconds(), int64(item.SendTime.GetNanos())).Format("2006/01/02 15:04:05")
			t.Rows = append(t.Rows, table.Row{
				ID: table.RowID(strconv.Itoa(int(item.Id))),
				CellsMap: map[table.ColumnKey]table.Cell{
					"NotifyName": table.NewTextCell(str).Build(),
					"Status": table.NewCompleteTextCell(commodel.Text{
						Text:   sdk.I18n(item.Status),
						Status: commodel.UnifiedStatus(status),
					}).Build(),
					"Channel":        table.NewTextCell(sdk.I18n(item.Channel)).Build(),
					"LinkedStrategy": table.NewTextCell(alertMap[notifyAttributes.AlertId]).Build(),
					"SendTime":       table.NewTextCell(sendTime).Build(),
				},
			})
		}
		p.StdDataPtr = &table.Data{
			Table: *t,
			Operations: map[cptype.OperationKey]cptype.Operation{
				table.OpTableChangePage{}.OpKey(): cputil.NewOpBuilder().WithServerDataPtr(&table.OpTableChangePageServerData{}).Build(),
				table.OpTableChangeSort{}.OpKey(): cputil.NewOpBuilder().Build(),
			},
		}
		return nil
	}
}

func (p *provider) getAlerts(ctx context.Context, params *common.InParams) ([]*common.IdNameValue, error) {
	resp, err := p.Monitor.QueryAlert(ctx, &monitor.QueryAlertRequest{
		Scope:    params.Scope,
		ScopeId:  params.ScopeID,
		PageSize: math.MaxInt64,
		PageNo:   1,
	})
	if err != nil {
		return nil, err
	}

	var values []*common.IdNameValue
	linq.From(resp.Data.List).Select(func(i interface{}) interface{} {
		return &common.IdNameValue{
			Name: i.(*monitor.Alert).Name,
			Id:   i.(*monitor.Alert).Id,
		}
	}).ToSlice(&values)

	return values, nil
}

func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func (p *provider) RegisterTablePagingOp(opData table.OpTableChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		common.SetPagingToGlobalState(*sdk.GlobalState, opData.ClientData)
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterTableChangePageOp(opData table.OpTableChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		common.SetPagingToGlobalState(*sdk.GlobalState, opData.ClientData)
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterTableSortOp(opData table.OpTableChangeSort) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		common.SetSortsToGlobalState(*sdk.GlobalState, opData.ClientData)
		p.RegisterRenderingOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterBatchRowsHandleOp(opData table.OpBatchRowsHandle) (opFunc cptype.OperationFunc) {
	return nil
}
func (p *provider) RegisterRowSelectOp(opData table.OpRowSelect) (opFunc cptype.OperationFunc) {
	return nil
}

func (p *provider) RegisterRowAddOp(opData table.OpRowAdd) (opFunc cptype.OperationFunc) {
	return nil
}

func (p *provider) RegisterRowEditOp(opData table.OpRowEdit) (opFunc cptype.OperationFunc) {
	return nil
}

func (p *provider) RegisterRowDeleteOp(opData table.OpRowDelete) (opFunc cptype.OperationFunc) {
	return nil
}

// Provide .
func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p
}

func init() {
	cpregister.RegisterProviderComponent(common.ScenarioKey, common.ComponentNameTable, &provider{})
}
