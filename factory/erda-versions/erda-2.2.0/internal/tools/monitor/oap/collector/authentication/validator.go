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

package authentication

import (
	"context"
	"sync"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
)

type AccessItemCollection map[string]*tokenpb.Token

type Validator interface {
	// Validate +Validate
	Validate(scope string, scopeId string, token string) bool
}

type accessKeyValidator struct {
	sync.RWMutex
	collection   AccessItemCollection
	TokenService tokenpb.TokenServiceServer
}

func (v *accessKeyValidator) syncFullAccessKeys(ctx context.Context) error {
	var (
		pageNumber int64 = 1
		pageSize   int64 = 100
	)
	results := make([]*tokenpb.Token, 0)
	for {
		resp, err := v.TokenService.QueryTokens(ctx, &tokenpb.QueryTokensRequest{
			PageNo:   pageNumber,
			PageSize: pageSize,
		})
		if err != nil {
			return err
		}
		if resp.Data != nil {
			results = append(results, resp.Data...)
		}
		if resp.Total <= pageNumber*pageSize {
			break
		}
		pageNumber++
	}
	v.Lock()
	defer v.Unlock()
	for k := range v.collection {
		delete(v.collection, k)
	}
	for _, item := range results {
		v.collection[item.AccessKey] = item
	}
	return nil
}

func (v *accessKeyValidator) Validate(scope string, scopeId string, token string) bool {
	v.RLock()
	defer v.RUnlock()
	item, ok := v.collection[token]
	return ok && item.AccessKey == token && item.ScopeId == scopeId && item.Scope == scope
}
