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

// Package recycle Release GC
package release

import (
	"context"
	"time"

	v3 "github.com/coreos/etcd/clientv3"
	"github.com/sirupsen/logrus"
)

// ImageGCCron Execute once every day at 00:00:00, Release recycles the entrance
func (p *provider) ImageGCCron(client *v3.Client) {
	go func() {
		key := "/dicehub/gc"

		// Prevent the key from remaining after OOM, and the cleanup operation cannot be performed after the new instance is up
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

			// Ensure that only one execution of multiple instances at any time
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
			if err := p.releaseService.RemoveDeprecatedsReleases(now); err != nil {
				logrus.Warnf("remove deprecated release error: %v", err)
			}
			if _, err := client.Delete(context.Background(), key); err != nil {
				// key delete false ，please clear from etcd manually，otherwise it will affect the next cleanup
				logrus.Errorf("[alert] dicehub clean txn key: %s fail during gc, please remove it manual from etcd, err: %v", key, err)
			}
			logrus.Infof("images gc success")
		}
	}()
}
