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

package client

import (
	"context"
	"fmt"
	"math/rand"
	"time"

	"github.com/sirupsen/logrus"
	corev1 "k8s.io/api/core/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/watch"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/cache"
	watchtools "k8s.io/client-go/tools/watch"

	"github.com/erda-project/erda/apistructs"
)

func (c *Client) watchClusterCredential(ctx context.Context) error {
	var (
		retryWatcher *watchtools.RetryWatcher
		err          error
	)

	// Wait cluster credential secret ready.
	for {
		retryWatcher, err = getInClusterRetryWatcher(c.cfg.ErdaNamespace)
		if err != nil {
			logrus.Errorf("get retry warcher, %v", err)
		} else if retryWatcher != nil {
			break
		}

		select {
		case <-ctx.Done():
			return nil
		case <-time.After(time.Duration(rand.Int()%10) * time.Second):
			logrus.Warnf("failed to get retry watcher, try agin")
		}
	}

	defer retryWatcher.Stop()

	logrus.Info("start retry watcher")

	for {
		select {
		case event := <-retryWatcher.ResultChan():
			sec, ok := event.Object.(*corev1.Secret)
			if !ok {
				logrus.Errorf("illegal secret object, igonre")
				continue
			}

			logrus.Debugf("event type: %v, object: %+v", event.Type, sec)

			switch event.Type {
			case watch.Deleted:
				// ignore delete event, if cluster offline, reconnect will be failed.
				continue
			case watch.Added, watch.Modified:
				ak, ok := sec.Data[apistructs.ClusterAccessKey]
				// If accidentally deleted credential key, use the latest access key
				if !ok {
					logrus.Errorf("cluster info doesn't contain cluster access key value")
					continue
				}

				// Access key values doesn't change, skip reconnect
				if string(ak) == c.getAccessKey() {
					logrus.Debug("cluster access key doesn't change, skip")
					continue
				}

				if c.getAccessKey() == "" {
					logrus.Infof("get cluster accesskey %s", string(ak))
				} else {
					logrus.Infof("cluster accesskey change from %s to %s", c.getAccessKey(), string(ak))
				}

				// change value
				c.setAccessKey(string(ak))
				// if connected, reconnect.
				if c.IsConnected() {
					c.disconnect <- struct{}{}
				}
			}
		case <-ctx.Done():
			return nil
		}
	}
}

func (c *Client) setAccessKey(ac string) {
	c.Lock()
	defer c.Unlock()
	c.accessKey = ac
}

func (c *Client) getAccessKey() string {
	return c.accessKey
}

func getInClusterRetryWatcher(ns string) (*watchtools.RetryWatcher, error) {
	rc, err := rest.InClusterConfig()
	if err != nil {
		return nil, fmt.Errorf("get incluster config error: %v", err)
	}

	cs, err := kubernetes.NewForConfig(rc)
	if err != nil {
		return nil, fmt.Errorf("create clientset error: %v", err)
	}

	// Get or create secret
	secInit, err := cs.CoreV1().Secrets(ns).Get(context.Background(), apistructs.ErdaClusterCredential, v1.GetOptions{})
	if err != nil {
		if !k8serrors.IsNotFound(err) {
			return nil, fmt.Errorf("get secret error: %v", err)
		}
		// try to create init cluster credential secret
		secInit, err = cs.CoreV1().Secrets(ns).Create(context.Background(), &corev1.Secret{
			ObjectMeta: v1.ObjectMeta{Name: apistructs.ErdaClusterCredential},
			Data:       map[string][]byte{apistructs.ClusterAccessKey: []byte("init")},
		}, v1.CreateOptions{})

		if err != nil {
			return nil, fmt.Errorf("create init cluster credential secret error: %v", err)
		}
	}

	// create retry watcher
	retryWatcher, err := watchtools.NewRetryWatcher(secInit.ResourceVersion, &cache.ListWatch{
		WatchFunc: func(options v1.ListOptions) (watch.Interface, error) {
			return cs.CoreV1().Secrets(ns).Watch(context.Background(), v1.ListOptions{
				FieldSelector: fmt.Sprintf("metadata.name=%s", apistructs.ErdaClusterCredential),
			})
		},
	})

	if err != nil {
		return nil, fmt.Errorf("create retry watcher error: %v", err)
	}

	return retryWatcher, nil
}
