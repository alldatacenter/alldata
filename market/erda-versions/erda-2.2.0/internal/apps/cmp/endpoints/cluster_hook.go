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

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	// clusterTypeK8S Identify the k8s cluster type in the colony event
	clusterTypeK8S  = "k8s"
	clusterTypeEdas = "edas"
)

// ClusterHook starts steve server when create cluster and stop steve server when delete cluster
func (e *Endpoints) ClusterHook(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	if r.Body == nil {
		return httpserver.HTTPResponse{Status: http.StatusBadRequest, Content: "nil body"}, nil
	}
	var req apistructs.ClusterEvent
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("failed to decode clusterhook request: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{Status: http.StatusBadRequest, Content: errstr}, nil
	}

	if !strutil.Equal(req.Content.Type, clusterTypeK8S) || !strutil.Equal(req.Content.Type, clusterTypeEdas) {
		return httpserver.HTTPResponse{Status: http.StatusOK}, nil
	}

	if strutil.Equal(req.Action, bundle.CreateAction, true) {
		logrus.Infof("received cluster creating event, add steve server for cluster %s", req.Content.Name)
		e.SteveAggregator.Add(convertToPbClusterInfo(&req.Content))
	}

	if strutil.Equal(req.Action, bundle.DeleteAction, true) {
		logrus.Infof("received cluster delete event, delete steve server for cluster %s", req.Content.Name)
		e.SteveAggregator.Delete(req.Content.Name)
	}

	if strutil.Equal(req.Action, bundle.UpdateAction, true) {
		logrus.Infof("received cluster updating event, update steve server for cluster %s", req.Content.Name)
		e.SteveAggregator.Delete(req.Content.Name)
		e.SteveAggregator.Add(convertToPbClusterInfo(&req.Content))
	}

	return httpserver.HTTPResponse{Status: http.StatusOK}, nil
}

func convertToPbClusterInfo(in *apistructs.ClusterInfo) *clusterpb.ClusterInfo {
	var manageConfig *clusterpb.ManageConfig
	if in.ManageConfig != nil {
		manageConfig = &clusterpb.ManageConfig{
			Type:             in.ManageConfig.Type,
			Address:          in.ManageConfig.Address,
			CaData:           in.ManageConfig.CaData,
			CertData:         in.ManageConfig.CertData,
			KeyData:          in.ManageConfig.KeyData,
			Token:            in.ManageConfig.Token,
			AccessKey:        in.ManageConfig.AccessKey,
			CredentialSource: in.ManageConfig.CredentialSource,
		}
	}
	return &clusterpb.ClusterInfo{
		Name:         in.Name,
		DisplayName:  in.DisplayName,
		Type:         in.Type,
		ManageConfig: manageConfig,
	}
}
