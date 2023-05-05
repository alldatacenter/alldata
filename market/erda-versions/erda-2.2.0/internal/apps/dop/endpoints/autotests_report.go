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
	"fmt"
	"net/http"
	"strconv"

	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

func (e *Endpoints) queryReportSets(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	// pipelineID
	pipelineID, err := strconv.ParseUint(vars["pipelineID"], 10, 64)
	if err != nil {
		return apierrors.ErrQueryPipelineReportSet.InvalidParameter(fmt.Errorf("invalid pipelineID: %s", vars["pipelineID"])).ToResp(), nil
	}

	// identity
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrQueryPipelineReportSet.AccessDenied().ToResp(), nil
	}
	_ = identityInfo

	result, err := e.bdl.GetPipelineReportSet(pipelineID, r.URL.Query()["type"])
	// query
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(result)
}
