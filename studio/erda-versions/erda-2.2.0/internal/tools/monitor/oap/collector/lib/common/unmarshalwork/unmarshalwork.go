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

package unmarshalwork

import (
	"sync"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib"
)

type UnmarshalWork interface {
	Unmarshal()
}

func Schedule(uw UnmarshalWork) {
	unmarshalWorkCh <- uw
}

func Start() {
	gomaxProcs := lib.AvailableCPUs()
	unmarshalWorkCh = make(chan UnmarshalWork, gomaxProcs)
	unmarshalWorkWG.Add(gomaxProcs)
	for i := 0; i < gomaxProcs; i++ {
		go func() {
			defer unmarshalWorkWG.Done()
			for uw := range unmarshalWorkCh {
				uw.Unmarshal()
			}
		}()
	}
}

func Stop() {
	close(unmarshalWorkCh)
	unmarshalWorkWG.Wait()
	unmarshalWorkCh = nil
}

var (
	unmarshalWorkCh chan UnmarshalWork
	unmarshalWorkWG sync.WaitGroup
)
