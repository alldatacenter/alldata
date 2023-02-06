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

package eventbox

import (
	"context"
	"encoding/json"
	"fmt"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/version"
	"github.com/erda-project/erda-proto-go/core/messenger/eventbox/pb"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/conf"
	inputhttp "github.com/erda-project/erda/internal/core/messenger/eventbox/input/http"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/monitor"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/register"
	emailsubscriber "github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber/email"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/webhook"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

type eventBoxService struct {
	HttpI        *inputhttp.HttpInput
	RegisterHTTP *register.RegisterHTTP
	WebHookHTTP  *webhook.WebHookHTTP
	MonitorHTTP  *monitor.MonitorHTTP
}

func (e *eventBoxService) Stat(ctx context.Context, request *pb.StatRequest) (*pb.StatResponse, error) {
	return e.MonitorHTTP.Stat(ctx, request, nil)
}

func (e *eventBoxService) PrefixGet(ctx context.Context, request *pb.PrefixGetRequest) (*pb.PrefixGetResponse, error) {
	return e.RegisterHTTP.PrefixGet(ctx, request, nil)
}

func (e *eventBoxService) Put(ctx context.Context, request *pb.PutRequest) (*pb.PutResponse, error) {
	return e.RegisterHTTP.Put(ctx, request, nil)
}

func (e *eventBoxService) Del(ctx context.Context, request *pb.DelRequest) (*pb.DelResponse, error) {
	return e.RegisterHTTP.Del(ctx, request, nil)
}

func (e *eventBoxService) GetVersion(ctx context.Context, request *pb.GetVersionRequest) (*pb.GetVersionResponse, error) {
	return &pb.GetVersionResponse{
		Data: version.String(),
	}, nil
}

func (e *eventBoxService) GetSMTPInfo(ctx context.Context, request *pb.GetSMTPInfoRequest) (*pb.GetSMTPInfoResponse, error) {
	userIdStr := apis.GetUserID(ctx)
	internalClient := apis.GetHeader(ctx, httputil.InternalHeader)
	if userIdStr == "" && internalClient == "" {
		return &pb.GetSMTPInfoResponse{
			Data: nil,
		}, fmt.Errorf("invalid identity info")
	}
	content, err := structpb.NewValue(emailsubscriber.NewMailSubscriberInfo(conf.SmtpHost(), conf.SmtpPort(), conf.SmtpUser(), conf.SmtpPassword(),
		conf.SmtpDisplayUser(), conf.SmtpIsSSL(), conf.SMTPInsecureSkipVerify()))
	if err != nil {
		return &pb.GetSMTPInfoResponse{
			Data: nil,
		}, err
	}
	data, err := json.Marshal(content)
	if err != nil {
		return &pb.GetSMTPInfoResponse{
			Data: nil,
		}, err
	}
	var result pb.MailSubscriberInfo
	err = json.Unmarshal(data, &result)
	if err != nil {
		return &pb.GetSMTPInfoResponse{
			Data: nil,
		}, err
	}
	return &pb.GetSMTPInfoResponse{
		Data: &result,
	}, nil
}

func (e *eventBoxService) ListHooks(ctx context.Context, request *pb.ListHooksRequest) (*pb.ListHooksResponse, error) {
	err := e.WebHookHTTP.CheckPermission(ctx, request.OrgId, request.ProjectId, request.ApplicationId)

	if err != nil {
		return &pb.ListHooksResponse{}, err
	}
	return e.WebHookHTTP.ListHooks(ctx, request, nil)
}

func (e *eventBoxService) InspectHook(ctx context.Context, request *pb.InspectHookRequest) (*pb.InspectHookResponse, error) {
	err := e.WebHookHTTP.CheckPermission(ctx, request.OrgId, request.ProjectId, request.ApplicationId)
	if err != nil {
		return &pb.InspectHookResponse{}, err
	}
	return e.WebHookHTTP.InspectHook(ctx, request, nil)
}

func (e *eventBoxService) CreateHook(ctx context.Context, request *pb.CreateHookRequest) (*pb.CreateHookResponse, error) {
	err := e.WebHookHTTP.CheckPermission(ctx, request.Org, request.Project, request.Application)
	if err != nil {
		return &pb.CreateHookResponse{}, err
	}
	return e.WebHookHTTP.CreateHook(ctx, request, nil)
}

func (e *eventBoxService) EditHook(ctx context.Context, request *pb.EditHookRequest) (*pb.EditHookResponse, error) {
	err := e.WebHookHTTP.CheckPermission(ctx, request.OrgId, request.ProjectId, request.ApplicationId)
	if err != nil {
		return &pb.EditHookResponse{}, err
	}
	return e.WebHookHTTP.EditHook(ctx, request, nil)
}

func (e *eventBoxService) PingHook(ctx context.Context, request *pb.PingHookRequest) (*pb.PingHookResponse, error) {
	err := e.WebHookHTTP.CheckPermission(ctx, request.OrgId, request.ProjectId, request.ApplicationId)
	if err != nil {
		return &pb.PingHookResponse{}, err
	}
	return e.WebHookHTTP.PingHook(ctx, request, nil)
}

func (e *eventBoxService) DeleteHook(ctx context.Context, request *pb.DeleteHookRequest) (*pb.DeleteHookResponse, error) {
	err := e.WebHookHTTP.CheckPermission(ctx, request.OrgId, request.ProjectId, request.ApplicationId)
	if err != nil {
		return &pb.DeleteHookResponse{}, err
	}
	return e.WebHookHTTP.DeleteHook(ctx, request, nil)
}

func (e *eventBoxService) ListHookEvents(ctx context.Context, request *pb.ListHookEventsRequest) (*pb.ListHookEventsResponse, error) {
	err := e.WebHookHTTP.CheckPermission(ctx, request.OrgId, request.ProjectId, request.ApplicationId)
	if err != nil {
		return &pb.ListHookEventsResponse{}, err
	}
	return e.WebHookHTTP.ListHookEvents(ctx, request, nil)
}

func (e *eventBoxService) CreateMessage(ctx context.Context, request *pb.CreateMessageRequest) (*pb.CreateMessageResponse, error) {
	err := e.HttpI.CreateMessage(ctx, request, nil)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.CreateMessageResponse{
		Data: "",
	}, nil
}
