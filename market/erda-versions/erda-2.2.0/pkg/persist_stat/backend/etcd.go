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

package backend

import (
	"context"
	"fmt"
	"path/filepath"
	"strconv"
	"sync"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/persist_stat"
)

type accumValue struct {
	Tag   string
	Value int64
}

type accumValueStored map[string]int64 // map[tag]value

type EtcdStat struct {
	dir          string
	js           jsonstore.JsonStore
	interval     int
	accumTp      persist_stat.AccumType
	preserveDays int

	accumValueCh chan accumValue

	memmetrics *persist_stat.MemMetrics

	lock sync.Mutex
}

func NewEtcd(js jsonstore.JsonStore, dir string) persist_stat.PersistStoreStat {
	e := &EtcdStat{
		dir:          filepath.Join("/persist_stat", dir),
		js:           js,
		interval:     60,
		accumTp:      persist_stat.SUM,
		accumValueCh: make(chan accumValue, 100),
		preserveDays: 1,
		memmetrics:   persist_stat.NewMemMetrics(),
	}
	go e.consumer()
	return e
}

func (e *EtcdStat) SetPolicy(policy persist_stat.Policy) error {
	e.lock.Lock()
	defer e.lock.Unlock()
	if policy.Interval < 60 {
		return fmt.Errorf("SetPolicy: interval must >= 60")
	}
	e.accumTp = policy.AccumTp
	e.interval = policy.Interval // TODO
	if policy.PreserveDays <= 0 {
		return fmt.Errorf("SetPolicy: bad preserveDays")
	}
	e.preserveDays = policy.PreserveDays
	return nil
}

func (e *EtcdStat) Emit(tag string, value int64) error {
	e.accumValueCh <- accumValue{tag, value}
	e.memmetrics.EmitMetric(tag, value)
	return nil
}

func (e *EtcdStat) consumer() {
	accumBuf := []accumValue{}
	e.lock.Lock()
	timer := time.NewTimer(time.Duration(e.interval) * time.Second) // TODO
	e.lock.Unlock()
	for {
		select {
		case <-timer.C:
			if len(accumBuf) != 0 {
				e.storeAccumValue(accumBuf)
			}
			cleardir := clearDir(e.dir, e.preserveDays, time.Now())
			if _, err := e.js.PrefixRemove(context.Background(), cleardir); err != nil {
				logrus.Errorf("[alert] persist_stat: consumer prefixremove: %v", err)
			}
			accumBuf = []accumValue{}
			e.lock.Lock()
			timer.Reset(time.Duration(e.interval) * time.Second)
			e.lock.Unlock()
		case v := <-e.accumValueCh:
			accumBuf = append(accumBuf, v)
		}
	}
}

func (e *EtcdStat) storeAccumValue(v []accumValue) {
	switch e.accumTp {
	case persist_stat.SUM:
		r := accumValueStored{}
		for _, i := range v {
			r[i.Tag] += i.Value
		}

		if err := e.js.Put(context.Background(), mkEtcdPath(e.dir, time.Now()), r); err != nil {
			logrus.Errorf("persist_stat: jsonstore Put: %v", err)
		}
	case persist_stat.AVG:
		r := accumValueStored{}
		count := map[string]int{}
		for _, i := range v {
			count[i.Tag]++
			r[i.Tag] += i.Value
		}
		for tag, v := range r {
			r[tag] = v / int64(count[tag])
		}
		if err := e.js.Put(context.Background(), mkEtcdPath(e.dir, time.Now()), r); err != nil {
			logrus.Errorf("persist_stat: jsonstore Put: %v", err)
		}
	default:
		logrus.Errorf("[alert] unsupported policy: %v", e.accumTp)
	}
}

func (e *EtcdStat) Clear(beforeTimeStamp time.Time) error {
	_, err := e.js.PrefixRemove(context.Background(), e.dir)
	return err
}

func (e *EtcdStat) Last5Min() (map[string]int64, error) {
	end := time.Now()
	beg := end.Add((-5) * time.Minute)
	return e.Stat(beg, end)
}
func (e *EtcdStat) Last20Min() (map[string]int64, error) {
	end := time.Now()
	beg := end.Add((-20) * time.Minute)
	return e.Stat(beg, end)
}
func (e *EtcdStat) Last1Hour() (map[string]int64, error) {
	end := time.Now()
	beg := end.Add((-1) * time.Hour)
	return e.Stat(beg, end)
}
func (e *EtcdStat) Last6Hour() (map[string]int64, error) {
	end := time.Now()
	beg := end.Add((-6) * time.Hour)
	return e.Stat(beg, end)
}
func (e *EtcdStat) Last1Day() (map[string]int64, error) {
	end := time.Now()
	beg := end.Add((-24) * time.Hour)
	return e.Stat(beg, end)
}
func (e *EtcdStat) Stat(beg, end time.Time) (map[string]int64, error) {
	accumValues := extractIntervalValue(e.js, e.dir, beg, end)

	midR := map[string][]int64{}
	for _, accV := range accumValues {
		for tag, v := range *accV {
			midR[tag] = append(midR[tag], v)
		}
	}
	r := map[string]int64{}
	for tag, vs := range midR {
		switch e.accumTp {
		case persist_stat.SUM:
			var sum int64
			for _, v := range vs {
				sum += v
			}
			r[tag] = sum
		case persist_stat.AVG:
			var sum int64
			for _, v := range vs {
				sum += v
			}
			r[tag] = sum / int64(len(vs))
		}
	}
	return r, nil
}

