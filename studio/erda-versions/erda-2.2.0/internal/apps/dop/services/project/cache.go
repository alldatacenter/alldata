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

package project

type memberCacheObject struct {
	m map[uint64]*memberItem
}

func newMemberCacheObject() *memberCacheObject {
	return &memberCacheObject{m: make(map[uint64]*memberItem)}
}

func (o *memberCacheObject) hasMemberIn(filter map[uint64]struct{}) (*memberItem, bool) {
	for id := range filter {
		if item, ok := o.m[id]; ok {
			return item, true
		}
	}
	return nil, false
}

func (o *memberCacheObject) first() *memberItem {
	for _, v := range o.m {
		return v
	}
	return ownerUnknown()
}

type memberItem struct {
	ID   uint64
	Name string
	Nick string
}

func ownerUnknown() *memberItem {
	return &memberItem{
		ID:   0,
		Name: "OwnerUnknown",
		Nick: "OwnerUnknown",
	}
}
