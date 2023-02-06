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

package leaderworker

import (
	"context"
	"fmt"
	"strconv"
	"time"

	"github.com/coreos/etcd/clientv3"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/worker"
	"github.com/erda-project/erda/pkg/strutil"
)

func (p *provider) workerContinueReportHeartbeat(ctx context.Context, w worker.Worker) {
	ticker := time.NewTicker(p.Cfg.Worker.Heartbeat.ReportInterval)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			err := p.workerOnceReportHeartbeat(ctx, w)
			if err != nil {
				p.Log.Errorf("failed to once report heartbeat(auto retry), err: %v", err)
				continue
			}
		}
	}
}

func (p *provider) workerOnceReportHeartbeat(ctx context.Context, w worker.Worker) error {
	hctx, hcancel := context.WithTimeout(ctx, p.Cfg.Worker.Heartbeat.ReportInterval)
	defer hcancel()
	alive := w.DetectHeartbeat(hctx)
	if !alive {
		return fmt.Errorf("failed to detect worker heartbeat, workerID: %s", w.GetID())
	}
	// update lastProbeAt
	nowSec := time.Now().Round(0).Unix()
	if _, err := p.EtcdClient.Put(ctx, p.makeEtcdWorkerHeartbeatKey(w.GetID()), strutil.String(nowSec)); err != nil {
		return fmt.Errorf("failed to update last heartbeat time into etcd, workerID: %s, err: %v", w.GetID(), err)
	}
	p.Log.Debugf("worker heartbeat reported, workerID: %s", w.GetID())
	return nil
}

// alive: false means dead
// reason: dead reason
// retryableErr: non-empty means invoker need retry this error
func (p *provider) probeOneWorkerLiveness(ctx context.Context, w worker.Worker) (alive bool, reason string, retryableErr error) {
	lastHeartbeatUnixTime, err, needRetry := p.getWorkerLastHeartbeatUnixTime(ctx, w.GetID())
	if needRetry {
		return false, "", err
	}
	if err != nil {
		return false, err.Error(), nil
	}
	// check by probe interval
	hbCfg := p.Cfg.Worker.Heartbeat
	earliestInterval := hbCfg.ReportInterval * time.Duration(hbCfg.AllowedMaxContinueLostContactTimes)
	if lastHeartbeatUnixTime > 0 {
		earliestLastProbeAt := time.Now().Add(-earliestInterval)
		actualLastProbeAt := time.Unix(lastHeartbeatUnixTime, 0)
		if actualLastProbeAt.Before(earliestLastProbeAt) {
			return false, fmt.Sprintf("liveness heartbeat failed, valid-earliest: %s, actual: %s", earliestLastProbeAt, actualLastProbeAt), nil
		}
		return true, "", nil
	}
	return true, "", nil
}

func (p *provider) getWorkerLastHeartbeatUnixTime(ctx context.Context, workerID worker.ID) (lastHeartbeatUnitTime int64, err error, needRetry bool) {
	getResp, err := p.EtcdClient.Get(ctx, p.makeEtcdWorkerHeartbeatKey(workerID))
	if err != nil {
		return 0, err, true
	}
	if len(getResp.Kvs) == 0 {
		return 0, fmt.Errorf("no heartbeat info"), false
	}
	lastHeartbeatTime, err := strconv.ParseInt(string(getResp.Kvs[0].Value), 10, 64)
	if err != nil {
		return 0, err, false
	}
	return lastHeartbeatTime, nil, false
}

func (p *provider) leaderSideWorkerLivenessProber(ctx context.Context) {
	p.Log.Infof("start worker liveness prober")
	defer p.Log.Infof("end worker liveness prober")

	ticker := time.NewTicker(p.Cfg.Worker.LivenessProbeInterval)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			workers, err := p.listWorkers(ctx)
			if err != nil {
				p.Log.Errorf("failed to do worker liveness probe(need retry), err: %v", err)
				continue
			}
			for _, w := range workers {
				go func(w worker.Worker) {
					alive, deadReason, err := p.probeOneWorkerLiveness(ctx, w)
					if err != nil {
						p.Log.Errorf("failed to probe worker liveness(auto retry), workerID: %s, err: %v", w.GetID(), err)
						return
					}
					if alive {
						return
					}
					// dead, delete key
					_, err = p.EtcdClient.Txn(ctx).Then(
						clientv3.OpDelete(p.makeEtcdWorkerKey(w.GetID(), worker.Candidate)),
						clientv3.OpDelete(p.makeEtcdWorkerKey(w.GetID(), worker.Official)),
					).Commit()
					if err != nil {
						p.Log.Errorf("failed to delete dead worker related keys(auto retry), workerID: %s, dead reason: %s, err: %v", w.GetID(), deadReason, err)
						return
					}
					p.Log.Infof("dead worker deleted, workerID: %s, dead reason: %s", w.GetID(), deadReason)
				}(w)
			}
		}
	}

}
