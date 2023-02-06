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

package slsimport

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/olivere/elastic"
)

const timeForSplitIndex int64 = 24 * int64(time.Hour)

// getIndex .
func (c *Consumer) getIndex(project string, timestamp int64) string {
	timestamp = (timestamp - timestamp%timeForSplitIndex) / 1000000
	return c.outputs.indexPrefix + strings.Replace(project, "-", "_", -1) + "-" + strconv.FormatInt(timestamp, 10)
}

func (p *provider) startIndexManager() {
	defer p.wg.Done()
	tick := time.Tick(p.C.AccountsReloadInterval)
	for {
		err := p.cleanIndices()
		if err != nil {
			p.L.Error(err)
		}
		select {
		case <-tick:
			continue
		case <-p.closeCh:
			return
		}
	}
}

func (p *provider) cleanIndices() error {
	indices, err := p.loadIndices()
	if err != nil {
		return fmt.Errorf("fail to load indices: %s", err)
	}
	now := time.Now().Add(-p.C.Output.Elasticsearch.IndexTTL)
	var removeList []string
	for _, item := range indices {
		if now.After(item.Timestamp) {
			removeList = append(removeList, item.Index)
		}
	}
	if len(removeList) > 0 {
		const size = 10 // 一次性删太多，请求太大会被拒绝
		for len(removeList) >= size {
			err := p.deleteIndices(removeList[:size])
			if err != nil {
				return err
			}
			removeList = removeList[size:]
		}
		if len(removeList) > 0 {
			err := p.deleteIndices(removeList)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

// deleteIndices
func (p *provider) deleteIndices(indices []string) error {
	ctx, cancel := context.WithTimeout(context.Background(), p.C.Output.Elasticsearch.RequestTimeout)
	defer cancel()
	resp, err := p.es.DeleteIndex(indices...).Do(ctx)
	if err != nil {
		if e, ok := err.(*elastic.Error); ok {
			if e.Status == 404 {
				return nil
			}
		}
		return err
	}
	if !resp.Acknowledged {
		return fmt.Errorf("delete indices Acknowledged=false")
	}
	p.L.Infof("clean indices %d, %v", len(indices), indices)
	return nil
}

type indexEntry struct {
	Index     string
	Timestamp time.Time
}

func (p *provider) loadIndices() ([]*indexEntry, error) {
	ctx, cancel := context.WithTimeout(context.Background(), p.C.Output.Elasticsearch.RequestTimeout)
	defer cancel()
	resps, err := p.es.CatIndices().Index(p.C.Output.Elasticsearch.IndexPrefix + "*").Columns("index").Do(ctx)
	if err != nil {
		return nil, err
	}
	var indices []*indexEntry
	for _, item := range resps {
		parts := strings.Split(item.Index, "-")
		if len(parts) != 3 {
			p.L.Debugf("invalid index format %s", item.Index)
			continue
		}
		last := len(parts) - 1
		timestamp, err := strconv.ParseInt(parts[last], 10, 64)
		if err != nil {
			p.L.Warnf("invalid timestamp in index %s, %s", item.Index, err)
			continue
		}
		ts := time.Unix(timestamp/1000, (timestamp%1000)*int64(time.Millisecond))
		indices = append(indices, &indexEntry{
			Index:     item.Index,
			Timestamp: ts,
		})
	}
	return indices, nil
}
