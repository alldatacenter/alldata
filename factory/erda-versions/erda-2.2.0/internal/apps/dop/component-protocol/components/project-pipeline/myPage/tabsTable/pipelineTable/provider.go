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

package pipelineTable

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/mitchellh/mapstructure"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/commodel"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda-proto-go/core/pipeline/definition/pb"
	commonpb "github.com/erda-project/erda-proto-go/core/pipeline/pb"
	projectpipelinepb "github.com/erda-project/erda-proto-go/dop/projectpipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/util"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/providers/projectpipeline"
	"github.com/erda-project/erda/internal/apps/dop/providers/projectpipeline/deftype"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/limit_sync_group"
	"github.com/erda-project/erda/pkg/strutil"
)

type PipelineTable struct {
	impl.DefaultTable

	bdl      *bundle.Bundle
	gsHelper *gshelper.GSHelper
	sdk      *cptype.SDK
	InParams *InParams      `json:"-"`
	PageNo   uint64         `json:"-"`
	PageSize uint64         `json:"-"`
	Total    uint64         `json:"-"`
	Sorts    []*common.Sort `json:"-"`
	UserIDs  []string       `json:"-"`

	ProjectPipelineSvc *projectpipeline.ProjectPipelineService
	PipelineCron       cronpb.CronServiceServer
}

const (
	ColumnPipelineName    table.ColumnKey = "pipelineName"
	ColumnPipeline        table.ColumnKey = "pipeline"
	ColumnPipelineStatus  table.ColumnKey = "pipelineStatus"
	ColumnCostTime        table.ColumnKey = "costTime"
	ColumnApplicationName table.ColumnKey = "applicationName"
	ColumnBranch          table.ColumnKey = "branch"
	ColumnExecutor        table.ColumnKey = "executor"
	ColumnStartTime       table.ColumnKey = "startTime"
	ColumnCreateTime      table.ColumnKey = "createTime"
	ColumnCreator         table.ColumnKey = "creator"
	ColumnPipelineID      table.ColumnKey = "pipelineID"
	ColumnMoreOperations  table.ColumnKey = "moreOperations"
	ColumnSource          table.ColumnKey = "source"
	ColumnSourceFile      table.ColumnKey = "sourceFile"
	ColumnProcess         table.ColumnKey = "process"
	ColumnIcon            table.ColumnKey = "icon"

	StateKeyTransactionPaging = "paging"
	StateKeyTransactionSort   = "sort"

	PipelineSourceRemoteAppIndex = 2
)

func (p *PipelineTable) BeforeHandleOp(sdk *cptype.SDK) {
	p.bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	p.gsHelper = gshelper.NewGSHelper(sdk.GlobalState)
	p.sdk = sdk
	var err error
	p.InParams.OrgID, err = strconv.ParseUint(p.sdk.Identity.OrgID, 10, 64)
	if err != nil {
		panic(err)
	}
	p.ProjectPipelineSvc = sdk.Ctx.Value(types.ProjectPipelineService).(*projectpipeline.ProjectPipelineService)
	p.PipelineCron = sdk.Ctx.Value(types.PipelineCronService).(cronpb.CronServiceServer)
	//cputil.MustObjJSONTransfer(&p.StdStatePtr, &p.State)
}

func (p *PipelineTable) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		p.SetPagingFromGlobalState()
		p.SetSortsFromGlobalState()
		p.StdDataPtr = &table.Data{
			Table: table.Table{
				Columns:  p.SetTableColumns(),
				Rows:     p.SetTableRows(),
				PageNo:   p.PageNo,
				PageSize: p.PageSize,
				Total:    p.Total,
			},
			Operations: map[cptype.OperationKey]cptype.Operation{
				table.OpTableChangePage{}.OpKey(): cputil.NewOpBuilder().WithServerDataPtr(&table.OpTableChangePageServerData{}).Build(),
				table.OpTableChangeSort{}.OpKey(): cputil.NewOpBuilder().Build(),
				table.OpBatchRowsHandle{}.OpKey(): cputil.NewOpBuilder().WithText("批量操作").WithServerDataPtr(&table.OpBatchRowsHandleServerData{
					Options: []table.OpBatchRowsHandleOption{
						{
							ID:              "batchRun",
							Text:            cputil.I18n(p.sdk.Ctx, "execute"),
							ForbiddenRowIDs: []string{},
						},
					},
				}).Build(),
			},
		}
		(*sdk.GlobalState)[protocol.GlobalInnerKeyUserIDs.String()] = strutil.DedupSlice(p.UserIDs, true)
		return nil
	}
}

