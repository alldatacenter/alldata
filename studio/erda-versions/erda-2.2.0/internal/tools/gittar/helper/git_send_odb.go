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

package helper

import (
	"path"

	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
)

// SendTextFile file
func SendTextFile(file string, c *webcontext.Context) {
	headerNoCache(c)
	c.Header("Content-Type", "text/plain; charset=utf-8")
	c.File(path.Join(c.Repository.DiskPath(), file))
}

// SendInfoPacks info packs
func SendInfoPacks(c *webcontext.Context) {
	headerNoCache(c)
	c.Header("Content-Type", "text/plain; charset=utf-8")
	c.File(c.MustGet("repository").(*gitmodule.Repository).InfoPacksPath())
}

// SendLooseObject loose object
func SendLooseObject(prefix string, suffix string, c *webcontext.Context) {
	headerCacheForever(c)
	c.Header("Content-Type", "application/x-git-loose-object")
	c.File(c.MustGet("repository").(*gitmodule.Repository).LooseObjectPath(prefix, suffix))
}

// SendPackIdxFile file
func SendPackIdxFile(pack string, isIdx bool, c *webcontext.Context) {
	headerCacheForever(c)
	if isIdx == false {
		c.Header("Content-Type", "application/x-git-packed-objects")
	} else {
		c.Header("Content-Type", "application/x-git-packed-objects-toc")
	}
	c.File(c.MustGet("repository").(*gitmodule.Repository).PackIdxPath(pack))
}
