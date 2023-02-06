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

package kmstypes

import (
	"context"

	"github.com/pkg/errors"
)

const (
	CtxKeyConfigMap = "configMap"
)

// PluginCreateFn be used to create a kms plugin instance
type PluginCreateFn func(ctx context.Context) Plugin

var PluginFactory = map[PluginKind]PluginCreateFn{}

func RegisterPlugin(kind PluginKind, create PluginCreateFn) error {
	if !kind.Validate() {
		return errors.Errorf("invalid plugin kind: %s", kind)
	}
	if _, ok := PluginFactory[kind]; ok {
		return errors.Errorf("duplicate to register kms plugin: %s", kind)
	}
	PluginFactory[kind] = create
	return nil
}

// StoreCreateFn be used to create a kms plugin instance
type StoreCreateFn func(ctx context.Context) Store

var StoreFactory = map[StoreKind]StoreCreateFn{}

func RegisterStore(kind StoreKind, create StoreCreateFn) error {
	if !kind.Validate() {
		return errors.Errorf("failed to register store, invalid store kind: %s", kind)
	}
	if _, ok := StoreFactory[kind]; ok {
		return errors.Errorf("failed to register store, duplicate store kind: %s", kind)
	}
	StoreFactory[kind] = create
	return nil
}
