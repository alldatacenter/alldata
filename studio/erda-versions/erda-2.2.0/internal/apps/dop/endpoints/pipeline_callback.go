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

// CDPCallback cdp hook的回调
func (e *Endpoints) CDPCallback(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		req           apistructs.PipelineInstanceEvent
		runningTaskID int64
	)
	if r.Body == nil {
		return apierrors.ErrDealCDPCallback.MissingParameter("body").ToResp(), nil
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrDealCDPCallback.InvalidParameter(err).ToResp(), nil
	}

	go func() {
		err := e.cdp.CdpNotifyProcess(&req)
		if err != nil {
			logrus.Errorf("failed to process cdp notify %s", err)
		}
	}()
	return httpserver.OkResp(runningTaskID)
}
