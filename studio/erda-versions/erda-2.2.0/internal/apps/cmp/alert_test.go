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

package cmp

import (
	"context"
	"fmt"
	"testing"

	"github.com/golang/mock/gomock"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/cmp/alert/pb"
	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
)

////go:generate mockgen -destination=./alert_register_test.go -package cmp github.com/erda-project/erda-infra/pkg/transport Register
////go:generate mockgen -destination=./alert_monitor_test.go -package cmp github.com/erda-project/erda-proto-go/core/monitor/alert/pb AlertServiceServer
func Test_provider_GetAlertConditions(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	monitorService := NewMockAlertServiceServer(ctrl)
	monitorService.EXPECT().GetAlertConditions(gomock.Any(), gomock.Any()).AnyTimes().Return(&monitor.GetAlertConditionsResponse{
		Data: []*monitor.Conditions{
			{
				Key:         "application_name",
				DisplayName: "应用",
			},
			{
				Key:         "service_name",
				DisplayName: "服务",
			},
		},
	}, nil)
	pro := &provider{
		Monitor: monitorService,
	}
	_, err := pro.GetAlertConditions(context.Background(), &pb.GetAlertConditionsRequest{
		ScopeType: "msp",
	})
	if err != nil {
		fmt.Println("should not err,err is ", err)
	}
}

func Test_alertService_GetAlertConditionsValue(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	monitorService := NewMockAlertServiceServer(ctrl)
	monitorService.EXPECT().GetAlertConditionsValue(gomock.Any(), gomock.Any()).AnyTimes().Return(&monitor.GetAlertConditionsValueResponse{
		Data: []*monitor.AlertConditionsValue{
			{
				Key: "cluster_name",
				Options: []*structpb.Value{
					structpb.NewStringValue("terminus-dev"),
				},
			},
		},
	}, nil)
	pro := &provider{
		Monitor: monitorService,
	}

	_, err := pro.GetAlertConditionsValue(context.Background(), &pb.GetAlertConditionsValueRequest{
		Conditions: []*monitor.ConditionsValueRequest{
			{
				Condition: "cluster_name",
				Filters: map[string]string{
					"org_name": "terminus",
				},
				Index: "host_summary",
			},
		},
	})
	if err != nil {
		fmt.Println("should not err,err is ", err)
	}
}
