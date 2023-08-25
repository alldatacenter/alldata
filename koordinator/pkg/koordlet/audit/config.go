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

package audit

import (
	"flag"
	"time"
)

type Config struct {
	LogDir               string
	Verbose              int
	MaxDiskSpaceMB       int
	MaxConcurrentReaders int
	ActiveReaderTTL      time.Duration
	DefaultEventsLimit   int
	MaxEventsLimit       int
	TickerDuration       time.Duration
}

func NewDefaultConfig() *Config {
	return &Config{
		LogDir:               "/var/log/koordlet",
		Verbose:              3,
		MaxDiskSpaceMB:       16,
		MaxConcurrentReaders: 4,
		ActiveReaderTTL:      time.Minute * 10,
		DefaultEventsLimit:   256,
		MaxEventsLimit:       2048,
		TickerDuration:       time.Minute,
	}
}

func (c *Config) InitFlags(fs *flag.FlagSet) {
	fs.StringVar(&c.LogDir, "audit-log-dir", c.LogDir, "The dir of audit log")
	fs.IntVar(&c.Verbose, "audit-verbose", c.Verbose, "The verbose of the audit log")
	fs.IntVar(&c.MaxDiskSpaceMB, "audit-max-disk-space-mb", c.MaxDiskSpaceMB, "Max disk space occupied of audit log")
	fs.IntVar(&c.MaxConcurrentReaders, "audit-max-concurrent-readers", c.MaxConcurrentReaders, "Max concurrent readers of the audit log")
	fs.IntVar(&c.MaxEventsLimit, "audit-max-events-limit", c.MaxEventsLimit, "Max events limit in one request of the audit log")
}