func (p *PipelineTable) SetTableColumns() table.ColumnsInfo {
	return table.ColumnsInfo{
		Merges: map[table.ColumnKey]table.MergedColumn{
			ColumnSource:   {[]table.ColumnKey{ColumnApplicationName, ColumnIcon, ColumnBranch}},
			ColumnPipeline: {[]table.ColumnKey{ColumnPipelineName, ColumnSourceFile}},
		},
		Orders: []table.ColumnKey{ColumnSource, ColumnPipeline, ColumnPipelineStatus, ColumnProcess, ColumnCostTime,
			ColumnExecutor, ColumnStartTime, ColumnCreateTime, ColumnCreator, ColumnPipelineID, ColumnMoreOperations},
		ColumnsMap: map[table.ColumnKey]table.Column{
			ColumnPipelineName:    {Title: cputil.I18n(p.sdk.Ctx, string(ColumnPipelineName))},
			ColumnApplicationName: {Title: cputil.I18n(p.sdk.Ctx, string(ColumnApplicationName))},
			ColumnBranch:          {Title: cputil.I18n(p.sdk.Ctx, string(ColumnBranch))},
			ColumnPipelineStatus:  {Title: cputil.I18n(p.sdk.Ctx, string(ColumnPipelineStatus))},
			ColumnProcess:         {Title: cputil.I18n(p.sdk.Ctx, string(ColumnProcess)), Tip: cputil.I18n(p.sdk.Ctx, "processTip")},
			ColumnCostTime:        {Title: cputil.I18n(p.sdk.Ctx, string(ColumnCostTime)), EnableSort: true},
			ColumnExecutor:        {Title: cputil.I18n(p.sdk.Ctx, string(ColumnExecutor)), Hidden: true},
			ColumnStartTime:       {Title: cputil.I18n(p.sdk.Ctx, string(ColumnStartTime)), EnableSort: true, Hidden: true},
			ColumnMoreOperations:  {Title: cputil.I18n(p.sdk.Ctx, string(ColumnMoreOperations))},
			ColumnCreator:         {Title: cputil.I18n(p.sdk.Ctx, string(ColumnCreator)), Hidden: true},
			ColumnPipelineID:      {Title: cputil.I18n(p.sdk.Ctx, string(ColumnPipelineID)), Hidden: true},
			ColumnCreateTime:      {Title: cputil.I18n(p.sdk.Ctx, string(ColumnCreateTime)), EnableSort: true, Hidden: true},
			ColumnSource:          {Title: cputil.I18n(p.sdk.Ctx, string(ColumnSource))},
			ColumnSourceFile:      {Title: cputil.I18n(p.sdk.Ctx, string(ColumnSourceFile))},
			ColumnIcon:            {Title: cputil.I18n(p.sdk.Ctx, string(ColumnIcon))},
			ColumnPipeline:        {Title: cputil.I18n(p.sdk.Ctx, string(ColumnPipeline))},
		},
	}
}

