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

package span

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_seriesIDSet_Has(t *testing.T) {
	st := newSeriesIDSet(10)
	st.Add(1)
	st.Add(2)
	st.Add(3)
	ass := assert.New(t)
	ass.Equal(3, len(st.seriesIDList))

	st.AddBatch([]uint64{4, 5, 6})
	ass.Equal(6, len(st.seriesIDList))

	st.CleanOldPart()
	ass.Equal(3, len(st.seriesIDList))
	ass.Equal(uint64(4), st.seriesIDList[0])

	ass.Equal(false, st.Has(uint64(1)))
	ass.Equal(true, st.Has(uint64(5)))
}
