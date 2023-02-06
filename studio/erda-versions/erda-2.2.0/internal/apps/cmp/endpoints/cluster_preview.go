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
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (e *Endpoints) ClusterPreview(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	i18nPrinter := ctx.Value("i18nPrinter").(*message.Printer)
	var req apistructs.CloudClusterRequest

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("failed to unmarshal to apistructs.CloudClusterRequest: %v", err)
		return mkResponse(apistructs.ClusterPreviewResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	logrus.Debugf("cloud-cluster request: %v", req)
	userid := r.Header.Get("User-ID")
	if userid == "" {
		errstr := fmt.Sprintf("failed to get user-id in http header")
		return mkResponse(apistructs.ClusterPreviewResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	data, err := e.clusters.ClusterPreview(req)
	if err != nil {
		errstr := fmt.Sprintf("failed to preview cluster info: %v", err)
		return mkResponse(apistructs.ClusterPreviewResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}

	for i, resource := range data {
		rsc := i18nPrinter.Sprintf(string(resource.Resource))
		data[i].Resource = apistructs.ClusterResourceType(rsc)
		for j, profile := range resource.ResourceProfile {
			data[i].ResourceProfile[j] = i18nPrinter.Sprintf(profile)
		}
	}

	return mkResponse(apistructs.ClusterPreviewResponse{
		Header: apistructs.Header{Success: true},
		Data:   data,
	})
}
