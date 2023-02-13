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
	"fmt"
	"strconv"
	"strings"
	"sync"
	"time"

	sls "github.com/aliyun/aliyun-log-go-sdk"
	slsconsumer "github.com/aliyun/aliyun-log-go-sdk/consumer"
	"github.com/recallsong/go-utils/encoding/md5x"
	"github.com/recallsong/go-utils/errorx"

	"github.com/erda-project/erda-infra/base/logs"
)

// 层级关系: account -> N * LogProject -> N * LogStore

// 管理 importers 的创建、启动、删除
func (p *provider) loadAll() error {
	accounts, err := p.getAccountInfo()
	if err != nil {
		p.L.Errorf("fail to load account list: %s", err)
		return nil
	}
	p.lock.Lock()
	defer p.lock.Unlock()
	for id, account := range accounts {
		imp, ok := p.importers[id]
		if ok {
			continue
		}
		imp = newImporter(account, p.L, p.filters, &p.outputs)
		err := imp.Start(p.C.ProjectsReloadInterval)
		if err != nil {
			p.L.Error(err)
			continue
		}
		p.importers[id] = imp
	}
	for id, imp := range p.importers {
		if _, ok := accounts[id]; !ok {
			err := imp.Close()
			if err != nil {
				p.L.Error(err)
			}
			delete(p.importers, id)
		}
	}
	return nil
}

// Importer 处理某个账号下，所有的 Project 日志的导入
type Importer struct {
	l        logs.Logger
	account  *AccountInfo
	clients  map[string]sls.ClientInterface
	projects map[string]map[string]*LogProject
	closeCh  chan struct{}
	wg       sync.WaitGroup
	filters  Filters
	outputs  *outputs
}

func newImporter(ai *AccountInfo, l logs.Logger, filters Filters, outputs *outputs) *Importer {
	clients := make(map[string]sls.ClientInterface)
	for _, endpoint := range ai.Endpoints {
		client := sls.CreateNormalInterface(endpoint, ai.AccessKey, ai.AccessSecretKey, "")
		clients[endpoint] = client
	}
	return &Importer{
		l:        l,
		account:  ai,
		clients:  clients,
		projects: make(map[string]map[string]*LogProject),
		closeCh:  make(chan struct{}),
		filters:  filters,
		outputs:  outputs,
	}
}

// Start .
func (i *Importer) Start(reloadInterval time.Duration) error {
	i.wg.Add(1)
	go func() {
		defer func() {
			i.clearProjects()
			i.wg.Done()
		}()
		i.loadAllEndpoints()
		tick := time.Tick(reloadInterval)
		for {
			select {
			case <-i.closeCh:
				return
			case <-tick:
				i.loadAllEndpoints()
			}
		}
	}()
	return nil
}

func (i *Importer) loadAllEndpoints() {
	for endpoint, client := range i.clients {
		i.loadProjects(endpoint, client)
	}
}

// loadProjects load all the projects below this region.
func (i *Importer) loadProjects(endpoint string, client sls.ClientInterface) {
	list, err := i.listProjects(client)
	if err != nil {
		i.l.Errorf("fail to list sls log projects: %s", err)
		return
	}
	projects := i.projects[endpoint]
	if projects == nil {
		projects = make(map[string]*LogProject)
		i.projects[endpoint] = projects
	}
	for name := range list {
		if !i.filters.Match(name) {
			continue
		}
		lp, ok := projects[name]
		if !ok {
			lp = newLogProject(i.account, endpoint, name, i.l, i.outputs)
			projects[name] = lp
		}
		lp.loadLogStores(client)
	}
	for name, lp := range projects {
		if _, ok := list[name]; !ok {
			err := lp.Close()
			if err != nil {
				i.l.Errorf("fail to close project %s: %s", name, err)
			}
			delete(projects, name)
		}
	}
}

