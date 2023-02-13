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

package dingtalktest

import (
	"context"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"

	pb "github.com/erda-project/erda-proto-go/admin/pb"
)

type dingTalkTestService struct {
	Log logs.Logger
	bdl *bundle.Bundle
}

func (s *dingTalkTestService) SendTestMessage(ctx context.Context, request *pb.DingTalkTestRequest) (*pb.DingTalkTestResponse, error) {
	eventRequest := &apistructs.EventBoxRequest{
		Sender:  "admin",
		Content: "Hello, this is a test message.",
		Labels: map[string]interface{}{
			"DINGDING": []apistructs.Target{
				{
					Receiver: request.GetWebhook(),
					Secret:   request.GetSecret(),
				},
			},
		},
	}
	err := s.bdl.CreateEventNotify(eventRequest)
	response := &pb.DingTalkTestResponse{
		Success: true,
	}
	if err != nil {
		response.Success = false
		response.Error = err.Error()
	}
	return response, nil
}
