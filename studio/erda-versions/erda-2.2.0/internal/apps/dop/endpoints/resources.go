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
	"net/http"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// ApplicationsResources responses the resources list for every applications in the project
func (e *Endpoints) ApplicationsResources(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	l := logrus.WithField("func", "*Endpoints.ApplicationsResources")
	if err := r.ParseForm(); err != nil {
		l.WithError(err).Errorln("failed to ParseForm")
		return apierrors.ErrApplicationsResources.InvalidParameter(err).ToResp(), nil
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		l.WithError(err).Errorln("failed to GetUserID")
		return apierrors.ErrApplicationsResources.InvalidParameter(err).ToResp(), nil
	}
	values := r.URL.Query()
	var req = apistructs.ApplicationsResourcesRequest{
		OrgID:     r.Header.Get("ORG-ID"),
		UserID:    string(userID),
		ProjectID: vars["projectID"],
		Query: &apistructs.ApplicationsResourceQuery{
			AppsIDs:  values["applicationID"],
			OwnerIDs: values["ownerID"],
			OrderBy:  strings.Split(values.Get("orderBy"), ","),
			PageNo:   values.Get("pageNo"),
			PageSize: values.Get("pageSize"),
		},
	}
	if err = req.Validate(); err != nil {
		l.WithError(err).Errorln("request validate fails")
		return apierrors.ErrApplicationsResources.InvalidParameter(err).ToResp(), nil
	}

	data, apiError := e.project.ApplicationsResources(ctx, &req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}
	return httpserver.OkResp(data)
}
