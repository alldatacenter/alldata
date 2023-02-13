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

package middleware

import (
	"context"
	"fmt"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"
	apiuser "k8s.io/apiserver/pkg/authentication/user"
	"k8s.io/apiserver/pkg/endpoints/request"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/steve/predefined"
	"github.com/erda-project/erda/pkg/http/httputil"
)

const varsKey = "stevePathVars"

type Authenticator struct {
	bdl        *bundle.Bundle
	clusterSvc clusterpb.ClusterServiceServer
}

// NewAuthenticator return a steve Authenticator with bundle.
// bdl need withCoreServices to check permission.
func NewAuthenticator(bdl *bundle.Bundle, clusterSvc clusterpb.ClusterServiceServer) *Authenticator {
	return &Authenticator{bdl: bdl, clusterSvc: clusterSvc}
}

// AuthMiddleware authenticate for steve server by bundle.
func (a *Authenticator) AuthMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(resp http.ResponseWriter, req *http.Request) {
		muxVars := parseVars(req)
		ctx := context.WithValue(req.Context(), varsKey, muxVars)
		req = req.WithContext(ctx)
		clusterName := muxVars["clusterName"]
		typ := muxVars["type"]

		userID := req.Header.Get("User-ID")
		orgID := req.Header.Get("Org-ID")

		logrus.Infof("Steve server receive request. User-ID: %s, Org-ID: %s, Path: %s.", userID, orgID, req.URL.String())

		scopeID, err := strconv.ParseUint(orgID, 10, 64)
		if err != nil {
			resp.WriteHeader(http.StatusBadRequest)
			resp.Write(apistructs.NewSteveError(apistructs.BadRequest, "invalid org id").JSON())
			return
		}

		clusters, err := a.listClusterByType(req.Context(), scopeID, "k8s", "edas")
		if err != nil {
			logrus.Errorf("failed to list cluster %s in steve authenticate, %v", clusterName, err)
			resp.WriteHeader(http.StatusInternalServerError)
			resp.Write(apistructs.NewSteveError(apistructs.ServerError, "check permission internal error").JSON())
			return
		}

		found := false
		for _, cluster := range clusters {
			if cluster.Name == clusterName {
				found = true
				break
			}
		}
		if !found {
			resp.WriteHeader(http.StatusNotFound)
			resp.Write(apistructs.NewSteveError(apistructs.NotFound,
				fmt.Sprintf("cluster %s not found in target org", clusterName)).JSON())
			return
		}

		r := &apistructs.ScopeRoleAccessRequest{
			Scope: apistructs.Scope{
				Type: apistructs.OrgScope,
				ID:   orgID,
			},
		}
		rsp, err := a.bdl.ScopeRoleAccess(userID, r)
		if err != nil {
			logrus.Errorf("failed to get scope role access in steve authentication, %v", err)
			resp.WriteHeader(http.StatusInternalServerError)
			resp.Write(apistructs.NewSteveError(apistructs.ServerError, "check permission internal error").JSON())
			return
		}
		if !rsp.Access {
			resp.WriteHeader(http.StatusForbidden)
			resp.Write(apistructs.NewSteveError(apistructs.PermissionDenied, "access denied").JSON())
			return
		}

		name := fmt.Sprintf("erda-user-%s", userID)
		user := &apiuser.DefaultInfo{
			Name: name,
			UID:  name,
		}
		for _, role := range rsp.Roles {
			group, ok := predefined.RoleToGroup[role]
			if !ok {
				continue
			}
			user.Groups = append(user.Groups, group)
		}

		if len(user.Groups) == 0 {
			resp.WriteHeader(http.StatusForbidden)
			resp.Write(apistructs.NewSteveError(apistructs.PermissionDenied, "access denied").JSON())
			return
		}

		if req.Method == http.MethodGet && typ == "nodes" {
			user = &apiuser.DefaultInfo{
				Name: "admin",
				UID:  "admin",
				Groups: []string{
					"system:masters",
					"system:authenticated",
				},
			}
		}

		ctx = request.WithUser(ctx, user)
		req = req.WithContext(ctx)
		next.ServeHTTP(resp, req)
	})
}

func (a *Authenticator) listClusterByType(ctx context.Context, orgID uint64, types ...string) ([]*clusterpb.ClusterInfo, error) {
	var result []*clusterpb.ClusterInfo
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	for _, typ := range types {
		resp, err := a.clusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{ClusterType: typ})
		if err != nil {
			return nil, err
		}
		result = append(result, resp.Data...)
	}
	return result, nil
}

func parseVars(req *http.Request) map[string]string {
	var match mux.RouteMatch
	m := mux.NewRouter().PathPrefix("/api/k8s/clusters/{clusterName}")
	s := m.Subrouter()
	s.Path("/")
	s.Path("/v1")
	s.Path("/v1/")
	s.Path("/v1/{type}")
	s.Path("/v1/{type}/{name}")
	s.Path("/v1/{type}/{namespace}/{name}")
	s.Path("/v1/{type}/{namespace}/{name}/{link}")
	s.Path("/api/{version}/namespaces/{namespace}/{type}")
	s.Path("/api/{version}/namespaces/{namespace}/{type}/{name}")
	s.Path("/api/{version}/namespaces/{namespace}/{type}/{name}/{link}")
	s.PathPrefix("/api")

	vars := make(map[string]string)
	if s.Match(req, &match) {
		vars = match.Vars
	}

	var shellMatch mux.RouteMatch
	shellRouter := mux.NewRouter().Path("/api/k8s/clusters/{clusterName}/kubectl-shell")
	if shellRouter.Match(req, &shellMatch) {
		vars = shellMatch.Vars
		vars["kubectl-shell"] = "true"
	}
	return vars
}
