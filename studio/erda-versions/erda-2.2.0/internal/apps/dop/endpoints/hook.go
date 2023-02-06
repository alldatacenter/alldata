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
	"fmt"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) RegisterWebhooks() error {
	for _, callback := range eventCallbacks {
		hook := apistructs.CreateHookRequest{
			Name:   callback.Name,
			Events: callback.Events,
			URL:    strutil.Concat("http://", discover.DOP(), callback.Path),
			Active: true,
			HookLocation: apistructs.HookLocation{
				Org:         "-1",
				Project:     "-1",
				Application: "-1",
			},
		}
		if err := e.bdl.CreateWebhook(hook); err != nil {
			return fmt.Errorf("failed to register hook %s to eventbox, err: %v, detail: %v", callback.Name, err, hook)
		}
		logrus.Infof("register hook %s to eventbox, detail: %v", callback.Name, hook)
	}
	return nil
}

func (e *Endpoints) GittarMRCreateCallback(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	logrus.Infof("received mr request")
	if r.ContentLength == 0 {
		return apierrors.ErrDoGitMrCreateCallback.MissingParameter(apierrors.MissingRequestBody).ToResp(), nil
	}
	var mrCreateEvent apistructs.RepoCreateMrEvent
	if err := json.NewDecoder(r.Body).Decode(&mrCreateEvent); err != nil {
		return apierrors.ErrDoGitMrCreateCallback.InvalidParameter(err).ToResp(), nil
	}

	// 打印
	logrus.Infof("received mr event: %+v", mrCreateEvent)

	go func() {
		pipelineID, err := e.cq.TriggerByMR(mrCreateEvent.Content)
		if err != nil {
			logrus.Errorf("MR CQ failed, err: %v", err)
		} else {
			logrus.Infof("MR CQ triggered, pipelineID: %d", pipelineID)
		}
	}()

	return httpserver.OkResp(nil)
}
