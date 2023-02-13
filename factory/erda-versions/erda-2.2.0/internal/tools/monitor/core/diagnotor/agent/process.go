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

package diagnotor

import (
	"context"
	"runtime"
	"strconv"
	"time"

	"github.com/shirou/gopsutil/mem"
	"github.com/shirou/gopsutil/process"

	"github.com/erda-project/erda-proto-go/core/monitor/diagnotor/pb"
)

type ioCountersStatEntry struct {
	stat     process.IOCountersStat
	lastTime time.Time
}

type procStat struct {
	hasCPUTimes bool
	*process.Process
}

func (p *procStat) Percent(d time.Duration) (float64, error) {
	cpuPerc, err := p.Process.Percent(d)
	if !p.hasCPUTimes && err == nil {
		p.hasCPUTimes = true
		return 0, nil
	}
	return cpuPerc, err
}

func (s *diagnotorAgentService) runGatherProcStat(ctx context.Context) error {
	timer := time.NewTimer(0)
	defer timer.Stop()
	for {
		err := s.gatherProcStat()
		if err != nil {
			s.p.Log.Errorf("failed to gather procstat: %s", err)
		}

		if len(s.procStats) <= 0 {
			// target container has exited, so exit
			go func() {
				s.p.exit()
			}()
			return nil
		}

		timer.Reset(s.p.Cfg.GatherInterval)
		select {
		case <-ctx.Done():
			return nil
		case <-timer.C:
		}
	}
}

func (s *diagnotorAgentService) gatherProcStat() error {
	lastIOStat := make(map[int32]*ioCountersStatEntry)
	procStats := make(map[int32]*procStat)

	status := &pb.HostProcessStatus{
		TotalMemory:       s.getTotalMemory(),
		TotalCpuCores:     s.getTotalCPU(),
		MemoryUsedPercent: -1,
		CpuUsedPercent:    -1,
	}
	procs, err := process.Processes()
	if err != nil {
		return err
	}
	for _, item := range procs {
		if int(item.Pid) == s.pid {
			continue
		}
		proc, ok := s.procStats[item.Pid]
		if !ok {
			proc = &procStat{Process: item}
		}
		procStats[proc.Pid] = proc

		ps := &pb.Process{
			Pid: strconv.Itoa(int(proc.Pid)),
		}
		ps.Name, _ = proc.Name()

		ps.Cmdline, _ = proc.Cmdline()
		// user
		user, _ := proc.Username()
		if len(user) <= 0 {
			uids, _ := proc.Uids()
			if len(uids) > 0 {
				user = strconv.Itoa(int(uids[0]))
			}
		}
		ps.User = user
		ps.CreateTime, _ = proc.CreateTime()
		numFDs, _ := proc.NumFDs()
		ps.NumFDs = int64(numFDs)
		numThreads, _ := proc.NumThreads()
		ps.NumThreads = int64(numThreads)
		// app type
		ps.AppType = getAppType(ps)

		// io
		now := time.Now()
		io, err := proc.IOCounters()
		if err == nil {
			ps.Io = &pb.ProcessIOStatus{
				ReadCount:  int64(io.ReadCount),
				ReadBytes:  int64(io.ReadBytes),
				WriteCount: int64(io.WriteCount),
				WriteBytes: int64(io.WriteBytes),
			}
			lastIOStat[proc.Pid] = &ioCountersStatEntry{
				stat:     *io,
				lastTime: now,
			}
			if len(s.lastIOStat) > 0 {
				if lastStat, ok := s.lastIOStat[proc.Pid]; ok {
					seconds := now.Sub(lastStat.lastTime).Seconds()
					if seconds > 0 {
						ps.Io.ReadRate = float64(io.ReadBytes-lastStat.stat.ReadBytes) / seconds
						ps.Io.WriteRate = float64(io.WriteBytes-lastStat.stat.WriteBytes) / seconds
					}
				}
			}
		}

		// cpu
		cpuTimes, err := proc.Times()
		if err == nil {
			ps.Cpu = &pb.ProcessCPUStatus{
				User:   cpuTimes.User,
				System: cpuTimes.System,
				// Idle:      cpuTimes.Idle,
				// Nice:      cpuTimes.Nice,
				IoWait: cpuTimes.Iowait,
				// Irq:       cpuTimes.Irq,
				// SoftIrq:   cpuTimes.Softirq,
				// Steal:     cpuTimes.Steal,
				// Guest:     cpuTimes.Guest,
				// GuestNice: cpuTimes.GuestNice,
			}
		}
		cpuPercent, err := proc.Percent(time.Duration(0))
		if err == nil {
			if ps.Cpu == nil {
				ps.Cpu = &pb.ProcessCPUStatus{}
			}
			ps.Cpu.UsedPercentInHost = cpuPercent
			ps.Cpu.UsedCores, ps.Cpu.UsedPercent = s.convertCpuUsageToLimitedCpuUsage(cpuPercent)
			status.CpuUsedCores += ps.Cpu.UsedCores
		}

		// memory
		mem, err := proc.MemoryInfo()
		if err == nil {
			ps.Memory = &pb.ProcessMemoryStatus{
				Rss:    int64(mem.RSS),
				Vms:    int64(mem.VMS),
				Swap:   int64(mem.Swap),
				Data:   int64(mem.Data),
				Stack:  int64(mem.Stack),
				Locked: int64(mem.Locked),
			}
			ps.Memory.UsedPercent = s.convertMemoryUsageToLimitedMemoryUsage(ps.Memory.Rss)
			status.MemoryUsed += ps.Memory.Rss
		}

		// connections
		connections, _ := proc.Connections()
		ps.Connections = int64(len(connections))
		status.Connections += ps.Connections

		// rlimit
		rlims, err := proc.RlimitUsage(true)
		if err == nil {
			ps.Rlimit = &pb.ProcessRLimit{}
			for _, rlim := range rlims {
				stat := &pb.ProcessRLimitStatus{
					Soft: int64(rlim.Soft),
					Hard: int64(rlim.Hard),
					Used: int64(rlim.Used),
				}
				switch rlim.Resource {
				case process.RLIMIT_CPU:
					ps.Rlimit.CpuTime = stat
				case process.RLIMIT_DATA:
					ps.Rlimit.MemoryData = stat
				case process.RLIMIT_STACK:
					ps.Rlimit.MemoryStack = stat
				case process.RLIMIT_RSS:
					ps.Rlimit.MemoryRss = stat
				case process.RLIMIT_NOFILE:
					ps.Rlimit.NumFDs = stat
				case process.RLIMIT_MEMLOCK:
					ps.Rlimit.MemoryLocked = stat
				case process.RLIMIT_AS:
					ps.Rlimit.MemoryVms = stat
				case process.RLIMIT_LOCKS:
					stat.Used = -1
					ps.Rlimit.FileLocks = stat
				case process.RLIMIT_SIGPENDING:
					ps.Rlimit.SignalsPending = stat
				case process.RLIMIT_NICE:
					ps.Rlimit.NicePriority = stat
				case process.RLIMIT_RTPRIO:
					ps.Rlimit.RealtimePriority = stat
				}
			}
		}

		// context switches
		ctxSwitches, err := proc.NumCtxSwitches()
		if err == nil {
			ps.ContextSwitches = &pb.ProcessContextSwitches{
				Voluntary:   ctxSwitches.Voluntary,
				Involuntary: ctxSwitches.Involuntary,
			}
		}

		status.Processes = append(status.Processes, ps)
	}

	if status.TotalMemory != 0 {
		status.MemoryUsedPercent = 100 * (float64(status.MemoryUsed) / float64(status.TotalMemory))
	}
	if status.TotalCpuCores != 0 {
		status.CpuUsedPercent = 100 * (float64(status.CpuUsedCores) / float64(status.TotalCpuCores))
	}

	// save last io status
	s.lastIOStat = lastIOStat
	s.procStats = procStats

	s.lock.Lock()
	s.lastStatus = status
	s.lock.Unlock()
	return nil
}

