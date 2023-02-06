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

package endpoints

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// IssueCallback 事件管理 hook的回调
func (e *Endpoints) IssueCallback(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		req apistructs.IssueEvent
	)
	if r.Body == nil {
		return apierrors.ErrIssueCallback.MissingParameter("body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		logrus.Errorf("failed to decode body, (%+v)", err)
		return apierrors.ErrIssueCallback.InvalidParameter(err).ToResp(), nil
	}

	marshal, _ := json.Marshal(req)

	logrus.Printf("action:%s StreamType:%s StreamParams:%s title:%s content:%s", req.Action, req.Content.StreamType, string(marshal), req.Content.Title, req.Content.Content)

	err := e.processIssueEvent(req)
	if err != nil {
		logrus.Errorf("failed to process issue event, (%+v)", err)
		return apierrors.ErrIssueCallback.InvalidParameter(err).ToResp(), nil
	}

	return httpserver.OkResp("")
}

func (e *Endpoints) processIssueEvent(req apistructs.IssueEvent) error {
	eventnName := "issue_" + req.Action
	notifyDetails, err := e.bdl.QueryNotifiesBySource(req.OrgID, "project", req.ProjectID, eventnName, "")
	if err != nil {
		return err
	}
	orgID, err := strconv.ParseInt(req.OrgID, 10, 64)
	if err != nil {
		return err
	}
	linkParams := req.GenEventParams(req.Env, conf.UIPublicURL())

	// send to specific recipient
	if err := e.sendIssueEventToSpecificRecipient(req); err != nil {
		return err
	}

	for _, notifyDetail := range notifyDetails {
		if notifyDetail.NotifyGroup == nil {
			continue
		}
		notifyItem := notifyDetail.NotifyItems[0]
		params := map[string]string{
			"issue_title":    req.Content.Title,
			"content":        req.Content.Content,
			"atUserIDs":      req.Content.AtUserIDs,
			"orgName":        req.Content.Params["orgName"],
			"projectName":    req.Content.Params["projectName"],
			"operator":       req.Content.Params["operator"],
			"issueEmailLink": linkParams["issueEmailLink"],
		}
		marshal, _ := json.Marshal(params)
		logrus.Debugf("issue params :%s", string(marshal))

		// 不使用notifyItemID是为了支持第三方通知项,如监控
		eventboxReqContent := apistructs.GroupNotifyContent{
			SourceName:            "",
			SourceType:            "project",
			SourceID:              req.ProjectID,
			NotifyName:            notifyDetail.Name,
			NotifyItemDisplayName: notifyItem.DisplayName,
			Channels:              []apistructs.GroupNotifyChannel{},
			Label:                 notifyItem.Label,
			CalledShowNumber:      notifyItem.CalledShowNumber,
			OrgID:                 orgID,
		}

		err := e.bdl.CreateGroupNotifyEvent(apistructs.EventBoxGroupNotifyRequest{
			Sender:        "adapter",
			GroupID:       notifyDetail.NotifyGroup.ID,
			Channels:      notifyDetail.Channels,
			NotifyItem:    notifyItem,
			NotifyContent: &eventboxReqContent,
			Params:        params,
		})
		if err != nil {
			return err
		}
	}
	return nil
}

func (e *Endpoints) sendIssueEventToSpecificRecipient(req apistructs.IssueEvent) error {
	if len(req.Content.Receivers) == 0 {
		return nil
	}

	emailTemplateName := fmt.Sprintf("notify.issue_%s.personal_message.email", strings.ToLower(req.Action))
	mboxTemplateName := fmt.Sprintf("notify.issue_%s.personal_message.markdown", strings.ToLower(req.Action))
	dingTalkTemplateName := fmt.Sprintf("notify.issue.issue_%s.ding_talk.markdown_template", strings.ToLower(req.Action))

	org, err := e.bdl.GetOrg(req.OrgID)
	if err != nil {
		return err
	}
	if org.Locale == "" {
		org.Locale = "zh-CN"
	}

	params := req.GenEventParams(org.Locale, conf.UIPublicURL())
	params["content"] = req.Content.Content
	params["atUserIDs"] = req.Content.AtUserIDs
	params["issue_title"] = req.Content.Title
	params["orgName"] = req.Content.Params["orgName"]
	params["projectName"] = req.Content.Params["projectName"]
	params["operator"] = req.Content.Params["operator"]

	var emailAddrs []string
	var mobiles []string
	users, err := e.bdl.ListUsers(apistructs.UserListRequest{Plaintext: true, UserIDs: req.Content.Receivers})
	if err != nil {
		return err
	}
	for _, v := range users.Users {
		emailAddrs = append(emailAddrs, v.Email)
		mobiles = append(mobiles, v.Phone)
	}

	logrus.Debugf("params of issue event is: %v", params)
	logrus.Debugf("email addr is: %v", emailAddrs)

	// if allowd send email for personal message
	if org.Config.EnablePersonalMessageEmail {
		if err := e.bdl.CreateEmailNotify(emailTemplateName, params, org.Locale, org.ID, emailAddrs); err != nil {
			logrus.Errorf("send personal issue %s event email err: %v", params["issueID"], err)
		}
	}
	if err := e.bdl.CreateMboxNotify(mboxTemplateName, params, org.Locale, org.ID, req.Content.Receivers); err != nil {
		logrus.Errorf("send personal issue %s event mbox err: %v", params["issueID"], err)
	}
	if err := e.bdl.CreateDingTalkWorkNotify(dingTalkTemplateName, params, org.Locale, org.ID, mobiles); err != nil {
		logrus.Errorf("send personal issue %s event ding talk work notice err: %v", params["issueID"], err)
	}

	return nil
}
