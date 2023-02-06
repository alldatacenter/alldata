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

package manager

import (
	"context"
	"testing"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/types"
)

func Test_defaultManager_Stop(t *testing.T) {
	// nil mgr
	var mgr types.QueueManager = (*defaultManager)(nil)
	mgr.Stop()

	// mgr with multiple stop channels
	ctx := context.Background()
	mgr = New(ctx)
	mgr.(*defaultManager).queueStopChanByID["id1"] = make(chan struct{})
	mgr.(*defaultManager).queueStopChanByID["id2"] = make(chan struct{})
	for _, stopCh := range mgr.(*defaultManager).queueStopChanByID {
		go func(ch chan struct{}) {
			<-ch
		}(stopCh)
	}
	// no blocking
	mgr.Stop()
}
