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

package scheduled

import (
	"strconv"
	"time"

	"github.com/go-redis/redis"
	"github.com/recallsong/go-utils/errorx"
)

// ScheduleStorage .
type ScheduleStorage interface {
	Nodes() ([]*Node, error)
	NodesKeepAlive(nodes []*Node, ttl time.Duration) error
	RemoveNode(nodeID string) error
	Get(nodeID string) (IDSet, error)
	Add(nodeID string, id int64) error
	Del(nodeID string, id int64) error
	Foreach(nodeID string, h func(int64) bool) error
}

// IDSet .
type IDSet map[int64]struct{}

func (s IDSet) Put(id int64) { s[id] = struct{}{} }
func (s IDSet) Contains(id int64) bool {
	_, ok := s[id]
	return ok
}

// Node .
type Node struct {
	ID string
}

// RedisScheduleStorage
type RedisScheduleStorage struct {
	Root      string
	Redis     *redis.Client
	NodesFunc func() ([]*Node, error)
}

func (s *RedisScheduleStorage) Nodes() ([]*Node, error) {
	return s.NodesFunc()
}

func (s *RedisScheduleStorage) nodeKey(nodeID string) string {
	return s.Root + "/" + nodeID
}

func (s *RedisScheduleStorage) NodesKeepAlive(nodes []*Node, ttl time.Duration) error {
	var errs errorx.Errors
	for _, n := range nodes {
		_, err := s.Redis.Expire(s.nodeKey(n.ID), ttl).Result()
		if err != nil {
			errs.Append(err)
		}
	}
	return errs.MaybeUnwrap()
}

func (s *RedisScheduleStorage) RemoveNode(nodeID string) error {
	return s.Redis.Del(s.nodeKey(nodeID)).Err()
}

func (s *RedisScheduleStorage) Get(nodeID string) (IDSet, error) {
	list, err := s.Redis.SMembers(s.nodeKey(nodeID)).Result()
	if err != nil {
		return nil, err
	}
	set := make(IDSet)
	for _, item := range list {
		v, err := strconv.ParseInt(item, 10, 64)
		if err == nil {
			set.Put(v)
		}
	}
	return set, nil
}

func (s *RedisScheduleStorage) Add(nodeID string, id int64) error {
	return s.Redis.SAdd(s.nodeKey(nodeID), strconv.FormatInt(id, 10)).Err()
}

func (s *RedisScheduleStorage) Del(nodeID string, id int64) error {
	return s.Redis.SRem(s.nodeKey(nodeID), strconv.FormatInt(id, 10)).Err()
}

func (s *RedisScheduleStorage) Foreach(nodeID string, h func(int64) bool) error {
	list, err := s.Redis.SMembers(s.nodeKey(nodeID)).Result()
	if err != nil {
		return err
	}
	for _, item := range list {
		v, err := strconv.ParseInt(item, 10, 64)
		if err == nil {
			if !h(v) {
				return nil
			}
		}
	}
	return nil
}
