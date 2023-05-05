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

package bundle

//import (
//	"fmt"
//	"os"
//	"testing"
//
//	"github.com/stretchr/testify/assert"
//	"github.com/stretchr/testify/require"
//)
//
//func TestBundle_GetApp(t *testing.T) {
//	os.Setenv("CMDB_ADDR", "cmdb.default.svc.cluster.local:9093")
//	defer func() {
//		os.Unsetenv("CMDB_ADDR")
//	}()
//	b := New(WithCMDB())
//	_, err := b.GetApp(1)
//	require.NoError(t, err)
//}
//
//func TestBundle_GetMyApp(t *testing.T) {
//	os.Setenv("CMDB_ADDR", "cmdb.default.svc.cluster.local:9093")
//	defer func() {
//		os.Unsetenv("CMDB_ADDR")
//	}()
//	b := New(WithCMDB())
//	apps, err := b.GetMyApps("2", 1)
//	// require.NoError(t, err)
//	assert.NoError(t, err)
//	fmt.Println(apps)
//}
