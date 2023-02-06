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

package cleaner

import (
	"context"
	"sort"
	"time"

	"github.com/olivere/elastic"
	"github.com/robfig/cron/v3"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

func (p *provider) runDiskCheckAndClean(ctx context.Context) {
	p.loader.WaitAndGetIndices(ctx)
	p.Log.Infof("run disk clean with interval(%v)", p.Cfg.DiskClean.CheckInterval)
	defer p.Log.Infof("exit disk clean")
	timer := time.NewTimer(20 * time.Second)
	for {
		select {
		case <-timer.C:
		case <-ctx.Done():
			return
		}

		err := p.checkDiskUsage(ctx, p.Cfg.DiskClean)
		if err != nil {
			p.Log.Errorf("failed to check disk: %s", err)
		}
		timer.Reset(p.Cfg.DiskClean.CheckInterval)
	}
}

func (p *provider) runDocsCheckAndClean(ctx context.Context) {
	if p.Cfg.DiskClean.TTL.Enable {
		c := cron.New(cron.WithSeconds())
		_, err := c.AddFunc(p.Cfg.DiskClean.TTL.TriggerSpecCron, p.deleteByQuery)
		if err != nil {
			panic("create cron failed.")
		}
		c.Start()
		p.Log.Infof("Enable disk clean cron task. Cron expression: %s, Max store time: %v", p.Cfg.DiskClean.TTL.TriggerSpecCron, p.Cfg.DiskClean.TTL.MaxStoreTime)
	}
}

func (p *provider) runTaskCheck(ctx context.Context) {
	p.Log.Infof("run ttl task check")
	defer p.Log.Infof("exit ttl task check")
	for {
		select {
		case <-ctx.Done():
			return
		case task := <-p.ttlTaskCh:
			tasksGetTask := p.loader.Client().TasksGetTask()
			taskStatus, err := tasksGetTask.TaskId(task.TaskId).Do(ctx)
			if err != nil {
				continue
			}
			if taskStatus != nil && !taskStatus.Completed {
				go func(task *TtlTask) {
					timer := time.NewTimer(time.Minute * time.Duration(p.Cfg.DiskClean.TTL.TaskCheckInterval))
					select {
					case <-timer.C:
						p.AddTask(task)
						p.Log.Infof("Task uncompleted. delay retry. taskId: %s, indices: %s", task.TaskId, task.Indices)
					}
				}(task)
				continue
			}
			p.Log.Infof("Task completed. taskId: %s, task status: %v", taskStatus.Task.Id, taskStatus.Task.Status)
			p.forceMerge(ctx, task.Indices...)
		}
	}
}

func (p *provider) AddTask(task *TtlTask) {
	if task != nil && task.TaskId != "" && len(task.Indices) > 0 {
		p.ttlTaskCh <- &TtlTask{
			TaskId:  task.TaskId,
			Indices: task.Indices,
		}
	}
}

func (p *provider) getNodeStats() (map[string]*elastic.NodesStatsNode, error) {
	ctx, cancel := context.WithTimeout(context.Background(), p.Cfg.RequestTimeout)
	defer cancel()
	resp, err := p.loader.Client().NodesStats().Metric("indices", "fs").Do(ctx)
	if err != nil {
		return nil, err
	}
	return resp.Nodes, nil
}

func (p *provider) getClusterState() (*elastic.ClusterStateResponse, error) {
	ctx, cancel := context.WithTimeout(context.Background(), p.Cfg.RequestTimeout)
	defer cancel()
	resp, err := p.loader.Client().ClusterState().Metric("routing_nodes").Do(ctx)
	if err != nil {
		return nil, err
	}
	return resp, nil
}

// NodeDiskUsage .
type NodeDiskUsage struct {
	ID           string
	Total        int64
	Used         int64
	Store        int64
	UsedPercent  float64
	StorePercent float64
	ExpectDelete int64
	Deleted      int64
}

func (p *provider) getNodeDiskUsage(filter func(*NodeDiskUsage) bool) (map[string]*NodeDiskUsage, error) {
	nodes, err := p.getNodeStats()
	if err != nil {
		return nil, err
	}
	diskUsage := make(map[string]*NodeDiskUsage)
	for id, node := range nodes {
		usage := &NodeDiskUsage{
			ID:           id,
			Total:        node.FS.Total.TotalInBytes,
			Used:         node.FS.Total.TotalInBytes - node.FS.Total.AvailableInBytes,
			Store:        node.Indices.Store.SizeInBytes,
			UsedPercent:  float64(node.FS.Total.TotalInBytes-node.FS.Total.AvailableInBytes) / float64(node.FS.Total.TotalInBytes) * 100,
			StorePercent: float64(node.Indices.Store.SizeInBytes) / float64(node.FS.Total.TotalInBytes) * 100,
		}
		if filter == nil || filter(usage) {
			diskUsage[id] = usage
		}
	}
	return diskUsage, nil
}

func (p *provider) getNodeIndices(filter func(*NodeDiskUsage) bool) (map[string]*NodeDiskUsage, map[string]map[string]*NodeDiskUsage, error) {
	nodes, err := p.getNodeDiskUsage(filter)
	if err != nil {
		return nil, nil, err
	}
	if len(nodes) <= 0 {
		return nil, nil, nil
	}
	state, err := p.getClusterState()
	if err != nil {
		return nodes, nil, err
	}
	routing := make(map[string]map[string]*NodeDiskUsage)
	for id, node := range nodes {
		indices := routing[id]
		if indices == nil {
			indices = make(map[string]*NodeDiskUsage)
			routing[id] = indices
		}
		shards := state.RoutingNodes.Nodes[id]
		for _, shard := range shards {
			if _, ok := indices[shard.Index]; !ok {
				indices[shard.Index] = node
			}
		}
	}
	return nodes, routing, nil
}

func (p *provider) checkDiskUsage(ctx context.Context, config diskClean) error {
	nodes, routing, err := p.getNodeIndices(func(n *NodeDiskUsage) bool {
		return n.UsedPercent >= config.HighDiskUsagePercent &&
			n.StorePercent >= config.MinIndicesStorePercent &&
			n.Store >= p.minIndicesStoreInDisk
	})
	if err != nil {
		return err
	}
	for {
		if len(nodes) <= 0 || len(routing) <= 0 {
			return nil
		}

		// estimate the number of deletes
		for _, n := range nodes {
			targetDiskUsage := int64(float64(config.LowDiskUsagePercent) / 100 * float64(n.Total))
			delBytes := n.Used - targetDiskUsage
			minStore := int64(float64(n.Total) * config.MinIndicesStorePercent / 100)
			if p.minIndicesStoreInDisk > minStore {
				minStore = p.minIndicesStoreInDisk
			}
			// clean up the index under the premise of ensuring minimum index storage
			if delBytes > minStore {
				delBytes = delBytes - minStore
			}
			n.ExpectDelete = delBytes
		}

		// find indices to clean
		sortedIndices := p.getSortedIndices()
		var removeList []string
		for _, entry := range sortedIndices {
			for _, indices := range routing {
				if node, ok := indices[entry.Index]; ok {
					if node.Deleted > node.ExpectDelete {
						continue
					}
					node.Deleted += entry.StoreBytes
					removeList = append(removeList, entry.Index)
				}
			}
		}
		if len(removeList) <= 0 && len(routing) > 0 && len(nodes) > 0 {
			break
		}

		// delete indices
		req := &clearRequest{
			list:   removeList,
			waitCh: make(chan struct{}),
		}
		select {
		case <-ctx.Done():
			return nil
		case p.clearCh <- req:
		}
		// wait for index deletion to complete
		select {
		case <-ctx.Done():
			return nil
		case <-req.waitCh:
		}

		p.loader.ReloadIndices()

		// continue to check
		nodes, routing, err = p.getNodeIndices(func(n *NodeDiskUsage) bool {
			return nodes[n.ID] != nil &&
				n.UsedPercent >= config.LowDiskUsagePercent &&
				n.StorePercent >= config.MinIndicesStorePercent &&
				n.Store >= p.minIndicesStoreInDisk
		})
		if err != nil {
			return err
		}
	}
	if len(p.rolloverBodyForDiskClean) > 0 {
		sortedIndices := p.getSortedRolloverIndices()
		var removeList []string
		for _, entry := range sortedIndices {
			for _, indices := range routing {
				if node, ok := indices[entry.Index]; ok {
					if node.Deleted > node.ExpectDelete {
						continue
					}
					alias := p.indexAlias(entry)
					if len(alias) > 0 {
						ok, _ := p.rolloverAlias(alias, p.rolloverBodyForDiskClean)
						if ok {
							node.Deleted += entry.StoreBytes
							removeList = append(removeList, entry.Index)
						}
					}
				}
			}
		}
		if len(removeList) > 0 {
			// delete indices
			req := &clearRequest{
				list:   removeList,
				waitCh: make(chan struct{}),
			}
			select {
			case <-ctx.Done():
				return nil
			case p.clearCh <- req:
			}
			// wait for index deletion to complete
			select {
			case <-ctx.Done():
				return nil
			case <-req.waitCh:
			}

			p.loader.ReloadIndices()
		} else {
			p.Log.Warnf("high disk usage, but not find indices to delete")
		}
	}
	return nil
}

func (p *provider) getSortedIndices() (list []*loader.IndexEntry) {
	indices := p.loader.AllIndices()
	if indices == nil {
		return nil
	}
	var getIndices func(indices *loader.IndexGroup)
	getIndices = func(indices *loader.IndexGroup) {
		for _, entry := range indices.List {
			if entry.Active {
				continue
			}
			list = append(list, entry)
		}
		for _, ig := range indices.Groups {
			getIndices(ig)
		}
	}
	getIndices(indices)

	// ascending by maximum time and size
	sort.Slice(list, func(i, j int) bool {
		a, b := list[i], list[j]
		at, bt := a.MaxT.Truncate(time.Hour), b.MaxT.Truncate(time.Hour)
		if at.Equal(bt) {
			return a.StoreSize < b.StoreSize
		}
		return at.Before(bt)
	})
	return list
}

func (p *provider) getSortedRolloverIndices() (list []*loader.IndexEntry) {
	indices := p.loader.AllIndices()
	if indices == nil {
		return nil
	}
	var getIndices func(indices *loader.IndexGroup)
	getIndices = func(indices *loader.IndexGroup) {
		if len(indices.List) == 1 && indices.List[0].Num >= 0 {
			list = append(list, indices.List[0])
		}
		for _, ig := range indices.Groups {
			getIndices(ig)
		}
	}
	getIndices(indices)

	sort.Slice(list, func(i, j int) bool {
		a, b := list[i], list[j]
		return a.StoreSize < b.StoreSize
	})
	return list
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
	}
	return resp.Acknowledged, nil
}

func (p *provider) indexAlias(entry *loader.IndexEntry) string {
	for _, ptn := range p.rolloverAliasPatterns {
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
