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

package apistructs

const (
	LabelOrgID   = "orgID"
	LabelOrgName = "orgName"

	LabelProjectID   = "projectID"
	LabelProjectName = "projectName"

	LabelTestPlanID = "testPlanID"

	LabelAppID   = "appID"
	LabelAppName = "appName"

	LabelDiceWorkspace = "diceWorkspace"

	LabelBranch       = "branch"
	LabelCommit       = "commit"
	LabelCommitDetail = "commitDetail"

	LabelPipelineYmlSource       = "pipelineYmlSource"
	LabelPipelineType            = "pipelineType"
	LabelPipelineTriggerMode     = "pipelineTriggerMode"
	LabelPipelineCronTriggerTime = "pipelineCronTriggerTime"
	LabelPipelineCronID          = "pipelineCronID"
	LabelPipelineCronCompensated = "cronCompensated"

	LabelBindPipelineQueueID               = "__bind_queue_id"
	LabelBindPipelineQueueCustomPriority   = "__bind_queue_custom_priority"
	LabelBindPipelineQueueEnqueueCondition = "__bind_queue_enqueue_condition"

	LabelUserID = "userID"

	LabelRunUserID    = "runUserID"
	LabelCreateUserID = "createUserID"

	// ---------------------- snippet some global labels
	// action
	LabelActionVersion = "actionVersion"
	LabelActionJson    = "actionJson"

	// dice
	LabelDiceSnippetScopeID   = "scopeID"
	LabelChooseSnippetVersion = "chooseVersion"
	// snippet
	LabelSnippetScope     = "snippet_scope"
	LabelActionEnv        = "action_env"
	DiceApplicationId     = "DICE_APPLICATION_ID"
	DiceApplicationName   = "DICE_APPLICATION_NAME"
	DiceWorkspaceEnv      = "DICE_WORKSPACE"
	GittarBranchEnv       = "GITTAR_BRANCH"
	LabelGittarYmlPath    = "gittarYmlPath"    // app snippetConfig label in order to specify the address of yml to address
	LabelAutotestExecType = "autotestExecType" // 新版自动化测试的snippet的执行类型
	LabelSceneSetID       = "sceneSetID"       // 新版自动化测试的场景集的 id
	LabelSceneID          = "sceneID"          // 新版自动化测试的场景的 id
	LabelSpaceID          = "spaceID"          // 空间 id
	LabelIterationID      = "iterationID"
	LabelIsRefSet         = "isRefSet"
	// FDP
	LabelFdpWorkflowID          = "CDP_WF_ID"
	LabelFdpWorkflowName        = "CDP_WF_NAME"
	LabelFdpWorkflowProcessType = "CDP_WF_PROCESS_TYPE"
	LabelFdpWorkflowRuntype     = "CDP_WF_RUNTYPE"
)
