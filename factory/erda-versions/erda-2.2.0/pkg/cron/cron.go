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

package cron

import (
	"context"
	"fmt"
	"log"
	"runtime"
	"sort"
	"strings"
	"sync"
	"time"

	v3 "github.com/coreos/etcd/clientv3"
	"github.com/robfig/cron"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/pkg/etcdclient"
)

// Cron keeps track of any number of entries, invoking the associated func as
// specified by the schedule. It may be started, stopped, and the entries may
// be inspected while running.
type Cron struct {
	entries  []*Entry
	stop     chan struct{}
	add      chan *Entry
	snapshot chan []*Entry
	remove   chan string
	running  bool
	ErrorLog *log.Logger
	location *time.Location
	mux      *sync.RWMutex

	// default false, means will use etcd dlock
	withoutDLock bool
	// etcd client
	etcdClient *v3.Client
}

// Job is an interface for submitted cron jobs.
type Job interface {
	Run()
}

// The Schedule describes a job's duty cycle.
type Schedule interface {
	// Return the next activation time, later than the given time.
	// Next is invoked initially, and then each time the job is run.
	Next(time.Time) time.Time
}

// Entry consists of a schedule and the func to execute on that schedule.
type Entry struct {
	// The schedule on which this job should be run.
	Schedule Schedule

	// The next time the job will run. This is the zero time if Cron has not been
	// started or this entry's schedule is unsatisfiable
	Next time.Time

	// The last time this job was run. This is the zero time if the job has never
	// been run.
	Prev time.Time

	// The Job to run.
	Job Job

	// The Job's name
	Name string
}

// byTime is a wrapper for sorting the entry array by time
// (with zero time at the end).
type byTime []*Entry

func (s byTime) Len() int      { return len(s) }
func (s byTime) Swap(i, j int) { s[i], s[j] = s[j], s[i] }
func (s byTime) Less(i, j int) bool {
	// Two zero times should return false.
	// Otherwise, zero is "greater" than any other time.
	// (To sort it at the end of the list.)
	if s[i].Next.IsZero() {
		return false
	}
	if s[j].Next.IsZero() {
		return true
	}
	return s[i].Next.Before(s[j].Next)
}

type OpOption func(*Cron)

func WithoutDLock(without bool) OpOption {
	return func(c *Cron) {
		c.withoutDLock = without
	}
}

// New returns a new Cron job runner, in the Local time zone.
func New(ops ...OpOption) *Cron {
	return NewWithLocation(time.Now().Location(), ops...)
}

// NewWithLocation returns a new Cron job runner.
func NewWithLocation(location *time.Location, ops ...OpOption) *Cron {
	c := &Cron{
		entries:      nil,
		add:          make(chan *Entry),
		stop:         make(chan struct{}),
		snapshot:     make(chan []*Entry),
		remove:       make(chan string),
		running:      false,
		ErrorLog:     nil,
		location:     location,
		mux:          new(sync.RWMutex),
		withoutDLock: false,
		etcdClient:   nil,
	}
	// apply options
	for _, op := range ops {
		op(c)
	}
	// etcd dlock
	if !c.withoutDLock {
		etcdClient, err := etcdclient.NewEtcdClient()
		if err != nil {
			logrus.Fatalf("[alert] failed to init crond etcdClient (%v)", err)
		}
		c.etcdClient = etcdClient
	}
	return c
}

// A wrapper that turns a func() into a cron.Job
type FuncJob func()

func (f FuncJob) Run() { f() }

// AddFunc adds a func to the Cron to be run on the given schedule.
func (c *Cron) AddFunc(spec string, cmd func(), names ...string) error {
	var name string
	if len(names) <= 0 {
		name = fmt.Sprintf("%d", time.Now().Unix())
	} else {
		name = names[0]
	}

	return c.AddJob(spec, FuncJob(cmd), name)
}

// AddFunc adds a func to the Cron to be run on the given schedule.
func (c *Cron) AddOnceFunc(spec string, cmd func(), names ...string) error {
	var name string
	if len(names) <= 0 {
		name = fmt.Sprintf("%d", time.Now().Unix())
	} else {
		name = names[0]
	}

	// Support Once run function.
	onceCmd := func() {
		cmd()
		c.Remove(name)
	}

	return c.AddJob(spec, FuncJob(onceCmd), name)
}

// AddJob adds a Job to the Cron to be run on the given schedule.
func (c *Cron) AddJob(spec string, cmd Job, names ...string) error {
	var name string
	var schedule Schedule
	var err error
	if len(strings.Fields(spec)) == 5 {
		schedule, err = cron.ParseStandard(spec)
	} else {
		schedule, err = cron.Parse(spec)
	}
	if err != nil {
		return err
	}
	if len(names) <= 0 {
		name = fmt.Sprintf("%d", time.Now().Unix())
	} else {
		name = names[0]
	}

	c.Schedule(schedule, cmd, name)
	return nil
}

// Remove an entry from being run in the future.
func (c *Cron) Remove(name string) error {
	c.mux.Lock()
	defer c.mux.Unlock()

	if !c.running {
		p := c.pos(name)
		if p == -1 {
			return fmt.Errorf("no entry name: %s", name)
		}

		c.entries = c.entries[:p+copy(c.entries[p:], c.entries[p+1:])]
		return nil
	}

	c.remove <- name
	return nil
}

// pos return index if name in the Cron entries else -1
func (c *Cron) pos(name string) int {
	for p, e := range c.entries {
		if e.Name == name {
			return p
		}
	}

	return -1
}

