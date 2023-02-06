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

package api

import (
	"fmt"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/responses"

	"github.com/erda-project/erda/bundle"
)

var (
	rc  *reqCli
	bdl = bundle.New(bundle.WithCoreServices(), bundle.WithCMP())
)

func init() {
	rc = &reqCli{
		vendors: make(map[string]map[string]CloudVendor),
	}
}

type reqCli struct {
	// map[orgId][vendor_name]vendor. because one org may use different vendor
	vendors map[string]map[string]CloudVendor
}

func RegisterVendor(orgId string, vendor CloudVendor) {
	if _, ok := rc.vendors[orgId]; !ok {
		rc.vendors[orgId] = make(map[string]CloudVendor)
	}
	switch vendor.(type) {
	case *aliyun:
		rc.vendors[orgId]["aliyun"] = vendor
	default:
	}
}

// DoReqToAliyun .
func (r *reqCli) DoReqToAliyun(orgId string, request requests.AcsRequest, response responses.AcsResponse) error {
	return r.DoReqToVendor(orgId, request, response, "aliyun")
}

func (r *reqCli) DoReqToVendor(orgId string, request interface{}, response interface{}, vendor string) error {
	org, ok := r.vendors[orgId]
	if !ok {
		return fmt.Errorf("can not find org. orgId=%s", orgId)
	}
	v, ok := org[vendor]
	if !ok {
		return nil
	}
	return v.DoReq(request, response)
}
