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

package dao

import (
	"time"

	"github.com/erda-project/erda/internal/apps/gallery/model"
	"github.com/erda-project/erda/pkg/cache"
)

var version2presentation *cache.Cache

func initCache() {
	if version2presentation != nil {
		return
	}
	version2presentation = cache.New("apps.gallery-version-to-presentation", time.Minute*10, func(i interface{}) (interface{}, bool) {
		presentation, ok, _ := getPresentationByVersionID(Q(), i.(string))
		if !ok {
			return nil, false
		}
		return presentation, true
	})
}

func GetPresentationByVersionID(versionID string) (*model.OpusPresentation, bool) {
	i, ok := version2presentation.LoadWithUpdate(versionID)
	if !ok {
		return nil, false
	}
	presentation, ok := i.(*model.OpusPresentation)
	return presentation, ok
}