func (p *PipelineTable) SetTableRows() []table.Row {
	var descCols, ascCols []string
	for _, v := range p.Sorts {
		field := func() string {
			if v.FieldKey == string(ColumnCostTime) {
				return "cost_time"
			}
			if v.FieldKey == string(ColumnStartTime) {
				return "started_at"
			}
			if v.FieldKey == string(ColumnCreateTime) {
				return "created_at"
			}
			return ""
		}()
		if field == "" {
			continue
		}
		if v.Ascending {
			ascCols = append(ascCols, field)
		} else {
			descCols = append(descCols, field)
		}
	}
	if len(ascCols) == 0 && len(descCols) == 0 {
		descCols = append(descCols, "started_at")
	}

	filter := p.gsHelper.GetGlobalTableFilter()
	list, total, err := p.ProjectPipelineSvc.List(p.sdk.Ctx, deftype.ProjectPipelineList{
		ProjectID: p.InParams.ProjectID,
		AppName: func() []string {
			if !strutil.InSlice(common.Participated, filter.App) {
				return filter.App
			}
			return strutil.DedupSlice(append(filter.App, p.gsHelper.GetGlobalMyAppNames()...), true)
		}(),
		Creator:  filter.Creator,
		Executor: filter.Executor,
		PageNo:   p.PageNo,
		PageSize: p.PageSize,
		Category: nil,
		Name:     filter.Title,
		Ref:      filter.Branch,
		TimeCreated: func() []string {
			timeCreated := make([]string, 0)
			if len(filter.CreatedAtStartEnd) == 2 {
				timeCreated = append(timeCreated, time.Unix(filter.CreatedAtStartEnd[0]/1000, 0).String())
				timeCreated = append(timeCreated, time.Unix(filter.CreatedAtStartEnd[1]/1000, 0).String())
			}
			return timeCreated
		}(),
		TimeStarted: func() []string {
			timeStarted := make([]string, 0)
			if len(filter.StartedAtStartEnd) == 2 {
				timeStarted = append(timeStarted, time.Unix(filter.StartedAtStartEnd[0]/1000, 0).String())
				timeStarted = append(timeStarted, time.Unix(filter.StartedAtStartEnd[1]/1000, 0).String())
			}
			return timeStarted
		}(),
		Status:       filter.Status,
		DescCols:     descCols,
		AscCols:      ascCols,
		CategoryKey:  p.InParams.FrontendPipelineCategoryKey,
		IdentityInfo: apistructs.IdentityInfo{UserID: p.sdk.Identity.UserID},
	})
	if err != nil {
		panic(fmt.Errorf("failed to list project pipeline, err: %s", err.Error()))
	}

	var (
		pipelineYmlNames []string
		pipelineSources  []string
	)

	definitionYmlSourceMap := make(map[string]string)
	for _, v := range list {
		var extraValue = apistructs.PipelineDefinitionExtraValue{}
		err = json.Unmarshal([]byte(v.Extra.Extra), &extraValue)
		if err != nil {
			logrus.Errorf("failed to list Unmarshal Extra, err: %s", err.Error())
		}
		pipelineYmlNames = append(pipelineYmlNames, extraValue.CreateRequest.PipelineYmlName)
		pipelineSources = append(pipelineSources, extraValue.CreateRequest.PipelineSource.String())
		definitionYmlSourceMap[v.ID] = fmt.Sprintf("%s%s", extraValue.CreateRequest.PipelineYmlName, extraValue.CreateRequest.PipelineSource)
	}

	worker := limit_sync_group.NewWorker(2)
	var crons *cronpb.CronPagingResponse
	var appNameIDMap *apistructs.GetAppIDByNamesResponseData

	worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
		if len(pipelineYmlNames) == 0 {
			return nil
		}
		crons, err = p.PipelineCron.CronPaging(context.Background(), &cronpb.CronPagingRequest{
			Sources:  pipelineSources,
			YmlNames: pipelineYmlNames,
			PageSize: int64(len(list)),
			PageNo:   1,
		})
		if err != nil {
			return fmt.Errorf("failed to list PageListPipelineCrons, err: %s", err.Error())
		}
		return nil
	})

	worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
		var appNames []string
		for _, v := range list {
			appName := getApplicationNameFromDefinitionRemote(v.Remote)
			if appName == "" {
				return fmt.Errorf("definition %v remote %v error", v.Name, v.Remote)
			}
			appNames = append(appNames, appName)
		}
		if len(appNames) == 0 {
			return nil
		}
		appNameIDMap, err = p.bdl.GetAppIDByNames(p.InParams.ProjectID, p.sdk.Identity.UserID, appNames)
		if err != nil {
			return err
		}
		return nil
	})
	if worker.Do().Error() != nil {
		panic(worker.Error())
	}

	ymlSourceMapCronMap := make(map[string]*commonpb.Cron)
	if crons != nil {
		for _, v := range crons.Data {
			ymlSourceMapCronMap[fmt.Sprintf("%s%s", v.PipelineYmlName, v.PipelineSource)] = v
		}
	}

	p.Total = uint64(total)
	rows := make([]table.Row, 0, len(list))
	for _, v := range list {
		p.UserIDs = append(p.UserIDs, v.Creator, v.Executor)
		rows = append(rows, table.Row{
			ID:         table.RowID(v.ID),
			Selectable: true,
			Selected:   false,
			CellsMap: map[table.ColumnKey]table.Cell{
				ColumnPipelineName: table.NewTextCell(v.Name).Build(),
				ColumnPipelineStatus: table.NewCompleteTextCell(commodel.Text{
					Text:   util.DisplayStatusText(p.sdk.Ctx, v.Status),
					Status: getStatus(apistructs.PipelineStatus(v.Status)),
				}).Build(),
				ColumnCostTime: table.NewDurationCell(commodel.Duration{
					Value: func() int64 {
						if !apistructs.PipelineStatus(v.Status).IsRunningStatus() &&
							!apistructs.PipelineStatus(v.Status).IsEndStatus() {
							return -1
						}
						return v.CostTime
					}(),
					Tip: func() string {
						if !apistructs.PipelineStatus(v.Status).IsRunningStatus() &&
							!apistructs.PipelineStatus(v.Status).IsEndStatus() {
							return ""
						}
						return fmt.Sprintf("%s : %s",
							cputil.I18n(p.sdk.Ctx, string(ColumnStartTime)),
							formatTimeToStr(v.StartedAt.AsTime()),
						)
					}(),
				}).Build(),
				ColumnProcess: table.NewProgressBarCell(commodel.ProgressBar{
					BarCompletedNum: func() int64 {
						if v.ExecutedActionNum < 0 {
							return 0
						}
						if v.ExecutedActionNum > v.TotalActionNum {
							return v.TotalActionNum
						}
						return v.ExecutedActionNum
					}(),
					BarTotalNum: v.TotalActionNum,
					Text: func() string {
						if !apistructs.PipelineStatus(v.Status).IsRunningStatus() &&
							!apistructs.PipelineStatus(v.Status).IsEndStatus() {
							return ""
						}
						if v.ExecutedActionNum < 0 {
							v.ExecutedActionNum = 0
						}
						if v.ExecutedActionNum > v.TotalActionNum {
							v.ExecutedActionNum = v.TotalActionNum
						}
						return fmt.Sprintf("%d/%d", v.ExecutedActionNum, v.TotalActionNum)
					}(),
					Status: getStatus(apistructs.PipelineStatus(v.Status)),
				}).Build(),
				ColumnApplicationName: table.NewTextCell(getApplicationNameFromDefinitionRemote(v.Remote)).Build(),
				ColumnBranch:          table.NewTextCell(v.Ref).Build(),
				ColumnPipelineID: table.NewTextCell(func() string {
					if v.PipelineID == 0 {
						return "-"
					}
					return strconv.FormatInt(v.PipelineID, 10)
				}()).Build(),
				ColumnExecutor:   table.NewUserCell(commodel.User{ID: v.Executor}).Build(),
				ColumnCreator:    table.NewUserCell(commodel.User{ID: v.Creator}).Build(),
				ColumnStartTime:  table.NewTextCell(formatTimeToStr(v.StartedAt.AsTime())).Build(),
				ColumnCreateTime: table.NewTextCell(formatTimeToStr(v.TimeCreated.AsTime())).Build(),
				ColumnMoreOperations: table.NewMoreOperationsCell(commodel.MoreOperations{
					Ops: p.SetTableMoreOpItem(v, definitionYmlSourceMap, ymlSourceMapCronMap, appNameIDMap),
				}).Build(),
				ColumnSourceFile: table.NewTextCell(func() string {
					return v.FileName
				}()).Build(),
				ColumnIcon: table.NewIconCell(commodel.Icon{
					Type: "branch",
				}).Build(),
			},
			Operations: map[cptype.OperationKey]cptype.Operation{
				table.OpRowSelect{}.OpKey(): func() cptype.Operation {
					build := cputil.NewOpBuilder().Build()
					serviceCnt := make(cptype.OpServerData)
					serviceCnt["id"] = v.ID
					build.ServerData = &serviceCnt
					return build
				}(),
				commodel.OpClick{}.OpKey(): func() cptype.Operation {
					build := cputil.NewOpBuilder().Build()
					build.SkipRender = true

					var inode = ""
					appName := getApplicationNameFromDefinitionRemote(v.Remote)
					if appName != "" && appNameIDMap != nil {
						if v.Path == "" {
							inode = fmt.Sprintf("%v/%v/tree/%v/%v", p.InParams.ProjectID, appNameIDMap.AppNameToID[appName], v.Ref, v.FileName)
						} else {
							inode = fmt.Sprintf("%v/%v/tree/%v/%v/%v", p.InParams.ProjectID, appNameIDMap.AppNameToID[appName], v.Ref, v.Path, v.FileName)
						}
					}
					build.ServerData = &cptype.OpServerData{
						"pipelineID":   v.PipelineID,
						"inode":        base64.URLEncoding.EncodeToString([]byte(inode)),
						"appName":      appName,
						"pipelineName": v.Name,
					}
					return build
				}(),
			},
		})
	}
	return rows
}

