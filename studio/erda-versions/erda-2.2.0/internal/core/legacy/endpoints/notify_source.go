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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// DeleteNotifySource 删除通知源
func (e *Endpoints) DeleteNotifySource(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrDeleteNotifySource.MissingParameter("Org-ID header is nil").ToResp(), nil
	}
	var deleteNotifySourceReq apistructs.DeleteNotifySourceRequest
	if err := json.NewDecoder(r.Body).Decode(&deleteNotifySourceReq); err != nil {
		return apierrors.ErrDeleteNotifySource.InvalidParameter("can't decode body").ToResp(), nil
	}
	if deleteNotifySourceReq.SourceID == "0" || deleteNotifySourceReq.SourceID == "" {
		return apierrors.ErrDeleteNotifySource.InvalidParameter("sourceId is null").ToResp(), nil
	}
	deleteNotifySourceReq.OrgID = orgID
	err = e.notifyGroup.DeleteNotifySource(&deleteNotifySourceReq)
	if err != nil {
		return apierrors.ErrDeleteNotifySource.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("")
}
