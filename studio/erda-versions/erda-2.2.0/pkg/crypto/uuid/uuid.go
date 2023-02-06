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

package uuid

import (
	"fmt"

	guuid "github.com/google/uuid"
	uuid "github.com/satori/go.uuid"
)

// New return UUID.
// see: https://en.wikipedia.org/wiki/Universally_unique_identifier#Format
//
// A UUID is a 128 bit (16 byte) Universal Unique IDentifier as defined in RFC 4122.
// In its canonical textual representation, the 16 octets of a UUID are represented as 32 hexadecimal (base-16) digits,
// displayed in five groups separated by hyphens, in the form 8-4-4-4-12 for a total of 36 characters (32 hexadecimal characters and 4 hyphens).
// For example:
//   123e4567-e89b-12d3-a456-426614174000
//   xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx
func New() string {
	return guuid.New().String()
}

// UUID return uuid.
// format:
//   xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
// Deprecated. Please use New()
func UUID() string {
	u := uuid.NewV4()
	return fmt.Sprintf("%x%x%x%x%x", u[:4], u[4:6], u[6:8], u[8:10], u[10:])
}
