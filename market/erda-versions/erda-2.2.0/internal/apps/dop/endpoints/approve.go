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
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// WatchApprovalStatusChanged 监听审批流状态变更，同步审批流状态至依赖方
func (e *Endpoints) WatchApprovalStatusChanged(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		event apistructs.ApprovalStatusChangedEvent
		err   error
	)
	if err = json.NewDecoder(r.Body).Decode(&event); err != nil {
		return apierrors.ErrApprovalStatusChanged.InvalidParameter(err).ToResp(), nil
	}
	logrus.Infof("approvalStatusChangedEvent: %+v", event)

	// 处理审批流状态变更通知
	switch event.Content.ApprovalType {
	case apistructs.ApproveCeritficate:
		err = e.appCertificate.ModifyApprovalStatus(int64(event.Content.ApprovalID), event.Content.ApprovalStatus)
	case apistructs.ApproveLibReference:
		err = e.libReference.UpdateApprovalStatus(event.Content.ApprovalID, event.Content.ApprovalStatus)
	}

	if err != nil {
		return apierrors.ErrApprovalStatusChanged.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("handle success")
}
