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

package extension

import (
	"fmt"
	"io/fs"
	"io/ioutil"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"sync"

	"github.com/fsnotify/fsnotify"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/dicehub/extension/pb"
	"github.com/erda-project/erda/pkg/cron"
	"github.com/erda-project/erda/pkg/limit_sync_group"
)

type Source interface {
	match(addr string) bool
	add(addr string) error
	remove(addr string) error
	start()
}

// git source
// Pull the code when adding, and then use FileExtensionSource to monitor the file, and wait for the regular pull to update the file
// remove remove file monitoring and associated information
// start continues to pull and update local files according to the association relationship
type GitExtensionSource struct {
	Cfg              *config
	GitCloneAddr     sync.Map
	GitCloneRepoName sync.Map
	f                *FileExtensionSource
}

// Match the address starting with http:// | https:// | git@
func (g *GitExtensionSource) match(addr string) bool {
	if strings.HasPrefix(addr, "http://") || strings.HasPrefix(addr, "https://") || strings.HasPrefix(addr, "git@") {
		return true
	}
	return false
}

// Download the addr address, and then generate the association relationship between the git address and the local address
// Then add the local file to the local file monitoring
func (g *GitExtensionSource) add(addr string) error {
	_, ok := g.GitCloneAddr.Load(addr)
	if !ok {
		dir, err := ioutil.TempDir(os.TempDir(), "*")
		if err != nil {
			logrus.Errorf("gitExtensionSource tempDir error %v", err)
			return err
		}

		command := exec.Command("sh", "-c", "git clone "+addr)
		command.Dir = dir
		output, err := command.CombinedOutput()
		if err != nil {
			logrus.Errorf("git clone extensions address stderr %v", string(output))
			return err
		}

		fileInfoList, err := ioutil.ReadDir(dir)
		if err != nil {
			return err
		}
		if len(fileInfoList) == 0 {
			return fmt.Errorf("repo not have file")
		}
		g.GitCloneRepoName.Store(addr, fileInfoList[0].Name())
		g.GitCloneAddr.Store(addr, dir)

		return g.f.add(dir)
	}
	return nil
}

// Take out the local address corresponding to git addr and remove the file monitoring of the local address
func (g *GitExtensionSource) remove(addr string) error {
	value, ok := g.GitCloneAddr.Load(addr)
	if ok {
		err := g.f.remove(value.(string))
		if err != nil {
			return err
		}
		g.GitCloneAddr.Delete(addr)
		g.GitCloneRepoName.Delete(addr)
	}
	return nil
}

// Timed task pulls and updates all git addresses
func (g *GitExtensionSource) start() {
	go func() {
		c := cron.New()
		err := c.AddFunc(g.Cfg.ExtensionSourcesCron, func() {
			var gitCloneRepoNameMap = map[string]string{}
			g.GitCloneRepoName.Range(func(key, value interface{}) bool {
				gitCloneRepoNameMap[key.(string)] = value.(string)
				return true
			})

			wait := limit_sync_group.NewSemaphore(10)
			g.GitCloneAddr.Range(func(gitAddr, localAddr interface{}) bool {
				wait.Add(1)
				go func(gitAddr, localAddr interface{}) {
					defer wait.Done()

					// Open the write lock, only after all the files are written, the local file monitoring can start execution
					lock := g.f.getAddrRLock(localAddr.(string))
					if lock != nil {
						lock.Lock()
						defer lock.Unlock()
					}

					command := exec.Command("sh", "-c", "git pull")
					command.Dir = filepath.Join(localAddr.(string), gitCloneRepoNameMap[gitAddr.(string)])
					output, err := command.CombinedOutput()
					if err != nil {
						logrus.Errorf("git clone extensions address stderr %v", string(output))
					}
				}(gitAddr, localAddr)
				return true
			})
			wait.Wait()

		})
		if err != nil {
			panic(fmt.Errorf("error to add cron task %v", err))
		} else {
			c.Start()
		}
	}()
}