func (i *Importer) listProjects(client sls.ClientInterface) (map[string]*sls.LogProject, error) {
	result := make(map[string]*sls.LogProject)
	offset := 0
	for {
		projects, count, total, err := client.ListProjectV2(offset, 100)
		if err != nil {
			return nil, err
		}
		for _, item := range projects {
			project := item // copy
			result[project.Name] = &project
		}
		if offset+count >= total {
			break
		}
		offset += count
	}
	return result, nil
}

func (i *Importer) clearProjects() {
	for endpoint, projects := range i.projects {
		for project, lp := range projects {
			err := lp.Close()
			if err != nil {
				i.l.Errorf("fail to close project %s / %s: %s", endpoint, project, err)
			}
		}
	}
	i.projects = make(map[string]map[string]*LogProject)
}

// Close .
func (i *Importer) Close() error {
	close(i.closeCh)
	i.wg.Wait()
	return nil
}

// LogProject 处理某个 Project 下面的 LogStore Consumer
type LogProject struct {
	l         logs.Logger
	account   *AccountInfo
	endpoint  string
	project   string
	consumers map[string]*Consumer // consumer for log store
	outputs   *outputs
}

func newLogProject(account *AccountInfo, endpoint, project string, l logs.Logger, outputs *outputs) *LogProject {
	return &LogProject{
		l:         l,
		account:   account,
		endpoint:  endpoint,
		project:   project,
		consumers: make(map[string]*Consumer),
		outputs:   outputs,
	}
}

func (p *LogProject) loadLogStores(client sls.ClientInterface) {
	logStores, err := client.ListLogStore(p.project)
	if err != nil {
		p.l.Errorf("fail to load log stores from project %s: %s", p.project, err)
		return
	}
	logStoresMap := make(map[string]bool)
	for _, logStore := range logStores {
		logStoresMap[logStore] = true
	}

	for logStore := range logStoresMap {
		c, ok := p.consumers[logStore]
		if !ok {
			c = newConsumer(p.account, p.endpoint, p.project, logStore, p.l, p.outputs)
			c.Start()
			if err != nil {
				p.l.Error(err)
				continue
			}
			p.consumers[logStore] = c
		}
	}
	for logStore, c := range p.consumers {
		if !logStoresMap[logStore] {
			err := c.Close()
			if err != nil {
				p.l.Errorf("fail to close log store %s consumer: %s", logStore, err)
			}
			delete(p.consumers, logStore)
		}
	}
}

// Close .
func (p *LogProject) Close() error {
	var errs errorx.Errors
	for _, c := range p.consumers {
		err := c.Close()
		if err != nil {
			errs = append(errs, err)
		}
	}
	return errs.MaybeUnwrap()
}

// Consumer 针对一个账号下，某个 Projects、LogStore 的消费者
type Consumer struct {
	l        logs.Logger
	ai       *AccountInfo
	endpoint string
	project  string
	logStore string
	worker   *slsconsumer.ConsumerWorker
	id       string
	outputs  *outputs
}

func newConsumer(ai *AccountInfo, endpoint, project, logStore string, l logs.Logger, outputs *outputs) *Consumer {
	c := &Consumer{
		l:        l,
		ai:       ai,
		endpoint: endpoint,
		project:  project,
		logStore: logStore,
		outputs:  outputs,
	}
	c.init(ai, endpoint, project, logStore)
	return c
}

func (c *Consumer) init(ai *AccountInfo, endpoint, project, logStore string) {
	c.id = project + "/" + logStore
	group := fmt.Sprintf("sls-%s-%s-%s-%s-dev-0", ai.OrgID, ai.AccessKey, project, logStore)
	group = md5x.SumString(group).String()
	handler := c.getLogHandler()
	if handler == nil {
		return
	}
	c.l.Infof("create log consumer: ak: %s, project  %s / %s / %s, group: %s", ai.AccessKey, endpoint, project, logStore, group)
	c.worker = slsconsumer.InitConsumerWorker(slsconsumer.LogHubConfig{
		Endpoint:          endpoint,
		AccessKeyID:       ai.AccessKey,
		AccessKeySecret:   ai.AccessSecretKey,
		Project:           project,
		Logstore:          logStore,
		ConsumerGroupName: group,
		ConsumerName:      fmt.Sprintf("sls-%s-%s-%s", ai.AccessKey, project, logStore),
		// This options is used for initialization, will be ignored once consumer group is created and each shard has been started to be consumed.
		// Could be "begin", "end", "specific time format in time stamp", it's log receiving time.
		CursorPosition: slsconsumer.END_CURSOR,
	}, handler)
}

