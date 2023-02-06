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
	"io/ioutil"
	"net/http"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/language"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
	"github.com/erda-project/erda/pkg/aliyunclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

type cachedRegionItem struct {
	regionId       string
	lastTimeUpdate time.Time
}

var cachedClusterRegions = map[string]cachedRegionItem{}

func acquireClusterRegion(akCtx aliyun_resources.Context, clusterName string) string {
	var backupRegion string
	if item, exist := cachedClusterRegions[clusterName]; exist {
		if time.Now().Sub(item.lastTimeUpdate) < time.Hour {
			return item.regionId
		}
		backupRegion = item.regionId
	}
	regions := aliyun_resources.ActiveRegionIDs(akCtx)
	vpcs, _, err := vpc.List(akCtx, aliyun_resources.DefaultPageOption, regions.VPC, clusterName)
	if err != nil {
		logrus.Errorf("vpc list failed, err: %+v", err)
		return backupRegion
	}
	if len(vpcs) == 0 {
		err = fmt.Errorf("vpc not found, clusterName: %s", clusterName)
		logrus.Error(err.Error())
		return backupRegion
	}
	cachedClusterRegions[clusterName] = cachedRegionItem{
		regionId:       vpcs[0].RegionId,
		lastTimeUpdate: time.Now(),
	}
	return vpcs[0].RegionId

}

func (e *Endpoints) DoRemoteAction(ctx context.Context, w http.ResponseWriter, r *http.Request, vars map[string]string) error {
	var req apistructs.RemoteActionRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		logrus.Errorf("unmarshal to remoteactionrequest failed, err: %+v", err)
		return err
	}
	ctx = context.WithValue(ctx, "i18nPrinter", message.NewPrinter(language.English))
	akCtx, resp := e.mkCtx(ctx, req.OrgID)
	if resp != nil {
		err := fmt.Errorf("mkCtx failed, orgId: %s", req.OrgID)
		logrus.Error(err.Error())
		return err
	}
	regionId := acquireClusterRegion(akCtx, req.ClusterName)
	client := &aliyunclient.Client{}
	err := client.InitWithAccessKey(regionId, akCtx.AccessKeyID, akCtx.AccessSecret)
	if err != nil {
		logrus.Errorf("init aliyun client failed, err: %+v", err)
		return err
	}
	client.EndpointMap = req.EndpointMap
	client.EndpointType = req.EndpointType
	rpcRequest := &requests.RpcRequest{}
	rpcRequest.InitWithApiInfo(req.Product, req.Version, req.ActionName, req.LocationServiceCode, req.LocationEndpointType)
	rpcRequest.Scheme = req.Scheme
	rpcRequest.QueryParams = req.QueryParams
	rpcRequest.Headers = req.Headers
	rpcRequest.FormParams = req.FormParams
	response, err := client.GetActionResponse(rpcRequest)
	if err != nil {
		logrus.Errorf("get action response failed, err:%+v", err)
		return err
	}
	defer response.Body.Close()
	for key, values := range response.Header {
		for _, value := range values {
			w.Header().Add(key, value)
		}
	}
	w.WriteHeader(response.StatusCode)
	respBody, err := ioutil.ReadAll(response.Body)
	if err != nil {
		logrus.Errorf("read body falied, err:%+v", err)
	}
	_, err = w.Write(respBody)
	if err != nil {
		logrus.Errorf("write response failed, err:%+v", err)
		return err
	}
	return nil
}

// GetCloudAccount Get cloud account
func (e *Endpoints) GetCloudAccount(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happend: %+v", err)
			resp, err = mkResponse(apistructs.CloudAccountResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
			})
		}
	}()
	orgID := r.URL.Query().Get("orgID")
	vendor := r.URL.Query().Get("vendor")
	if orgID == "" {
		err = errors.New("orgID is empty")
		return
	}
	switch vendor {
	case "aliyun":
	default:
		err = errors.Errorf("unknown vendor: %s", vendor)
		return
	}
	akCtx, resp := e.mkCtx(ctx, orgID)
	if resp != nil {
		return
	}
	resp, err = mkResponse(apistructs.CloudAccountResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CloudAccount{
			AccessKeyID:  akCtx.AccessKeyID,
			AccessSecret: akCtx.AccessSecret,
		},
	})
	return
}
