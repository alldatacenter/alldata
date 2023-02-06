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

package snapshot

import (
	"os"
	"strconv"
	"strings"
)

// Sampling returns true if it needs to sampling when snapshot.
// Set it by env "PIPELINE_MIGRATION_DATABASE=true"
func Sampling() bool {
	return strings.EqualFold(os.Getenv("PIPELINE_MIGRATION_SAMPLING"), "true")
}

// MaxSamplingSize returns the max sampling size.
// Set it by env "PIPELINE_MIGRATION_SAMPLING_SIZE"
func MaxSamplingSize() uint64 {
	size := os.Getenv("PIPELINE_MIGRATION_SAMPLING_SIZE")
	n, err := strconv.ParseUint(size, 10, 32)
	if err != nil {
		return 300
	}
	return n
}