// Start .
func (c *Consumer) Start() error {
	if c.worker == nil {
		return nil
	}
	c.worker.Start()
	return nil
}

// Close .
func (c *Consumer) Close() error {
	if c.worker == nil {
		return nil
	}
	c.l.Infof("close log consumer: ak: %s, project  %s / %s / %s", c.ai.AccessKey, c.endpoint, c.project, c.logStore)
	c.worker.StopAndWait()
	return nil
}

// 云产品的一些日志规则
// 某些有专属的logProject&logStore，例如WAF, OSS
// 某些则为自定义，但是不能删除已自定义的logProject&logStore
// 某些自定义，可任意更改、不做限制
func (c *Consumer) getLogHandler() func(shardID int, groups *sls.LogGroupList) string {
	switch c.logStore {
	case "waf-logstore":
		return c.wafProcess
	case "api-gateway":
		return c.apiGatewayProcess
	default:
		return c.commonProcess
	}
	return nil
}

func (c *Consumer) commonProcess(shardID int, groups *sls.LogGroupList) string {
	for _, group := range groups.LogGroups {
		switch group.GetTopic() {
		case "rds_audit_log":
			c.rdsProcess(shardID, &sls.LogGroupList{LogGroups: []*sls.LogGroup{group}})
		default:
			c.l.Warnf("topic %s don't support", group.GetTopic())
		}
	}
	return ""
}

func (c *Consumer) newTags(group *sls.LogGroup) map[string]string {
	tags := make(map[string]string)
	tags["org_name"] = c.ai.OrgName
	tags["dice_org_id"] = c.ai.OrgID
	tags["dice_org_name"] = c.ai.OrgName
	tags["origin"] = "sls"
	tags["sls_project"] = c.project
	tags["sls_log_store"] = c.logStore
	tags["sls_topic"] = group.GetTopic()
	tags["sls_source"] = group.GetTopic()
	tags["sls_category"] = group.GetCategory()
	for _, t := range group.LogTags {
		key := t.GetKey()
		val := t.GetValue()
		tags[key] = val
	}
	return tags
}

// Contents .
type Contents []*sls.LogContent

func (cs Contents) Len() int {
	return len(cs)
}

func (cs Contents) Less(i, j int) bool {
	return cs[i].GetKey() < cs[j].GetKey()
}

func (cs Contents) Swap(i, j int) {
	cs[i], cs[j] = cs[j], cs[i]
}

func (c *Consumer) getTagsByContents(group *sls.LogGroup, contents []*sls.LogContent, requestIDKey string) map[string]string {
	tags := c.newTags(group)
	for _, content := range contents {
		if strings.HasPrefix(content.GetKey(), "_") {
			continue
		}
		if len(requestIDKey) > 0 && content.GetKey() == requestIDKey {
			tags["request-id"] = content.GetValue()
		}
		tags[content.GetKey()] = content.GetValue()
	}
	return tags
}

func (c *Consumer) newMetricTags(group *sls.LogGroup, product string) map[string]string {
	tags := c.newTags(group)
	tags["_meta"] = "true"
	tags["_metric_scope"] = "org"
	tags["_metric_scope_id"] = c.ai.OrgName
	tags["product"] = product
	return tags
}

func parseHTTPStatus(status string) (int64, string, error) {
	val, err := strconv.ParseInt(status, 10, 64)
	if err != nil {
		return 0, "", err
	}
	return val, strconv.FormatInt(val/100, 10) + "XX", nil
}
