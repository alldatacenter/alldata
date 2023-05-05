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

package event_overview_info

import (
	"context"
	"fmt"
	"strconv"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
)

func (cp *ComponentEventOverviewInfo) countAlertEvents(ctx context.Context, eventId string) (int64, error) {
	reqParams := map[string]*structpb.Value{
		"eventId": structpb.NewStringValue(eventId),
	}

	statement := fmt.Sprintf("SELECT count(timestamp) FROM analyzer_alert " +
		"WHERE family_id::tag=$eventId AND alert_suppressed::tag='false' ")
	resp, err := cp.Metric.QueryWithInfluxFormat(ctx, &metricpb.QueryWithInfluxFormatRequest{
		Start:     "0",
		End:       strconv.FormatInt(time.Now().UnixNano()/1e6, 10),
		Statement: statement,
		Params:    reqParams,
	})
	if err != nil {
		return 0, err
	}
	if len(resp.Results[0].Series[0].Rows) == 0 {
		return 0, fmt.Errorf("empty result")
	}
	total := resp.Results[0].Series[0].Rows[0].Values[0].GetNumberValue()
	return int64(total), nil
}
