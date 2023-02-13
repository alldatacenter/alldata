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

package dbclient

import (
	"fmt"
	"strings"
	"sync"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	definitiondb "github.com/erda-project/erda/internal/tools/pipeline/providers/definition/db"
	sourcedb "github.com/erda-project/erda/internal/tools/pipeline/providers/source/db"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreatePipeline: base + extra + labels
func (client *Client) CreatePipeline(p *spec.Pipeline, ops ...SessionOption) error {
	// base
	// TODO the edge pipeline should add init report status
	p.PipelineBase.EdgeReportStatus = apistructs.InitEdgeReportStatus

	if err := client.CreatePipelineBase(&p.PipelineBase, ops...); err != nil {
		return errors.Errorf("failed to create pipeline base, err: %v", err)
	}

	// extra
	p.PipelineExtra.PipelineID = p.ID
	if p.Extra.Namespace == "" {
		p.Extra.Namespace = fmt.Sprintf("pipeline-%d", p.ID)
	}
	if err := client.CreatePipelineExtra(&p.PipelineExtra, ops...); err != nil {
		return errors.Errorf("failed to create pipeline extra, err: %v", err)
	}

	// labels
	if err := client.CreatePipelineLabels(p, ops...); err != nil {
		return errors.Errorf("failed to create pipeline labels, err: %v", err)
	}

	return nil
}

type dbError struct{ msg string }

func (e dbError) Error() string        { return e.msg }
func (e dbError) Is(target error) bool { return target != nil && target.Error() == e.msg }

var NotFoundBaseError = dbError{msg: "not found base"}

// GetPipeline: base + extra + labels
func (client *Client) GetPipeline(id interface{}, ops ...SessionOption) (spec.Pipeline, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var base spec.PipelineBase
	found, err := session.ID(id).Get(&base)
	if err != nil {
		return spec.Pipeline{}, err
	}
	if !found {
		return spec.Pipeline{}, NotFoundBaseError
	}

	var extra spec.PipelineExtra
	var labels []spec.PipelineLabel

	result, found, err := client.GetPipelineExtraByPipelineID(base.ID, ops...)
	if err != nil {
		return spec.Pipeline{}, err
	}
	if !found {
		return spec.Pipeline{}, errors.New("not found extra")
	}
	extra = result

	labelsResult, err := client.ListLabelsByPipelineID(base.ID, ops...)
	if err != nil {
		return spec.Pipeline{}, err
	}
	labels = labelsResult

	// combine pipeline
	var p spec.Pipeline
	p.PipelineBase = base
	p.PipelineExtra = extra
	p.Labels = make(map[string]string, len(labels))
	for _, label := range labels {
		p.Labels[label.Key] = label.Value
	}
	p.EnsureGC()
	return p, nil
}

// GetPipelineWithExistInfo 当 id 对应的流水线记录不存在时，error = nil, found = false
func (client *Client) GetPipelineWithExistInfo(id interface{}, ops ...SessionOption) (spec.Pipeline, bool, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	p, err := client.GetPipeline(id, ops...)
	if err != nil {
		if strutil.Contains(err.Error(), "not found") {
			return spec.Pipeline{}, false, nil
		}
		return spec.Pipeline{}, false, err
	}
	return p, true, nil
}

// UpdatePipelineShowMessage 更新 extra.ExtraInfo.ShowMessage
func (client *Client) UpdatePipelineShowMessage(pipelineID uint64, showMessage apistructs.ShowMessage, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	// get extra
	extra, found, err := client.GetPipelineExtraByPipelineID(pipelineID, ops...)
	if err != nil {
		return err
	}
	if !found {
		return errors.Errorf("failed to find pipeline extra by pipelineID: %d", pipelineID)
	}

	// update
	extra.Extra.ShowMessage = &showMessage
	_, err = session.ID(extra.PipelineID).Cols("extra").Update(&extra)
	return err
}

func (client *Client) StoreAnalyzedCrossCluster(pipelineID uint64, analyzedCrossCluster bool, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	// get extra
	extra, found, err := client.GetPipelineExtraByPipelineID(pipelineID, ops...)
	if err != nil {
		return err
	}
	if !found {
		return errors.Errorf("failed to find pipeline extra by pipelineID: %d", pipelineID)
	}

	// update
	extra.Snapshot.AnalyzedCrossCluster = &analyzedCrossCluster
	_, err = session.ID(extra.PipelineID).Cols("snapshot").Update(&extra)
	return err
}

// RefreshPipeline 更新 pipeline
func (client *Client) RefreshPipeline(p *spec.Pipeline) error {
	r, err := client.GetPipeline(p.ID)
	if err != nil {
		return err
	}
	*p = r
	return nil
}

// UpdateWholeStatusBorn 状态更新顺序：task -> stage -> pipeline
func (client *Client) UpdateWholeStatusBorn(pipelineID uint64, ops ...SessionOption) (err error) {
	defer func() {
		err = errors.Wrap(err, "failed to update whole pipeline status to born")
	}()

	session := client.NewSession(ops...)
	defer session.Close()

	base, found, err := client.GetPipelineBase(pipelineID)
	if err != nil {
		return err
	}
	if !found {
		return errors.Errorf("pipeline not found")
	}

	stages, err := client.ListPipelineStageByPipelineID(base.ID)
	if err != nil {
		return err
	}
	for _, stage := range stages {
		tasks, err := client.ListPipelineTasksByStageID(stage.ID)
		if err != nil {
			return err
		}
		for _, task := range tasks {
			if task.Status.IsEndStatus() {
				continue
			}
			if task.Status == apistructs.PipelineStatusPaused {
				continue
			}
			if task.Status == apistructs.PipelineStatusDisabled {
				continue
			}
			if err = client.UpdatePipelineTaskStatus(task.ID, apistructs.PipelineStatusBorn); err != nil {
				return err
			}
		}
	}

	if base.Status == apistructs.PipelineStatusAnalyzed {
		if err = client.UpdatePipelineTaskStatus(base.ID, apistructs.PipelineStatusBorn); err != nil {
			return err
		}
	}

	return nil
}

type PageListPipelinesResult struct {
	Pipelines         []spec.Pipeline
	PagingPipelineIDs []uint64
	Total             int64
	CurrentPageSize   int64
}

// PageListPipelines return pagingPipelines, pagingPipelineIDs, total, currentPageSize, error
func (client *Client) PageListPipelines(req apistructs.PipelinePageListRequest, ops ...SessionOption) (*PageListPipelinesResult, error) {

	session := client.NewSession(ops...)
	defer session.Close()

	var (
		err                 error
		needQueryDefinition = false
	)

	// default pageNum = 1
	if req.PageNo > 0 {
		req.PageNum = req.PageNo
	}
	if req.PageNum <= 0 {
		req.PageNum = 1
	}
	// default pageSize = 20 (Range: 0 < pageSize <= 100)
	if req.PageSize <= 0 || (req.PageSize > 100 && !req.LargePageSize) {
		req.PageSize = 20
	}

	if !req.AllSources && len(req.Sources) == 0 {
		return nil, errors.New("missing pipeline sources")
	}

	// label
	if req.MustMatchLabels == nil {
		req.MustMatchLabels = make(map[string][]string)
	}
	if req.AnyMatchLabels == nil {
		req.AnyMatchLabels = make(map[string][]string)
	}

	// 并行查询
	var wg sync.WaitGroup
	var errs []string

	// labels 获取到的 pipelineIDs
	var labelPipelineIDs []uint64
	var needFilterByLabel bool
	wg.Add(1)
	go func() {
		defer wg.Done()
		// select by labels
		if len(req.MustMatchLabels) > 0 || len(req.AnyMatchLabels) > 0 {
			needFilterByLabel = true
			labelRequest := apistructs.TargetIDSelectByLabelRequest{
				Type:                   apistructs.PipelineLabelTypeInstance,
				PipelineSources:        req.Sources,
				AllowNoPipelineSources: req.AllSources,
				PipelineYmlNames:       req.YmlNames,
				MustMatchLabels:        req.MustMatchLabels,
				AnyMatchLabels:         req.AnyMatchLabels,
			}
			labelPipelineIDs, err = client.SelectTargetIDsByLabels(labelRequest)
			if err != nil {
				errs = append(errs, err.Error())
				return
			}
		}
	}()

	// 基础信息表获取到的 pipelineIDs
	baseSQL := session.Table(&spec.PipelineBase{}).Where("").Cols(tableFieldName((&spec.PipelineBase{}).TableName(), "id"))

	// FORCE INDEX
	var forceIndexes []string
	// idx_id_source_cluster_status
	if !req.AllSources && len(req.Sources) > 0 &&
		len(req.ClusterNames) > 0 &&
		len(req.Statuses) > 0 &&
		len(req.AscCols) == 0 && len(req.DescCols) == 0 {
		if !req.StartTimeBegin.IsZero() || !req.EndTimeBegin.IsZero() {
			forceIndexes = append(forceIndexes, "`idx_source_status_cluster_timebegin_timeend_id`")
		} else {
			forceIndexes = append(forceIndexes, "`idx_id_source_cluster_status`")
		}
	}
	// idx_id_source_cluster_status_timebegin_timeend
	// 使用 alias 注入实现 xorm 插入 FORCE INDEX
	if len(forceIndexes) > 0 {
		baseSQL.Alias(fmt.Sprintf("`%s` USE INDEX (%s)", (&spec.PipelineBase{}).TableName(), strings.Join(forceIndexes, ",")))
	}

	if req.PipelineDefinitionRequest != nil {
		var definitionReq = req.PipelineDefinitionRequest
		needQueryDefinition = true

		baseSQL.Where(tableFieldName((&spec.PipelineBase{}).TableName(), "pipeline_definition_id") + " is not null ")
		baseSQL.Where(tableFieldName((&spec.PipelineBase{}).TableName(), "pipeline_definition_id") + " != '' ")
		if !definitionReq.IsEmptyValue() {
			baseSQL.Join("INNER", definitiondb.PipelineDefinition{}.TableName(), fmt.Sprintf("%v.id = %v.pipeline_definition_id", definitiondb.PipelineDefinition{}.TableName(), (&spec.PipelineBase{}).TableName()))
			baseSQL.Join("INNER", sourcedb.PipelineSource{}.TableName(), fmt.Sprintf("%v.id = %v.pipeline_source_id", sourcedb.PipelineSource{}.TableName(), definitiondb.PipelineDefinition{}.TableName()))
			if len(definitionReq.Name) > 0 {
				baseSQL.Where(fmt.Sprintf("%v.name like ?", definitiondb.PipelineDefinition{}.TableName()), "%"+definitionReq.Name+"%")
			}
			if definitionReq.Location != "" {
				baseSQL.Where(fmt.Sprintf("%s.location = ?", definitiondb.PipelineDefinition{}.TableName()), definitionReq.Location)
			}
			if len(definitionReq.SourceRemotes) > 0 {
				baseSQL.In(tableFieldName(sourcedb.PipelineSource{}.TableName(), "remote"), definitionReq.SourceRemotes)
			}
			if len(definitionReq.Creators) > 0 {
				baseSQL.In(tableFieldName(definitiondb.PipelineDefinition{}.TableName(), "creator"), definitionReq.Creators)
			}
		}
	}

	if !req.AllSources && len(req.Sources) > 0 {
		baseSQL.In(tableFieldName((&spec.PipelineBase{}).TableName(), "pipeline_source"), req.Sources)
	}
	if len(req.YmlNames) > 0 {
		baseSQL.In(tableFieldName((&spec.PipelineBase{}).TableName(), "pipeline_yml_name"), req.YmlNames)
	}
	if len(req.Statuses) > 0 {
		baseSQL.In(tableFieldName((&spec.PipelineBase{}).TableName(), "status"), req.Statuses)
	}
	if len(req.NotStatuses) > 0 {
		baseSQL.NotIn(tableFieldName((&spec.PipelineBase{}).TableName(), "status"), req.NotStatuses)
	}
	if len(req.TriggerModes) > 0 {
		baseSQL.In(tableFieldName((&spec.PipelineBase{}).TableName(), "trigger_mode"), req.TriggerModes)
	}
	if len(req.ClusterNames) > 0 {
		baseSQL.In(tableFieldName((&spec.PipelineBase{}).TableName(), "cluster_name"), req.ClusterNames)
	}
	baseSQL.Where(tableFieldName((&spec.PipelineBase{}).TableName(), "is_snippet")+" = ?", req.IncludeSnippet)
	if !req.StartTimeBegin.IsZero() {
		baseSQL.Where(tableFieldName((&spec.PipelineBase{}).TableName(), "time_begin")+" >= ? ", req.StartTimeBegin)
	}
	if !req.EndTimeBegin.IsZero() {
		baseSQL.Where(tableFieldName((&spec.PipelineBase{}).TableName(), "time_begin")+" <= ?", req.EndTimeBegin)
	}
	if !req.StartTimeCreated.IsZero() {
		baseSQL.Where(tableFieldName((&spec.PipelineBase{}).TableName(), "time_created")+" >= ?", req.StartTimeCreated)
	}
	if !req.EndTimeCreated.IsZero() {
		baseSQL.Where(tableFieldName((&spec.PipelineBase{}).TableName(), "time_created")+" <= ?", req.EndTimeCreated)
	}
	if len(req.AscCols) == 0 && len(req.DescCols) == 0 {
		baseSQL.Desc(tableFieldName((&spec.PipelineBase{}).TableName(), "id"))
	}
	if len(req.AscCols) > 0 {
		var newAsc []string
		for _, v := range req.AscCols {
			newAsc = append(newAsc, tableFieldName((&spec.PipelineBase{}).TableName(), v))
		}
		baseSQL.Asc(newAsc...)
	}
	if len(req.DescCols) > 0 {
		var newDesc []string
		for _, v := range req.DescCols {
			newDesc = append(newDesc, tableFieldName((&spec.PipelineBase{}).TableName(), v))
		}
		baseSQL.Desc(newDesc...)
	}

	var basePipelineIDs []uint64
	wg.Add(1)
	go func() {
		defer wg.Done()
		if err := baseSQL.Find(&basePipelineIDs); err != nil {
			errs = append(errs, err.Error())
			return
		}
	}()

	wg.Wait()

	if len(errs) > 0 {
		return nil, errors.New(strutil.Join(errs, "\n"))
	}

	// 获取最终 pipelineIDs
	var pipelineIDs []uint64
	if needFilterByLabel {
		pipelineIDs = filterAndOrder(basePipelineIDs, labelPipelineIDs)
	} else {
		pipelineIDs = basePipelineIDs
	}

	// 在内存中做分页
	pagingPipelineIDs := paging(pipelineIDs, req.PageNum, req.PageSize)
	currentPageSize := int64(len(pagingPipelineIDs))
	total := int64(len(pipelineIDs))

	if req.CountOnly {
		return &PageListPipelinesResult{
			PagingPipelineIDs: pagingPipelineIDs,
			Total:             total,
			CurrentPageSize:   currentPageSize,
		}, nil
	}

	// select columns
	if len(req.SelectCols) > 0 {
		session.Cols(req.SelectCols...)
	}
	pipelines, err := client.ListPipelinesByIDs(pagingPipelineIDs, needQueryDefinition, ops...)
	if err != nil {
		return &PageListPipelinesResult{
			PagingPipelineIDs: pagingPipelineIDs,
			Total:             -1,
			CurrentPageSize:   -1,
		}, err
	}

	return &PageListPipelinesResult{
		Pipelines:         pipelines,
		PagingPipelineIDs: pagingPipelineIDs,
		Total:             total,
		CurrentPageSize:   currentPageSize,
	}, nil
}

func tableFieldName(tableName string, field string) string {
	return fmt.Sprintf("%v.%v", tableName, field)
}

// ListPipelineIDsByStatuses
func (client *Client) ListPipelineIDsByStatuses(status ...apistructs.PipelineStatus) ([]uint64, error) {
	var ids []uint64
	err := client.Table(&spec.PipelineBase{}).Cols("id").Where("is_snippet = ?", false).In("status", status).Find(&ids)
	if err != nil {
		return nil, err
	}
	return ids, nil
}

// GetPipelineWithTasks
func (client *Client) GetPipelineWithTasks(id uint64) (*spec.PipelineWithTasks, error) {
	p, err := client.GetPipeline(id)
	if err != nil {
		return nil, err
	}

	tasks, err := client.ListPipelineTasksByPipelineID(id)
	if err != nil {
		return nil, err
	}

	taskResult := make([]*spec.PipelineTask, 0, len(tasks))
	for i := range tasks {
		taskResult = append(taskResult, &tasks[i])
	}

	return &spec.PipelineWithTasks{
		Pipeline: &p,
		Tasks:    taskResult,
	}, nil
}

func (client *Client) ParseRerunFailedDetail(detail *spec.RerunFailedDetail) (
	map[string]*spec.PipelineTask, map[string]*spec.PipelineTask, error) {

	if detail == nil {
		return nil, nil, nil
	}

	batchParseID2Task := func(in map[string]uint64, optionalOutput bool) (
		map[string]*spec.PipelineTask, error) {
		result := make(map[string]*spec.PipelineTask)
		for name, taskID := range in {
			task, err := client.GetPipelineTask(taskID)
			if err != nil {
				return nil, err
			}
			result[name] = &task
		}
		return result, nil
	}

	successTaskMap, err := batchParseID2Task(detail.SuccessTasks, false)
	if err != nil {
		return nil, nil, err
	}
	failedTaskMap, err := batchParseID2Task(detail.FailedTasks, true)
	if err != nil {
		return nil, nil, err
	}
	return successTaskMap, failedTaskMap, nil
}

// PipelineStatistic pipeline 执行情况统计
func (client *Client) PipelineStatistic(source, clusterName string) (*apistructs.PipelineStatisticResponseData, error) {
	var (
		success    int64
		failed     int64
		processing int64
		err        error
	)

	forceIndexSQL := fmt.Sprintf("`%s` FORCE INDEX (`idx_source_status_cluster_timebegin_timeend_id`)", (&spec.PipelineBase{}).TableName())

	successSQL := client.Alias(forceIndexSQL).Where("pipeline_source = ?", source).Where("status = ?", apistructs.PipelineStatusSuccess)
	processingSQL := client.Alias(forceIndexSQL).Where("pipeline_source = ?", source).In("status", []string{string(apistructs.PipelineStatusQueue), string(apistructs.PipelineStatusRunning)})
	failedSQL := client.Alias(forceIndexSQL).Where("pipeline_source = ?", source).In("status", []string{string(apistructs.PipelineStatusFailed), string(apistructs.PipelineStatusTimeout)})

	if clusterName != "" {
		successSQL = successSQL.Where("cluster_name = ?", clusterName)
		processingSQL = processingSQL.Where("cluster_name = ?", clusterName)
		failedSQL = failedSQL.Where("cluster_name = ?", clusterName)
	}

	success, err = successSQL.Count(&spec.PipelineBase{})
	if err != nil {
		return nil, err
	}

	processing, err = processingSQL.Count(&spec.PipelineBase{})
	if err != nil {
		return nil, err
	}

	failed, err = failedSQL.Count(&spec.PipelineBase{})
	if err != nil {
		return nil, err
	}

	return &apistructs.PipelineStatisticResponseData{
		Success:    uint64(success),
		Processing: uint64(processing),
		Failed:     uint64(failed),
		Completed:  uint64(success + failed),
	}, nil
}

func (client *Client) DeletePipeline(id uint64, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	// base
	if _, err := session.ID(id).Delete(&spec.PipelineBase{}); err != nil {
		return err
	}

	// extra
	if _, err := session.Where("pipeline_id = ?", id).Delete(&spec.PipelineExtra{}); err != nil {
		return err
	}

	return nil
}

func (client *Client) ListPipelineSources() ([]apistructs.PipelineSource, error) {
	result := make([]apistructs.PipelineSource, 0)
	err := client.Table(&spec.PipelineBase{}).Distinct("pipeline_source").Select("pipeline_source").Find(&result)
	return result, err
}

// GetPipelineOutputs 返回 pipeline 下所有 task 的 output
func (client *Client) GetPipelineOutputs(pipelineID uint64) (map[string]map[string]string, error) {
	tasks, err := client.ListPipelineTasksByPipelineID(pipelineID)
	if err != nil {
		return nil, err
	}

	outputs := make(map[string]map[string]string)

	for _, task := range tasks {
		for _, metadatum := range task.GetMetadata() {
			if outputs[task.Name] == nil {
				outputs[task.Name] = make(map[string]string)
			}
			outputs[task.Name][metadatum.Name] = metadatum.Value
		}
	}

	return outputs, nil
}

func (client *Client) ListPipelinesByIDs(pipelineIDs []uint64, needQueryDefinition bool, ops ...SessionOption) ([]spec.Pipeline, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	// 并行查询
	var wg sync.WaitGroup

	var errs []string

	basesMap := make(map[uint64]spec.PipelineBase, len(pipelineIDs))
	baseWithDefinitionMap := make(map[uint64]spec.PipelineBaseWithDefinition, len(pipelineIDs))
	extrasMap := make(map[uint64]spec.PipelineExtra, len(pipelineIDs))
	labelsMap := make(map[uint64][]spec.PipelineLabel, len(pipelineIDs))

	// pipeline_base
	wg.Add(1)
	go func() {
		defer wg.Done()
		if needQueryDefinition {
			innerBaseWithDefinitionMap, err := client.ListPipelineBaseWithDefinitionByIDs(pipelineIDs, ops...)
			if err != nil {
				errs = append(errs, err.Error())
				return
			}
			baseWithDefinitionMap = innerBaseWithDefinitionMap
		} else {
			innerBasesMap, err := client.ListPipelineBasesByIDs(pipelineIDs, ops...)
			if err != nil {
				errs = append(errs, err.Error())
				return
			}
			basesMap = innerBasesMap
		}
	}()

	// pipeline_extra
	wg.Add(1)
	go func() {
		defer wg.Done()

		innerExtrasMap, err := client.ListPipelineExtrasByPipelineIDs(pipelineIDs, ops...)
		if err != nil {
			errs = append(errs, err.Error())
			return
		}
		extrasMap = innerExtrasMap
	}()

	// pipeline_labels
	wg.Add(1)
	go func() {
		defer wg.Done()

		innerLabelsMap, err := client.ListPipelineLabelsByTypeAndTargetIDs(apistructs.PipelineLabelTypeInstance, pipelineIDs, ops...)
		if err != nil {
			errs = append(errs, err.Error())
			return
		}
		labelsMap = innerLabelsMap
	}()

	wg.Wait()

	if len(errs) > 0 {
		return nil, errors.New(strutil.Join(errs, "\n"))
	}

	// combine pipelines
	var pipelines []spec.Pipeline
	for _, pipelineID := range pipelineIDs {
		var pipeline spec.Pipeline
		if needQueryDefinition {
			if baseWithDefinition, ok := baseWithDefinitionMap[pipelineID]; !ok {
				continue
			} else {
				pipeline.PipelineBase = baseWithDefinition.PipelineBase
				pipeline.Definition = &baseWithDefinition.PipelineDefinition
				pipeline.Source = &baseWithDefinition.PipelineSource
			}
		} else {
			if base, ok := basesMap[pipelineID]; !ok {
				continue
			} else {
				pipeline.PipelineBase = base
			}
		}

		if extra, ok := extrasMap[pipelineID]; !ok {
			continue
		} else {
			pipeline.PipelineExtra = extra
		}

		pipeline.Labels = make(map[string]string, len(labelsMap[pipelineID]))
		for _, label := range labelsMap[pipelineID] {
			pipeline.Labels[label.Key] = label.Value
		}
		pipelines = append(pipelines, pipeline)
	}
	return pipelines, nil
}
