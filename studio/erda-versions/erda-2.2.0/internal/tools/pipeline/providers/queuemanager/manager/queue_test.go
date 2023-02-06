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

package manager

import (
	"context"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/jsonstore"
)

func TestSend(t *testing.T) {
	js := &jsonstore.JsonStoreImpl{}
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(js), "Put", func(j *jsonstore.JsonStoreImpl, ctx context.Context, key string, object interface{}) error {
		return nil
	})
	defer pm.Unpatch()

	mgr := New(context.Background(), WithJsonStore(js))
	t.Run("Send", func(t *testing.T) {
		mgr.SendQueueToEtcd(1)
	})
}

func TestSendPipelineIDS(t *testing.T) {
	js := &jsonstore.JsonStoreImpl{}
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(js), "Put", func(j *jsonstore.JsonStoreImpl, ctx context.Context, key string, object interface{}) error {
		return nil
	})
	defer pm.Unpatch()

	mgr := New(context.Background(), WithJsonStore(js))
	t.Run("Send", func(t *testing.T) {
		mgr.SendUpdatePriorityPipelineIDsToEtcd(1, []uint64{1, 2, 3})
	})
}

func TestParseIDFromWatchedKey(t *testing.T) {
	queueID, err := parseIDFromWatchedKey("/devops/pipeline/queue_manager/actions/update/123", etcdQueueWatchPrefix)
	assert.NoError(t, err)
	assert.Equal(t, uint64(123), queueID)

	pipelineID, err := parseIDFromWatchedKey("/devops/pipeline/queue_manager/actions/pop-out-pipeline/1001", etcdQueuePopOutPipelineWatchPrefix)
	assert.NoError(t, err)
	assert.Equal(t, uint64(1001), pipelineID)
}

func TestSendPopOutPipelineIDToEtcd(t *testing.T) {
	js := &jsonstore.JsonStoreImpl{}
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(js), "Put", func(j *jsonstore.JsonStoreImpl, ctx context.Context, key string, object interface{}) error {
		return nil
	})
	defer pm.Unpatch()

	mgr := New(context.Background(), WithJsonStore(js))
	t.Run("SendPopOutPipelineIDToEtcd", func(t *testing.T) {
		mgr.SendPopOutPipelineIDToEtcd(123)
	})
}
