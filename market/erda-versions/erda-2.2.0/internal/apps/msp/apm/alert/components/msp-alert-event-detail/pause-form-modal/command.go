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

package pause_form_modal

import (
	"context"
	"fmt"
	"strconv"

	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-detail/common"
)

func (cp *ComponentPauseModalFormInfo) GetAlertEvent(ctx context.Context) (*monitorpb.AlertEventItem, error) {
	events, err := cp.MonitorAlertService.GetAlertEvents(ctx, &monitorpb.GetAlertEventRequest{
		Scope:    cp.inParams.Scope,
		ScopeId:  cp.inParams.ScopeId,
		PageNo:   1,
		PageSize: 1,
		Condition: &monitorpb.GetAlertEventRequestCondition{
			Ids: []string{cp.inParams.AlertEventId},
		},
	})
	if err != nil || len(events.Items) == 0 {
		return nil, fmt.Errorf("failed to get alert event info: %s", err)
	}
	return events.Items[0], nil
}

func (cp *ComponentPauseModalFormInfo) PauseAlertEvent(alertEvent *monitorpb.AlertEventItem) error {
	result, err := cp.MonitorAlertService.SuppressAlertEvent(cp.ctx, &monitorpb.SuppressAlertEventRequest{
		AlertEventID: alertEvent.Id,
		OrgID:        strconv.FormatInt(alertEvent.OrgID, 10),
		Scope:        alertEvent.Scope,
		ScopeID:      alertEvent.ScopeId,
		SuppressType: common.AlertEventStatePause,
		ExpireTime:   cp.State.FormData.PauseExpireTime,
	})
	if err != nil {
		return err
	}

	if !result.Result {
		return fmt.Errorf("server response false result")
	}

	return nil
}
