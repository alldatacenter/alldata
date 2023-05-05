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

package adaptor

import (
	"context"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/conf"
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/dao"
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/ucclient"
	"github.com/erda-project/erda/pkg/cron"
	"github.com/erda-project/erda/pkg/dlock"
	"github.com/erda-project/erda/pkg/strutil"
)

// Adaptor 结构体
type Adaptor struct {
	db        *dao.DBClient
	uc        *ucclient.UCClient
	cron      *cron.Cron
	bdl       *bundle.Bundle
	receivers []Receiver
}

// Receiver 接收uc事件通知的对象
type Receiver interface {
	SendAudits(ucaudits *apistructs.UCAuditsListResponse) ([]int64, error)
	Name() string
}

// Option CDP 配置选项
type Option func(*Adaptor)

// New CDP service
func New(options ...Option) *Adaptor {
	a := &Adaptor{
		cron: cron.New(),
	}
	for _, op := range options {
		op(a)
	}
	a.RegistryReceiver(NewAuditReceiver(a.bdl), NewMemberReceiver(a.bdl))
	a.startAdaptorJob()
	return a
}

// WithDBClient 配置 Issue 数据库选项
func WithDBClient(db *dao.DBClient) Option {
	return func(adaptor *Adaptor) {
		adaptor.db = db
	}
}

