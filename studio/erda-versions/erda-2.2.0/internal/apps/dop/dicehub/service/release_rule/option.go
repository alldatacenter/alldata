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

package release_rule

import (
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
)

// Option defines *ReleaseRule configurations
type Option func(rule *ReleaseRule)

// WithDBClient sets the db client to *ReleaseRule
func WithDBClient(db *dbclient.DBClient) Option {
	return func(rule *ReleaseRule) {
		rule.db = db
	}
}
