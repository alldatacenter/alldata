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

package config

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"syscall"
	"time"

	"github.com/fsnotify/fsnotify"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/klog/v2"
)

const (
	defaultConfigFileNums = 2
)

type ManagerInterface interface {
	GetAllHook() []*RuntimeHookConfig
	Run() error
}

type Manager struct {
	sync.Mutex
	configs map[string]*RuntimeHookConfigItem
	watcher *fsnotify.Watcher
}

type RuntimeHookConfigItem struct {
	filePath   string
	fileIno    uint64
	updateTime time.Time
	*RuntimeHookConfig
}

func (m *Manager) GetAllHook() []*RuntimeHookConfig {
	var runtimeConfigs []*RuntimeHookConfig
	m.Lock()
	defer m.Unlock()
	for _, config := range m.configs {
		runtimeConfigs = append(runtimeConfigs, config.RuntimeHookConfig)
	}
	return runtimeConfigs
}

func (m *Manager) getAllRegisteredFiles() []string {
	var files []string
	m.Lock()
	defer m.Unlock()
	for filepath := range m.configs {
		files = append(files, filepath)
	}
	return files
}

func NewConfigManager() *Manager {
	return &Manager{
		configs: make(map[string]*RuntimeHookConfigItem, defaultConfigFileNums),
	}
}

func (m *Manager) registerFileToWatchIfNeed(file string) error {
	fileInfo, err := os.Stat(file)
	if err != nil {
		return err
	}
	stat, ok := fileInfo.Sys().(*syscall.Stat_t)
	if !ok {
		return fmt.Errorf("fail to get file ino: %v", file)
	}
	m.Lock()
	defer m.Unlock()
	config, exist := m.configs[file]
	if exist && config.fileIno == stat.Ino {
		return nil
	}
	if exist && config.fileIno != stat.Ino {
		m.watcher.Remove(file)
		klog.Infof("remove previous file %v with inode number %v", file, config.fileIno)
	}
	m.watcher.Add(file)
	m.configs[file] = &RuntimeHookConfigItem{
		filePath: file,
		fileIno:  stat.Ino,
	}
	klog.Infof("add new watching file %v with inode number %v", file, stat.Ino)
	return nil
}

func (m *Manager) removeFileToWatch(filepath string) {
	m.Lock()
	defer m.Unlock()
	if _, exist := m.configs[filepath]; !exist {
		return
	}
	err := m.watcher.Remove(filepath)
	if err != nil {
		klog.Errorf("fail to remove %s to watch", filepath)
	}
	delete(m.configs, filepath)
	klog.Infof("remove watching file %v", filepath)
}

func (m *Manager) needRefreshConfig(filepath string) bool {
	fileStat, err := os.Stat(filepath)
	if err != nil {
		klog.Errorf("fail to stat %v", err)
		return false
	}
	fileModTime := fileStat.ModTime()
	lastUpdateTimestamp := func(filepath string) time.Time {
		m.Lock()
		defer m.Unlock()
		if config, exist := m.configs[filepath]; !exist {
			return time.Time{}
		} else {
			return config.updateTime
		}
	}(filepath)

	return lastUpdateTimestamp.Before(fileModTime)
}

// updateHookConfig loads config file, and register file to fsnotify watcher to watch
// config file content changed
// the filepath should be absolute path
func (m *Manager) updateHookConfig(filepath string) error {
	if !strings.HasSuffix(filepath, "json") {
		return nil
	}

	if err := m.registerFileToWatchIfNeed(filepath); err != nil {
		klog.Errorf("fail to registry file %v", filepath)
		return err
	}

	if !m.needRefreshConfig(filepath) {
		return nil
	}

	updateTime := time.Now()
	data, err := os.ReadFile(filepath)
	if err != nil {
		return err
	}
	config := &RuntimeHookConfig{}
	if err := json.Unmarshal(data, config); err != nil {
		return err
	}

	m.Lock()
	defer m.Unlock()

	configItem, exist := m.configs[filepath]
	if !exist {
		return fmt.Errorf("no found config file %v", filepath)
	}
	configItem.RuntimeHookConfig = config
	configItem.updateTime = updateTime
	klog.Infof("update config for %v %v", filepath, config)
	return nil
}

func (m *Manager) Run() error {
	if _, err := os.Stat(defaultRuntimeHookConfigPath); os.IsNotExist(err) {
		klog.Infof("create %v", defaultRuntimeHookConfigPath)
		if err := os.MkdirAll(defaultRuntimeHookConfigPath, 0755); err != nil {
			klog.Errorf("fail to create %v %v", defaultRuntimeHookConfigPath, err)
			return err
		}
	}
	// watch the newly generated config file
	watcher, err := fsnotify.NewWatcher()
	if err != nil {
		return err
	}
	m.watcher = watcher

	if err := m.watcher.Add(defaultRuntimeHookConfigPath); err != nil {
		return err
	}
	go m.syncLoop()

	// collect the existing config
	m.collectAllConfigs()
	go m.healthCheck()

	return nil
}

func (m *Manager) collectAllConfigs() error {
	items, err := os.ReadDir(defaultRuntimeHookConfigPath)
	if err != nil {
		return err
	}
	for _, item := range items {
		if item.IsDir() {
			continue
		}
		if err := m.updateHookConfig(filepath.Join(defaultRuntimeHookConfigPath, item.Name())); err != nil {
			continue
		}
	}
	return nil
}

func (m *Manager) syncLoop() error {
	for {
		select {
		case event, ok := <-m.watcher.Events:
			if !ok {
				klog.Infof("config manager channel is closed")
				return nil
			}
			// only reload config when write/rename/remove events
			if event.Op&(fsnotify.Chmod) > 0 {
				klog.V(5).Infof("ignore event from runtime hook config dir %v", event)
				continue
			}
			// should add the config file to watcher if event.Op is fsnotify.Create
			klog.V(5).Infof("receive change event from runtime hook config dir %v", event)
			m.updateHookConfig(event.Name)
		case err := <-m.watcher.Errors:
			if err != nil {
				klog.Errorf("failed to continue to sync %v", defaultRuntimeHookConfigPath)
			}
		}
	}
}

func (m *Manager) removeUnusedConfigs() {
	for _, file := range m.getAllRegisteredFiles() {
		if _, err := os.Stat(file); os.IsNotExist(err) {
			m.removeFileToWatch(file)
		}
	}
}

func (m *Manager) healthCheck() {
	wait.Until(func() {
		m.removeUnusedConfigs()
		m.collectAllConfigs()
		allFiles := m.getAllRegisteredFiles()
		klog.V(6).Infof("current runtime hook config infos %v(%v)", allFiles, len(allFiles))
	}, time.Minute, wait.NeverStop)
}
