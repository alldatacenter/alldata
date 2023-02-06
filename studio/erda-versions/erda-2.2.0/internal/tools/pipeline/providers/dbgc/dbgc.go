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

package dbgc

import (
	"context"
	"encoding/json"
	"fmt"
	"math/rand"
	"strconv"
	"strings"
	"time"

	v3 "github.com/coreos/etcd/clientv3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/dbgc/db"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/rutil"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	etcdDBGCWatchPrefix    = "/devops/pipeline/dbgc/pipeline/"
	etcdDBGCDLockKeyPrefix = "/devops/pipeline/dbgc/dlock/"
)

// PipelineDatabaseGC remove ListenDatabaseGC and EnsureDatabaseGC these two methods，
// these two methods will create a lot of etcd ttl, will cause high load on etcd
// use fixed gc time, traverse the data in the database every day
func (p *provider) PipelineDatabaseGC(ctx context.Context) {
	rutil.ContinueWorking(ctx, p.Log, func(ctx context.Context) rutil.WaitDuration {
		// analyzed snippet and non-snippet pipeline database gc
		p.doAnalyzedPipelineDatabaseGC(true)
		p.doAnalyzedPipelineDatabaseGC(false)

		// not analyzed snippet and non-snippet pipeline database gc
		p.doNotAnalyzedPipelineDatabaseGC(true)
		p.doNotAnalyzedPipelineDatabaseGC(false)

		// pipeline archive database clean up
		p.doAnalyzedPipelineArchiveGC()
		p.doNotAnalyzedPipelineArchiveGC()

		return rutil.ContinueWorkingWithDefaultInterval
	}, rutil.WithContinueWorkingDefaultRetryInterval(p.Cfg.PipelineDBGCDuration))
}

// doPipelineDatabaseGC query the data in the database according to req paging to perform gc
func (p *provider) doPipelineDatabaseGC(req apistructs.PipelinePageListRequest) {
	var pageNum = req.PageNum
	pipelines := make([]spec.Pipeline, 0)
	for {
		req.PageNum = pageNum
		result, err := p.dbClient.PageListPipelines(req)
		pageNum += 1
		if err != nil {
			p.Log.Errorf("failed to compensate pipeline req: %v, err: %v", req, err)
			continue
		}
		pipelineResults := result.Pipelines
		if len(pipelineResults) <= 0 {
			break
		}
		pipelines = append(pipelines, pipelineResults...)
	}
	for _, pipeline := range pipelines {
		if !pipeline.Status.CanDelete() {
			continue
		}
		// gc logic
		if err := p.DoDBGC(pipeline.PipelineID, apistructs.PipelineGCDBOption{NeedArchive: needArchive(pipeline)}); err != nil {
			p.Log.Errorf("failed to do gc logic, pipelineID: %d, err: %v", pipeline.PipelineID, err)
			continue
		}
	}
}

func needArchive(p spec.Pipeline) bool {
	if p.Status == apistructs.PipelineStatusAnalyzed {
		if p.Extra.GC.DatabaseGC.Analyzed.NeedArchive != nil {
			return *p.Extra.GC.DatabaseGC.Analyzed.NeedArchive
		}
	} else {
		if p.Extra.GC.DatabaseGC.Finished.NeedArchive != nil {
			return *p.Extra.GC.DatabaseGC.Finished.NeedArchive
		}
	}
	return false
}

// doAnalyzedPipelineDatabaseGC gc Analyzed status pipeline
func (p *provider) doAnalyzedPipelineDatabaseGC(isSnippetPipeline bool) {
	var req apistructs.PipelinePageListRequest
	req.Statuses = []string{apistructs.PipelineStatusAnalyzed.String()}
	req.IncludeSnippet = isSnippetPipeline
	req.DescCols = []string{"id"}
	req.EndTimeCreated = time.Now().Add(-time.Second * time.Duration(conf.AnalyzedPipelineDefaultDatabaseGCTTLSec()))
	req.PageSize = 100
	req.LargePageSize = true
	req.PageNum = 1
	req.AllSources = true

	p.doPipelineDatabaseGC(req)
}

// doNotAnalyzedPipelineDatabaseGC gc other status pipeline
func (p *provider) doNotAnalyzedPipelineDatabaseGC(isSnippetPipeline bool) {
	var req apistructs.PipelinePageListRequest
	req.NotStatuses = []string{apistructs.PipelineStatusAnalyzed.String()}
	req.IncludeSnippet = isSnippetPipeline
	req.DescCols = []string{"id"}
	req.EndTimeCreated = time.Now().Add(-time.Second * time.Duration(conf.FinishedPipelineDefaultDatabaseGCTTLSec()))
	req.PageSize = 100
	req.LargePageSize = true
	req.PageNum = 1
	req.AllSources = true

	p.doPipelineDatabaseGC(req)
}

