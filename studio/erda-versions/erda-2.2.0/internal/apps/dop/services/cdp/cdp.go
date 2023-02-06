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

// Package cdp pipeline相关的结构信息
package cdp

import (
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/conf"
)

var (
	defaultMaxLogLines   = 10
	defaultMaxLogColumns = 1024
)

// CDP pipeline 结构体
type CDP struct {
	bdl           *bundle.Bundle
	resourceTrans i18n.Translator
}

// Option CDP 配置选项
type Option func(*CDP)

// New CDP service
func New(options ...Option) *CDP {
	r := &CDP{}
	for _, op := range options {
		op(r)
	}
	return r
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(f *CDP) {
		f.bdl = bdl
	}
}

func WithResourceTranslator(resourceTrans i18n.Translator) Option {
	return func(f *CDP) {
		f.resourceTrans = resourceTrans
	}
}

func (cdp *CDP) CdpNotifyProcess(pipelineEvent *apistructs.PipelineInstanceEvent) error {
	pipelineData := pipelineEvent.Content
	orgID, err := strconv.ParseInt(pipelineEvent.OrgID, 10, 64)
	if err != nil {
		return err
	}
	org, err := cdp.bdl.GetOrg(orgID)
	if err != nil {
		logrus.Errorf("failed to get org info err: %s", err)
		return err
	}
	pipelineDetail, err := cdp.bdl.GetPipeline(pipelineData.PipelineID)
	if err != nil {
		return err
	}
	if strings.Index(pipelineData.Source, "cdp-") == 0 {
		logrus.Infof("cdp event: %+v", pipelineEvent)
		workflowID, ok := pipelineDetail.Labels["CDP_WF_ID"]
		if !ok {
			return fmt.Errorf("failed to get workflow id pipelineID:%d", pipelineData.PipelineID)
		}
		workflowName, ok := pipelineDetail.Labels["CDP_WF_NAME"]
		if !ok {
			return fmt.Errorf("failed to get workflow name pipelineID:%d", pipelineData.PipelineID)
		}
		fdpNotifyLabel, fdpOK := pipelineDetail.Labels["FDP_NOTIFY_LABLE"]
		if !ok {
			logrus.Infof("failed to get FDP_NOTIFY_LABLE: %d", pipelineData.PipelineID)
		}
		logrus.Debugf("fdpNotifyLabel and pipelineID is: %s, %d", fdpNotifyLabel, pipelineData.PipelineID)
		notifyDetails, err := cdp.bdl.QueryNotifiesBySource(pipelineEvent.OrgID, "workflow", workflowID,
			pipelineData.Status, fdpNotifyLabel, pipelineDetail.ClusterName)
		if err != nil {
			return err
		}
		// 一条事件多个接收者时，一个接收者发生错误后，需要继续进行下去
		for _, notifyDetail := range notifyDetails {
			if notifyDetail.NotifyGroup == nil {
				continue
			}
			var notifyItem *apistructs.NotifyItem
			for _, item := range notifyDetail.NotifyItems {
				if item.Name == pipelineData.Status {
					notifyItem = item
				}
			}
			if notifyItem == nil {
				continue
			}

			params := map[string]string{
				"pipelineID":     strconv.FormatUint(pipelineData.PipelineID, 10),
				"workflowID":     workflowID,
				"workflowName":   workflowName,
				"notifyItemName": notifyItem.DisplayName,
				"clusterName":    pipelineData.ClusterName,
			}
			if fdpOK {
				// fdp 的通知历史记录label
				params["fdpNotifyLabel"] = fdpNotifyLabel
			}

			//失败情况尝输出错误日志
			if notifyItem.Name == "Failed" {
				time.Sleep(time.Second * 5)
				failedDetailLogs, err := cdp.getFailedTaskLogs(pipelineDetail, true)
				if err != nil {
					logrus.Errorf("get cdp workflow's failed log err: %v", err)
					continue
				}
				params["failedDetail"] = failedDetailLogs

			}
			// 不使用notifyItemID是为了支持第三方通知项,如监控
			eventboxReqContent := apistructs.GroupNotifyContent{
				SourceName:            workflowName,
				SourceType:            "workflow",
				SourceID:              workflowID,
				NotifyName:            notifyDetail.Name,
				NotifyItemDisplayName: notifyItem.DisplayName,
				Channels:              []apistructs.GroupNotifyChannel{},
				Label:                 notifyItem.Label,
				CalledShowNumber:      notifyItem.CalledShowNumber,
				ClusterName:           pipelineDetail.ClusterName,
				OrgID:                 orgID,
			}

			err := cdp.bdl.CreateGroupNotifyEvent(apistructs.EventBoxGroupNotifyRequest{
				Sender:        "adapter",
				GroupID:       notifyDetail.NotifyGroup.ID,
				Channels:      notifyDetail.Channels,
				NotifyItem:    notifyItem,
				NotifyContent: &eventboxReqContent,
				Params:        params,
			})
			if err != nil {
				logrus.Errorf("create group notify event failed err: %v", err)
				continue
			}
		}
	} else {
		logrus.Infof("pipeline event: %+v", pipelineEvent)
		//普通pipeline
		eventName := "pipeline_" + strings.ToLower(pipelineData.Status)
		sourceType := "app"
		sourceID := strconv.FormatUint(pipelineDetail.ApplicationID, 10)
		notifyDetails, err := cdp.bdl.QueryNotifiesBySource(pipelineEvent.OrgID,
			sourceType, sourceID, eventName, "")
		if err != nil {
			return err
		}
		// 一条事件多个接收者时，一个接收者发生错误后，需要继续进行下去
		for _, notifyDetail := range notifyDetails {
			if notifyDetail.NotifyGroup == nil {
				continue
			}
			notifyItem := notifyDetail.NotifyItems[0]
			lang, _ := i18n.ParseLanguageCode(org.Locale)
			params := map[string]string{
				"pipelineID":     strconv.FormatUint(pipelineData.PipelineID, 10),
				"notifyItemName": notifyItem.DisplayName,
				"appID":          strconv.FormatUint(pipelineDetail.ApplicationID, 10),
				"appName":        pipelineDetail.ApplicationName,
				"projectID":      strconv.FormatUint(pipelineDetail.ProjectID, 10),
				"projectName":    pipelineDetail.ProjectName,
				"orgName":        org.Name,
				"branch":         pipelineDetail.Branch,
				"uiPublicURL":    conf.UIPublicURL(),
				"diceWorkspace":  cdp.resourceTrans.Text(lang, pipelineDetail.Extra.DiceWorkspace),
			}
			if runUser := pipelineDetail.Extra.RunUser; runUser != nil {
				params["operatorName"] = runUser.Name
			}

			//失败情况尝输出错误日志
			if notifyItem.Name == "pipeline_failed" {
				// 等待日志采集
				time.Sleep(time.Second * 5)
				failedDetailLogs, err := cdp.getFailedTaskLogs(pipelineDetail, true)
				if err != nil {
					logrus.Errorf("get cdp workflow's failed log err: %v", err)
					failedDetailLogs = "Log cannot be displayed"
				}
				params["failedDetail"] = failedDetailLogs
			}
			// 不使用notifyItemID是为了支持第三方通知项,如监控
			eventboxReqContent := apistructs.GroupNotifyContent{
				SourceName:            strconv.FormatUint(pipelineData.PipelineID, 10),
				SourceType:            sourceType,
				SourceID:              sourceID,
				NotifyName:            notifyDetail.Name,
				NotifyItemDisplayName: notifyItem.DisplayName,
				Channels:              []apistructs.GroupNotifyChannel{},
				ClusterName:           pipelineDetail.ClusterName,
				CalledShowNumber:      notifyItem.CalledShowNumber,
				Label:                 notifyItem.Label,
				OrgID:                 orgID,
			}

			err := cdp.bdl.CreateGroupNotifyEvent(apistructs.EventBoxGroupNotifyRequest{
				Sender:        "adapter",
				GroupID:       notifyDetail.NotifyGroup.ID,
				Channels:      notifyDetail.Channels,
				NotifyItem:    notifyItem,
				NotifyContent: &eventboxReqContent,
				Params:        params,
			})
			if err != nil {
				logrus.Errorf("get cdp workflow's failed log err: %v", err)
				continue
			}
		}
	}
	return nil
}

