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

package taskop

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"math"
	"net/http"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/actionagent"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/plugins/k8sjob"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/pvolumes"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/container_provider"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/containers"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/errorsx"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/taskrun"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/metadata"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
)

type prepare taskrun.TaskRun

func NewPrepare(tr *taskrun.TaskRun) *prepare {
	return (*prepare)(tr)
}

func (pre *prepare) Op() taskrun.Op {
	return taskrun.Prepare
}

func (pre *prepare) TaskRun() *taskrun.TaskRun {
	return (*taskrun.TaskRun)(pre)
}

func (pre *prepare) Processing() (interface{}, error) {
	return nil, nil
}

func (pre *prepare) WhenDone(data interface{}) error {
	needRetry, err := pre.makeTaskRun()
	if needRetry {
		pre.Task.Status = apistructs.PipelineStatusAnalyzeFailed
		if err != nil {
			return err
		}
		return fmt.Errorf("need retry")
	}
	// no need retry
	if err != nil {
		pre.Task.Status = apistructs.PipelineStatusAnalyzeFailed
		pre.Task.Inspect.Errors = pre.Task.Inspect.Errors.AppendError(&taskerror.Error{Msg: err.Error()})
		return nil
	}

	logrus.Infof("reconciler: pipelineID: %d, taskID: %d, taskName: %s end prepare (%s -> %s)",
		pre.P.ID, pre.Task.ID, pre.Task.Name, apistructs.PipelineStatusAnalyzed, apistructs.PipelineStatusBorn)
	return nil
}

func (pre *prepare) WhenLogicError(err error) error {
	pre.Task.Status = apistructs.PipelineStatusAnalyzeFailed
	return nil
}

func (pre *prepare) WhenTimeout() error {
	return nil
}

func (pre *prepare) WhenCancel() error {
	return pre.TaskRun().WhenCancel()
}

func (pre *prepare) TimeoutConfig() (<-chan struct{}, context.CancelFunc, time.Duration) {
	return nil, nil, -1
}

func (pre *prepare) TuneTriggers() taskrun.TaskOpTuneTriggers {
	return taskrun.TaskOpTuneTriggers{
		BeforeProcessing: aoptypes.TuneTriggerTaskBeforePrepare,
		AfterProcessing:  aoptypes.TuneTriggerTaskAfterPrepare,
	}
}