func (p *provider) doAnalyzedPipelineArchiveGC() {
	var req db.ArchiveDeleteRequest
	req.Statuses = []string{apistructs.PipelineStatusAnalyzed.String()}
	req.EndTimeCreated = time.Now().Add(-p.Cfg.AnalyzedPipelineArchiveDefaultRetainHour)
	if err := p.dbClient.DeletePipelineArchives(req); err != nil {
		p.Log.Errorf("failed to delete analyzed pipeline archive, err: %v", err)
	}
}

func (p *provider) doNotAnalyzedPipelineArchiveGC() {
	var req db.ArchiveDeleteRequest
	req.NotStatuses = []string{apistructs.PipelineStatusAnalyzed.String()}
	fmt.Println(p.Cfg.FinishedPipelineArchiveDefaultRetainHour)
	req.EndTimeCreated = time.Now().Add(-p.Cfg.FinishedPipelineArchiveDefaultRetainHour)
	if err := p.dbClient.DeletePipelineArchives(req); err != nil {
		p.Log.Errorf("failed to delete finished pipeline archive, err: %v", err)
	}
}

// ListenDatabaseGC 监听需要 GC 的 pipeline database record.
func (p *provider) ListenDatabaseGC() {
	p.Log.Info("start watching gc pipelines")
	for {
		ctx := context.Background()

		err := p.js.IncludeWatch().Watch(ctx, etcdDBGCWatchPrefix, true, false, false, apistructs.PipelineGCInfo{},
			func(key string, value interface{}, t storetypes.ChangeType) error {

				// async handle, non-blocking, so we can watch subsequent incoming pipelines
				go func() {

					p.Log.Infof("gc watched a key change, key: %s, changeType: %s", key, t.String())
					// only listen del op
					if t != storetypes.Del {
						return
					}

					pipelineID, parseErr := getPipelineIDFromDBGCWatchedKey(key)
					if parseErr != nil {
						p.Log.Errorf("failed to get pipelineID from key, key: %s, err: %v", key, parseErr)
						return
					}

					// acquire a dlock
					gcDBLockKey := makeDBGCDLockKey(pipelineID)
					lock, err := p.etcd.GetClient().Txn(context.Background()).
						If(v3.Compare(v3.Version(gcDBLockKey), "=", 0)).
						Then(v3.OpPut(gcDBLockKey, "")).
						Commit()
					defer func() {
						_, _ = p.etcd.GetClient().Txn(context.Background()).Then(v3.OpDelete(gcDBLockKey)).Commit()
					}()
					if err != nil {
						return
					}
					if lock != nil && !lock.Succeeded {
						return
					}

					gcOption, err := getGCOptionFromValue(value.(*apistructs.PipelineGCInfo).Data)
					if err != nil {
						p.Log.Errorf("failed to get gc option from value, pipelineID: %d, err: %v", pipelineID, err)
						return
					}

					// gc logic
					if err := p.DoDBGC(pipelineID, gcOption); err != nil {
						p.Log.Errorf("failed to do gc logic, pipelineID: %d, err: %v", pipelineID, err)
						return
					}
				}()
				return nil
			},
		)
		if err != nil {
			p.Log.Errorf("failed to gc watch, err: %v", err)
		}
	}
}

func (p *provider) WaitDBGC(pipelineID uint64, ttl uint64, needArchive bool) {
	var err error
	defer func() {
		if err != nil {
			p.Log.Errorf("failed to gc wait, pipelineID: %d, err: %v",
				pipelineID, err)
		} else {
			p.Log.Debugf("gc pipeline: %d in the future (%s) (TTL: %ds)",
				pipelineID, time.Now().Add(time.Duration(int64(time.Second)*int64(ttl))).Format(time.RFC3339), ttl)
		}
	}()

	// 设置 gc 等待时间
	lease := v3.NewLease(p.etcd.GetClient())
	grant, err := lease.Grant(context.Background(), int64(ttl))
	if err != nil {
		return
	}
	leaseID := strconv.FormatInt(int64(grant.ID), 16)
	p.Log.Debugf("grant lease, pipelineID: %d, leaseID: %s", pipelineID, leaseID)

	// 插入或更新 key
	gcOptionByte, err := generateGCOption(needArchive)
	if err != nil {
		return
	}
	gcInfo := apistructs.MakePipelineGCInfo(ttl, leaseID, gcOptionByte)
	_, err = p.js.PutWithOption(context.Background(),
		makeDBGCKey(pipelineID), gcInfo,
		[]interface{}{v3.WithLease(grant.ID)})
	if err != nil {
		return
	}
}

