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

package creator

import (
	"context"
	"fmt"
	"strings"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
)

// Ensure .
func (p *provider) Ensure(keys ...string) (_ <-chan error, alias string) {
	// find pattern
	keylen := len(keys)
	var pattern *indexAliasPattern
	for _, ptn := range p.patterns {
		if ptn.alias.KeyNum == keylen {
			pattern = ptn
			break
		}
	}
	if pattern == nil {
		ch := make(chan error, 1)
		ch <- index.ErrKeyLength
		close(ch)
		return ch, ""
	}
	for i, key := range keys {
		keys[i] = index.NormalizeKey(key)
	}

	// get index alias name by keys
	alias, _ = pattern.alias.Fill(keys...)

	// check index exist
	igroup := p.loader.IndexGroup(keys...)
	if igroup != nil && len(igroup.List) > 0 && igroup.List[0].Num >= 0 {
		return nil, alias
	}

	ch := make(chan error, 1)

	// get index name by keys
	indexName, err := pattern.index.Fill(keys...)
	if err != nil {
		ch <- index.ErrKeyLength
		close(ch)
		return ch, ""
	}

	// send request and wait
	p.createCh <- request{
		Index: indexName,
		Alias: alias,
		Wait:  ch,
	}
	return ch, alias
}

func (p *provider) createIndex(ctx context.Context, index, alias string) (_ bool, err error) {
	ctx, cancel := context.WithTimeout(ctx, p.Cfg.RequestTimeout)
	defer cancel()
	resp, err := p.loader.Client().CreateIndex(index).BodyJson(
		map[string]interface{}{
			"aliases": map[string]interface{}{
				alias: make(map[string]interface{}),
			},
		},
	).Do(ctx)
	if err != nil {
		if err, ok := err.(*elastic.Error); ok {
			if err.Status == 400 && err.Details != nil && strings.Contains(err.Details.Reason, "already exists") {
				return false, nil
			}
		}
		return false, fmt.Errorf("failed to create index=%q, alias=%q: %s", index, alias, err)
	}
	if resp != nil && !resp.Acknowledged {
		return false, fmt.Errorf("failed to create index=%q, alias=%q: not Acknowledged", index, alias)
	}
	return true, nil
}

func (p *provider) Run(ctx context.Context) error {
	for {
		select {
		case <-ctx.Done():
			return nil
		case req := <-p.createCh:
			if func(req request) bool {
				p.createdLock.Lock()
				defer p.createdLock.Unlock()
				if !p.created[req.Alias] {
					for {
						ok, err := p.createIndex(ctx, req.Index, req.Alias)
						if err != nil {
							p.Log.Error(err)
							select {
							case <-ctx.Done():
								return true
							default:
							}
							continue
						}
						if ok {
							p.Log.Infof("create index %q with alias %q ok", req.Index, req.Alias)
						}
						break
					}
					// avoid duplicate index creation
					p.created[req.Alias] = true
				}
				if req.Wait != nil {
					req.Wait <- nil
					close(req.Wait)
				}
				return false
			}(req) {
				return nil
			}
		}
	}
}

func (p *provider) FixedIndex(keys ...string) (string, error) {
	keylen := len(keys)
	for _, ptn := range p.fixedPatterns {
		if ptn.KeyNum == keylen {
			list := make([]string, keylen)
			for i, k := range keys {
				list[i] = index.NormalizeKey(k)
			}
			index, _ := ptn.Fill(list...)
			return index, nil
		}
	}

	return "", index.ErrKeyLength
}

func (p *provider) removeConflictingIndices(ctx context.Context) error {
	for _, ptn := range p.patterns {
		keys := make([]string, ptn.alias.KeyNum, ptn.alias.KeyNum)
		for i := 0; i < ptn.alias.KeyNum; i++ {
			keys[i] = "*"
		}
		indexName, err := ptn.alias.Fill(keys...)
		if err != nil {
			// never reach
			return err
		}
		if ptn.alias.Segments[len(ptn.alias.Segments)-1].Type != index.PatternSegmentStatic {
			p.Log.Warnf("can't to remove %s, the pattern %q has no static suffix", indexName, ptn.alias.Pattern)
			continue
		}
		err = func(index string) error {
			p.Log.Debugf("try to remove %s", index)
			ctx, cancel := context.WithTimeout(ctx, p.Cfg.RequestTimeout)
			defer cancel()
			// remove this indices, avoid collisions with aliases
			_, err := p.loader.Client().DeleteIndex(index).Do(ctx)
			return err
		}(indexName)
		if err != nil {
			return fmt.Errorf("failed to delete index %s: %s", indexName, err)
		}
	}
	return nil
}
