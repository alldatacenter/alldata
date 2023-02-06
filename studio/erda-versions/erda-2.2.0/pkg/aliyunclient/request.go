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

package aliyunclient

import (
	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/pkg/errors"
)

type RemoteActionRequest struct {
	ClusterName          string
	Product              string
	Version              string
	ActionName           string
	LocationServiceCode  string
	LocationEndpointType string
	QueryParams          map[string]string
	Headers              map[string]string
	FormParams           map[string]string
}

func NewRemoteActionRequest(acsReq requests.AcsRequest) (*RemoteActionRequest, error) {
	req := &RemoteActionRequest{
		Product:              acsReq.GetProduct(),
		Version:              acsReq.GetVersion(),
		ActionName:           acsReq.GetActionName(),
		LocationServiceCode:  acsReq.GetLocationServiceCode(),
		LocationEndpointType: acsReq.GetLocationEndpointType(),
	}
	err := requests.InitParams(acsReq)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	req.QueryParams = acsReq.GetQueryParams()
	req.Headers = acsReq.GetHeaders()
	req.FormParams = acsReq.GetFormParams()
	return req, nil
}