func (cdp *CDP) getFailedTaskLogs(pipelineDetail *apistructs.PipelineDetailDTO, withTruncate bool) (string, error) {
	failedTasks := map[string]string{}
	for _, stage := range pipelineDetail.PipelineStages {
		for _, task := range stage.PipelineTasks {
			if task.Status == "Failed" {
				failedTasks[task.Name] = task.Extra.UUID
			}
		}
	}
	failedDetailLogs := ""
	for taskName, taskID := range failedTasks {
		stdError, err := cdp.bdl.GetLog(pipelineDetail.OrgName, apistructs.DashboardSpotLogRequest{
			ID:     taskID,
			Source: apistructs.DashboardSpotLogSourceJob,
			Stream: apistructs.DashboardSpotLogStreamStderr,
			Count:  -10,
			Start:  0,
			End:    time.Duration(time.Now().UnixNano()),
		})
		if err != nil {
			return "", err
		}
		stdOut, err := cdp.bdl.GetLog(pipelineDetail.OrgName, apistructs.DashboardSpotLogRequest{
			ID:     taskID,
			Source: apistructs.DashboardSpotLogSourceJob,
			Stream: apistructs.DashboardSpotLogStreamStdout,
			Count:  -10,
			Start:  0,
			End:    time.Duration(time.Now().UnixNano()),
		})
		if err != nil {
			return "", err
		}
		stderrLines := ""
		stdoutLines := ""
		for _, line := range stdError.Lines {
			stderrLines += line.Content + "\n"
		}
		for _, line := range stdOut.Lines {
			stdoutLines += line.Content + "\n"
		}
		if withTruncate {
			stderrLines = truncateLog(stderrLines, defaultMaxLogLines, defaultMaxLogColumns)
			stdoutLines = truncateLog(stdoutLines, defaultMaxLogLines, defaultMaxLogColumns)
		}
		if len(stdoutLines) > 0 {
			failedDetailLogs += fmt.Sprintf(`<p>%s stdout:</p>
<pre style="background:#E8E8E8"><code>%s</pre></code>`, taskName, stdoutLines)
		}

		if len(stderrLines) > 0 {
			failedDetailLogs += fmt.Sprintf(`<p>%s stderr:</p>
<pre style="background:#E8E8E8"><code>%s</pre></code>`, taskName, stderrLines)
		}
	}
	return failedDetailLogs, nil
}

// truncateLog return truncated logs with given lines and columns
func truncateLog(sourceLogs string, lines int, columns int) string {
	if lines <= 0 || columns <= 0 {
		return sourceLogs
	}
	logSplits := strings.Split(sourceLogs, "\n")
	if len(logSplits) == 0 {
		return sourceLogs
	}
	// truncate log line if larger than the given
	if len(logSplits) >= lines {
		logSplits = logSplits[len(logSplits)-lines:]
	}
	// truncate log column if larger than the given
	for i := range logSplits {
		if len(logSplits[i]) > columns {
			logSplits[i] = logSplits[i][:columns] + "..."
		}
	}
	return strings.Join(logSplits, "\n")
}
