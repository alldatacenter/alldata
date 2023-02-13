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

package clickhouse

import (
	"context"
	"fmt"
	"strconv"

	"github.com/erda-project/erda-infra/providers/clickhouse"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
	tablepkg "github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table"
)

func (p *provider) NewWriter(ctx context.Context) (storekit.BatchWriter, error) {
	return p.clickhouse.NewWriter(&clickhouse.WriterOptions{
		Encoder: func(data interface{}) (item *clickhouse.WriteItem, err error) {
			logData := data.(*log.LabeledLog)
			item = &clickhouse.WriteItem{
				Data: &logData.Log,
			}
			var wait <-chan error
			var table string

			if p.Retention == nil {
				return nil, fmt.Errorf("provider storage-retention-strategy@log is required")
			}

			key := p.Retention.GetConfigKey(logData.Source, logData.Tags)
			ttl := p.Retention.GetTTL(key)
			if len(key) > 0 {
				wait, table = p.Creator.Ensure(ctx, logData.Tags["dice_org_name"], key, tablepkg.FormatTTLToDays(ttl))
			} else {
				wait, table = p.Creator.Ensure(ctx, logData.Tags["dice_org_name"], "", tablepkg.FormatTTLToDays(ttl))
			}

			if wait != nil {
				select {
				case <-wait:
				case <-ctx.Done():
					return nil, storekit.ErrExitConsume
				}
			}
			p.fillLogInfo(&logData.Log)
			item.Table = table
			return item, nil
		},
	}), nil
}

func (p *provider) fillLogInfo(logData *log.Log) {
	id := logData.ID
	if len(id) > 12 {
		id = id[:12]
	}
	logData.UniqId = strconv.FormatInt(logData.Timestamp, 36) + "-" + id
	logData.OrgName = logData.Tags["dice_org_name"]
	tenantId := logData.Tags["monitor_log_key"]
	if len(tenantId) == 0 {
		tenantId = logData.Tags["msp_env_id"]
	}
	logData.TenantId = tenantId
}
