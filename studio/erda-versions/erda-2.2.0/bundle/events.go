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

// Package bundle 见 bundle.go
package bundle

import (
	"bytes"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
)

// Sender alias as string
type Sender = string

// Event sender collections
const (
	SenderCMDB           Sender = "cmdb"
	SenderDiceHub        Sender = "dicehub"
	SenderScheduler      Sender = "scheduler"
	SenderOrchestrator   Sender = "orchestrator"
	SenderCoreServices   Sender = "coreServices"
	SenderClusterManager Sender = "clusterManager"
	SenderDOP            Sender = "dop"
	SenderGittar         Sender = "gittar"
)

// Event types
const (
	ClusterEvent               = "cluster"
	OrgEvent                   = "org"
	ProjectEvent               = "project"
	ApplicationEvent           = "application"
	RuntimeEvent               = "runtime"
	ReleaseEvent               = "release"
	ApproveEvent               = "approve"
	ApprovalStatusChangedEvent = "approvalStatusChanged"
	IssueEvent                 = "issue"
	PipelineYmlEvent           = "pipelineYml"
	GitPushEvent               = "git_push"
	AutoTestPlanExecuteEvent   = "autoTestPlanExecute"
)

// Event actions
const (
	CreateAction = "create"
	UpdateAction = "update"
	DeleteAction = "delete"
)

