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
	"path"
	"strings"

	"github.com/erda-project/erda/internal/tools/gittar/helper"
	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
)

// GetRepoHead function
func GetRepoHead(context *webcontext.Context) {
	helper.SendTextFile("HEAD", context)
}

// GetRepoInfoRefs function
func GetRepoInfoRefs(c *webcontext.Context) {
	serviceName := strings.Trim(c.Query("service"), "git-")
	if serviceName != "" {
		helper.RunAdvertisement(serviceName, c)
	} else {
		helper.SendInfoPacks(c)
	}
}

// GetRepoObjects function
func GetRepoObjects(c *webcontext.Context) {
	prefix := c.Param("prefix")
	suffix := c.Param("suffix")
	switch prefix {
	case "pack":
		pack := suffix
		isIdx := strings.HasSuffix(pack, "idx")
		helper.SendPackIdxFile(suffix, isIdx, c)
	case "info":
		if suffix == "packs" {
			helper.SendInfoPacks(c)
		} else {
			file := suffix
			helper.SendTextFile(path.Join("objects", "info", file), c)
		}
	default:
		helper.SendLooseObject(prefix, suffix, c)
	}
}

// ServiceRepoRPC function
func ServiceRepoRPC(c *webcontext.Context) {
	service := c.Param("service")
	if service == "receive-pack" {
		// 检查仓库是否锁定
		isLocked, err := c.Service.GetRepoLocked(c.Repository.ProjectId, c.Repository.ApplicationId)
		if err != nil {
			c.Abort(err)
			return
		}
		if isLocked {
			c.Abort(ERROR_REPO_LOCKED)
			return
		}
	}
	helper.RunProcess(service, c)
}