// local file source
// match matches the local folder
// add Add a folder address to the file change monitoring
// remove removes a folder from file change monitoring
// start Open file monitoring, continuously monitor file changes, and update extension
type FileExtensionSource struct {
	watcher    *fsnotify.Watcher
	s          *extensionService
	lock       sync.Mutex
	AddrRWLock map[string]*sync.RWMutex
}

// Obtain the read-write lock of the local file
func (f *FileExtensionSource) getAddrRLock(addr string) *sync.RWMutex {
	f.lock.Lock()
	defer f.lock.Unlock()
	rwLock, ok := f.AddrRWLock[addr]
	if ok {
		return rwLock
	}
	return nil
}

// Create a read-write lock for the local address
func (f *FileExtensionSource) setAddrRLock(addr string) {
	f.lock.Lock()
	defer f.lock.Unlock()
	_, ok := f.AddrRWLock[addr]
	if !ok {
		f.AddrRWLock[addr] = &sync.RWMutex{}
	}
}

// match directory
func (f *FileExtensionSource) match(addr string) bool {
	return isDir(addr)
}

func isDir(addr string) bool {
	s, err := os.Stat(addr)
	if err != nil {
		return false
	}
	return s.IsDir()
}

// Mandatory overwrite extension
// Add the local address to the monitoring list
func (f *FileExtensionSource) add(addr string) error {
	f.lock.Lock()
	err := f.s.InitExtension(addr, true)
	if err != nil {
		return err
	}
	f.lock.Unlock()

	f.setAddrRLock(addr)
	f.addDirWatch(addr)
	return nil
}

// Get all the folders of the file address and add them to the monitoring list
func (f *FileExtensionSource) addDirWatch(addr string) {
	if err := filepath.Walk(addr, func(path string, info fs.FileInfo, err error) error {
		if info.IsDir() {

			if strings.Contains(path, "/.git") {
				return nil
			}

			fmt.Println("addDirWatch dir", path)
			err := f.watcher.Add(path)

			if err != nil {
				logrus.Errorf("addDirWatch add watch %v error %v", path, err)
			}
		}
		return nil
	}); err != nil {
		logrus.Errorf("addDirWatch error %v", err)
	}
}

// Get all the folders of the file address and move them out to the monitoring list
func (f *FileExtensionSource) remoteDirWatch(addr string) {
	if err := filepath.Walk(addr, func(path string, info fs.FileInfo, err error) error {
		if info.IsDir() {

			if strings.Contains(path, "/.git") {
				return nil
			}
			fmt.Println("remoteDirWatch dir", path)

			err := f.watcher.Remove(path)

			if err != nil {
				logrus.Errorf("remoteDirWatch add watch %v error %v", path, err)
			}
		}
		return nil
	}); err != nil {
		logrus.Errorf("remoteDirWatch error %v", err)
	}
}

// Remove the monitoring list
func (f *FileExtensionSource) remove(addr string) error {
	f.remoteDirWatch(addr)
	return nil
}

