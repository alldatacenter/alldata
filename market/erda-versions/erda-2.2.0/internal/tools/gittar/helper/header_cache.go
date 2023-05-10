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
	"fmt"
	"time"

	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
)

// HeaderNoCache writing function
func headerNoCache(c *webcontext.Context) {
	c.Header("Pragma", "no-cache")
	c.Header("Expires", "Fri, 01 Jan 1980 00:00:00 GMT")
	c.Header("cache-Control", "no-cache, max-age=0, must-revalidate")
}

// HeaderCacheForever writing function
func headerCacheForever(c *webcontext.Context) {
	now := time.Now().Unix()
	expires := now + 31536000
	c.Header("Date", fmt.Sprintf("%d", now))
	c.Header("Expires", fmt.Sprintf("%d", expires))
	c.Header("cache-Control", "public, max-age=31536000")
}
