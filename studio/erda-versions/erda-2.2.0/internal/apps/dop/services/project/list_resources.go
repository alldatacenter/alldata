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

package project

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	cmpPb "github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

func (p *Project) ApplicationsResources(ctx context.Context, req *apistructs.ApplicationsResourcesRequest) (
	*apistructs.ApplicationsResourcesResponse, *errorresp.APIError) {
	l := logrus.WithField("func", "*Application.ApplicationsResources")
	if req == nil {
		err := errors.New("request can not be nil")
		l.WithError(err).Errorln("the req is nil")
		return nil, apierrors.ErrApplicationsResources.InvalidParameter(err)
	}
	var (
		projectID, _ = req.GetProjectID()
		orgID, _     = req.GetOrgID()
		appsIDs      = req.Query.GetAppIDs()
		ownersIDs    = req.Query.GetOwnerIDs()
	)
	l = l.WithFields(map[string]interface{}{"projectID": projectID, "filter appIDS": appsIDs, "filter ownerIDs": ownersIDs})
	var (
		applicationFilter = make(map[uint64]struct{})
		ownerFilter       = make(map[uint64]struct{})
	)
	for _, applicationID := range appsIDs {
		applicationFilter[applicationID] = struct{}{}
	}
	for _, ownerID := range ownersIDs {
		ownerFilter[ownerID] = struct{}{}
	}

	var data apistructs.ApplicationsResourcesResponse

	// query project info from core-services
	project, err := p.bdl.GetProject(projectID)
	if err != nil {
		l.WithError(err).Errorf("failed to GetProject(%v)", req.ProjectID)
		return nil, apierrors.ErrApplicationsResources.InternalError(err)
	}
	if len(project.ClusterConfig) == 0 {
		l.WithField("projectID", req.ProjectID).Warnln("clusters not found")
		return &data, nil
	}

	// query applications list from core-services
	// todo: pageSize
	apps, err := p.bdl.GetAppsByProject(projectID, orgID, req.UserID)
	if err != nil {
		l.WithError(err).Errorln("failed to GetAppsByProject")
		return nil, apierrors.ErrApplicationsResources.InternalError(err)
	}
	if apps.Total == 0 || len(apps.List) == 0 {
		l.Warnln("GetAppsByProject: no application in the project")
		return &data, nil
	}
	var (
		applicationsIDs []uint64
		items           = make(map[uint64]*apistructs.ApplicationsResourcesItem)
	)
	for _, application := range apps.List {
		if _, ok := applicationFilter[application.ID]; !ok && len(applicationFilter) > 0 {
			continue
		}
		var item = new(apistructs.ApplicationsResourcesItem)
		item.ID = application.ID
		item.Name = application.Name
		item.DisplayName = application.DisplayName
		owner := ownerUnknown()
		if cacheItem, _ := p.appOwnerCache.LoadWithUpdate(application.ID); cacheItem != nil {
			owners := cacheItem.(*memberCacheObject)
			if chosen, ok := owners.hasMemberIn(ownerFilter); ok {
				owner = chosen
			} else if len(ownerFilter) > 0 {
				continue
			}
		}
		item.OwnerUserID = owner.ID
		item.OwnerUserName = owner.Name
		item.OwnerUserNickname = owner.Nick
		applicationsIDs = append(applicationsIDs, application.ID)
		items[application.ID] = item
		data.List = append(data.List, item)
	}
	if len(applicationsIDs) == 0 {
		l.Warnln("no application in the given applications in the project")
		return &data, nil
	}

	// query runtimes list from orchestrator
	runtimesM, err := p.bdl.ListRuntimesGroupByApps(orgID, req.UserID, applicationsIDs, "")
	if err != nil {
		l.WithError(err).Errorln("failed to ListRuntimesGroupByApps")
		return nil, apierrors.ErrApplicationsResources.InternalError(err)
	}
	if len(runtimesM) == 0 {
		l.Warnln("runtime record not found")
	}
	runtimeMContent, _ := json.Marshal(runtimesM)
	l.Infof("runtimeM: %s", runtimeMContent)

	// serviceGroupID group by applicationID and workspace
	var (
		// {applicationID: {workspace: []serviceGroupID}}
		applicationWorkspaceServiceGroups = make(map[uint64]map[string][]string)
	)
	for applicationID, runtimes := range runtimesM {
		l := l.WithField("applicationID", applicationID)
		workspaceServiceGroups, ok := applicationWorkspaceServiceGroups[applicationID]
		if !ok {
			workspaceServiceGroups = make(map[string][]string)
		}
		if len(runtimes) == 0 {
			l.Warnln("len(runtimes) == 0")
		}
		for _, runtime := range runtimes {
			if runtime.Extra == nil {
				runtimeContent, _ := json.Marshal(runtime)
				l.WithField("runtime", string(runtimeContent)).Warnln("runtime.Extra == nil")
				continue
			}
			workspace := strings.ToUpper(runtime.Extra.Workspace)
			workspaceServiceGroups[workspace] = append(workspaceServiceGroups[workspace], runtime.ServiceGroupName)
		}
		applicationWorkspaceServiceGroups[applicationID] = workspaceServiceGroups
	}

	// query pods by the label "servicegroup-id" for every application and workspace from cmp
	for applicationsID, workspaceServiceGroups := range applicationWorkspaceServiceGroups {
		item := items[applicationsID]
		for workspace, serviceGroupIDs := range workspaceServiceGroups {
			cluster := project.ClusterConfig[workspace]
			labels := strings.Join(serviceGroupIDs, ",")
			l := l.WithFields(map[string]interface{}{"cluster": cluster, "servicegroup-id in": labels, "workspace": workspace})
			pods, err := p.cmp.GetPodsByLabels(ctx, &cmpPb.GetPodsByLabelsRequest{
				Cluster: project.ClusterConfig[workspace],
				Labels:  []string{fmt.Sprintf("servicegroup-id in (%s)", labels)},
			})
			if err != nil {
				l.WithError(err).Errorln("failed to GetPodsByLabels")
			}
			if len(pods.List) == 0 {
				l.Warnln("len(pods.List) == 0")
			}
			for _, pod := range pods.List {
				item.AddResource(workspace, 1, pod.CpuRequest, pod.MemRequest)
			}
		}
	}

	data.Total = len(data.List)
	data.OrderBy(req.Query.OrderBy...)
	data.Paging(req.Query.GetPageSize(), req.Query.GetPageNo())
	return &data, nil
}

