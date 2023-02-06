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

package lru

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/jsonstore/mem"
)

var objects = [][2]string{
	{"k0", "v0"},
	{"k1", "v1"},
	{"k2", "v2"},
	{"k3", "v3"},
	{"k4", "v4"},
}
var memStore, _ = mem.New()
var store, _ = New(4, memStore)

func TestLruMiss(t *testing.T) {
	ctx := context.Background()

	for _, o := range objects {
		err := store.Put(ctx, o[0], o[1])
		assert.Nil(t, err)
	}
	_, err := store.Get(ctx, "k0")
	assert.NotNil(t, err)
	assert.Equal(t, "not found", err.Error())
}

func TestLruGetUpdate(t *testing.T) {
	ctx := context.Background()

	for _, o := range objects {
		err := store.Put(ctx, o[0], o[1])
		assert.Nil(t, err)
	}
	_, err := store.Get(ctx, "k3")
	assert.Nil(t, err)
	assert.Equal(t, "k3", store.keyList.Front().Value.(string))
}
