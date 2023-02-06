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

package core

import (
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
)

func (p *provider) CreateIssueEvent(req *common.IssueStreamCreateRequest) error {
	if req.StreamType == "" && len(req.StreamTypes) == 0 {
		return nil
	}
	var content string
	var err error
	issue, err := p.db.GetIssue(req.IssueID)
	if err != nil {
		return err
	}
	receivers, err := p.db.GetReceiversByIssueID(req.IssueID)
	if err != nil {
		logrus.Errorf("get issue %d  recevier error: %v, recevicer will be empty", req.IssueID, err)
		receivers = []string{}
	}
	receivers = filterReceiversByOperatorID(receivers, req.Operator)
	projectModel, err := p.bdl.GetProject(issue.ProjectID)
	if err != nil {
		return err
	}
	orgModel, err := p.bdl.GetOrg(int64(projectModel.OrgID))
	if err != nil {
		return err
	}
	operator, err := p.bdl.GetCurrentUser(req.Operator)
	if err != nil {
		return err
	}
	if len(req.StreamTypes) == 0 {
		content, err = getDefaultContentForMsgSending(req.StreamType, req.StreamParams, p.commonTran, orgModel.Locale)
	} else {
		content, err = groupEventContent(req.StreamTypes, req.StreamParams, p.commonTran, orgModel.Locale)
	}
	if err != nil {
		logrus.Errorf("get issue %d content error: %v, content will be empty", req.IssueID, err)
	}
	logrus.Debugf("old issue content is: %s", content)
	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:         bundle.IssueEvent,
			Action:        common.GetEventAction(req.StreamType),
			OrgID:         strconv.FormatInt(int64(projectModel.OrgID), 10),
			ProjectID:     strconv.FormatUint(issue.ProjectID, 10),
			ApplicationID: "-1",
			TimeStamp:     time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender: bundle.SenderDOP,
		Content: common.IssueEventData{
			Title:        issue.Title,
			Content:      content,
			AtUserIDs:    issue.Assignee,
			IssueType:    issue.Type,
			StreamType:   req.StreamType,
			StreamTypes:  req.StreamTypes,
			StreamParams: req.StreamParams,
			Receivers:    receivers,
			Params: map[string]string{
				"orgName":     orgModel.Name,
				"projectName": projectModel.Name,
				"issueID":     strconv.FormatInt(req.IssueID, 10),
				"operator":    operator.Nick,
			},
		},
	}

	return p.bdl.CreateEvent(ev)
}

func filterReceiversByOperatorID(receivers []string, operatorID string) []string {
	users := make([]string, 0)
	for _, userID := range receivers {
		if userID != operatorID {
			users = append(users, userID)
		}
	}
	return users
}

func groupEventContent(streamTypes []string, param common.ISTParam, tran i18n.Translator, locale string) (string, error) {
	var content string
	interval := ";"
	for _, streamType := range streamTypes {
		tmp, err := getDefaultContentForMsgSending(streamType, param, tran, locale)
		if err != nil {
			return "", err
		}
		content += interval + tmp
	}
	return strings.TrimLeft(content, interval), nil
}