func (p *Project) updateMemberCache(key interface{}) (interface{}, bool) {
	l := logrus.WithField("func", "*Project.updateMemberCache")

	unknown := ownerUnknown()
	object := newMemberCacheObject()
	object.m[unknown.ID] = unknown

	applicationID := key.(uint64)
	if _, err := p.bdl.GetApp(applicationID); err != nil {
		return nil, false
	}

	members, err := p.bdl.GetMembers(apistructs.MemberListRequest{
		ScopeType: "app",
		ScopeID:   int64(applicationID),
		Roles:     []string{"Owner"},
		Labels:    nil,
		Q:         "",
		PageNo:    1,
		PageSize:  999,
	})
	if err != nil || len(members.List) == 0 {
		l.WithError(err).Errorln("failed to GetMembers")
		object.m[unknown.ID] = unknown
		return object, true
	}

	for _, member := range members.List {
		userID, err := strconv.ParseUint(member.UserID, 10, 64)
		if err != nil {
			l.WithError(err).Warnf("failed to ParseUint, member userID: %s", members.List[0].UserID)
			continue
		}
		object.m[userID] = &memberItem{
			ID:   userID,
			Name: member.Name,
			Nick: member.Nick,
		}
	}
	if len(object.m) == 0 {
		object.m[unknown.ID] = unknown
	}

	return object, true
}
