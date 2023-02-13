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

package actionmgr

import (
	"context"
	"fmt"
	"time"

	"github.com/erda-project/erda/apistructs"
)

func (s *provider) continuousRefreshAction(ctx context.Context) {
	ticker := time.NewTicker(s.Cfg.RefreshInterval)
	defer ticker.Stop()
	for ; true; <-ticker.C {
		if err := s.constructAllActions(); err != nil {
			s.Log.Errorf("failed to construct all actions: %v", err)
		}
	}
}

func (s *provider) constructAllActions() error {
	allExtensions, err := s.bdl.QueryExtensions(apistructs.ExtensionQueryRequest{
		All:  true,
		Type: "action",
	})
	if err != nil {
		return fmt.Errorf("failed to query all extension: %v", err)
	}
	s.pools.Start()
	for i := range allExtensions {
		extension := allExtensions[i]
		s.pools.MustGo(func() {
			s.updateExtensionCache(extension)
		})
	}
	s.pools.Stop()
	return nil
}
