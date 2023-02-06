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

package query

import (
	"context"
	"reflect"
	"testing"

	"github.com/golang/mock/gomock"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-proto-go/core/monitor/event/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/event"
)

// -go:generate mockgen -destination=./mock_storage.go -package query -source=../storage/storage.go Storage
// -go:generate mockgen -destination=./mock_log.go -package query github.com/erda-project/erda-infra/base/logs Logger
func Test_eventQueryService_GetEvents(t *testing.T) {

	type args struct {
		ctx context.Context
		req *pb.GetEventsRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetEventsResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//{
		//	"case 1",
		//	"erda.core.monitor.event.EventQueryService",
		//	`
		//	erda.core.monitor.event.query:
		//	`,
		//	args{
		//		context.TODO(),
		//		&pb.GetEventsRequest{
		//			// TODO: setup fields
		//		},
		//	},
		//	&pb.GetEventsResponse{
		//		// TODO: setup fields.
		//	},
		//	false,
		//},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.EventQueryServiceServer)
			got, err := srv.GetEvents(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("eventQueryService.GetEvents() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("eventQueryService.GetEvents() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_eventQueryService_GetEvents_WithValidParams_Should_Return_NonEmptyList(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	storage := NewMockStorage(ctrl)
	storage.EXPECT().
		QueryPaged(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).
		Return([]*event.Event{
			{EventID: "event-id-1"},
		}, nil)

	querySvc := &eventQueryService{
		storageReader: storage,
	}
	result, err := querySvc.GetEvents(context.Background(), &pb.GetEventsRequest{
		Start:        1,
		End:          2,
		TraceId:      "trace-id",
		RelationId:   "res-id",
		RelationType: "res-type",
		Tags: map[string]string{
			"key-1": "val-1",
		},
	})
	if err != nil {
		t.Errorf("should not throw error")
	}
	if result == nil || len(result.Data.Items) != 1 {
		t.Errorf("assert result failed")
	}

}

func Test_eventQueryService_GetEvents_WithNilTags_Should_Not_Throw(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	storage := NewMockStorage(ctrl)
	storage.EXPECT().
		QueryPaged(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).
		Return([]*event.Event{
			{EventID: "event-id-1"},
		}, nil)

	querySvc := &eventQueryService{
		storageReader: storage,
	}
	result, err := querySvc.GetEvents(context.Background(), &pb.GetEventsRequest{
		Start:        1,
		End:          2,
		TraceId:      "trace-id",
		RelationId:   "res-id",
		RelationType: "res-type",
	})
	if err != nil {
		t.Errorf("should not throw error")
	}
	if result == nil || len(result.Data.Items) != 1 {
		t.Errorf("assert result failed")
	}

}

func Test_eventQueryService_GetEvents_With_NilStorage_Should_Not_Return_Error(t *testing.T) {
	ctrl := gomock.NewController(t)
	logger := NewMockLogger(ctrl)
	defer ctrl.Finish()
	logger.EXPECT().Warnf(gomock.Any(), gomock.Any())
	querySvc := &eventQueryService{
		storageReader: nil,
		p: &provider{
			Log: logger,
		},
	}
	result, err := querySvc.GetEvents(context.Background(), &pb.GetEventsRequest{
		Start:        1,
		End:          2,
		TraceId:      "trace-id",
		RelationId:   "res-id",
		RelationType: "res-type",
	})
	if result == nil || err != nil {
		t.Errorf("should not throw error")
	}
}
