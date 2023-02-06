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

package more_operations

import (
	"context"
	"fmt"
	"strconv"

	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-detail/common"
)

func (b *ComponentOperationButton) GetAlertEvent(ctx context.Context) (*monitorpb.AlertEventItem, error) {
	events, err := b.MonitorAlertService.GetAlertEvents(ctx, &monitorpb.GetAlertEventRequest{
		Scope:    b.inParams.Scope,
		ScopeId:  b.inParams.ScopeId,
		PageNo:   1,
		PageSize: 1,
		Condition: &monitorpb.GetAlertEventRequestCondition{
			Ids: []string{b.inParams.AlertEventId},
		},
	})
	if err != nil || len(events.Items) == 0 {
		return nil, fmt.Errorf("failed to get alert event info: %s", err)
	}
	return events.Items[0], nil
}

func (b *ComponentOperationButton) StopAlertEvent(alertEvent *monitorpb.AlertEventItem) error {
	result, err := b.MonitorAlertService.SuppressAlertEvent(b.ctx, &monitorpb.SuppressAlertEventRequest{
		AlertEventID: alertEvent.Id,
		OrgID:        strconv.FormatInt(alertEvent.OrgID, 10),
		Scope:        alertEvent.Scope,
		ScopeID:      alertEvent.ScopeId,
		SuppressType: common.AlertEventStateStop,
	})
	if err != nil {
		return err
	}

	if !result.Result {
		return fmt.Errorf("server response false result")
	}

	return nil
}

func (b *ComponentOperationButton) CancelSuppressAlertEvent(alertEvent *monitorpb.AlertEventItem) interface{} {
	result, err := b.MonitorAlertService.CancelSuppressAlertEvent(b.ctx, &monitorpb.CancelSuppressAlertEventRequest{
		AlertEventID: alertEvent.Id,
	})
	if err != nil {
		return err
	}

	if !result.Result {
		return fmt.Errorf("server response false result")
	}

	return nil
}