func (p *provider) DoDBGC(pipelineID uint64, gcOption apistructs.PipelineGCDBOption) error {
	pipeline, exist, err := p.dbClient.GetPipelineWithExistInfo(pipelineID)
	if err != nil {
		return err
	}
	if !exist {
		p.Log.Infof("no need to gc db, pipeline already not exist in db, pipelineID: %d", pipelineID)
		return nil
	}

	// If the pipeline is referenced by the definition and the record is GC
	// the definition cannot see the execution record.
	// There are two ways:
	// 		one is to update the definition information contact binding
	//	 	and the other is not to go to the GC pipeline record
	// It is decided not to record this pipeline
	if strings.TrimSpace(pipeline.PipelineDefinitionID) != "" {
		definition, _, err := p.dbClient.GetPipelineDefinitionByPipelineID(pipeline.PipelineID)
		if err != nil {
			p.Log.Errorf("failed to GetPipelineDefinitionByPipelineID, id: %d, err: %v", pipeline.ID, err)
		}
		if definition != nil {
			p.Log.Infof("referenced by definition can not gc pipeline id: %v", pipeline.PipelineID)
			return nil
		}
	}

	if gcOption.NeedArchive {
		// 归档
		_, err = p.dbClient.ArchivePipeline(pipeline.ID)
		if err != nil {
			p.Log.Errorf("failed to archive pipeline, id: %d, err: %v", pipeline.ID, err)
		}
		p.Log.Debugf("archive pipeline success, id: %d", pipeline.ID)
	} else {
		// 删除
		if err = p.dbClient.DeletePipelineRelated(pipeline.ID); err != nil {
			p.Log.Errorf("failed to delete pipeline, id: %d, err: %v", pipeline.ID, err)
		}
		p.Log.Debugf("delete pipeline success, id: %d", pipeline.ID)
	}
	return nil
}

// ex: /devops/pipeline/dbgc/pipeline/10000001
func makeDBGCKey(pipelineID uint64) string {
	return fmt.Sprintf("%s%d", etcdDBGCWatchPrefix, pipelineID)
}

// ex: /devops/pipeline/dbgc/dlock/10000001
func makeDBGCDLockKey(pipelineID uint64) string {
	return fmt.Sprintf("%s%d", etcdDBGCDLockKeyPrefix, pipelineID)
}

func generateGCOption(needArchive bool) ([]byte, error) {
	return json.Marshal(&apistructs.PipelineGCDBOption{NeedArchive: needArchive})
}

func getGCOptionFromValue(data []byte) (op apistructs.PipelineGCDBOption, err error) {
	err = json.Unmarshal(data, &op)
	return
}

// makeDBGCEnsureKey 生成用于 dbgc ensure 的分布式锁 key
func makeDBGCEnsureKey() string {
	return makeDBGCDLockKey(0)
}

