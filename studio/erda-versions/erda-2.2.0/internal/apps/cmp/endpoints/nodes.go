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
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (e *Endpoints) AddNodes(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.AddNodesRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("failed to unmarshal to apistructs.AddNodesRequest: %v", err)
		return mkResponse(apistructs.AddNodesResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	userid := r.Header.Get("User-ID")
	if userid == "" {
		errstr := fmt.Sprintf("failed to get user-id in http header")
		return mkResponse(apistructs.AddNodesResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}

	// permission check
	p := apistructs.PermissionCheckRequest{
		UserID:   userid,
		Scope:    apistructs.OrgScope,
		ScopeID:  req.OrgID,
		Resource: apistructs.CloudResourceResource,
		Action:   apistructs.CreateAction,
	}
	rspData, err := e.bdl.CheckPermission(&p)
	if err != nil {
		logrus.Errorf("check permission error: %v", err)
		return mkResponse(apistructs.CloudClusterResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: "check permission internal error"},
			},
		})
	}
	if !rspData.Access {
		return mkResponse(apistructs.CloudClusterResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: "access denied"},
			},
		})
	}

	recordID, err := e.nodes.AddNodes(req, userid)
	if err != nil {
		errstr := fmt.Sprintf("failed to add nodes: %v", err)
		return mkResponse(apistructs.AddNodesResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	return mkResponse(apistructs.AddNodesResponse{
		Header: apistructs.Header{Success: true},
		Data:   apistructs.AddNodesData{RecordID: recordID},
	})
}

func (e *Endpoints) RmNodes(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.RmNodesRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("failed to unmarshal to apistructs.RmNodesRequest: %v", err)
		return mkResponse(apistructs.RmNodesResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	userid := r.Header.Get("User-ID")
	if userid == "" {
		errstr := fmt.Sprintf("failed to get user-id in http header")
		return mkResponse(apistructs.RmNodesResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	orgid := r.Header.Get("Org-ID")
	scopeID, err := strconv.ParseUint(orgid, 10, 64)
	if err != nil {
		logrus.Errorf("parse orgid failed, orgid: %v, error: %v", orgid, err)
		return mkResponse(apistructs.CloudClusterResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: "parse orgid failed"},
			},
		})
	}
	p := apistructs.PermissionCheckRequest{
		UserID:   userid,
		Scope:    apistructs.OrgScope,
		ScopeID:  scopeID,
		Resource: apistructs.CloudResourceResource,
		Action:   apistructs.DeleteAction,
	}
	rspData, err := e.bdl.CheckPermission(&p)
	if err != nil {
		logrus.Errorf("check permission failed, request: %v, error: %v", p, err)
		return mkResponse(apistructs.CloudClusterResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: "check permission internal error"},
			},
		})
	}
	if !rspData.Access {
		logrus.Errorf("check permission failed, request: %v, response: %v", p, rspData)
		return mkResponse(apistructs.CloudClusterResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: "access denied"},
			},
		})
	}

	recordID, err := e.nodes.RmNodes(req, userid, orgid)
	if err != nil {
		errstr := fmt.Sprintf("failed to rm nodes: %v", err)
		return mkResponse(apistructs.RmNodesResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	return mkResponse(apistructs.RmNodesResponse{
		Header: apistructs.Header{Success: true},
		Data:   apistructs.RmNodesData{RecordID: recordID},
	})
}

func mkResponse(content interface{}) (httpserver.Responser, error) {
	return httpserver.HTTPResponse{
		Status:  http.StatusOK,
		Content: content,
	}, nil
}