// CreateEvent 创建一个 event 发送到 eventbox 服务.
func (b *Bundle) CreateEvent(ev *apistructs.EventCreateRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/dice/eventbox/message/create").
		Header("Accept", "application/json").
		JSONBody(&ev).
		Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to create event, status-code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

func (b *Bundle) CreateEventNotify(ev *apistructs.EventBoxRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/dice/eventbox/message/create").
		Header("Accept", "application/json").
		JSONBody(&ev).
		Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to create event notify, status-code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

func (b *Bundle) CreateMboxNotify(templatename string, params map[string]string, locale string, orgid uint64, users []string) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc
	request := map[string]interface{}{
		"template": b.GetLocaleLoader().Locale(locale).Get(templatename),
		"type":     "markdown",
		"params":   params,
		"orgID":    int64(orgid),
	}
	if _, ok := params["mboxDeduplicateID"]; ok {
		request["deduplicateId"] = params["mboxDeduplicateID"]
	}
	eventBoxRequest := &apistructs.EventBoxRequest{
		Sender: "bundle",
		Labels: map[string]interface{}{
			"MBOX": users,
		},
		Content: request,
	}
	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/dice/eventbox/message/create").
		Header("Accept", "application/json").
		JSONBody(&eventBoxRequest).
		Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to create email-notify, status-code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

func (b *Bundle) CreateDingTalkWorkNotify(templatename string, params map[string]string, locale string, orgid uint64, mobiles []string) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc
	request := map[string]interface{}{
		"template": b.GetLocaleLoader().Locale(locale).Get(templatename),
		"type":     "markdown",
		"params":   params,
		"orgID":    int64(orgid),
	}
	eventBoxRequest := &apistructs.EventBoxRequest{
		Sender: "bundle",
		Labels: map[string]interface{}{
			"DINGTALK_WORK_NOTICE": mobiles,
		},
		Content: request,
	}
	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/dice/eventbox/message/create").
		Header("Accept", "application/json").
		JSONBody(&eventBoxRequest).
		Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to create ding-talk-work-notice, status-code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

func (b *Bundle) CreateEmailNotify(templatename string, params map[string]string, locale string, orgid uint64, emailaddrs []string) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	request := map[string]interface{}{
		"template": b.GetLocaleLoader().Locale(locale).Get(templatename),
		"type":     "markdown",
		"params":   params,
		"orgID":    int64(orgid),
	}

	eventBoxRequest := &apistructs.EventBoxRequest{
		Sender: "bundle",
		Labels: map[string]interface{}{
			"EMAIL": emailaddrs,
		},
		Content: request,
	}

	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/dice/eventbox/message/create").
		Header("Accept", "application/json").
		JSONBody(&eventBoxRequest).
		Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to create email-notify, status-code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

func (b *Bundle) CreateGroupNotifyEvent(groupNotifyRequest apistructs.EventBoxGroupNotifyRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	eventBoxRequest := &apistructs.EventBoxRequest{
		Sender: groupNotifyRequest.Sender,
		Labels: map[string]interface{}{
			"GROUP": groupNotifyRequest.GroupID,
		},
	}
	eventboxReqContent := groupNotifyRequest.NotifyContent
	notifyItem := groupNotifyRequest.NotifyItem
	params := groupNotifyRequest.Params
	eventboxReqContent.Channels = []apistructs.GroupNotifyChannel{}
	channels := strings.Split(groupNotifyRequest.Channels, ",")
	for _, channel := range channels {
		if channel == "sms" {
			p, ok := params["failedDetail"]
			if ok && len(p) > 900 {
				delete(params, "failedDetail")
			}
			eventboxReqContent.Channels = append(eventboxReqContent.Channels, apistructs.GroupNotifyChannel{
				Name:     channel,
				Template: notifyItem.MobileTemplate,
				Params:   params,
			})
		} else if channel == "vms" {
			eventboxReqContent.Channels = append(eventboxReqContent.Channels, apistructs.GroupNotifyChannel{
				Name:     channel,
				Template: notifyItem.VMSTemplate,
				Params:   params,
			})
		} else if channel == "email" {
			channelData := apistructs.GroupNotifyChannel{
				Name:     channel,
				Template: notifyItem.EmailTemplate,
				Params:   params,
			}
			if channelData.Template == "" {
				channelData.Template = notifyItem.MarkdownTemplate
				channelData.Type = "markdown"
			}
			eventboxReqContent.Channels = append(eventboxReqContent.Channels, channelData)
		} else if channel == "dingding" {
			channelData := apistructs.GroupNotifyChannel{
				Name:     channel,
				Template: notifyItem.DingdingTemplate,
				Params:   params,
			}
			if channelData.Template == "" {
				channelData.Template = notifyItem.MarkdownTemplate
			}
			eventboxReqContent.Channels = append(eventboxReqContent.Channels, channelData)
		} else if channel == "mbox" {
			channelData := apistructs.GroupNotifyChannel{
				Name:     channel,
				Template: notifyItem.MBoxTemplate,
				Params:   params,
			}
			if channelData.Template == "" {
				channelData.Template = notifyItem.MarkdownTemplate
			}
			eventboxReqContent.Channels = append(eventboxReqContent.Channels, channelData)
		} else if channel == "webhook" {
			channelData := apistructs.GroupNotifyChannel{
				Name:     channel,
				Template: notifyItem.MBoxTemplate, // TODO webhook template
				Params:   params,
			}
			if channelData.Template == "" {
				channelData.Template = notifyItem.MarkdownTemplate
			}
			eventboxReqContent.Channels = append(eventboxReqContent.Channels, channelData)
		}
	}
	eventBoxRequest.Content = eventboxReqContent

	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/dice/eventbox/message/create").
		Header("Accept", "application/json").
		JSONBody(&eventBoxRequest).
		Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to create event, status-code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

func (b *Bundle) CreateWebhook(r apistructs.CreateHookRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc
	list := apistructs.WebhookListResponse{}
	resp, err := hc.Get(host).Path("/api/dice/eventbox/webhooks").
		Param("orgId", r.Org).
		Header("Accept", "application/json").
		Do().JSON(&list)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to list webhook, status-code: %d", resp.StatusCode()))
	}

	if !list.Success {
		return apierrors.ErrInvoke.InternalError(errors.New(list.Error.Msg))
	}

	for i := range list.Data {
		// update webhook if already exist
		if list.Data[i].Name == r.Name {
			// update
			updateReq := apistructs.WebhookUpdateRequestBody{
				Events: r.Events, URL: r.URL, Active: true,
			}
			updatebody := apistructs.WebhookUpdateResponse{}
			resp, err := hc.Put(host).Path("/api/dice/eventbox/webhooks/"+list.Data[i].ID).
				Header("Accept", "application/json").
				Header("Internal-Client", "bundle").
				JSONBody(&updateReq).
				Do().JSON(&updatebody)
			if err != nil {
				return apierrors.ErrInvoke.InternalError(err)
			}
			if !resp.IsOK() {
				return apierrors.ErrInvoke.InternalError(
					errors.Errorf("failed to update webhook, status-code: %d", resp.StatusCode()))
			}
			if !updatebody.Success {
				return apierrors.ErrInvoke.InternalError(
					errors.Errorf("failed to create webhook: %+v", updatebody.Error))
			}
			return nil
		}
	}
	createbody := apistructs.WebhookCreateResponse{}
	resp, err = hc.Post(host).Path("/api/dice/eventbox/webhooks").
		Header("Accept", "application/json").
		JSONBody(&r).
		Do().JSON(&createbody)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to create webhook, status-code: %d", resp.StatusCode()))
	}
	if !createbody.Success {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to create webhook: %+v", createbody.Error))
	}
	return nil
}

func (b *Bundle) DeleteWebhook(r apistructs.DeleteHookRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc
	list := apistructs.WebhookListResponse{}
	resp, err := hc.Get(host).Path("/api/dice/eventbox/webhooks").
		Param("orgId", r.Org).
		Header("Accept", "application/json").
		Do().JSON(&list)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to list webhook, status-code: %d", resp.StatusCode()))
	}

	if !list.Success {
		return apierrors.ErrInvoke.InternalError(errors.New(list.Error.Msg))
	}

	for i := range list.Data {
		// delete webhook if already exist
		if list.Data[i].Name == r.Name {
			req := apistructs.WebhookDeleteRequestBody{
				OrgId:         r.Org,
				ProjectId:     r.Project,
				ApplicationId: r.Application,
				Id:            list.Data[i].ID,
			}
			var deleteRsp apistructs.WebhookDeleteResponse
			resp, err := hc.Delete(host).Path("/api/dice/eventbox/webhooks/"+list.Data[i].ID).
				Header("Accept", "application/json").
				Header("Internal-Client", "bundle").
				JSONBody(&req).
				Do().JSON(&deleteRsp)
			if err != nil {
				return apierrors.ErrInvoke.InternalError(err)
			}
			if !resp.IsOK() {
				return apierrors.ErrInvoke.InternalError(
					errors.Errorf("failed to delete webhook, status-code: %d", resp.StatusCode()))
			}
			if !deleteRsp.Success {
				return apierrors.ErrInvoke.InternalError(
					errors.Errorf("failed to delete webhook: %+v", deleteRsp.Error))
			}
			return nil
		}
	}
	return nil
}
