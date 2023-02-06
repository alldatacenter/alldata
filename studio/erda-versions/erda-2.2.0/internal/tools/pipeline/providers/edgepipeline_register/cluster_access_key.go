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

package edgepipeline_register

import (
	"context"
	"fmt"
	"math/rand"
	"strings"
	"time"

	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	v12 "k8s.io/apimachinery/pkg/apis/meta/v1"
	watch2 "k8s.io/apimachinery/pkg/watch"
	"k8s.io/client-go/tools/cache"
	"k8s.io/client-go/tools/watch"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/k8sclient"
)

func (p *provider) watchClusterCredential(ctx context.Context) {
	// if specified cluster access key, preferred to use it.
	if p.ClusterAccessKey() != "" {
		return
	}

	var (
		retryWatcher *watch.RetryWatcher
		err          error
	)

	// Wait cluster credential secret ready.
	for {
		retryWatcher, err = p.getInClusterRetryWatcher(p.Cfg.ErdaNamespace)
		if err != nil {
			p.Log.Errorf("get retry warcher, %v", err)
		} else if retryWatcher != nil {
			break
		}

		select {
		case <-ctx.Done():
			return
		case <-time.After(time.Duration(rand.Int()%10) * time.Second):
			p.Log.Warnf("failed to get retry watcher, try again")
		}
	}

	defer retryWatcher.Stop()

	p.Log.Info("start retry watcher")

	for {
		select {
		case event := <-retryWatcher.ResultChan():
			sec, ok := event.Object.(*v1.Secret)
			if !ok {
				p.Log.Errorf("illegal secret object, igonre")
				continue
			}

			p.Log.Debugf("event type: %v, object: %+v", event.Type, sec)

			switch event.Type {
			case watch2.Deleted:
				// ignore delete event, if cluster offline, reconnect will be failed.
				continue
			case watch2.Added, watch2.Modified:
				ak, ok := sec.Data[apistructs.ClusterAccessKey]
				// If accidentally deleted credential key, use the latest access key
				if !ok {
					p.Log.Errorf("cluster info doesn't contain cluster access key value")
					continue
				}

				// Access key values doesn't change, skip reconnect
				if string(ak) == p.ClusterAccessKey() {
					p.Log.Debug("cluster access key doesn't change, skip")
					continue
				}

				if p.ClusterAccessKey() == "" {
					p.Log.Infof("get cluster access key: %s", string(ak))
				} else {
					p.Log.Infof("cluster access key change from %s to %s", p.ClusterAccessKey(), string(ak))
				}

				// change value
				p.setAccessKey(string(ak))
				if err := p.storeClusterAccessKey(string(ak)); err != nil {
					p.Log.Error(err)
				}
			}
		case <-ctx.Done():
			return
		}
	}
}

func (p *provider) getInClusterRetryWatcher(ns string) (*watch.RetryWatcher, error) {
	cs, err := k8sclient.New(p.Cfg.ClusterName, k8sclient.WithPreferredToUseInClusterConfig())
	if err != nil {
		return nil, fmt.Errorf("create clientset error: %v", err)
	}

	// Get or create secret
	secInit, err := cs.ClientSet.CoreV1().Secrets(ns).Get(context.Background(), apistructs.ErdaClusterCredential, v12.GetOptions{})
	if err != nil {
		if !errors.IsNotFound(err) {
			return nil, fmt.Errorf("get secret error: %v", err)
		}
		// try to create init cluster credential secret
		secInit, err = cs.ClientSet.CoreV1().Secrets(ns).Create(context.Background(), &v1.Secret{
			ObjectMeta: v12.ObjectMeta{Name: apistructs.ErdaClusterCredential},
			Data:       map[string][]byte{apistructs.ClusterAccessKey: []byte("init")},
		}, v12.CreateOptions{})

		if err != nil {
			return nil, fmt.Errorf("create init cluster credential secret error: %v", err)
		}
	}

	// create retry watcher
	retryWatcher, err := watch.NewRetryWatcher(secInit.ResourceVersion, &cache.ListWatch{
		WatchFunc: func(options v12.ListOptions) (watch2.Interface, error) {
			return cs.ClientSet.CoreV1().Secrets(ns).Watch(context.Background(), v12.ListOptions{
				FieldSelector: fmt.Sprintf("metadata.name=%s", apistructs.ErdaClusterCredential),
			})
		},
	})

	if err != nil {
		return nil, fmt.Errorf("create retry watcher error: %v", err)
	}

	return retryWatcher, nil
}

func (p *provider) ClusterAccessKey() string {
	p.Lock()
	ac := p.Cfg.ClusterAccessKey
	p.Unlock()
	return ac
}

func (p *provider) setAccessKey(ac string) {
	p.Lock()
	defer p.Unlock()
	p.Cfg.ClusterAccessKey = ac
}

func (p *provider) storeClusterAccessKey(ac string) error {
	_, err := p.EtcdClient.Put(context.Background(), p.makeEtcdKeyOfClusterAccessKey(ac), "")
	if err != nil {
		return fmt.Errorf("store cluster access key error: %v", err)
	}
	return nil
}

func (p *provider) makeEtcdKeyOfClusterAccessKey(ac string) string {
	return fmt.Sprintf("%s/%s", p.Cfg.EtcdPrefixOfClusterAccessKey, ac)
}

// checkEtcdPrefixKey checks if the key is a valid etcd prefix key.
// a valid key is a string that should start with '/' and not end with '/', non-empty.
func (p *provider) checkEtcdPrefixKey(key string) error {
	if len(key) == 0 {
		return fmt.Errorf("etcd prefix key can not be empty")
	}
	if strings.HasSuffix(key, "/") {
		return fmt.Errorf("etcd prefix key must not end with '/'")
	}
	if !strings.HasPrefix(key, "/") {
		return fmt.Errorf("etcd prefix key must start with '/'")
	}
	return nil
}