func (e *EtcdStat) Metrics() map[string]int64 {
	return e.memmetrics.GetMetrics()
}

// /<dir>/<year>/<month>/<day>/<hour>/<timestamp>
func mkEtcdPath(dir string, t time.Time) string {
	return filepath.Join(dir, strconv.Itoa(t.Year()), strconv.Itoa(int(t.Month())),
		strconv.Itoa(t.Day()), strconv.Itoa(t.Hour()), strconv.FormatInt(t.UnixNano(), 10))
}

func extractIntervalValue(js jsonstore.JsonStore, dir string, beg, end time.Time) []*accumValueStored {
	prefixes := commonPrefix(beg, end).intervalPrefixes
	allElems := [](*accumValueStored){}
	begUnix := beg.UnixNano()
	endUnix := end.UnixNano()
	var unused accumValueStored
	for _, prefix := range prefixes {
		if err := js.ForEach(context.Background(), filepath.Join(dir, prefix), unused,
			func(k string, v interface{}) error {
				timestamp, err := strconv.ParseInt(filepath.Base(k), 10, 64)
				if err != nil { // ignore
					return nil
				}
				if timestamp >= begUnix && timestamp <= endUnix {
					allElems = append(allElems, v.(*accumValueStored))
				}
				return nil
			}); err != nil {
			logrus.Errorf("[alert] persist_stat: foreach: %v", err)
		}
	}
	return allElems
}

type commonPrefixInfo struct {
	commonPrefix     string
	intervalPrefixes []string
}

func commonPrefix(beg, end time.Time) commonPrefixInfo {
	commonPrefix := []string{}
	intervalPrefixes := []string{}
	if beg.Year() != end.Year() {
		years := intervalYears(beg, end)
		commonPrefixJoined := filepath.Join(commonPrefix...)
		for _, year := range years {
			intervalPrefixes = append(intervalPrefixes, filepath.Join(commonPrefixJoined, year))
		}
		return commonPrefixInfo{
			commonPrefix:     commonPrefixJoined,
			intervalPrefixes: intervalPrefixes,
		}
	}
	commonPrefix = append(commonPrefix, strconv.Itoa(beg.Year()))
	if beg.Month() != end.Month() {
		months := intervalMonths(beg, end)
		commonPrefixJoined := filepath.Join(commonPrefix...)
		for _, month := range months {
			intervalPrefixes = append(intervalPrefixes, filepath.Join(commonPrefixJoined, month))
		}
		return commonPrefixInfo{
			commonPrefix:     commonPrefixJoined,
			intervalPrefixes: intervalPrefixes,
		}

	}
	commonPrefix = append(commonPrefix, strconv.Itoa(int(beg.Month())))
	if beg.Day() != end.Day() {
		days := intervalDays(beg, end)
		commonPrefixJoined := filepath.Join(commonPrefix...)
		for _, day := range days {
			intervalPrefixes = append(intervalPrefixes, filepath.Join(commonPrefixJoined, day))
		}
		return commonPrefixInfo{
			commonPrefix:     commonPrefixJoined,
			intervalPrefixes: intervalPrefixes,
		}
	}
	commonPrefix = append(commonPrefix, strconv.Itoa(beg.Day()))
	if beg.Hour() != end.Hour() {
		hours := intervalHours(beg, end)
		commonPrefixJoined := filepath.Join(commonPrefix...)
		for _, hour := range hours {
			intervalPrefixes = append(intervalPrefixes, filepath.Join(commonPrefixJoined, hour))
		}
		return commonPrefixInfo{
			commonPrefix:     commonPrefixJoined,
			intervalPrefixes: intervalPrefixes,
		}
	}
	commonPrefix = append(commonPrefix, strconv.Itoa(beg.Hour()))

	commonPrefixJoined := filepath.Join(commonPrefix...)
	return commonPrefixInfo{
		commonPrefix:     commonPrefixJoined,
		intervalPrefixes: []string{commonPrefixJoined},
	}
}

func intervalYears(beg, end time.Time) []string {
	r := []string{}
	if beg.Year() > end.Year() {
		return r
	}
	for i := beg.Year(); i <= end.Year(); i++ {
		r = append(r, strconv.Itoa(i))
	}
	return r
}

func intervalMonths(beg, end time.Time) []string {
	r := []string{}
	if beg.Month() > end.Month() {
		return r
	}
	for i := int(beg.Month()); i <= int(end.Month()); i++ {
		r = append(r, strconv.Itoa(i))
	}
	return r
}

func intervalDays(beg, end time.Time) []string {
	r := []string{}
	if beg.Day() > end.Day() {
		return r
	}
	for i := beg.Day(); i <= end.Day(); i++ {
		r = append(r, strconv.Itoa(i))
	}
	return r
}
func intervalHours(beg, end time.Time) []string {
	r := []string{}
	if beg.Hour() > end.Hour() {
		return r
	}
	for i := beg.Hour(); i <= end.Hour(); i++ {
		r = append(r, strconv.Itoa(i))
	}
	return r
}

func clearDir(dir string, preserveDays int, t time.Time) string {
	before := t.Add(time.Duration((-preserveDays-1)*24) * time.Hour)
	return filepath.Join(dir, strconv.Itoa(before.Year()), strconv.Itoa(int(before.Month())), strconv.Itoa(before.Day()))
}