// makeTaskRun return flag needRetry and err.
func (pre *prepare) makeTaskRun() (needRetry bool, err error) {
	task := pre.Task
	p := pre.P
	tasks, err := pre.DBClient.ListPipelineTasksByPipelineID(p.ID)
	if err != nil {
		return true, err
	}

	// 如果 task 已经是终态，无需 taskRun
	if task.Status.IsEndStatus() {
		return false, nil
	}

	// 获取集群信息
	cluster, err := pre.ClusterInfo.GetClusterInfoByName(p.ClusterName)
	if err != nil {
		return true, apierrors.ErrGetCluster.InternalError(err)
	}
	clusterInfo := cluster.CM
	pre.Ctx = context.WithValue(pre.Ctx, apistructs.ClusterNameContextKey, p.ClusterName)

	// TODO 目前 initSQL 需要存储在 网盘上，暂时不能用 volume 来解
	mountPoint := clusterInfo.MustGet(apistructs.DICE_STORAGE_MOUNTPOINT)

	// 解析 pipeline yml
	refs := pipelineyml.Refs{}
	workdirs := pvolumes.GetAvailableTaskContainerWorkdirs(tasks, *task)
	for k, v := range workdirs {
		refs[k] = v
	}
	// OUTPUT
	dbOutputs, err := pre.DBClient.GetPipelineOutputs(p.ID)
	if err != nil {
		return true, apierrors.ErrGetPipelineOutputs.InternalError(err)
	}
	outputs := pipelineyml.Outputs{}
	for actionAlias, kvs := range dbOutputs {
		outputs[pipelineyml.ActionAlias(actionAlias)] = kvs
	}

	allSecrets := make(map[string]string)
	for k, v := range p.Snapshot.Secrets {
		allSecrets[k] = v
	}
	for k, v := range p.Snapshot.PlatformSecrets { // platformSecrets 的优先级更高
		allSecrets[k] = v
	}
	for fileName, fileUUID := range p.Snapshot.CmsDiceFiles {
		// cmsDiceFiles 生成容器内的路径
		// ((a.cert)) -> /.pipeline/container/cms/dice_files/a.cert
		fileContainerPath := pvolumes.MakeTaskContainerDiceFilesPath(fileName)
		allSecrets[fileName] = fileContainerPath
		// 作为特殊上下文，由 agent 处理
		task.Context.CmsDiceFiles = append(task.Context.CmsDiceFiles, pvolumes.GenerateTaskDiceFileVolume(fileName, fileUUID, fileContainerPath))
	}
	pipelineYml, err := pipelineyml.New(
		[]byte(p.PipelineYml),
		pipelineyml.WithEnvs(p.Snapshot.Envs),
		pipelineyml.WithSecrets(allSecrets),
		pipelineyml.WithAliasesToCheckRefOp(p.Labels, pipelineyml.ActionAlias(task.Name)),
		pipelineyml.WithRefs(refs),
		pipelineyml.WithRefOpOutputs(outputs),
		pipelineyml.WithActionTypeMapping(conf.ActionTypeMapping()),
		//pipelineyml.WithRenderSnippet(p.Labels, p.Snippets),
		pipelineyml.WithFlatParams(true),
		pipelineyml.WithRunParams(p.Snapshot.RunPipelineParams),
		pipelineyml.WithTriggerLabels(p.Labels),
	)
	if err != nil {
		return false, errorsx.UserErrorf(err.Error())
	}

	// 从 extension marketplace 获取 image 和 resource limit
	extSearchReq := make([]string, 0)
	extSearchReq = append(extSearchReq, getActionAgentTypeVersion())
	extSearchReq = append(extSearchReq, pre.ActionMgr.MakeActionTypeVersion(&task.Extra.Action))
	actionDiceYmlJobMap, actionSpecYmlJobMap, err := pre.ActionMgr.SearchActions(extSearchReq, pre.ActionMgr.MakeActionLocationsBySource(p.PipelineSource),
		actionmgr.SearchOpWithRender(map[string]string{"storageMountPoint": mountPoint}))
	if err != nil {
		return true, err
	}

	// 校验 action agent
	agentDiceYmlJob := actionDiceYmlJobMap[getActionAgentTypeVersion()]
	if agentDiceYmlJob == nil || agentDiceYmlJob.Image == "" {
		return false, apierrors.ErrDownloadActionAgent.InvalidState(fmt.Sprintf("not found agent image (%s)", getActionAgentTypeVersion()))
	}
	agentMD5, ok := agentDiceYmlJob.Labels["MD5"]
	if !ok || agentMD5 == "" {
		return false, apierrors.ErrValidateActionAgent.MissingParameter("MD5 (labels)")
	}
	if err := pre.ActionAgentSvc.Ensure(clusterInfo, agentDiceYmlJob.Image, agentMD5); err != nil {
		return true, err
	}

	// action
	action, err := pipelineyml.GetAction(pipelineYml.Spec(), pipelineyml.ActionAlias(task.Name))
	if err != nil {
		return false, apierrors.ErrParsePipelineYml.InternalError(err)
	}
	task.Extra.Action = *action
	// --- uuid ---
	task.Extra.UUID = fmt.Sprintf("pipeline-task-%d", task.ID)
	task.Extra.EncryptSecretKeys = p.Snapshot.EncryptSecretKeys

	const (
		PipelineTaskLogID = "PIPELINE_TASK_LOG_ID"
		PipelineDebugMode = "PIPELINE_DEBUG_MODE"
		AgentEnvPrefix    = "ACTIONAGENT_"
		PipelineTimeBegin = "PIPELINE_TIME_BEGIN_TIMESTAMP"
	)

	// --- envs ---
	if task.Extra.PrivateEnvs == nil {
		task.Extra.PrivateEnvs = make(map[string]string)
	}
	if task.Extra.PublicEnvs == nil {
		task.Extra.PublicEnvs = make(map[string]string)
	}
	// global envs
	for k, v := range pipelineYml.Spec().Envs {
		task.Extra.PrivateEnvs[k] = v
	}
	// action params -> envs
	for k, v := range action.Params {
		newK := strings.Replace(strings.Replace(strings.ToUpper(k), ".", "_", -1), "-", "_", -1)
		task.Extra.PrivateEnvs["ACTION_"+newK] = fmt.Sprintf("%v", v)
	}
	// secrets -> envs
	for k, v := range p.Snapshot.Secrets {
		newK := strings.Replace(strings.Replace(strings.ToUpper(k), ".", "_", -1), "-", "_", -1)
		task.Extra.PrivateEnvs["PIPELINE_SECRET_"+newK] = v
		task.Extra.PrivateEnvs[newK] = v
	}
	// platform secrets -> envs
	for k, v := range p.Snapshot.PlatformSecrets {
		newK := strings.Replace(strings.Replace(strings.ToUpper(k), ".", "_", -1), "-", "_", -1)
		// snippet 逻辑，可能之前 task 创建的时候给自己设置了 appID 和 appName
		if existContinuePrivateEnv(task.Extra.PrivateEnvs, newK) {
			continue
		}
		task.Extra.PrivateEnvs[newK] = v
	}
	// action agent envs -> envs
	for k, v := range agentDiceYmlJob.Envs {
		// enable agent debug mode at dice.yml envs.
		// If set to privateEnvs, cannot set to debug mode if agent invoke platform to fetch privateEnvs failed.
		task.Extra.PublicEnvs[AgentEnvPrefix+k] = v
	}
	task.Extra.PublicEnvs["PIPELINE_ID"] = strconv.FormatUint(p.ID, 10)
	task.Extra.PublicEnvs["PIPELINE_TASK_ID"] = fmt.Sprintf("%v", task.ID)
	task.Extra.PublicEnvs["PIPELINE_TASK_NAME"] = task.Name
	task.Extra.PublicEnvs[PipelineTaskLogID] = task.Extra.UUID
	task.Extra.PublicEnvs[PipelineDebugMode] = "false"
	task.Extra.PrivateEnvs[actionagent.CONTEXTDIR] = pvolumes.ContainerContextDir
	task.Extra.PrivateEnvs[actionagent.WORKDIR] = pvolumes.MakeTaskContainerWorkdir(task.Name)
	task.Extra.PrivateEnvs[actionagent.METAFILE] = pvolumes.MakeTaskContainerMetafilePath(task.Name)
	task.Extra.PrivateEnvs[actionagent.UPLOADDIR] = pvolumes.ContainerUploadDir
	task.Extra.PublicEnvs[pvolumes.EnvKeyMesosFetcherURI] = pvolumes.MakeMesosFetcherURI4AliyunRegistrySecret(mountPoint)
	task.Extra.PublicEnvs[PipelineTimeBegin] = strconv.FormatInt(time.Now().Unix(), 10)
	if p.TimeBegin != nil {
		task.Extra.PublicEnvs[PipelineTimeBegin] = strconv.FormatInt(p.TimeBegin.Unix(), 10)
	}
	// handle dice openapi
	for k, v := range task.Extra.PrivateEnvs {
		if strings.HasPrefix(k, "DICE_OPENAPI_") {
			task.Extra.PublicEnvs[k] = v
		}
	}

	// --- labels ---
	if task.Extra.Labels == nil {
		task.Extra.Labels = make(map[string]string)
	}
	task.Extra.Labels[apistructs.TerminusDefineTag] = task.Extra.UUID

	// --- image ---
	// 所有 action，包括 custom-script，都需要在 ext market 注册；
	// 从 ext market 获取 action 的 job dice.yml，解析 image 和 resource；
	// 只有 custom-script 可以设置自定义镜像，优先级高于默认自定义镜像。
	diceYmlJob, ok := actionDiceYmlJobMap[pre.ActionMgr.MakeActionTypeVersion(action)]
	if !ok || diceYmlJob == nil || diceYmlJob.Image == "" {
		return false, apierrors.ErrRunPipeline.InvalidState(
			fmt.Sprintf("not found image, actionType: %q, version: %q", action.Type, action.Version))
	}
	task.Extra.Image = diceYmlJob.Image
	if action.Type.IsCustom() && action.Image != "" {
		task.Extra.Image = action.Image
	}
	// 将 action dice.yml 中声明的 envs 注入运行时
	for k, v := range diceYmlJob.Envs {
		task.Extra.PrivateEnvs[k] = v
	}
	// 将 action dice.yml 中声明的 labels 注入运行时
	for k, v := range diceYmlJob.Labels {
		task.Extra.Labels[k] = v
	}

	// 调度相关标签
	task.Extra.Labels[apistructs.EnvDiceWorkspace] = string(p.Extra.DiceWorkspace)
	task.Extra.Labels[apistructs.EnvDiceOrgName] = p.GetOrgName()
	task.Extra.Labels[apistructs.EnvDiceOrgID] = p.MergeLabels()[apistructs.LabelOrgID]
	// 若 action 未声明 dice.yml labels，则由平台根据 source 按照默认规则分配调度标签
	if len(diceYmlJob.Labels) == 0 {
		// 大数据任务加上 JOB_KIND = bigdata，调度到有大数据标签的机器上
		// 非大数据任务带上 PACK = true 的标
		if p.PipelineSource.IsBigData() {
			task.Extra.Labels[apistructs.LabelJobKind] = apistructs.TagBigdata
			for key, label := range p.MergeLabels() {
				if key == labelconfig.BIGDATA_AFFINITY_LABELS {
					task.Extra.Labels[key] = label
				}
			}
		} else {
			task.Extra.Labels[apistructs.LabelPack] = "true"
		}
	}

	// --- task containers ---
	taskContainers, err := containers.GenContainers(task)
	if err != nil {
		return false, apierrors.ErrRunPipeline.InvalidState(
			fmt.Sprintf("failed to make task containers err: %v", err))
	}
	task.Extra.TaskContainers = taskContainers

	// --- resource ---
	container_provider.DealTaskRuntimeResource(task)
	if diceYmlJob.Resources.Disk > 0 {
		task.Extra.RuntimeResource.Disk = float64(diceYmlJob.Resources.Disk)
	}
	if action.Resources.Disk > 0 {
		task.Extra.RuntimeResource.Disk = float64(action.Resources.Disk)
	}

	// resource 相关环境变量
	task.Extra.PublicEnvs["PIPELINE_LIMITED_CPU"] = fmt.Sprintf("%g", task.Extra.RuntimeResource.MaxCPU)
	task.Extra.PublicEnvs["PIPELINE_LIMITED_MEM"] = fmt.Sprintf("%g", task.Extra.RuntimeResource.MaxMemory)
	task.Extra.PublicEnvs["PIPELINE_LIMITED_DISK"] = fmt.Sprintf("%g", task.Extra.RuntimeResource.Disk)
	task.Extra.PublicEnvs["PIPELINE_REQUESTED_CPU"] = fmt.Sprintf("%g", task.Extra.RuntimeResource.CPU)
	task.Extra.PublicEnvs["PIPELINE_REQUESTED_MEM"] = fmt.Sprintf("%g", task.Extra.RuntimeResource.Memory)
	task.Extra.PublicEnvs["PIPELINE_REQUESTED_DISK"] = fmt.Sprintf("%g", task.Extra.RuntimeResource.Disk)

	// edge pipeline envs
	edgePipelineEnvs := pre.EdgeRegister.GetEdgePipelineEnvs()
	task.Extra.PublicEnvs[apistructs.EnvIsEdgePipeline] = strconv.FormatBool(pre.EdgeRegister.IsEdge())
	task.Extra.PublicEnvs[apistructs.EnvPipelineAddr] = edgePipelineEnvs.Get(apistructs.ClusterManagerDataKeyPipelineAddr)

	// 条件表达式存在
	if jump := condition(task); jump {
		return false, nil
	}

	if p.Extra.StorageConfig.EnableNFSVolume() &&
		!p.Extra.StorageConfig.EnableShareVolume() &&
		task.ExecutorKind.IsK8sKind() {
		// --- cmd ---
		// task.Context.InStorages
	continueContextVolumes:
		for _, out := range pvolumes.GetAvailableTaskOutStorages(tasks) {
			name := out.Name
			// 如果在 task 的 output 中存在，则不需要注入上次结果
			for _, output := range task.Extra.Action.Namespaces {
				if name == output {
					continue continueContextVolumes
				}
			}
			// 如果 stageOrder >= 当前 order，不注入，只注入前置 stage 的 volume
			if len(out.Labels) == 0 {
				continue
			}
			stageOrderStr, ok := out.Labels["stageOrder"]
			if !ok {
				continue
			}
			stageOrder, err := strconv.Atoi(stageOrderStr)
			if err != nil {
				return false, apierrors.ErrParsePipelineContext.InternalError(err)
			}
			if stageOrder >= task.Extra.StageOrder {
				// 如果说 action 的 need 中有对应的挂载的名称，对应的就是 snippet 的状态，各个 task 的 stageOrder 相等会导致的问题
				// 这时候还是给对应的 inStorage 挂载上
				for _, need := range task.Extra.Action.Needs {
					if need.String() == out.Name {
						task.Context.InStorages = append(task.Context.InStorages, out)
						continue continueContextVolumes
					}
				}
				continue
			}

			task.Context.InStorages = append(task.Context.InStorages, out)
		}
	}

	// for get action callback openapi oauth2 token
	specYmlJob, ok := actionSpecYmlJobMap[pre.ActionMgr.MakeActionTypeVersion(action)]
	if !ok || specYmlJob == nil {
		return false, apierrors.ErrRunPipeline.InvalidState(
			fmt.Sprintf("not found action spec, actionType: %q, version: %q", action.Type, action.Version))
	}
	task.Extra.OpenapiOAuth2TokenPayload = apistructs.OAuth2TokenPayload{
		AccessTokenExpiredIn: handleAccessTokenExpiredIn(task),
		AccessibleAPIs: append(specYmlJob.AccessibleAPIs,
			// PIPELINE_PLATFORM_CALLBACK
			apistructs.AccessibleAPI{
				Path:   "/api/pipelines/actions/callback",
				Method: http.MethodPost,
				Schema: "http",
			},
		),
		Metadata: map[string]string{
			"pipelineID":                  strconv.FormatUint(task.PipelineID, 10),
			"taskID":                      strconv.FormatUint(task.ID, 10),
			httputil.UserHeader:           pre.P.GetRunUserID(),
			httputil.InternalHeader:       handleInternalClient(pre.P),
			httputil.InternalActionHeader: handleInternalClient(pre.P),
			httputil.OrgHeader:            pre.P.Labels[apistructs.LabelOrgID],
		},
	}

	// task.Context.OutStorages
	if p.Extra.StorageConfig.EnableShareVolume() && task.ExecutorKind == spec.PipelineTaskExecutorKindK8sJob {
		// only k8sjob support create job volume
		k8sjobExecutor, ok := pre.Executor.(*k8sjob.K8sJob)
		if !ok {
			goto makeOutStorages
		}
		// 添加共享pv
		if p.Extra.ShareVolumeID == "" {
			var volumeID string
			volumeID, err = k8sjobExecutor.JobVolumeCreate(context.Background(), apistructs.JobVolume{
				Namespace:   p.Extra.Namespace,
				Name:        "local-pv-default",
				Type:        "local",
				Executor:    "",
				ClusterName: p.ClusterName,
				Kind:        "",
			})
			if err != nil {
				return true, fmt.Errorf("error create createJobVolume: %v", err)
			}
			p.Extra.ShareVolumeID = volumeID
			err = pre.DBClient.UpdatePipelineExtraByPipelineID(p.ID, &p.PipelineExtra)
			if err != nil {
				return true, err
			}
		}
		task.Context.OutStorages = append(task.Context.OutStorages,
			pvolumes.GenerateLocalVolume(p.Extra.Namespace, &p.Extra.ShareVolumeID))
		isNewWorkspace := false
		if specYmlJob != nil {
			_, isNewWorkspace = specYmlJob.Labels["new_workspace"]
		}
		if isNewWorkspace {
			// action带有new_workspace标签,使用独立目录
			task.Extra.PrivateEnvs[actionagent.WORKDIR] = pvolumes.MakeTaskContainerWorkdir(task.Name)
		} else {
			if len(p.Extra.TaskWorkspaces) > 0 {
				// 使用现有目录
				task.Extra.PrivateEnvs[actionagent.WORKDIR] = pvolumes.MakeTaskContainerWorkdir(p.Extra.TaskWorkspaces[0])
			} else {
				// 没有有效的workspace,使用根目录
				task.Extra.PrivateEnvs[actionagent.WORKDIR] = pvolumes.MakeTaskContainerWorkdir("")
			}
		}
		if task.Extra.Action.Workspace != "" {
			// 显式定义了workdir,使用指定值
			task.Extra.PrivateEnvs[actionagent.WORKDIR] = pvolumes.MakeTaskContainerWorkdir(task.Extra.Action.Workspace)
		}

		for _, namespace := range task.Extra.Action.Namespaces {
			task.Context.OutStorages = append(task.Context.OutStorages, pvolumes.GenerateFakeVolume(
				namespace,
				task.Extra.PrivateEnvs[actionagent.WORKDIR],
				&p.Extra.ShareVolumeID))
		}

	}

makeOutStorages:
	if p.Extra.StorageConfig.EnableNFSVolume() &&
		!p.Extra.StorageConfig.EnableShareVolume() &&
		task.ExecutorKind.IsK8sKind() {
		for _, namespace := range task.Extra.Action.Namespaces {
			task.Context.OutStorages = append(task.Context.OutStorages, pvolumes.GenerateTaskVolume(*task, namespace, nil))
		}
	}

	// loop
	// 若 retriedTimes != nil，说明已经是在循环了，不能重新赋值
	if task.Extra.LoopOptions == nil {
		task.Extra.LoopOptions = getLoopOptions(*specYmlJob, action.Loop)
	}

	// dedup context
	task.Context.Dedup()
	// cmd
	cmd, args, err := generateTaskCMDs(action, task.Context, p.ID, task.ID)
	if err != nil {
		return false, apierrors.ErrRunPipeline.InternalError(err)
	}
	task.Extra.Cmd = cmd
	task.Extra.CmdArgs = args
	// --- status ---
	if task.Status == apistructs.PipelineStatusAnalyzed {
		task.Status = apistructs.PipelineStatusBorn
	}

	if (p.Extra.StorageConfig.EnableNFSVolume() || p.Extra.StorageConfig.EnableShareVolume()) && task.ExecutorKind.IsK8sKind() {
		// 处理 task caches
		pvolumes.HandleTaskCacheVolumes(p, task, diceYmlJob, mountPoint)
		// --- binds ---
		task.Extra.Binds = pvolumes.GenerateTaskCommonBinds(mountPoint)
		jobBinds, err := pvolumes.ParseDiceYmlJobBinds(diceYmlJob)
		if err != nil {
			return false, apierrors.ErrRunPipeline.InternalError(err)
		}
		for _, bind := range jobBinds {
			task.Extra.Binds = append(task.Extra.Binds, bind)
		}
		// --- volumes ---
		task.Extra.Volumes = contextVolumes(task.Context)
	}

	// --- preFetcher ---
	const agentHostPath = "/devops/ci/action-agent/agent"
	task.Extra.PreFetcher = &apistructs.PreFetcher{
		FileFromImage: agentDiceYmlJob.Image,
		ContainerPath: conf.AgentPreFetcherDestDir(), // 文件要被拷贝到的目录
		// 非 k8s 集群从网盘加载 action-agent
		// 需要和 download_file_from_image 脚本内的路径要保持一致
		FileFromHost: filepath.Dir(filepath.Join(mountPoint, agentHostPath)),
	}
	task.Extra.PublicEnvs["AGENT_PRE_FETCHER_DEST_DIR"] = conf.AgentPreFetcherDestDir()

	// pull bootstrap info
	if err := pre.generateOpenapiTokenForPullBootstrapInfo(task); err != nil {
		return true, err
	}

	return false, nil
}

