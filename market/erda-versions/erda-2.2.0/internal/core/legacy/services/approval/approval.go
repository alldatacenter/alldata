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

package approval

import (
	"github.com/erda-project/erda/internal/core/legacy/dao"
)

type Approval struct {
	db *dao.DBClient
}

// Option .
type Option func(*Approval)

// New 新建 approval service
func New(options ...Option) *Approval {
	a := &Approval{}
	for _, op := range options {
		op(a)
	}
	return a
}

// WithDBClient 设置 dbclient
func WithDBClient(db *dao.DBClient) Option {
	return func(a *Approval) {
		a.db = db
	}
}
