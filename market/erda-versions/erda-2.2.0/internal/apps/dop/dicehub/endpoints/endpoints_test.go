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

package endpoints_test

import (
	"testing"

	"github.com/gorilla/schema"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/endpoints"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/publish_item"
	release2 "github.com/erda-project/erda/internal/apps/dop/dicehub/service/release"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/release_rule"
)

func TestNew(t *testing.T) {
	var (
		db          = new(dbclient.DBClient)
		bdl         = new(bundle.Bundle)
		release     = new(release2.Release)
		publishItem = new(publish_item.PublishItem)
		rule        = new(release_rule.ReleaseRule)
		decoder     = new(schema.Decoder)
	)
	endpoints.New(
		endpoints.WithDBClient(db),
		endpoints.WithBundle(bdl),
		endpoints.WithReleaseRule(rule),
		endpoints.WithPublishItem(publishItem),
		endpoints.WithRelease(release),
		endpoints.WithQueryStringDecoder(decoder),
	)
}