func existContinuePrivateEnv(privateEnvs map[string]string, key string) bool {
	if privateEnvs[apistructs.DiceApplicationName] != "" && key == apistructs.DiceApplicationName {
		return true
	}
	if privateEnvs[apistructs.DiceApplicationId] != "" && key == apistructs.DiceApplicationId {
		return true
	}
	if privateEnvs[apistructs.DiceWorkspaceEnv] != "" && key == apistructs.DiceWorkspaceEnv {
		return true
	}
	if privateEnvs[apistructs.GittarBranchEnv] != "" && key == apistructs.GittarBranchEnv {
		return true
	}
	return false
}

func handleAccessTokenExpiredIn(task *spec.PipelineTask) string {
	if task.Extra.Timeout == -1 {
		return "0"
	}
	if task.Extra.Timeout < -1 || task.Extra.Timeout == 0 {
		task.Extra.Timeout = conf.TaskDefaultTimeout()
	}
	return fmt.Sprintf("%ds", task.Extra.Timeout/time.Duration(math.Pow10(9))+30) // 增加 30s 用于 agent 结束信号处理
}

func handleInternalClient(p *spec.Pipeline) string {
	if p.Extra.InternalClient != "" {
		return p.Extra.InternalClient
	}
	return "pipeline-signed-openapi-token"
}

