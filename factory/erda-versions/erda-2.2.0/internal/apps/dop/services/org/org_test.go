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

package org_test

import (
	"testing"

	redis2 "github.com/go-redis/redis"

	"github.com/erda-project/erda-infra/providers/i18n"
	dashboardPb "github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/nexussvc"
	"github.com/erda-project/erda/internal/apps/dop/services/org"
	"github.com/erda-project/erda/internal/apps/dop/services/publisher"
	"github.com/erda-project/erda/pkg/ucauth"
)

func TestNew(t *testing.T) {
	var (
		db    *dao.DBClient
		uc    *ucauth.UCClient
		bdl   *bundle.Bundle
		pub   *publisher.Publisher
		nexus *nexussvc.NexusSvc
		redis *redis2.Client
		cmp   dashboardPb.ClusterResourceServer
		trans i18n.Translator
	)
	org.New(
		org.WithDBClient(db),
		org.WithUCClient(uc),
		org.WithBundle(bdl),
		org.WithPublisher(pub),
		org.WithNexusSvc(nexus),
		org.WithRedisClient(redis),
		org.WithCMP(cmp),
		org.WithTrans(trans),
	)
}