func formatTimeToStr(t time.Time) string {
	if t.Unix() <= 0 {
		return "-"
	}
	return t.In(time.FixedZone("UTC+8", int((8 * time.Hour).Seconds()))).Format("2006-01-02 15:04:05")
}

func (p *PipelineTable) SetTableMoreOpItem(definition *pb.PipelineDefinition, definitionYmlSourceMap map[string]string, ymlSourceMapCronMap map[string]*commonpb.Cron, appNameIDMap *apistructs.GetAppIDByNamesResponseData) []commodel.MoreOpItem {
	items := make([]commodel.MoreOpItem, 0)
	build := cputil.NewOpBuilder().Build()

	items = append(items, commodel.MoreOpItem{
		ID: func() string {
			if apistructs.PipelineStatus(definition.Status).IsRunningStatus() {
				return "cancelRun"
			}
			return "run"
		}(),
		Text: cputil.I18n(p.sdk.Ctx, func() string {
			if apistructs.PipelineStatus(definition.Status).IsRunningStatus() {
				return "cancelRun"
			}
			return "run"
		}()),
		Icon: &commodel.Icon{
			Type: func() string {
				if apistructs.PipelineStatus(definition.Status).IsRunningStatus() {
					return "pause"
				}
				return "play"
			}(),
		},

		Operations: map[cptype.OperationKey]cptype.Operation{
			commodel.OpMoreOperationsItemClick{}.OpKey(): func() cptype.Operation {

				if apistructs.PipelineStatus(definition.Status).IsRunningStatus() {
					return build
				}

				build := cputil.NewOpBuilder().Build()
				build.SkipRender = true

				appName := getApplicationNameFromDefinitionRemote(definition.Remote)
				inode := p.makeInode(appName, definition, appNameIDMap)
				build.ServerData = &cptype.OpServerData{
					"inode":        base64.URLEncoding.EncodeToString([]byte(inode)),
					"appName":      appName,
					"pipelineID":   definition.PipelineID,
					"pipelineName": definition.Name,
				}
				return build
			}(),
		},
	})
	if apistructs.PipelineStatus(definition.Status).IsFailedStatus() {
		items = append(items, commodel.MoreOpItem{
			ID:   "rerunFromFail",
			Text: cputil.I18n(p.sdk.Ctx, "rerunFromFail"),
			Operations: map[cptype.OperationKey]cptype.Operation{
				commodel.OpMoreOperationsItemClick{}.OpKey(): build,
			},
			Icon: &commodel.Icon{
				Type: "refresh",
			},
		})
	}
	if apistructs.PipelineStatus(definition.Status).IsFailedStatus() {
		items = append(items, commodel.MoreOpItem{
			ID:   "rerun",
			Text: cputil.I18n(p.sdk.Ctx, "rerun"),
			Operations: map[cptype.OperationKey]cptype.Operation{
				commodel.OpMoreOperationsItemClick{}.OpKey(): build,
			},
			Icon: &commodel.Icon{
				Type: "restart",
			},
		})
	}

	if v, ok := ymlSourceMapCronMap[definitionYmlSourceMap[definition.ID]]; ok && strings.TrimSpace(v.CronExpr) != "" {
		items = append(items, commodel.MoreOpItem{
			ID: func() string {
				if v.Enable.Value {
					return "cancelCron"
				}
				return "cron"
			}(),
			Text: cputil.I18n(p.sdk.Ctx, func() string {
				if v.Enable.Value {
					return "cancelCron"
				}
				return "cron"
			}()),
			Icon: func() *commodel.Icon {
				if v.Enable.Value {
					return &commodel.Icon{
						Type: "start-timing",
					}
				}
				return &commodel.Icon{
					Type: "stop",
				}
			}(),
			Operations: map[cptype.OperationKey]cptype.Operation{
				commodel.OpMoreOperationsItemClick{}.OpKey(): build,
			},
		})
	}

	// No delete button in running and timing
	if apistructs.PipelineStatus(definition.Status).IsRunningStatus() {
		return items
	}
	if v, ok := ymlSourceMapCronMap[definitionYmlSourceMap[definition.ID]]; ok {
		if v.Enable.Value {
			return items
		}
	}
	if definition.Creator == p.sdk.Identity.UserID {
		items = append(items, commodel.MoreOpItem{
			ID:   "delete",
			Text: cputil.I18n(p.sdk.Ctx, "delete"),
			Operations: map[cptype.OperationKey]cptype.Operation{
				commodel.OpMoreOperationsItemClick{}.OpKey(): build,
			},
			Icon: &commodel.Icon{
				Type: "delete1",
			},
		})
	}

	items = append(items, commodel.MoreOpItem{
		ID:   "update",
		Text: cputil.I18n(p.sdk.Ctx, "update"),
		Icon: &commodel.Icon{
			Type: "edit1",
		},
		Operations: map[cptype.OperationKey]cptype.Operation{
			commodel.OpMoreOperationsItemClick{}.OpKey(): func() cptype.Operation {
				build := cputil.NewOpBuilder().Build()
				build.SkipRender = true

				appName := getApplicationNameFromDefinitionRemote(definition.Remote)
				inode := p.makeInode(appName, definition, appNameIDMap)
				build.ServerData = &cptype.OpServerData{
					"inode":        base64.URLEncoding.EncodeToString([]byte(inode)),
					"appName":      appName,
					"pipelineID":   definition.PipelineID,
					"pipelineName": definition.Name,
					"appID":        appNameIDMap.AppNameToID[appName],
				}
				return build
			}(),
		},
	})
	return items
}

