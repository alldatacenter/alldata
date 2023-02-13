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

package projectCache

import (
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/pkg/cache"
)

var (
	projectID2Namespaces          *cache.Cache
	projectID2NamespacesCacheName = "core-services/project/id-to-namespaces"

	projectID2Member          *cache.Cache
	projectID2MemberCacheName = "core-services/project/id-to-member"

	db *dao.DBClient
)

// New make new caches about projcts
func New(dbClient *dao.DBClient) {
	db = dbClient

	projectID2Namespaces = cache.New(projectID2NamespacesCacheName, time.Minute, retrieveNamespaces)
	projectID2Member = cache.New(projectID2MemberCacheName, time.Minute*10, retrieveMember)

	go func() {
		projects, _ := db.GetAllProjects()
		for _, project := range projects {
			GetNamespacesByProjectID(uint64(project.ID))
			GetMemberByProjectID(uint64(project.ID))
		}
	}()
}

// ProjectClusterNamespaces caches the relationship for project:cluster:namespace
type ProjectClusterNamespaces struct {
	ProjectID  uint64
	Namespaces map[string][]string
}

// ProjectMember caches the relationship for project:owner
type ProjectMember struct {
	ProjectID uint64
	UserID    uint
	Name      string
	Nick      string
}

// GetNamespacesByProjectID receive a project id and returns its cluster and namespaces from caches
func GetNamespacesByProjectID(id uint64) (*ProjectClusterNamespaces, bool) {
	obj, ok := projectID2Namespaces.LoadWithUpdate(id)
	if !ok {
		return nil, false
	}
	return obj.(*ProjectClusterNamespaces), true
}

// GetMemberByProjectID receive a project id and returns its owner from caches
func GetMemberByProjectID(id uint64) (*ProjectMember, bool) {
	obj, ok := projectID2Member.LoadWithUpdate(id)
	if !ok {
		return nil, false
	}
	return obj.(*ProjectMember), true
}

func retrieveNamespaces(i interface{}) (interface{}, bool) {
	projectID := i.(uint64)
	item := newProjectClusterNamespaceItem(projectID)
	if err := db.GetProjectClustersNamespacesByProjectID(item.Namespaces, projectID); err != nil {
		logrus.WithField("cacheName", projectID2NamespacesCacheName).
			WithField("projectID", projectID).
			Warnln("failed to GetProjectClustersNamespacesByProjectID")
		return nil, false
	}
	return item, true
}

func retrieveMember(i interface{}) (interface{}, bool) {
	projectID := i.(uint64)
	var memberListReq = apistructs.MemberListRequest{
		ScopeType: "project",
		ScopeID:   int64(projectID),
		Roles:     []string{"Owner", "Lead"},
		Labels:    nil,
		Q:         "",
		PageNo:    1,
		PageSize:  1000,
	}
	var member model.Member
	switch _, members, err := db.GetMembersByParam(&memberListReq); {
	case err != nil:
		logrus.WithError(err).WithField("projectID", projectID).
			WithField("memberListReq", memberListReq).
			Warnln("failed to GetMembersByParam")
		return nil, false
	case len(members) == 0:
		logrus.WithError(err).WithField("projectID", projectID).
			WithField("memberListReq", memberListReq).
			Warnln("not found owner for the project")
		return &ProjectMember{ProjectID: projectID}, true
	default:
		hitFirstValidOwnerOrLead(&member, members)
		userID, err := strconv.ParseUint(member.UserID, 10, 64)
		// hit nothing
		if err != nil {
			logrus.WithError(err).WithField("projectID", projectID).
				WithField("member", member).
				Warnln("failed to ParseUint")
			return &ProjectMember{ProjectID: projectID}, true
		}
		return &ProjectMember{
			ProjectID: projectID,
			UserID:    uint(userID),
			Name:      member.Name,
			Nick:      member.Nick,
		}, true
	}
}

func newProjectClusterNamespaceItem(projectID uint64) *ProjectClusterNamespaces {
	return &ProjectClusterNamespaces{
		ProjectID:  projectID,
		Namespaces: make(map[string][]string),
	}
}

func hitFirstValidOwnerOrLead(defaultOne *model.Member, members []model.Member) {
	if defaultOne == nil {
		return
	}
	for _, role_ := range []string{"Owner", "Lead"} {
		for _, member := range members {
			for _, role := range member.Roles {
				if strings.EqualFold(role, role_) {
					*defaultOne = member
					return
				}
			}
		}
	}
}
