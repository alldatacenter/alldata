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

package pipelinesvc

import (
	"context"
	"fmt"
	"strconv"
	"time"

	"github.com/mohae/deepcopy"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"github.com/xormplus/xorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/commonutil/thirdparty/gittarutil"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/events"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/action_info"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cms"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

// Deprecated
func (s *PipelineSvc) Create(req *apistructs.PipelineCreateRequest) (*spec.Pipeline, error) {
	p, err := s.makePipelineFromRequest(req)
	if err != nil {
		return nil, err
	}
	var stages []spec.PipelineStage
	if stages, err = s.CreatePipelineGraph(p); err != nil {
		return nil, err
	}
	// PreCheck
	_ = s.PreCheck(p, stages, p.GetUserID(), req.AutoRun)
	return p, nil
}

func (s *PipelineSvc) makePipelineFromRequest(req *apistructs.PipelineCreateRequest) (*spec.Pipeline, error) {
	p := &spec.Pipeline{
		PipelineExtra: spec.PipelineExtra{
			NormalLabels: make(map[string]string),
			Extra: spec.PipelineExtraInfo{
				// --- empty user ---
				SubmitUser: &apistructs.PipelineUser{},
				RunUser:    &apistructs.PipelineUser{},
				CancelUser: &apistructs.PipelineUser{},
			},
		},
		Labels: make(map[string]string),
	}

	// --- app info ---
	app, err := s.appSvc.GetWorkspaceApp(req.AppID, req.Branch)
	if err != nil {
		return nil, apierrors.ErrGetApp.InternalError(err)
	}
	p.Labels[apistructs.LabelOrgID] = strconv.FormatUint(app.OrgID, 10)
	p.NormalLabels[apistructs.LabelOrgName] = app.OrgName
	p.Labels[apistructs.LabelProjectID] = strconv.FormatUint(app.ProjectID, 10)
	p.NormalLabels[apistructs.LabelProjectName] = app.ProjectName
	p.Labels[apistructs.LabelAppID] = strconv.FormatUint(app.ID, 10)
	p.NormalLabels[apistructs.LabelAppName] = app.Name
	p.ClusterName = app.ClusterName
	p.Extra.DiceWorkspace = app.Workspace

	// --- repo info ---
	repo := gittarutil.NewRepo(discover.Gittar(), app.GitRepoAbbrev)
	commit, err := repo.GetCommit(req.Branch, req.UserID)
	if err != nil {
		return nil, apierrors.ErrGetGittarRepo.InternalError(err)
	}
	p.Labels[apistructs.LabelBranch] = req.Branch
	p.CommitDetail = apistructs.CommitDetail{
		CommitID: commit.ID,
		Repo:     app.GitRepo,
		RepoAbbr: app.GitRepoAbbrev,
		Author:   commit.Committer.Name,
		Email:    commit.Committer.Email,
		Time: func() *time.Time {
			commitTime, err := time.Parse("2006-01-02T15:04:05-07:00", commit.Committer.When)
			if err != nil {
				return nil
			}
			return &commitTime
		}(),
		Comment: commit.CommitMessage,
	}

	// --- yaml info ---
	if req.Source == "" {
		return nil, apierrors.ErrCreatePipeline.MissingParameter("source")
	}
	if !req.Source.Valid() {
		return nil, apierrors.ErrCreatePipeline.InvalidParameter(errors.Errorf("source: %s", req.Source))
	}
	p.PipelineSource = req.Source

	if req.PipelineYmlName == "" {
		req.PipelineYmlName = apistructs.DefaultPipelineYmlName
	}

	// PipelineYmlNameV1 用于从 gittar 中获取 pipeline.yml 内容
	p.Extra.PipelineYmlNameV1 = req.PipelineYmlName
	p.PipelineYmlName = p.GenerateV1UniquePipelineYmlName(p.Extra.PipelineYmlNameV1)

	if req.PipelineYmlSource == "" {
		return nil, apierrors.ErrCreatePipeline.MissingParameter("pipelineYmlSource")
	}
	if !req.PipelineYmlSource.Valid() {
		return nil, apierrors.ErrCreatePipeline.InvalidParameter(errors.Errorf("pipelineYmlSource: %s", req.PipelineYmlSource))
	}
	p.Extra.PipelineYmlSource = req.PipelineYmlSource
	switch req.PipelineYmlSource {
	case apistructs.PipelineYmlSourceGittar:
		// get yaml
		f, err := repo.FetchFile(req.Branch, p.Extra.PipelineYmlNameV1, req.UserID)
		if err != nil {
			return nil, apierrors.ErrGetGittarRepoFile.InternalError(err)
		}
		p.PipelineYml = string(f)
	case apistructs.PipelineYmlSourceContent:
		if req.PipelineYmlContent == "" {
			return nil, apierrors.ErrCreatePipeline.MissingParameter("pipelineYmlContent (pipelineYmlSource=content)")
		}
		p.PipelineYml = req.PipelineYmlContent
	}

	// --- run info ---
	p.Type = apistructs.PipelineTypeNormal
	p.TriggerMode = apistructs.PipelineTriggerModeManual
	if req.IsCronTriggered {
		p.TriggerMode = apistructs.PipelineTriggerModeCron
	}
	p.Status = apistructs.PipelineStatusAnalyzed

	// set storageConfig
	p.Extra.StorageConfig.EnableNFS = true
	if conf.DisablePipelineVolume() {
		p.Extra.StorageConfig.EnableNFS = false
		p.Extra.StorageConfig.EnableLocal = false
	}

	// --- extra ---
	p.Extra.ConfigManageNamespaceOfSecretsDefault = cms.MakeAppDefaultSecretNamespace(strconv.FormatUint(req.AppID, 10))
	ns, err := cms.MakeAppBranchPrefixSecretNamespace(strconv.FormatUint(req.AppID, 10), req.Branch)
	if err != nil {
		return nil, apierrors.ErrMakeConfigNamespace.InvalidParameter(err)
	}
	p.Extra.ConfigManageNamespaceOfSecrets = ns
	if req.UserID != "" {
		p.Extra.SubmitUser = s.user.TryGetUser(context.Background(), req.UserID)
	}
	p.Extra.IsAutoRun = req.AutoRun
	p.Extra.CallbackURLs = req.CallbackURLs

	// --- cron ---
	pipelineYml, err := pipelineyml.New([]byte(p.PipelineYml))
	if err != nil {
		return nil, apierrors.ErrParsePipelineYml.InternalError(err)
	}
	p.Extra.CronExpr = pipelineYml.Spec().Cron
	if err := s.UpdatePipelineCron(p, nil, nil, pipelineYml.Spec().CronCompensator); err != nil {
		return nil, apierrors.ErrCreatePipeline.InternalError(err)
	}

	version, err := pipelineyml.GetVersion([]byte(p.PipelineYml))
	if err != nil {
		return nil, apierrors.ErrParsePipelineYml.InvalidParameter("version")
	}
	p.Extra.Version = version

	p.CostTimeSec = -1
	p.Progress = -1

	return p, nil
}

// traverse the stage of yml and save it to the database
func (s *PipelineSvc) createPipelineGraphStage(p *spec.Pipeline, pipelineYml *pipelineyml.PipelineYml, ops ...dbclient.SessionOption) (stages []*spec.PipelineStage, err error) {
	var dbStages []*spec.PipelineStage

	for si := range pipelineYml.Spec().Stages {
		ps := &spec.PipelineStage{
			PipelineID:  p.ID,
			Name:        "",
			Status:      apistructs.PipelineStatusAnalyzed,
			CostTimeSec: -1,
			Extra:       spec.PipelineStageExtra{StageOrder: si},
		}
		if err := s.dbClient.CreatePipelineStage(ps, ops...); err != nil {
			return nil, apierrors.ErrCreatePipelineGraph.InternalError(err)
		}
		dbStages = append(dbStages, ps)
	}
	return dbStages, nil
}

// replace the tasks parsed by yml and tasks in the database with the same name
func (s *PipelineSvc) MergePipelineYmlTasks(pipelineYml *pipelineyml.PipelineYml, dbTasks []spec.PipelineTask, p *spec.Pipeline, dbStages []spec.PipelineStage, passedDataWhenCreate *action_info.PassedDataWhenCreate) (mergeTasks []spec.PipelineTask, err error) {
	// loop yml actions to make actionTasks
	actionTasks := s.getYmlActionTasks(pipelineYml, p, dbStages, passedDataWhenCreate)

	// determine whether the task status was disabled or Paused according to the TaskOperates of the pipeline
	var operateActionTasks []spec.PipelineTask
	for _, actionTask := range actionTasks {
		operateTask, err := s.OperateTask(p, &actionTask)
		if err != nil {
			return nil, apierrors.ErrListPipelineTasks.InternalError(err)
		}
		operateActionTasks = append(operateActionTasks, *operateTask)
	}

	// combine the task converted from yml with the task in the database
	return ymlTasksMergeDBTasks(operateActionTasks, dbTasks), nil
}

// generate task array according to yml structure
func (s *PipelineSvc) getYmlActionTasks(pipelineYml *pipelineyml.PipelineYml, p *spec.Pipeline, dbStages []spec.PipelineStage, passedDataWhenCreate *action_info.PassedDataWhenCreate) []spec.PipelineTask {
	if pipelineYml == nil || p == nil || len(dbStages) <= 0 {
		return nil
	}

	// loop yml actions to make actionTasks
	var actionTasks []spec.PipelineTask
	pipelineYml.Spec().LoopStagesActions(func(stageIndex int, action *pipelineyml.Action) {
		var task *spec.PipelineTask
		if action.Type.IsSnippet() {
			task = s.makeSnippetPipelineTask(p, &dbStages[stageIndex], action)
		} else {
			task = s.makeNormalPipelineTask(p, &dbStages[stageIndex], action, passedDataWhenCreate)
		}
		actionTasks = append(actionTasks, *task)
	})

	return actionTasks
}

// combine the task converted from yml with the task in the database
func ymlTasksMergeDBTasks(actionTasks []spec.PipelineTask, dbTasks []spec.PipelineTask) []spec.PipelineTask {
	var mergeTasks []spec.PipelineTask
	for actionIndex := range actionTasks {
		var actionTask = actionTasks[actionIndex]
		// actionTask the same dbTask pipelineID,stagesID,type and name replace with dbTask
		var mergeTask *spec.PipelineTask
		for index := range dbTasks {
			var dbTask = dbTasks[index]
			if actionTask.PipelineID != dbTask.PipelineID || actionTask.StageID != dbTask.StageID {
				continue
			}
			if actionTask.Type != dbTask.Type || actionTask.Name != dbTask.Name {
				continue
			}
			mergeTask = &dbTask
		}
		if mergeTask == nil {
			mergeTask = &actionTask
		}

		mergeTasks = append(mergeTasks, *mergeTask)
	}
	return mergeTasks
}

// determine whether the task status is disabled according to the TaskOperates of the pipeline and task extra disable field
func (s *PipelineSvc) OperateTask(p *spec.Pipeline, task *spec.PipelineTask) (*spec.PipelineTask, error) {
	for _, taskOp := range p.Extra.TaskOperates {
		// the name of the disabled task matches the task name
		if taskOp.TaskAlias != task.Name {
			continue
		}
		// task have id mean can not disable
		if task.ID > 0 {
			continue
		}

		var opAction OperateAction
		wrapError := func(err error) error {
			return errors.Wrapf(err, "failed to operate pipeline, task [%v], action [%s]", taskOp.TaskAlias, opAction)
		}
		// disable
		if taskOp.Disable != nil {
			if *taskOp.Disable {
				opAction = OpDisableTask
				if !(task.Status == apistructs.PipelineStatusAnalyzed || task.Status == apistructs.PipelineStatusPaused) {
					return nil, wrapError(errors.Errorf("invalid status [%v]", task.Status))
				}
				task.Status = apistructs.PipelineStatusDisabled
			} else {
				opAction = OpEnableTask
				task.Status = apistructs.PipelineStatusAnalyzed
			}
			// needUpdatePipelineCtx = true
		}

		// pause: task cannot be modified after starting execution
		if taskOp.Pause != nil {
			if *taskOp.Pause {
				opAction = OpPauseTask
				if !task.Status.CanPause() {
					return nil, wrapError(errors.Errorf("status [%s]", task.Status))
				}
				task.Status = apistructs.PipelineStatusPaused
			} else {
				opAction = OpUnpauseTask
				if !task.Status.CanUnpause() {
					return nil, wrapError(errors.Errorf("status [%s]", task.Status))
				}
				// Determine the stage status of the current node:
				// 1. If it is Born, it means that the stage can be pushed by the thruster, then task.status = Born
				// 2. Otherwise, it means that the stage has been executed and will not be advanced again, so task.status = Mark
				stage, err := s.dbClient.GetPipelineStage(task.StageID)
				if err != nil {
					return nil, err
				}
				if stage.Status == apistructs.PipelineStatusBorn {
					task.Status = apistructs.PipelineStatusBorn
				} else {
					task.Status = apistructs.PipelineStatusMark
				}
			}
			task.Extra.Pause = *taskOp.Pause
		}
	}
	return task, nil
}

// CreatePipelineGraph recursively create pipeline graph.
func (s *PipelineSvc) CreatePipelineGraph(p *spec.Pipeline) (newStages []spec.PipelineStage, err error) {
	// parse yml
	pipelineYml, err := pipelineyml.New(
		[]byte(p.PipelineYml),
	)
	if err != nil {
		return nil, apierrors.ErrParsePipelineYml.InternalError(err)
	}

	// init pipeline gc setting
	p.EnsureGC()

	// only create pipeline and stages, tasks waiting pipeline run
	var stages []*spec.PipelineStage
	_, err = s.dbClient.Transaction(func(session *xorm.Session) (interface{}, error) {
		// create pipeline
		if err := s.createPipelineAndCheckNotEndStatus(p, session); err != nil {
			return nil, err
		}
		// create pipeline stages
		stages, err = s.createPipelineGraphStage(p, pipelineYml, dbclient.WithTxSession(session))
		if err != nil {
			return nil, err
		}

		// calculate pipeline applied resource after all snippetTask created
		pipelineAppliedResources, err := s.calculatePipelineResources(pipelineYml, p)
		if err != nil {
			return nil, apierrors.ErrCreatePipelineGraph.InternalError(
				fmt.Errorf("failed to search pipeline action resrouces, err: %v", err))
		}
		if pipelineAppliedResources != nil {
			p.Snapshot.AppliedResources = *pipelineAppliedResources
		}
		if err := s.dbClient.UpdatePipelineExtraSnapshot(p.ID, p.Snapshot, dbclient.WithTxSession(session)); err != nil {
			return nil, apierrors.ErrCreatePipelineGraph.InternalError(
				fmt.Errorf("failed to update pipeline snapshot for applied resources, err: %v", err))
		}
		return nil, nil
	})
	if err != nil {
		return nil, apierrors.ErrCreatePipelineGraph.InternalError(err)
	}

	// cover stages
	for _, stage := range stages {
		newStages = append(newStages, *stage)
	}

	// events
	events.EmitPipelineInstanceEvent(p, p.GetSubmitUserID())
	return newStages, nil
}

func (s *PipelineSvc) createPipelineAndCheckNotEndStatus(p *spec.Pipeline, session *xorm.Session) error {
	// Check whether the parent pipeline has an end state
	for _, parentPipelineID := range p.Extra.SnippetChain {
		parentPipeline, _, err := s.dbClient.GetPipelineBase(parentPipelineID, dbclient.WithTxSession(session))
		if err != nil {
			logrus.Errorf("check whether the parent pipeline has an end state, error %v", err)
			continue
		}
		if parentPipeline.Status.IsEndStatus() {
			return fmt.Errorf("parent pipeline was end status")
		}
	}

	// create pipeline
	if s.edgeRegister.IsEdge() {
		p.IsEdge = true
	}
	if err := s.dbClient.CreatePipeline(p, dbclient.WithTxSession(session)); err != nil {
		return apierrors.ErrCreatePipeline.InternalError(err)
	}
	return nil
}

func getString(v interface{}) string {
	if v == nil {
		return ""
	}
	return fmt.Sprintf("%v", v)
}

func (s *PipelineSvc) makePipelineFromCopy(o *spec.Pipeline) (p *spec.Pipeline, err error) {
	r := deepcopy.Copy(o)
	p, ok := r.(*spec.Pipeline)
	if !ok {
		return nil, errors.New("failed to copy pipeline")
	}

	now := time.Now()

	// 初始化一些字段
	p.ID = 0
	p.Status = apistructs.PipelineStatusAnalyzed
	p.PipelineExtra.PipelineID = 0
	p.Snapshot = spec.Snapshot{}
	p.Snapshot.Envs = o.Snapshot.Envs
	p.Snapshot.RunPipelineParams = o.Snapshot.RunPipelineParams
	p.Extra.Namespace = o.Extra.Namespace
	p.Extra.SubmitUser = &apistructs.PipelineUser{}
	p.Extra.RunUser = &apistructs.PipelineUser{}
	p.Extra.CancelUser = &apistructs.PipelineUser{}
	p.Extra.ShowMessage = nil
	p.Extra.CopyFromPipelineID = &o.ID
	p.Extra.RerunFailedDetail = nil
	p.Extra.CronTriggerTime = nil
	p.Extra.CompleteReconcilerGC = false
	p.TriggerMode = apistructs.PipelineTriggerModeManual // 手动触发
	p.TimeCreated = &now
	p.TimeUpdated = &now
	p.TimeBegin = nil
	p.TimeEnd = nil
	p.CostTimeSec = -1
	p.PipelineDefinitionID = o.PipelineDefinitionID

	return p, nil
}