func getApplicationNameFromDefinitionRemote(remote string) string {
	values := strings.Split(remote, string(filepath.Separator))
	if len(values) != 3 {
		return remote
	}
	return values[2]
}

func (p *PipelineTable) SetSortsFromGlobalState() {
	globalState := *p.sdk.GlobalState
	var sorts []*common.Sort
	if sortCol, ok := globalState[StateKeyTransactionSort]; ok && sortCol != nil {
		var clientSort table.OpTableChangeSortClientData
		clientSort, ok = sortCol.(table.OpTableChangeSortClientData)
		if !ok {
			ok = mapstructure.Decode(sortCol, &clientSort) == nil
		}
		if ok {
			col := clientSort.DataRef
			if col != nil && col.AscOrder != nil {
				sorts = append(sorts, &common.Sort{
					FieldKey:  col.FieldBindToOrder,
					Ascending: *col.AscOrder,
				})
			}
		}
	}
	p.Sorts = sorts
}

func (p *PipelineTable) SetPagingFromGlobalState() {
	globalState := *p.sdk.GlobalState
	pageNo, pageSize := 1, common.DefaultPageSize
	if paging, ok := globalState[StateKeyTransactionPaging]; ok && paging != nil {
		var clientPaging table.OpTableChangePageClientData
		clientPaging, ok = paging.(table.OpTableChangePageClientData)
		if !ok {
			ok = mapstructure.Decode(paging, &clientPaging) == nil
		}
		if ok {
			pageNo = int(clientPaging.PageNo)
			pageSize = int(clientPaging.PageSize)
		}
	}
	p.PageNo = uint64(pageNo)
	p.PageSize = uint64(pageSize)
}