func generateTaskCMDs(action *pipelineyml.Action, taskCtx spec.PipelineTaskContext,
	pipelineID, pipelineTaskID uint64) (cmd string, args []string, err error) {
	// --- cmd ---
	// action agent 作为启动命令
	cmd = conf.AgentContainerPathWhenExecute()

	// --- args ---
	// action agent 的参数作为 args; agent 目前只接收一个 base64 编码的 ActionAgentReq 参数

	agentArg := actionagent.NewAgentArgForPull(pipelineID, pipelineTaskID)
	reqByte, err := json.Marshal(agentArg)
	if err != nil {
		return "", nil, apierrors.ErrRunPipeline.InternalError(err)
	}
	args = []string{base64.StdEncoding.EncodeToString(reqByte)}
	return
}

// return: agent@1.0
func getActionAgentTypeVersion() string {
	return "agent@1.0"
}

func contextVolumes(context spec.PipelineTaskContext) []metadata.MetadataField {
	vos := make([]metadata.MetadataField, 0)
	for _, vo := range append(context.InStorages, context.OutStorages...) {
		vos = append(vos, vo)
	}
	return vos
}

func (pre *prepare) generateOpenapiTokenForPullBootstrapInfo(task *spec.PipelineTask) error {

	if task.Type == apistructs.ActionTypeWait || task.Type == apistructs.ActionTypeAPITest || task.Type == apistructs.ActionTypeSnippet {
		return nil
	}

	var tokenInfo *apistructs.OAuth2Token
	var err error
	// the applied token can only request the get-bootstrap-info api, and ensure that pipelineID and taskID must match
	req := apistructs.OAuth2TokenGetRequest{
		ClientID:     "pipeline",
		ClientSecret: "devops/pipeline",
		Payload: apistructs.OAuth2TokenPayload{
			AccessTokenExpiredIn: "0", // there is currently no timeout period from the time the token is applied until the agent runs, so setting 0 means it will not expire
			AllowAccessAllAPIs:   false,
			AccessibleAPIs: []apistructs.AccessibleAPI{
				// PIPELINE_TASK_GET_BOOTSTRAP_INFO
				{
					Path:   "/api/pipelines/<pipelineID>/tasks/<taskID>/actions/get-bootstrap-info",
					Method: http.MethodGet,
					Schema: "http",
				},
				// CMDB_FILE_DOWNLOAD
				{
					Path:   "/api/files",
					Method: http.MethodGet,
					Schema: "http",
				},
				// CMDB_FILE_UPLOAD
				{
					Path:   "/api/files",
					Method: http.MethodPost,
					Schema: "http",
				},
			},
			Metadata: map[string]string{
				"pipelineID":            strconv.FormatUint(task.PipelineID, 10),
				"taskID":                strconv.FormatUint(task.ID, 10),
				httputil.UserHeader:     pre.P.GetRunUserID(),
				httputil.InternalHeader: pre.P.Extra.InternalClient,
				httputil.OrgHeader:      pre.P.Labels[apistructs.LabelOrgID],
			},
		},
	}
	if pre.EdgeRegister.IsEdge() {
		tokenInfo, err = pre.EdgeRegister.GetAccessToken(req)
	} else {
		tokenInfo, err = pre.Bdl.GetOAuth2Token(req)
	}
	if err != nil {
		return err
	}
	task.Extra.PublicEnvs[apistructs.EnvOpenapiTokenForActionBootstrap] = tokenInfo.AccessToken
	return nil
}