// Schedule adds a Job to the Cron to be run on the given schedule.
func (c *Cron) Schedule(schedule Schedule, cmd Job, names ...string) {
	c.mux.Lock()
	defer c.mux.Unlock()

	var name string
	if len(names) <= 0 {
		name = fmt.Sprintf("%d", time.Now().Unix())
	} else {
		name = names[0]
	}
	entry := &Entry{
		Schedule: schedule,
		Job:      cmd,
		Name:     name,
	}
	if !c.running {
		p := c.pos(name)
		if p != -1 {
			c.logf("Duplicate names not allowed")
		}

		c.entries = append(c.entries, entry)
		return
	}

	c.add <- entry
}

// Entries returns a snapshot of the cron entries.
func (c *Cron) Entries() []*Entry {
	if c.running {
		c.snapshot <- nil
		x := <-c.snapshot
		return x
	}
	return c.entrySnapshot()
}

// Location gets the time zone location
func (c *Cron) Location() *time.Location {
	return c.location
}

// Start the cron scheduler in its own go-routine, or no-op if already started.
func (c *Cron) Start() {
	if c.running {
		return
	}
	c.running = true
	go c.run()
}

// Run the cron scheduler, or no-op if already running.
func (c *Cron) Run() {
	if c.running {
		return
	}
	c.running = true
	c.run()
}

func (c *Cron) runWithRecovery(j Job, name string, t time.Time) {
	defer func() {
		if r := recover(); r != nil {
			const size = 64 << 10
			buf := make([]byte, size)
			buf = buf[:runtime.Stack(buf, false)]
			c.logf("cron: panic running job: %v\n%s", r, buf)
		}
	}()

	// 获取分布式锁成功才能执行 job
	if c.etcdClient != nil {
		key := "/devops/pipeline/crond/dlock/" + name + "/" + t.String()
		r, err := c.etcdClient.Txn(context.Background()).
			If(v3.Compare(v3.Version(key), "=", 0)).
			Then(v3.OpPut(key, "")).
			Commit()
		defer func() { _, _ = c.etcdClient.Txn(context.Background()).Then(v3.OpDelete(key)).Commit() }()
		if err != nil {
			panic(err)
		}
		if r != nil && !r.Succeeded {
			return
		}
	}

	logrus.Infof("crond: triggered success! name: %s, time: %s", name, t.String())
	j.Run()
}

// Run the scheduler. this is private just due to the need to synchronize
// access to the 'running' state variable.
func (c *Cron) run() {
	// Figure out the next activation times for each entry.
	now := c.now()
	for _, entry := range c.entries {
		entry.Next = entry.Schedule.Next(now)
	}

	for {
		// Determine the next entry to run.
		sort.Sort(byTime(c.entries))

		var timer *time.Timer
		if len(c.entries) == 0 || c.entries[0].Next.IsZero() {
			// If there are no entries yet, just sleep - it still handles new entries
			// and stop requests.
			timer = time.NewTimer(100000 * time.Hour)
		} else {
			// if not reset time, the next corn execution time is incorrect when another cron is removed
			// eg: one cron execute every hour (0 0 * * * ?), and remove another cron at 2:30,
			// and now is 2:00, because the now isn't reset.
			// now the first cron execution time will become 1:00 2:00 3:30 4:00 ..., not 1:00 2:00 3:00 4:00
			now = c.now()
			timer = time.NewTimer(c.entries[0].Next.Sub(now))
		}

		for {
			select {
			case now = <-timer.C:
				now = now.In(c.location)
				// Run every entry whose next time was less than now
				for _, e := range c.entries {
					if e.Next.After(now) || e.Next.IsZero() {
						break
					}
					go c.runWithRecovery(e.Job, e.Name, e.Next)
					e.Prev = e.Next
					e.Next = e.Schedule.Next(now)
				}

			case newEntry := <-c.add:
				timer.Stop()
				now = c.now()
				newEntry.Next = newEntry.Schedule.Next(now)
				c.entries = append(c.entries, newEntry)

			case name := <-c.remove:
				p := c.pos(name)
				if p == -1 {
					break
				}

				c.entries = c.entries[:p+copy(c.entries[p:], c.entries[p+1:])]

			case <-c.snapshot:
				c.snapshot <- c.entrySnapshot()
				continue

			case <-c.stop:
				timer.Stop()
				return
			}

			break
		}
	}
}

// Logs an error to stderr or to the configured error log
func (c *Cron) logf(format string, args ...interface{}) {
	if c.ErrorLog != nil {
		c.ErrorLog.Printf(format, args...)
	} else {
		log.Printf(format, args...)
	}
}

// Stop stops the cron scheduler if it is running; otherwise it does nothing.
func (c *Cron) Stop() {
	if !c.running {
		return
	}
	c.stop <- struct{}{}
	c.running = false
}

// Stop timer and all connection
func (c *Cron) Close() {
	c.Stop()
	if c.etcdClient != nil {
		c.etcdClient.Close()
	}
}

// entrySnapshot returns a copy of the current cron entry list.
func (c *Cron) entrySnapshot() []*Entry {
	var entries []*Entry
	for _, e := range c.entries {
		entries = append(entries, &Entry{
			Schedule: e.Schedule,
			Next:     e.Next,
			Prev:     e.Prev,
			Job:      e.Job,
			Name:     e.Name,
		})
	}
	return entries
}

// now returns current time in c location
func (c *Cron) now() time.Time {
	return time.Now().In(c.location)
}
