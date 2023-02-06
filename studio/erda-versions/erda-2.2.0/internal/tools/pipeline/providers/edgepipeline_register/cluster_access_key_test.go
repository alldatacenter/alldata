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
	"testing"

	"github.com/coreos/etcd/clientv3"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/base/logs/logrusx"
)

func Test_getAccessKey(t *testing.T) {
	p := &provider{
		Cfg: &Config{
			ClusterAccessKey: "xxx",
		},
	}
	assert.Equal(t, "xxx", p.ClusterAccessKey())
}

func Test_setAccessKey(t *testing.T) {
	p := &provider{Cfg: &Config{}}
	p.setAccessKey("xxx")
	assert.Equal(t, "xxx", p.ClusterAccessKey())
}

func Test_makeClusterAccessEtcdKey(t *testing.T) {
	p := &provider{
		Cfg: &Config{
			EtcdPrefixOfClusterAccessKey: "/devops/pipeline/v2/edge/cluster-access-key",
		},
	}
	ac := "aaa"
	acEtcdKey := p.makeEtcdKeyOfClusterAccessKey(ac)
	assert.Equal(t, "/devops/pipeline/v2/edge/cluster-access-key/aaa", acEtcdKey)
}

func Test_storeClusterAccessKey(t *testing.T) {
	etcdClient := &clientv3.Client{
		KV: &MockKV{},
	}
	p := &provider{
		Cfg: &Config{
			IsEdge:           true,
			ClusterAccessKey: "xxx",
		},
		EtcdClient: etcdClient,
		Log:        logrusx.New(),
	}
	err := p.storeClusterAccessKey("aaa")
	assert.NoError(t, err)
}
