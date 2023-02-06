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
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// GetAccessKey get access key with cluster name
func (e *Endpoints) GetAccessKey(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	clusterName := r.URL.Query().Get("clusterName")

	if clusterName == "" {
		errStr := fmt.Sprintf("empty cluster name")
		logrus.Error(errStr)
		return mkResponse(&apistructs.ClusterGetAkResponse{
			Header: apistructs.Header{
				Success: false,
				Error: apistructs.ErrorResponse{
					Msg: errStr,
				},
			},
		})
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return
	}

	res, err := e.clusters.GetAccessKey(clusterName)
	if err != nil {
		return mkResponse(&apistructs.ClusterGetAkResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}

	var respData *apistructs.ClusterAk

	if res.Total == 0 {
		respData = nil
	} else {
		respData = &apistructs.ClusterAk{Id: res.Data[0].Id, AccessKey: res.Data[0].AccessKey}
	}

	return mkResponse(&apistructs.ClusterGetAkResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: respData,
	})
}

func (e *Endpoints) CreateAccessKey(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	var req apistructs.ClusterCreateAkRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal to request body: %v", err)
		logrus.Error(err)
		return
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.CreateAction)
	if err != nil {
		return
	}

	res, err := e.clusters.GetOrCreateAccessKeyWithRecord(ctx, req.ClusterName, i.UserID, i.OrgID)
	if err != nil {
		return mkResponse(&apistructs.ClusterCreateAkResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}

	return mkResponse(&apistructs.ClusterCreateAkResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: res.AccessKey,
	})
}

func (e *Endpoints) ResetAccessKey(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	var req apistructs.ClusterResetAkRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal to request body: %v", err)
		logrus.Error(err)
		return
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.CreateAction)
	if err != nil {
		return
	}

	res, err := e.clusters.ResetAccessKeyWithRecord(ctx, req.ClusterName, i.UserID, i.OrgID)
	if err != nil {
		return mkResponse(&apistructs.ClusterResetAkResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}

	return mkResponse(&apistructs.ClusterCreateAkResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: res.AccessKey,
	})
}
