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

package retention

import (
	"context"
	"encoding/json"
	"strings"
	"time"

	"github.com/recallsong/go-utils/reflectx"

	"github.com/erda-project/erda/pkg/router"
)

func (p *provider) getConfig(ctx context.Context) *retentionConfig {
	v, _ := p.value.Load().(*retentionConfig)
	for v == nil {
		select {
		case <-ctx.Done():
			return nil
		case <-time.After(1 * time.Second):
		}
		v, _ = p.value.Load().(*retentionConfig)
	}
	return v
}

func (p *provider) GetTTL(key string) time.Duration {
	cfg := p.getConfig(context.Background())
	d, ok := cfg.keysTTL[key]
	if ok {
		return d
	}
	return p.Cfg.DefaultTTL
}

func (p *provider) DefaultTTL() time.Duration {
	return p.Cfg.DefaultTTL
}

func (p *provider) GetConfigKey(name string, tags map[string]string) string {
	cfg := p.getConfig(context.Background())
	matched := cfg.matcher.Find(name, tags)
	if matched != nil {
		c := matched.(*configItem)
		return c.Key
	}
	return ""
}

func (p *provider) GetTTLByTags(name string, tags map[string]string) time.Duration {
	cfg := p.getConfig(context.Background())
	matched := cfg.matcher.Find(name, tags)
	if matched != nil {
		c := matched.(*configItem)
		return c.Duration
	}
	return p.Cfg.DefaultTTL
}

type (
	configItem struct {
		Duration time.Duration
		Key      string
	}
	retentionConfig struct {
		matcher *router.Router
		keysTTL map[string]time.Duration
	}
)

func (p *provider) loadConfig() error {
	type MonitorConfig struct {
		Names   string `gorm:"column:names"`
		Filters string `gorm:"column:filters"`
		Config  string `gorm:"column:config"`
		Key     string `gorm:"column:key"`
	}
	type configData struct {
		TTL string `json:"ttl"`
	}
	var list []*MonitorConfig
	if err := p.DB.Table("sp_monitor_config").Where("`type`=? AND `enable`=1", p.typ).Find(&list).Error; err != nil {
		p.Log.Errorf("failed to load sp_monitor_config: %s", err)
		return err
	}

	rc := &retentionConfig{
		matcher: router.New(),
		keysTTL: make(map[string]time.Duration),
	}
	for _, item := range list {
		if len(item.Key) <= 0 || len(item.Config) <= 0 {
			continue
		}

		cfg := &configData{}
		err := json.Unmarshal(reflectx.StringToBytes(item.Config), cfg)
		if err != nil || len(cfg.TTL) <= 0 {
			p.Log.Errorf("invalid %s retention config, key=%s", p.typ, item.Key)
			continue
		}
		d, err := time.ParseDuration(cfg.TTL)
		if err != nil || int64(d) < int64(time.Second) {
			p.Log.Errorf("invalid %s retention ttl, key=%s: %s", p.typ, item.Key, err)
			continue
		}
		if int64(d) <= int64(time.Minute) {
			p.Log.Errorf("too small %s retention ttl %s, key=%s", p.typ, cfg.TTL, item.Key)
			continue
		}
		if int64(rc.keysTTL[item.Key]) < int64(d) {
			rc.keysTTL[item.Key] = d
		}

		var filter []*router.KeyValue
		err = json.Unmarshal(reflectx.StringToBytes(item.Filters), &filter)
		if err != nil {
			p.Log.Errorf("invalid %s retention filters, key=%s", p.typ, item.Key)
			continue
		}
		for _, name := range strings.Split(item.Names, ",") {
			rc.matcher.Add(name, filter, &configItem{
				Duration: d,
				Key:      item.Key,
			})
		}
	}
	p.value.Store(rc)
	p.Log.Infof("load %s retention config ok", p.typ)

	if p.Cfg.PrintDetails {
		rc.matcher.PrintTree(false)
	}
	return nil
}