func (p *PipelineTable) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func (p *PipelineTable) RegisterTableChangePageOp(opData table.OpTableChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		(*sdk.GlobalState)[StateKeyTransactionPaging] = opData.ClientData
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *PipelineTable) RegisterTableSortOp(opData table.OpTableChangeSort) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		(*sdk.GlobalState)[StateKeyTransactionSort] = opData.ClientData
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *PipelineTable) RegisterBatchRowsHandleOp(opData table.OpBatchRowsHandle) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		switch opData.ClientData.DataRef.ID {
		case "batchRun":
			_, err := p.ProjectPipelineSvc.BatchRun(p.sdk.Ctx, deftype.ProjectPipelineBatchRun{
				PipelineDefinitionIDs: opData.ClientData.SelectedRowIDs,
				ProjectID:             p.InParams.ProjectID,
				IdentityInfo:          apistructs.IdentityInfo{UserID: p.sdk.Identity.UserID},
			})
			if err != nil {
				panic(err)
			}
		}
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *PipelineTable) RegisterRowSelectOp(opData table.OpRowSelect) (opFunc cptype.OperationFunc) {
	return nil
}

func (p *PipelineTable) RegisterRowAddOp(opData table.OpRowAdd) (opFunc cptype.OperationFunc) {
	return nil
}