// Turn on local file monitoring
func (f *FileExtensionSource) start() {
	go func() {
		for {
			select {
			case event, ok := <-f.watcher.Events:
				if !ok {
					continue
				}

				fmt.Println("event name: ", event.Name)
				fmt.Println("event op: ", event.Op.String())

				if event.Op&fsnotify.Write == fsnotify.Write {
					if !isWatchFile(event.Name) {
						continue
					}
					err := f.updateOrCreateExtension(event.Name)
					if err != nil {
						logrus.Errorf("file write watch: failed to update or create action event %v error %v", event.Name, err)
					}
				}
				if event.Op&fsnotify.Remove == fsnotify.Remove {
					if isDir(event.Name) {
						f.remoteDirWatch(event.Name)
					}
				}
				if event.Op&fsnotify.Create == fsnotify.Create {
					go func(event fsnotify.Event) {

						var matchAddr string
						for addr := range f.AddrRWLock {
							if strings.HasPrefix(event.Name, addr) {
								matchAddr = addr
							}
						}

						// Use read locks to prevent conflicts with write locks
						// Because pull may not completely update the file, it may cause problems with recursive scanning of files and adding them to monitoring
						lock := f.getAddrRLock(matchAddr)
						if lock != nil {
							lock.RLock()
							defer lock.RUnlock()
						}

						if isDir(event.Name) {
							// Add all new files to monitoring
							f.addDirWatch(event.Name)
							// Scan all files, whether there is an extension that needs to be updated
							if err := filepath.Walk(event.Name, func(path string, info fs.FileInfo, err error) error {
								if !info.IsDir() && isWatchFile(path) {
									err := f.updateOrCreateExtension(path)
									if err != nil {
										logrus.Errorf("file create watch: failed to update or create action event %v error %v", path, err)
									}
								}
								return nil
							}); err != nil {
								logrus.Errorf("watch create error %v:", err)
							}
						}

						// If it is created for update operation
						if !isWatchFile(event.Name) {
							return
						}
						err := f.updateOrCreateExtension(event.Name)
						if err != nil {
							logrus.Errorf("file create watch: failed to update or create action event %v error %v", event.Name, err)
						}
					}(event)
				}
			case err, ok := <-f.watcher.Errors:
				if !ok {
					continue
				}
				logrus.Println("file error watch:", err)
			}
		}
	}()
}

func (f *FileExtensionSource) updateOrCreateExtension(fileAddr string) error {
	version, err := NewVersion(filepath.Dir(fileAddr))
	if err != nil {
		return err
	}
	specData := version.Spec

	var request = &pb.ExtensionVersionCreateRequest{
		Name:        specData.Name,
		Version:     specData.Version,
		SpecYml:     string(version.SpecContent),
		DiceYml:     string(version.DiceContent),
		SwaggerYml:  string(version.SwaggerContent),
		Readme:      string(version.ReadmeContent),
		Public:      specData.Public,
		ForceUpdate: true,
		All:         true,
		IsDefault:   specData.IsDefault,
	}
	_, err = f.s.CreateExtensionVersionByRequest(request)
	if err != nil {
		return err
	}
	return nil
}

func isWatchFile(name string) bool {
	if strings.HasSuffix(name, "dice.yml") || strings.HasSuffix(name, "spec.yml") || strings.HasSuffix(name, "README.md") {
		return true
	}
	return false
}

func NewFileExtensionSource(s *extensionService) *FileExtensionSource {
	var fileExtensionSource FileExtensionSource
	watcher, err := fsnotify.NewWatcher()
	if err != nil {
		panic(err)
	}
	fileExtensionSource.watcher = watcher
	fileExtensionSource.s = s
	fileExtensionSource.AddrRWLock = map[string]*sync.RWMutex{}
	return &fileExtensionSource
}

func NewGitExtensionSource(cfg *config, f *FileExtensionSource) *GitExtensionSource {
	var gitExtensionSource GitExtensionSource
	gitExtensionSource.Cfg = cfg
	gitExtensionSource.f = f
	return &gitExtensionSource
}

var extensionSources []Source
var once sync.Once

func RegisterExtensionSource(source Source) {
	extensionSources = append(extensionSources, source)
}

func StartSyncExtensionSource() {
	once.Do(func() {
		for _, source := range extensionSources {
			source.start()
		}
	})
}

func AddSyncExtension(addr string) error {
	for _, source := range extensionSources {
		if source.match(addr) {
			err := source.add(addr)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func RemoveSyncExtension(addr string) error {
	for _, source := range extensionSources {
		if source.match(addr) {
			err := source.remove(addr)
			if err != nil {
				return err
			}
		}
	}
	return nil
}
