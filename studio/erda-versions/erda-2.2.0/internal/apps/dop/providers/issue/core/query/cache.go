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

package query

import (
	"fmt"
	"time"

	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/cache"
)

type issueCache struct {
	db             *dao.DBClient
	iterationCache *cache.Cache
	stateCache     *cache.Cache
}

func NewIssueCache(db *dao.DBClient) (*issueCache, error) {
	v := issueCache{db: db}
	v.iterationCache = cache.New("iteration", time.Minute, func(i interface{}) (interface{}, bool) {
		iteration, err := v.db.GetIteration(i.(uint64))
		if err != nil {
			return nil, false
		}
		return iteration, true
	})
	v.stateCache = cache.New("state", time.Minute, func(i interface{}) (interface{}, bool) {
		state, err := v.db.GetIssueStateByID(i.(int64))
		if err != nil {
			return nil, false
		}
		return state, true
	})
	return &v, nil
}

func (v *issueCache) TryGetIteration(iterationID int64) (*dao.Iteration, error) {
	if iterationID <= 0 {
		return nil, nil
	}
	iteration, ok := v.iterationCache.LoadWithUpdate(uint64(iterationID))
	if !ok {
		return nil, fmt.Errorf("failed to get iteration")
	}
	i, ok := iteration.(*dao.Iteration)
	if !ok {
		return nil, fmt.Errorf("interface conversion: interface is %T, not *dao.Iteration", i)
	}
	return i, nil
}

func (v *issueCache) TryGetState(stateID int64) (*dao.IssueState, error) {
	if stateID <= 0 {
		return nil, fmt.Errorf("state id: %v is not valid", stateID)
	}
	state, ok := v.stateCache.LoadWithUpdate(stateID)
	if !ok {
		return nil, fmt.Errorf("failed to get state")
	}
	s, ok := state.(*dao.IssueState)
	if !ok {
		return nil, fmt.Errorf("interface conversion: interface is %T, not *dao.IssueState", s)
	}
	return s, nil
}