func (p *PipelineTable) RegisterRowEditOp(opData table.OpRowEdit) (opFunc cptype.OperationFunc) {
	return nil
}

func (p *PipelineTable) RegisterRowDeleteOp(opData table.OpRowDelete) (opFunc cptype.OperationFunc) {
	return nil
}

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, "pipelineTable", func() servicehub.Provider {
		return &PipelineTable{}
	})
}

func (p *PipelineTable) RegisterMoreOperationOp(opData OpMoreOperationsItemClick) {
	id := string(opData.ClientData.ParentDataRef.ID)
	switch opData.ClientData.DataRef.ID {
	case "run":
		_, err := p.ProjectPipelineSvc.Run(p.sdk.Ctx, &projectpipelinepb.RunProjectPipelineRequest{
			PipelineDefinitionID: id,
			ProjectID:            int64(p.InParams.ProjectID),
		})
		if err != nil {
			panic(err)
		}
	case "cancelRun":
		_, err := p.ProjectPipelineSvc.Cancel(p.sdk.Ctx, &projectpipelinepb.CancelProjectPipelineRequest{
			PipelineDefinitionID: id,
			ProjectID:            int64(p.InParams.ProjectID),
		})
		if err != nil {
			panic(err)
		}
	case "rerun":
		_, err := p.ProjectPipelineSvc.Rerun(p.sdk.Ctx, &projectpipelinepb.RerunProjectPipelineRequest{
			PipelineDefinitionID: id,
			ProjectID:            int64(p.InParams.ProjectID),
		})
		if err != nil {
			panic(err)
		}
	case "rerunFromFail":
		_, err := p.ProjectPipelineSvc.RerunFailed(p.sdk.Ctx, &projectpipelinepb.RerunFailedProjectPipelineRequest{
			PipelineDefinitionID: id,
			ProjectID:            int64(p.InParams.ProjectID),
		})
		if err != nil {
			panic(err)
		}
	case "cron":
		_, err := p.ProjectPipelineSvc.StartCron(p.sdk.Ctx, deftype.ProjectPipelineStartCron{
			PipelineDefinitionID: id,
			ProjectID:            p.InParams.ProjectID,
			IdentityInfo:         apistructs.IdentityInfo{UserID: cputil.GetUserID(p.sdk.Ctx)},
		})
		if err != nil {
			panic(err)
		}
	case "cancelCron":
		_, err := p.ProjectPipelineSvc.EndCron(p.sdk.Ctx, deftype.ProjectPipelineEndCron{
			PipelineDefinitionID: id,
			ProjectID:            p.InParams.ProjectID,
			IdentityInfo:         apistructs.IdentityInfo{UserID: cputil.GetUserID(p.sdk.Ctx)},
		})
		if err != nil {
			panic(err)
		}
	case "delete":
		_, err := p.ProjectPipelineSvc.Delete(p.sdk.Ctx, deftype.ProjectPipelineDelete{
			ID:           id,
			ProjectID:    p.InParams.ProjectID,
			IdentityInfo: apistructs.IdentityInfo{UserID: cputil.GetUserID(p.sdk.Ctx)},
		})
		if err != nil {
			panic(err)
		}
	}
	p.RegisterInitializeOp()(p.sdk)
}

