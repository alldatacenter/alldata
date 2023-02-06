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

package globals

import (
	"time"

	"github.com/erda-project/erda-infra/providers/kafka"
)

type Config struct {
	AccountReload     time.Duration        `file:"account_reload"`
	ProductListReload time.Duration        `file:"product_list_reload"`
	GatherWindow      time.Duration        `file:"gather_window"`
	MaxQPS            int                  `file:"max_qps" env:"CLOUDCAT_MAX_QPS" default:"1"`
	ReqLimit          uint64               `file:"req_limit" env:"CLOUDCAT_REQ_LIMIT" default:"3000"`
	ReqLimitDuration  time.Duration        `file:"req_limit_duration" env:"CLOUDCAT_REQ_LIMIT_DURATION" default:"1h"`
	ReqLimitTimeout   time.Duration        `file:"req_limit_timeout" env:"CLOUDCAT_REQ_LIMIT_TIMEOUT" default:"5m"`
	OrgIds            string               `file:"org_ids" env:"CLOUDCAT_IMPORT_ORG_IDS" desc:"list of orgs to import cloud telemetry"`
	ProductsCfg       string               `file:"products" env:"CLOUDCAT_PRODUCTS"`
	Output            kafka.ProducerConfig `file:"output"`
	Products          []string
}