// EnsureDatabaseGC etcd lease ttl reset 存在问题，因此要定期巡检，主动 delete 那些已经到了 gcAt 时间仍然存在的 etcd key 来触发 dbgc
// github issue: https://github.com/etcd-io/etcd/issues/9395
func (p *provider) EnsureDatabaseGC() {
	p.Log.Info("start ensure dbgc pipelines")

	// 防止多实例启动时同时申请布式锁，先等待随机时间
	rand.Seed(time.Now().UnixNano())
	randN := rand.Intn(60)
	p.Log.Debugf("random sleep %d seconds...", randN)
	time.Sleep(time.Duration(randN) * time.Second)

	for {
		ctx := context.Background()

		done := make(chan struct{})
		errDone := make(chan error)

		go func() {

			// 先获取分布式锁
			ensureLockKey := makeDBGCEnsureKey()
			lock, err := p.etcd.GetClient().Txn(context.Background()).
				If(v3.Compare(v3.Version(ensureLockKey), "=", 0)).
				Then(v3.OpPut(ensureLockKey, "")).
				Commit()
			defer func() {
				_, _ = p.etcd.GetClient().Txn(context.Background()).Then(v3.OpDelete(ensureLockKey)).Commit()
			}()
			if err != nil {
				errDone <- fmt.Errorf("failed to get dlock: %s, err: %v", ensureLockKey, err)
				return
			}
			if lock != nil && !lock.Succeeded {
				done <- struct{}{}
				return
			}

			// 获取所有 key 列表
			keys, err := p.js.ListKeys(ctx, etcdDBGCWatchPrefix)
			if err != nil {
				errDone <- fmt.Errorf("failed to list dbgc keys, err: %v", err)
				return
			}

			// 没有 etcd dbgc 的老数据，生成 etcd dbgc key 后 delete key 走相同的逻辑（归档或删除），而不是直接删除
			if len(keys) > 0 {
				// 检查点，在检查点之前的 pipeline id，没有 etcd dbgc key
				checkPointKey := keys[0]
				p.handleOldNonDBGCPipelines(checkPointKey)
			}

			for _, key := range keys {
				var gcInfo apistructs.PipelineGCInfo
				if err := p.js.Get(ctx, key, &gcInfo); err != nil {
					p.Log.Errorf("failed to get dbgc key: %s, continue, err: %v", key, err)
					continue
				}
				now := time.Now().Round(0)
				// already expired
				if gcInfo.GCAt.Before(now) {
					if err := p.js.Remove(ctx, key, nil); err != nil {
						p.Log.Errorf("failed to delete already expired dbgc key: %s, continue, err: %v", key, err)
						continue
					}
					p.Log.Infof("remove already expired key: %s, gcAt: %s", key, gcInfo.GCAt.Format(time.RFC3339))
				}
			}

			done <- struct{}{}
		}()

		select {
		// 正常结束
		case <-done:
			// 完成本次 ensure 后等待 2h 开始下一次处理
			p.Log.Infof("sleep 2 hours for next ensure...")
			time.Sleep(time.Hour * 2)

		// 异常结束
		case err := <-errDone:
			p.Log.Errorf("failed to ensure, wait 5 mins for next ensure, err: %v", err)
			time.Sleep(time.Minute * 5)
		}
	}
}

func getPipelineIDFromDBGCWatchedKey(key string) (uint64, error) {
	s := strutil.TrimPrefixes(key, etcdDBGCWatchPrefix)
	id, err := strconv.ParseUint(s, 10, 64)
	if err == nil {
		return id, nil
	}
	return 0, fmt.Errorf("invalid key: %s", key)
}

func (p *provider) handleOldNonDBGCPipelines(checkPointDBGCKey string) {
	checkPointPID, err := getPipelineIDFromDBGCWatchedKey(checkPointDBGCKey)
	if err != nil {
		p.Log.Errorf("failed to get check point pid for handle old non-dbgc pipelines, err: %v", err)
		return
	}
	// fetch only id and status is enough
	var oldPipelineBases []spec.PipelineBase
	if err := p.dbClient.Cols(`id`, `status`).Where("id < ?", checkPointPID).Find(&oldPipelineBases); err != nil {
		p.Log.Errorf("failed to query pipelines before check point, err: %v", err)
		return
	}
	// transfer to pipeline for default ensureGC options
	var oldPipelines []spec.Pipeline
	for _, oldBase := range oldPipelineBases {
		oldPipelines = append(oldPipelines, spec.Pipeline{
			PipelineBase: oldBase,
			PipelineExtra: spec.PipelineExtra{
				Extra: spec.PipelineExtraInfo{
					GC: apistructs.PipelineGC{},
				},
			},
		})
	}
	for _, pipeline := range oldPipelines {
		// get default gc options
		pipeline.EnsureGC()
		// already expired, so ttl 5s is enough
		var ttl uint64 = 5
		// archive according to status
		needArchive := pipeline.Status.IsEndStatus()
		// put into etcd dbgc
		p.Log.Infof("put old non-dbgc pipeline with etcd dbgc key, id: %d, needArchive: %t", pipeline.ID, needArchive)
		p.WaitDBGC(pipeline.ID, ttl, needArchive)
	}
}

func (p *provider) GetPipelineIncludeArchived(ctx context.Context, pipelineID uint64) (spec.Pipeline, bool, bool, error) {
	return p.dbClient.GetPipelineIncludeArchived(pipelineID)
}

func (p *provider) GetPipelineTasksIncludeArchived(ctx context.Context, pipelineID uint64) ([]spec.PipelineTask, bool, error) {
	return p.dbClient.GetPipelineTasksIncludeArchived(pipelineID)
}
