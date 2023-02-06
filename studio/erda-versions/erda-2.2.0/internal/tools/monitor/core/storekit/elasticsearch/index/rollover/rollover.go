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

package rollover

import (
	"context"
	"time"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

func (p *provider) runIndexRollover(ctx context.Context) {
	p.loader.WaitAndGetIndices(ctx)
	p.Log.Infof("run index rollover with interval(%v)", p.Cfg.CheckInterval)
	defer p.Log.Info("exit index rollover")
	timer := time.NewTimer(15 * time.Second) // 15s, to avoid restart in short time
	for {
		select {
		case <-timer.C:
		case <-ctx.Done():
			return
		}
		p.RolloverIndices(ctx, func(*loader.IndexEntry) bool { return true })
		timer.Reset(p.Cfg.CheckInterval)
	}
}

func (p *provider) RolloverIndices(ctx context.Context, filter loader.Matcher) error {
	return p.rolloverIndices(filter, p.rolloverBody)
}

func (p *provider) rolloverIndices(filter loader.Matcher, body string) error {
	indices := p.loader.AllIndices()
	if indices == nil {
		return nil
	}
	var num int

	var fn func(indices *loader.IndexGroup)
	fn = func(indices *loader.IndexGroup) {
		if len(indices.List) > 0 && indices.List[0].Num >= 0 && filter(indices.List[0]) {
			alias := p.indexAlias(indices.List[0])
			if len(alias) > 0 {
				ok, _ := p.rolloverAlias(alias, body)
				if ok {
					num++
				}
			}
		}
		for _, ig := range indices.Groups {
			fn(ig)
		}
	}
	fn(indices)

	if num > 0 {
		p.loader.ReloadIndices()
	}
	return nil
}

func (p *provider) rolloverAlias(alias, body string) (bool, error) {
	ctx, cancel := context.WithTimeout(context.Background(), p.Cfg.RequestTimeout)
	defer cancel()
	resp, err := p.loader.Client().RolloverIndex(alias).BodyString(body).Do(ctx)
	if err != nil {
		p.Log.Errorf("failed to rollover alias %s : %s", alias, err)
		return false, err
	}
	if resp.Acknowledged {
		p.Log.Infof("rollover alias %s from %s to %s, %v", alias, resp.OldIndex, resp.NewIndex, resp.Acknowledged)
	} else if p.Cfg.Verbose {
		p.Log.Debugf("rollover alias %s from %s to %s, %v", alias, resp.OldIndex, resp.NewIndex, resp.Acknowledged)
	}
	return resp.Acknowledged, nil
}

func (p *provider) indexAlias(entry *loader.IndexEntry) string {
	for _, ptn := range p.patterns {
		result, match := ptn.index.Match(entry.Index, index.InvalidPatternValueChars)
		if match {
			alias, err := ptn.alias.Fill(result.Keys...)
			if err != nil {
				p.Log.Errorf("failed to fill keys %v into alias pattern %q: %s", result.Keys, err, ptn.alias.Pattern)
			}
			return alias
		}
	}
	return ""
}