func getAppType(proc *pb.Process) string {
	switch proc.Name {
	case "java":
		return "java"
	case "sh", "bash", "zsh":
		return "shell"
	}
	return "unknown"
}

func (s *diagnotorAgentService) getTotalCPU() float64 {
	total := runtime.NumCPU()
	limit := float64(s.p.Cfg.TargetContainerCpuLimit) / 1000
	if limit <= 0 {
		limit = float64(total)
	}
	return limit
}

func (s *diagnotorAgentService) getTotalMemory() int64 {
	limit := s.p.Cfg.TargetContainerMemLimit
	if limit <= 0 {
		machineMemory, err := mem.VirtualMemoryWithContext(context.Background())
		if err != nil {
			return -1
		}
		limit = int64(machineMemory.Total)
	}
	return limit
}

func (s *diagnotorAgentService) convertCpuUsageToLimitedCpuUsage(percent float64) (usedCores float64, usedPercent float64) {
	total := runtime.NumCPU()
	usedCores = float64(total) * (percent / 100)

	limit := s.getTotalCPU()
	if limit != 0 {
		usedPercent = 100 * (usedCores / limit)
	} else {
		usedPercent = -1
	}
	return usedCores, usedPercent
}

func (s *diagnotorAgentService) convertMemoryUsageToLimitedMemoryUsage(used int64) float64 {
	limit := s.getTotalMemory()
	if limit == 0 {
		return -1
	}
	return 100 * (float64(used) / float64(limit))
}