func (p *PipelineTable) RegisterCompNonStdOps() (opFuncs map[cptype.OperationKey]cptype.OperationFunc) {
	return map[cptype.OperationKey]cptype.OperationFunc{
		commodel.OpMoreOperationsItemClick{}.OpKey(): func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
			p.RegisterMoreOperationOp(*cputil.MustObjJSONTransfer(&sdk.Event.OperationData, &OpMoreOperationsItemClick{}).(*OpMoreOperationsItemClick))
			return nil
		},
	}
}

type OpMoreOperationsItemClick struct {
	commodel.OpMoreOperationsItemClick
	ClientData OpMoreOperationsItemClickClientData `json:"clientData"`
}

type OpMoreOperationsItemClickClientData struct {
	DataRef       *commodel.MoreOpItem `json:"dataRef,omitempty"`
	ParentDataRef table.Row            `json:"parentDataRef,omitempty"`
}

type OpMoreOperationsItemClickServerData struct {
	ID string `json:"id"`
}

func getStatus(status apistructs.PipelineStatus) commodel.UnifiedStatus {
	if status.IsRunningStatus() {
		return commodel.ProcessingStatus
	}
	if status.IsFailedStatus() {
		return commodel.ErrorStatus
	}
	if status.IsSuccessStatus() {
		return commodel.SuccessStatus
	}
	return commodel.DefaultStatus
}

func (p *PipelineTable) makeInode(appName string, definition *pb.PipelineDefinition, appNameIDMap *apistructs.GetAppIDByNamesResponseData) string {
	var inode string
	if appName != "" && appNameIDMap != nil {
		if definition.Path == "" {
			inode = fmt.Sprintf("%v/%v/tree/%v/%v", p.InParams.ProjectID, appNameIDMap.AppNameToID[appName], definition.Ref, definition.FileName)
		} else {
			inode = fmt.Sprintf("%v/%v/tree/%v/%v/%v", p.InParams.ProjectID, appNameIDMap.AppNameToID[appName], definition.Ref, definition.Path, definition.FileName)
		}
	}
	return inode
}