// getLoopOptions 从 action spec.yml 定义和 action 运行时配置中获取 loop 选项
func getLoopOptions(actionSpec apistructs.ActionSpec, taskLoop *apistructs.PipelineTaskLoop) *apistructs.PipelineTaskLoopOptions {
	// 均未声明，则为空
	if actionSpec.Loop == nil && taskLoop == nil {
		return nil
	}
	opt := apistructs.PipelineTaskLoopOptions{
		TaskLoop:       taskLoop,
		SpecYmlLoop:    actionSpec.Loop,
		CalculatedLoop: nil,
		LoopedTimes:    apistructs.TaskLoopTimeBegin, // 当前这次运行即为 1
	}
	// calculate
	//
	opt.CalculatedLoop = opt.SpecYmlLoop.Duplicate()
	if opt.TaskLoop != nil {
		opt.CalculatedLoop = opt.TaskLoop.Duplicate()
	}

	// 默认值
	if opt.CalculatedLoop.Break == "" {
		// 默认退出条件为任务成功
		opt.CalculatedLoop.Break = `task_status == 'Success'`
	}
	if opt.CalculatedLoop.Strategy == nil {
		// 默认策略
		opt.CalculatedLoop.Strategy = &apistructs.PipelineTaskDefaultLoopStrategy
	}

	if opt.CalculatedLoop.Strategy.IntervalSec == 0 {
		opt.CalculatedLoop.Strategy.IntervalSec = apistructs.PipelineTaskDefaultLoopStrategy.IntervalSec
	}

	if opt.CalculatedLoop.Strategy.DeclineRatio <= 0 {
		if opt.CalculatedLoop.Strategy.IntervalSec == 0 {
			opt.CalculatedLoop.Strategy.DeclineRatio = apistructs.PipelineTaskDefaultLoopStrategy.DeclineRatio
		} else {
			opt.CalculatedLoop.Strategy.DeclineRatio = 1
		}
	}

	if opt.CalculatedLoop.Strategy.DeclineLimitSec == 0 {
		if opt.CalculatedLoop.Strategy.IntervalSec == 0 {
			opt.CalculatedLoop.Strategy.DeclineLimitSec = apistructs.PipelineTaskDefaultLoopStrategy.DeclineLimitSec
		} else {
			opt.CalculatedLoop.Strategy.DeclineLimitSec = int64(opt.CalculatedLoop.Strategy.IntervalSec)
		}
	}

	return &opt
}

func condition(task *spec.PipelineTask) bool {

	// 条件判断不存在就跳过
	if task.Extra.Action.If == "" {
		return false
	}

	sign := expression.Reconcile(task.Extra.Action.If)
	if sign.Err != nil {
		task.Status = apistructs.PipelineStatusFailed
		if sign.Err != nil {
			task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{
				Msg: sign.Err.Error(),
			})
		}

		if sign.Msg != "" {
			task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{
				Msg: sign.Msg,
			})
		}
		return true
	}

	if sign.Sign == expression.TaskJumpOver {
		task.Status = apistructs.PipelineStatusNoNeedBySystem
		if sign.Err != nil {
			task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{
				Msg: sign.Err.Error(),
			})
		}

		if sign.Msg != "" {
			task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{
				Msg: sign.Msg,
			})
		}
		return true
	}

	return false
}
