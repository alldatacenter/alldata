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

package common

import (
	"net/url"
	"strconv"
	"strings"

	"github.com/pkg/errors"

	"github.com/pkg/browser"

	"github.com/erda-project/erda/tools/cli/command"
)

type EntityType string

type ErdaEntity struct {
	Type           EntityType
	Org            string
	OrgID          uint64
	ProjectID      uint64
	ApplicationID  uint64
	IssueID        uint64
	MergeRequestID uint64
	GittarBranch   string
}

var (
	OrgEntity        EntityType = "org"
	ProjectEntity    EntityType = "project"
	AppEntity        EntityType = "app"
	ReleaseEntity    EntityType = "release"
	IssueEntity      EntityType = "issue"
	MrEntity         EntityType = "mr"
	MrCompaireEntity EntityType = "mr-compare"
	GittarBranch     EntityType = "gittar-branch"
)

func Open(ctx *command.Context, entity ErdaEntity, params url.Values) error {
	url, err := ConstructURL(ctx, entity, params)
	if err != nil {
		return err
	}

	err = browser.OpenURL(url)
	if err != nil {
		return err
	}

	return nil
}

func ConstructURL(ctx *command.Context, entity ErdaEntity, params url.Values) (string, error) {
	if entity.Org == "" && entity.OrgID != 0 {
		o, err := GetOrgDetail(ctx, strconv.FormatUint(entity.OrgID, 10))
		if err != nil {
			return "", err
		}
		entity.Org = o.Name
	}

	paths := []string{strings.Replace(ctx.CurrentOpenApiHost, "openapi.", "", 1)}
	switch entity.Type {
	case OrgEntity:
		paths = append(paths, entity.Org)
	case ProjectEntity:
		paths = append(paths, entity.Org, "dop/projects", strconv.FormatUint(entity.ProjectID, 10))
	case AppEntity:
		paths = append(paths, entity.Org, "dop/projects", strconv.FormatUint(entity.ProjectID, 10),
			"apps", strconv.FormatUint(entity.ApplicationID, 10))
	case IssueEntity:
		paths = append(paths, entity.Org, "dop/projects", strconv.FormatUint(entity.ProjectID, 10),
			"issues/all")
	case MrCompaireEntity:
		paths = append(paths, entity.Org, "dop/projects", strconv.FormatUint(entity.ProjectID, 10),
			"apps", strconv.FormatUint(entity.ApplicationID, 10), "repo/mr/open/createMR")
	case MrEntity:
		paths = append(paths, entity.Org, "dop/projects", strconv.FormatUint(entity.ProjectID, 10),
			"apps", strconv.FormatUint(entity.ApplicationID, 10), "repo/mr/open",
			strconv.FormatUint(entity.MergeRequestID, 10))
	case GittarBranch:
		paths = append(paths, entity.Org, "dop/projects", strconv.FormatUint(entity.ProjectID, 10),
			"apps", strconv.FormatUint(entity.ApplicationID, 10), "repo/tree", entity.GittarBranch)
	default:
		return "", errors.Errorf("Invalid erda resource %s", string(entity.Type))
	}
	url := strings.Join(paths, "/")
	if len(params) > 0 {
		url += "?" + params.Encode()
	}

	return url, nil
}
