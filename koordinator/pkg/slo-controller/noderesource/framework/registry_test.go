/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package framework

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRegistry(t *testing.T) {
	p0 := newTestPlugin("p0")
	p1 := newTestPlugin("p1")
	p2 := newTestPlugin("p2")
	p3 := newTestPlugin("p3")
	t.Run("test plugin registry", func(t *testing.T) {
		r := NewRegistry("test registry")
		assert.Equal(t, 0, r.Size())

		initPlugins := []Plugin{p0, p1, p2}
		r.MustRegister(initPlugins...)
		assert.Equal(t, initPlugins, r.GetAll())

		wantPlugins := []Plugin{p0, p1, p2, p3}
		r.MustRegister(p3)
		assert.Equal(t, wantPlugins, r.GetAll())

		wantPlugins1 := []Plugin{p1, p2, p3}
		r.Unregister(p0.Name())
		assert.Equal(t, wantPlugins1, r.GetAll())
	})
}

var _ Plugin = (*testPlugin)(nil)

type testPlugin struct {
	name string
}

func newTestPlugin(name string) Plugin {
	return &testPlugin{
		name: name,
	}
}

func (t *testPlugin) Name() string {
	return t.name
}
