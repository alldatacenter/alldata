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

package pipelineymlv1

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestFilter_DoDisable(t *testing.T) {
	filterWithOnlyAndExcept := Filter{
		Type: GIT_BRANCH,
		Onlys: []string{
			`^release/.+$`,
			`^master$`,
			`^support/.+$`,
			`^feature/pass`,
		},
		Excepts: []string{
			`^dev`,
			`^support/1\.0$`,
		},
	}
	require.False(t, filterWithOnlyAndExcept.doDisable("master"))
	require.True(t, filterWithOnlyAndExcept.doDisable("master_1.0"))
	require.False(t, filterWithOnlyAndExcept.doDisable("release/1.0"))
	require.False(t, filterWithOnlyAndExcept.doDisable("release/online"))
	require.False(t, filterWithOnlyAndExcept.doDisable("support/2.10.0"))
	require.True(t, filterWithOnlyAndExcept.doDisable("support/1.0"))
	require.True(t, filterWithOnlyAndExcept.doDisable("develop"))
	require.True(t, filterWithOnlyAndExcept.doDisable("dev"))
	require.True(t, filterWithOnlyAndExcept.doDisable("development"))
	require.True(t, filterWithOnlyAndExcept.doDisable("feature/envs"))
	require.False(t, filterWithOnlyAndExcept.doDisable("feature/pass"))
	require.True(t, filterWithOnlyAndExcept.doDisable("feature/filter"))

	filterWithoutOnlyOrExcept := Filter{
		Type: GIT_BRANCH,
	}
	require.False(t, filterWithoutOnlyOrExcept.doDisable("master"))
	require.False(t, filterWithoutOnlyOrExcept.doDisable("develop"))
	require.False(t, filterWithoutOnlyOrExcept.doDisable("release/2.12.1"))
	require.False(t, filterWithoutOnlyOrExcept.doDisable("feature/test"))

	filterWithOnly := Filter{
		Type: GIT_BRANCH,
		Onlys: []string{
			"^master$",
			"^release/.+$",
		},
	}
	require.False(t, filterWithOnly.doDisable("master"))
	require.False(t, filterWithOnly.doDisable("release/2.12.1"))
	require.True(t, filterWithOnly.doDisable("release/"))
	require.True(t, filterWithOnly.doDisable("develop"))
	require.True(t, filterWithOnly.doDisable("arelease/2.12.1"))

	filterWithExcept := Filter{
		Type: GIT_BRANCH,
		Excepts: []string{
			"test",
			"^master$",
		},
	}
	require.True(t, filterWithExcept.doDisable("test"))
	require.True(t, filterWithExcept.doDisable("test1"))
	require.True(t, filterWithExcept.doDisable("atest1"))
	require.True(t, filterWithExcept.doDisable("feature/test1"))
	require.True(t, filterWithExcept.doDisable("master"))
	require.False(t, filterWithExcept.doDisable("master1"))
	require.False(t, filterWithExcept.doDisable("tes"))
	require.False(t, filterWithExcept.doDisable("release/2.12.1"))
	require.False(t, filterWithExcept.doDisable("develop"))
}

func TestEnvFilter(t *testing.T) {
	var FILTERKEY = "FILTER_KEY"

	filter := Filter{
		Type: GLOBAL_ENV,
		Key:  FILTERKEY,
		Onlys: []string{
			`^release/.+$`,
			`^master$`,
			`^support/.+$`,
			`^feature/pass`,
		},
		Excepts: []string{
			`^dev`,
			`^support/1\.0$`,
		},
	}

	globalEnvs := map[string]string{
		"KEY":     "VALUE",
		FILTERKEY: "support/1.0",
	}

	require.True(t, filter.needDisable("", globalEnvs))
}

func TestWrongSyntaxFilter(t *testing.T) {
	filter := Filter{
		Type: GIT_BRANCH,
		Onlys: []string{
			`(((`,
		},
	}

	require.False(t, filter.doDisable("master"))
}

func TestFilters_Parse(t *testing.T) {
	panicFilter := Filter{
		Type: GIT_BRANCH,
		Onlys: []string{
			`^release/.+$`,
			`^master$`,
			`^support/.+$`,
			`^feature/pass`,
			`(((`,
			`)))`,
		},
		Excepts: []string{
			`^dev`,
			`^support/1\.0$`,
			`((((`,
		},
	}
	require.Error(t, panicFilter.parse())

	fs := Filters{panicFilter}
	require.Error(t, fs.parse())

	nilFilter := Filter{}
	require.Nil(t, nilFilter.parse())
}