// WithUCClient 配置 uc client
func WithUCClient(uc *ucclient.UCClient) Option {
	return func(adaptor *Adaptor) {
		adaptor.uc = uc
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(adaptor *Adaptor) {
		adaptor.bdl = bdl
	}
}

// RegistryReceiver 注册uc审计事件的接收者
func (a *Adaptor) RegistryReceiver(receiver ...Receiver) {
	a.receivers = append(a.receivers, receiver...)
}

func (a *Adaptor) startAdaptorJob() {
	a.cron.AddFunc(conf.UCAuditorCron(), a.syncUcAudit)
	a.cron.AddFunc(conf.CompensationExecCron(), a.compensateUcAudit)
	a.cron.AddFunc(conf.UCSyncRecordCleanCron(), a.deleteUCSyncRecord)
	a.cron.Start()
}

func (a *Adaptor) syncUcAudit() {
	lock, err := getLock("/audit/synclock")
	if err != nil {
		logrus.Errorf("get lock err: %v", err)
		return
	}
	defer func() {
		lock.UnlockAndClose()
	}()

	audits, err := a.PullAudits()
	if err != nil {
		logrus.Error(err)
	}
	if audits == nil || len(audits.Result) == 0 {
		return
	}
	// 执行每个receiver对审计的执行逻辑
	for _, r := range a.receivers {
		go func(r Receiver) {
			ucIDs, err := r.SendAudits(audits)
			if err != nil {
				logrus.Errorf("%v send uc audits err: %v", r.Name(), err)
				// 标记失败的uc事件，之后慢慢补偿
				if err := a.markSendFailedUCEvent(r.Name(), ucIDs); err != nil {
					logrus.Errorf("%v mark uc audits failed err: %v", r.Name(), err)
				}
			}
		}(r)
	}
}

// compensateUcAudit uc事件同步失败补偿
func (a *Adaptor) compensateUcAudit() {
	lock, err := getLock("/audit/compensatelock")
	if err != nil {
		logrus.Errorf("get lock err: %v", err)
		return
	}
	defer func() {
		lock.UnlockAndClose()
	}()

	var ucIDs []int64
	records, err := a.db.GetFaieldRecord(10)
	if err != nil {
		logrus.Errorf("get faield record err: %v", err)
		return
	}
	for _, v := range records {
		ucIDs = append(ucIDs, v.UCID)
	}
	audits, err := a.PullAuditsByIDs(ucIDs)
	if err != nil {
		logrus.Errorf("pull uc audits by ids error: %v", err)
		return
	}
	if audits == nil {
		logrus.Info("no data need compensate")
		return
	}
	logrus.Infof("starting compensate %v data", len(audits.Result))
	// 执行每个receiver对审计的执行逻辑
	for _, r := range a.receivers {
		go func(r Receiver) {
			_, err := r.SendAudits(audits)
			if err != nil {
				logrus.Errorf("%v compensate uc audits err: %v", r.Name(), err)
				return
			}
			if err := a.removeSendFailedReceiver(r.Name(), ucIDs); err != nil {
				logrus.Errorf("%v remove failed receiver err: %v", r.Name(), err)
			}
		}(r)
	}
}

// deleteUCSyncRecord 定时清理uc的同步记录
func (a *Adaptor) deleteUCSyncRecord() {
	t := time.Now().AddDate(0, 0, -7)
	if err := a.db.DeleteRecordByTime(t); err != nil {
		logrus.Errorf("clean uc sync record err: %v", err)
	}
}

// PullAudits 拉取uc的审计事件
func (a *Adaptor) PullAudits() (*apistructs.UCAuditsListResponse, error) {
	ucAuditReq, byID, err := a.genUCAuditReq()
	if err != nil {
		return nil, err
	}

	var resp *apistructs.UCAuditsListResponse
	if byID {
		logrus.Infof("last ucid is %v", ucAuditReq.LastID)
		resp, err = a.uc.ListUCAuditsByLastID(*ucAuditReq)
		if err != nil {
			return nil, err
		}
	} else {
		resp, err = a.uc.ListUCAuditsByEventTime(*ucAuditReq)
		if err != nil {
			return nil, err
		}
	}

	// 插入同步记录
	var records []*dao.UCSyncRecord
	for _, v := range resp.Result {
		t, err := time.ParseInLocation("2006-01-02 15:04:05", time.Unix(v.EventTime/1e3, 0).Format("2006-01-02 15:04:05"), time.Local)
		if err != nil {
			return nil, err
		}

		records = append(records, &dao.UCSyncRecord{UCID: v.ID, UCEventTime: t})
	}

	if err := a.db.BatchCreateUCSyncRecord(records); err != nil {
		return nil, err
	}

	return resp, nil
}

// PullAuditsByIDs 指定uc事件id获取事件
func (a *Adaptor) PullAuditsByIDs(ucIDs []int64) (*apistructs.UCAuditsListResponse, error) {
	l := len(ucIDs)
	if l == 0 {
		return nil, nil
	}

	req := &apistructs.UCAuditsListRequest{LastID: ucIDs[0] - 1, Size: 1}
	ucAudits := make([]apistructs.UCAudit, 0, l)
	for i := 0; i < l; i++ {
		if i+1 < l && ucIDs[i+1]-ucIDs[i] == 1 {
			req.Size++
			continue
		}

		resp, err := a.uc.ListUCAuditsByLastID(*req)
		if err != nil {
			return nil, err
		}
		// 重置
		req.LastID = ucIDs[i] - 1
		req.Size = 1
		ucAudits = append(ucAudits, resp.Result...)
	}

	return &apistructs.UCAuditsListResponse{Result: ucAudits}, nil
}

func (a *Adaptor) genUCAuditReq() (*apistructs.UCAuditsListRequest, bool, error) {
	lastRecord, err := a.db.GetLastNRecord(1)
	if err != nil {
		return nil, false, err
	}

	// 首次启动或者事件太久远了的（3天）就舍弃，从过去10分钟开始拉取
	if len(lastRecord) == 0 || lastRecord[0].UCEventTime.AddDate(0, 0, 3).Unix() < time.Now().Unix() {
		// 返回根据时间获取审计的请求
		return &apistructs.UCAuditsListRequest{
			Size:      conf.UCAuditorPullSize(),
			EventTime: time.Now().Add(-10*time.Minute).Unix() * 1000,
		}, false, nil
	}

	return &apistructs.UCAuditsListRequest{
		LastID: lastRecord[0].UCID,
		Size:   conf.UCAuditorPullSize(),
	}, true, nil
}

// markSendFailedUCEvent 标记发送失败的uc事件
func (a *Adaptor) markSendFailedUCEvent(unReceiver string, ucIDs []int64) error {
	records, err := a.db.GetRecordByUCIDs(ucIDs)
	if err != nil {
		return err
	}

	for _, record := range records {
		if record.UnReceiver == "" {
			record.UnReceiver = unReceiver
		} else {
			record.UnReceiver = record.UnReceiver + "," + unReceiver
		}

		if err := a.db.UpdateRecord(&record); err != nil {
			logrus.Errorf("mark uc sync record send failed err: %v", err)
		}
	}

	return nil
}

// removeSendFailedReceiver 重试成功后删除被标记的接收者
func (a *Adaptor) removeSendFailedReceiver(Receiver string, ucIDs []int64) error {
	records, err := a.db.GetRecordByUCIDs(ucIDs)
	if err != nil {
		return err
	}

	for _, record := range records {
		if record.UnReceiver == "" {
			continue
		}
		receivers := strutil.SplitIfEmptyString(record.UnReceiver, ",")
		l := len(receivers)
		for i, v := range receivers {
			if v == Receiver {
				if i == l-1 {
					receivers = receivers[:i]
				} else {
					receivers = append(receivers[:i], receivers[i+1:]...)
				}
			}
		}
		record.UnReceiver = strings.Join(receivers, ",")
		if err := a.db.UpdateRecord(&record); err != nil {
			logrus.Errorf("remove uc sync record send failed receiver err: %v", err)
		}
	}

	return nil
}

// ListSyncRecord 查看uc同步历史记录
func (a *Adaptor) ListSyncRecord() ([]dao.UCSyncRecord, error) {
	lastRecord, err := a.db.GetLastNRecord(100)
	if err != nil {
		return nil, err
	}

	return lastRecord, nil
}

func getLock(key string) (*dlock.DLock, error) {
	lock, err := dlock.New(key, func() {})
	if err != nil {
		return nil, err
	}
	if err := lock.Lock(context.Background()); err != nil {
		return nil, err
	}

	return lock, nil
}
