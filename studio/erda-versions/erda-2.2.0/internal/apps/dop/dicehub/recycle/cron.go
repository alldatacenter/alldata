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

// Package recycle Release GC处理
package recycle

import (
	"context"
	"time"

	v3 "github.com/coreos/etcd/clientv3"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/release"
)

// ImageGCCron 每天00:00:00执行一次, Release回收入口
func ImageGCCron(release *release.Release, client *v3.Client) {
	go func() {
		key := "/dicehub/gc"

		// 防止OOM后key残留，新实例起来后不能执行清理操作
		_, err := client.Txn(context.Background()).
			If(v3.Compare(v3.Version(key), ">", 0)).
			Then(v3.OpDelete(key)).Commit()
		if err != nil {
			logrus.Errorf("[alert] dicehub gc transaction err: %v", err)
		}

		for {
			now := time.Now()
			next := now.Add(time.Hour * 24)
			next = time.Date(next.Year(), next.Month(), next.Day(), 0, 0, 0, 0, next.Location())
			t := time.NewTimer(next.Sub(now))
			<-t.C

			// 确保多个实例任意时刻只有一个执行
			r, err := client.Txn(context.Background()).
				If(v3.Compare(v3.Version(key), "=", 0)).
				Then(v3.OpPut(key, "true")).
				Commit()
			if err != nil {
				logrus.Errorf("[alert] dicehub gc transaction err: %v", err)
				continue
			}
			if !r.Succeeded {
				logrus.Infof("key: %s already exists in etcd, don't run during this turn", key)
				continue
			}
			if err := release.RemoveDeprecatedsReleases(now); err != nil {
				logrus.Warnf("remove deprecated release error: %v", err)
			}
			if _, err := client.Delete(context.Background(), key); err != nil {
				// key删除失败，请手动从etcd清除，否则影响下次清理
				logrus.Errorf("[alert] dicehub clean txn key: %s fail during gc, please remove it manual from etcd, err: %v", key, err)
			}
			logrus.Infof("images gc success")
		}
	}()
}
