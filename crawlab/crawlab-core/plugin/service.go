package plugin

import (
	"bufio"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/apex/log"
	"github.com/cenkalti/backoff/v4"
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/entity"
	errors2 "github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/grpc/server"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/client"
	"github.com/crawlab-team/crawlab-core/models/delegate"
	"github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-core/models/service"
	"github.com/crawlab-team/crawlab-core/node/config"
	"github.com/crawlab-team/crawlab-core/process"
	"github.com/crawlab-team/crawlab-core/sys_exec"
	"github.com/crawlab-team/crawlab-core/utils"
	grpc "github.com/crawlab-team/crawlab-grpc"
	vcs "github.com/crawlab-team/crawlab-vcs"
	"github.com/crawlab-team/go-trace"
	"github.com/google/uuid"
	"github.com/imroc/req"
	"github.com/joeshaw/multierror"
	"github.com/spf13/viper"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.uber.org/dig"
	"io/ioutil"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"
)

type Service struct {
	// settings variables
	fsPathBase      string
	monitorInterval time.Duration
	ps              entity.PluginSetting

	// dependencies
	cfgSvc                     interfaces.NodeConfigService
	modelSvc                   service.ModelService
	clientModelSvc             interfaces.GrpcClientModelService
	clientModelNodeSvc         interfaces.GrpcClientModelNodeService
	clientModelPluginSvc       interfaces.GrpcClientModelPluginService
	clientModelPluginStatusSvc interfaces.GrpcClientModelPluginStatusService
	svr                        interfaces.GrpcServer

	// internals
	daemonMap sync.Map
	stopped   bool
	n         *models.Node // current node
}

func (svc *Service) Init() (err error) {
	return nil
}

func (svc *Service) Start() {
	// get current node
	if err := svc.getCurrentNode(); err != nil {
		panic(err)
	}

	// get global settings
	if err := svc.getGlobalSettings(); err != nil {
		panic(err)
	}

	svc.initPlugins()
}

func (svc *Service) Wait() {
	utils.DefaultWait()
}

func (svc *Service) Stop() {
	// do nothing
}

func (svc *Service) SetFsPathBase(path string) {
	svc.fsPathBase = path
}

func (svc *Service) SetMonitorInterval(interval time.Duration) {
	svc.monitorInterval = interval
}

func (svc *Service) InstallPlugin(id primitive.ObjectID) (err error) {
	// plugin
	p, err := svc.getPluginById(id)
	if err != nil {
		return err
	}

	// plugin status
	ps, err := svc.getPluginStatus(id)
	if err != nil {
		return err
	}

	// save status (installing)
	ps.Status = constants.PluginStatusInstalling
	ps.Error = ""
	_ = svc.savePluginStatus(ps)

	// get plugin base url
	if err := svc.getGlobalSettings(); err != nil {
		return err
	}

	// install
	switch p.InstallType {
	case constants.PluginInstallTypePublic:
		err = svc.installPublic(p)
	case constants.PluginInstallTypeGit:
		err = svc.installGit(p)
	case constants.PluginInstallTypeLocal:
		err = svc.installLocal(p)
	default:
		err = errors2.ErrorPluginNotImplemented
	}
	if err != nil {
		ps.Status = constants.PluginStatusInstallError
		ps.Error = err.Error()
		return trace.TraceError(err)
	}

	// wait
	for i := 0; i < 10; i++ {
		query := bson.M{
			"plugin_id": id,
			"node_id":   svc.n.Id,
		}
		ps, err = svc.modelSvc.GetPluginStatus(query, nil)
		if err == nil {
			break
		}
		time.Sleep(1 * time.Second)
	}

	// refresh plugin
	if err := delegate.NewModelDelegate(p).Refresh(); err != nil {
		trace.PrintError(err)
	}

	if p.AutoStart {
		// start plugin
		_ = svc.StartPlugin(p.Id)

		// send start plugin messages to active worker nodes if deploy mode is all
		if p.DeployMode == constants.PluginDeployModeAll {
			_ = svc.sendStartPluginMessages(p)
		}
	} else {
		// save status (stopped)
		ps.Status = constants.PluginStatusStopped
		ps.Error = ""
		_ = svc.savePluginStatus(ps)
	}

	return nil
}

func (svc *Service) UninstallPlugin(id primitive.ObjectID) (err error) {
	// plugin
	_, err = svc.getPluginById(id)
	if err != nil {
		return err
	}

	// plugin status
	ps, err := svc.getPluginStatus(id)
	if err != nil {
		return err
	}

	// stop
	if ps.Status == constants.PluginStatusRunning {
		if err := svc.StopPlugin(id); err != nil {
			return err
		}
	}

	// delete fs (master)
	if svc.cfgSvc.IsMaster() {
		// fs service
		fsSvc, err := NewPluginFsService(id)
		if err != nil {
			return err
		}

		// delete fs
		fsPath := fsSvc.GetFsPath()
		if err := fsSvc.GetFsService().Delete(fsPath); err != nil {
			return err
		}
	}

	return nil
}

func (svc *Service) StartPlugin(id primitive.ObjectID) (err error) {
	log.Debugf("[PluginService] starting plugin[%s]", id.Hex())

	// plugin
	p, err := svc.getPluginById(id)
	if err != nil {
		return err
	}

	// check whether the plugin is allowed
	if !svc.isAllowed(p) {
		log.Debugf("[PluginService] plugin[%s] is not allowed", p.Id.Hex())
		return
	}

	// plugin status
	ps, err := svc.getPluginStatus(id)
	if err != nil {
		return err
	}

	// save pid
	ps.Pid = 0
	ps.Status = constants.PluginStatusRunning
	_ = svc.savePluginStatus(ps)

	// fs service
	fsSvc, err := NewPluginFsService(id)
	if err != nil {
		return err
	}

	// sync to workspace
	if err := svc.syncToWorkspace(fsSvc); err != nil {
		return err
	}

	// process daemon
	d := process.NewProcessDaemon(svc.getNewCmdFn(p, fsSvc))

	// add to daemon map
	svc.addDaemon(id, d)

	// run (async)
	go func() {
		// start (async)
		go func() {
			if err := d.Start(); err != nil {
				svc.handleCmdError(p, ps, err)
				return
			}
		}()

		// listening to signal from daemon
		stopped := false
		for {
			if stopped {
				break
			}
			ch := d.GetCh()
			sig := <-ch
			switch sig {
			case process.SignalStart:
				// prevent startup failure caused by cmd.Process nil
				if d.GetCmd().Process != nil {
					// save pid
					ps.Pid = d.GetCmd().Process.Pid
				} else {
					log.Warnf("Plugin process cannot get, work dir: %s, cmd: '%s'", d.GetCmd().Dir, strings.Join(d.GetCmd().Args, " "))
				}
				_ = svc.savePluginStatus(ps)
			case process.SignalStopped, process.SignalReachedMaxErrors:
				// break for loop
				stopped = true
			default:
				continue
			}
		}

		// stopped
		ps.Status = constants.PluginStatusStopped
		ps.Pid = 0
		ps.Error = ""
		if _, err := svc.getPluginStatus(p.GetId()); err != nil {
			if err.Error() != mongo.ErrNoDocuments.Error() {
				trace.PrintError(err)
			}
			return
		}
		_ = svc.savePluginStatus(ps)
	}()

	return nil
}

func (svc *Service) StopPlugin(id primitive.ObjectID) (err error) {
	var d interfaces.ProcessDaemon
	if d = svc.getDaemon(id); d == nil {
		return trace.TraceError(errors2.ErrorPluginNotExists)
	}
	d.Stop()
	svc.deleteDaemon(id)
	return nil
}

func (svc *Service) GetPublicPluginList() (res interface{}, err error) {
	// get from db cache
	fn := svc._getPublicPluginList
	s, err := utils.GetFromDbCache(constants.CacheKeyPublicPlugins, fn)
	if err != nil {
		return nil, err
	}

	// deserialize
	var repos []entity.PublicPlugin
	if err := json.Unmarshal([]byte(s), &repos); err != nil {
		return nil, err
	}

	return repos, nil
}

func (svc *Service) GetPublicPluginInfo(fullName string) (res interface{}, err error) {
	// get from db cache
	fn := func() (string, error) {
		return svc._getPublicPluginInfo(fullName)
	}
	s, err := utils.GetFromDbCache(constants.CacheKeyPublicPluginInfo+":"+fullName, fn)
	if err != nil {
		return nil, err
	}

	// deserialize
	var info bson.M
	if err := json.Unmarshal([]byte(s), &info); err != nil {
		return nil, err
	}

	return info, nil
}

func (svc *Service) installPublic(p interfaces.Plugin) (err error) {
	if utils.IsDocker() {
		p.SetInstallUrl(fmt.Sprintf("%s/%s", constants.DefaultSettingPluginBaseUrlDocker, p.GetName()))
		return svc.installLocal(p)
	} else {
		p.SetInstallUrl(fmt.Sprintf("%s/%s", svc.ps.PluginBaseUrl, p.GetFullName()))
		return svc.installGit(p)
	}
}

func (svc *Service) installGit(p interfaces.Plugin) (err error) {
	log.Infof("git installing %s", p.GetInstallUrl())

	// git clone to temporary directory
	pluginPath := filepath.Join(os.TempDir(), uuid.New().String())
	gitClient, err := vcs.CloneGitRepo(pluginPath, p.GetInstallUrl())
	if err != nil {
		return err
	}

	// plugin.json
	_p, err := svc.getPluginFromJson(pluginPath)
	if err != nil {
		return err
	}

	// set plugin name
	if p.GetFullName() != "" && _p.GetFullName() == "" {
		_p.SetFullName(p.GetFullName())
	}

	// fill plugin data and save to db
	if svc.cfgSvc.IsMaster() {
		_p.SetId(p.GetId())
		if err := svc.savePlugin(_p); err != nil {
			return err
		}
	}

	// if not in docker or non-public plugin, build plugin binary and upload to fs
	if !utils.IsDocker() || p.GetInstallType() != constants.PluginInstallTypePublic {
		if svc._buildPlugin(pluginPath, p) != nil {
			return err
		}
	}

	// dispose temporary directory
	if err := gitClient.Dispose(); err != nil {
		return err
	}

	log.Infof("git installed %s", p.GetInstallUrl())

	return nil
}

func (svc *Service) installLocal(p interfaces.Plugin) (err error) {
	log.Infof("local installing %s", p.GetInstallUrl())

	// plugin path
	pluginPath := p.GetInstallUrl()
	if strings.HasPrefix(p.GetInstallUrl(), "file://") {
		pluginPath = strings.Replace(p.GetInstallUrl(), "file://", "", 1)
		if !utils.Exists(pluginPath) {
			return trace.TraceError(errors2.ErrorPluginPathNotExists)
		}
	}

	// plugin.json
	_p, err := svc.getPluginFromJson(pluginPath)
	if err != nil {
		return err
	}
	log.Debugf("_p: %v", _p)

	// fill plugin data and save to db
	if svc.cfgSvc.IsMaster() {
		_p.SetId(p.GetId())
		_p.SetInstallUrl(p.GetInstallUrl())
		_p.SetInstallType(p.GetInstallType())
		if err := svc.savePlugin(_p); err != nil {
			return err
		}
	}

	// if not in docker or non-public plugin, build plugin binary and upload to fs
	if !utils.IsDocker() || p.GetInstallType() != constants.PluginInstallTypePublic {
		if svc._buildPlugin(pluginPath, p) != nil {
			return err
		}
	}

	log.Infof("local installed %s", p.GetInstallUrl())

	return nil
}

func (svc *Service) installRemote(p interfaces.Plugin) (err error) {
	log.Infof("remote installing %s", p.GetInstallUrl())

	// download plugin.json
	res, err := req.Get(p.GetInstallUrl())
	if err != nil {
		return trace.TraceError(err)
	}
	pluginPath := filepath.Join(os.TempDir(), uuid.New().String())
	_ = os.MkdirAll(pluginPath, os.FileMode(0766))
	if err := ioutil.WriteFile(path.Join(pluginPath, "plugin.json"), res.Bytes(), os.FileMode(0766)); err != nil {
		return trace.TraceError(err)
	}

	// plugin.json
	_p, err := svc.getPluginFromJson(pluginPath)
	if err != nil {
		return err
	}

	// set plugin name
	log.Debugf("_p: %v", _p)
	if p.GetFullName() != "" && _p.GetFullName() == "" {
		_p.SetFullName(p.GetFullName())
	}

	// fill plugin data and save to db
	if svc.cfgSvc.IsMaster() {
		_p.SetId(p.GetId())
		if err := svc.savePlugin(_p); err != nil {
			return err
		}
	}

	// sync to fs
	fsSvc, err := GetPluginFsService(p.GetId())
	if err != nil {
		return err
	}
	if err := fsSvc.GetFsService().GetFs().SyncLocalToRemote(pluginPath, fsSvc.GetFsPath()); err != nil {
		return err
	}

	log.Infof("remote installed %s", p.GetInstallUrl())

	return nil
}

func (svc *Service) getDaemon(id primitive.ObjectID) (d interfaces.ProcessDaemon) {
	res, ok := svc.daemonMap.Load(id)
	if !ok {
		return nil
	}
	d, ok = res.(interfaces.ProcessDaemon)
	if !ok {
		return nil
	}
	return d
}

func (svc *Service) addDaemon(id primitive.ObjectID, d interfaces.ProcessDaemon) {
	svc.daemonMap.Store(id, d)
}

func (svc *Service) deleteDaemon(id primitive.ObjectID) {
	svc.daemonMap.Delete(id)
}

func (svc *Service) handleCmdError(p *models.Plugin, ps *models.PluginStatus, err error) {
	trace.PrintError(err)
	ps.Status = constants.PluginStatusError
	ps.Pid = 0
	ps.Error = err.Error()
	_ = svc.savePluginStatus(ps)
	svc.deleteDaemon(p.Id)
}

func (svc *Service) getNewCmdFn(p *models.Plugin, fsSvc interfaces.PluginFsService) func() (cmd *exec.Cmd) {
	return func() (cmd *exec.Cmd) {
		// command
		if utils.IsDocker() {
			cmd = sys_exec.BuildCmd(p.DockerCmd)
			cmd.Dir = p.DockerDir
		} else {
			cmd = sys_exec.BuildCmd(p.Cmd)
			cmd.Dir = fsSvc.GetWorkspacePath()
		}

		// inherit system envs
		for _, env := range os.Environ() {
			cmd.Env = append(cmd.Env, env)
		}

		// bind all viper keys to envs
		for _, key := range viper.AllKeys() {
			value := viper.Get(key)
			_, ok := value.(string)
			if !ok {
				continue
			}
			envName := fmt.Sprintf("%s_%s", "CRAWLAB", strings.ReplaceAll(strings.ToUpper(key), ".", "_"))
			envValue := viper.GetString(key)
			env := fmt.Sprintf("%s=%s", envName, envValue)
			cmd.Env = append(cmd.Env, env)
		}

		// node key
		cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", "CRAWLAB_NODE_KEY", svc.cfgSvc.GetNodeKey()))

		// goproxy
		if svc.ps.GoProxy != "" {
			cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", "GOPROXY", svc.ps.GoProxy))
		}

		// logging
		sys_exec.ConfigureCmdLogging(cmd, func(scanner *bufio.Scanner) {
			for scanner.Scan() {
				line := fmt.Sprintf("[Plugin-%s] %s\n", p.GetName(), scanner.Text())
				_, _ = os.Stdout.WriteString(line)
			}
		})

		return cmd
	}
}

func (svc *Service) initPlugins() {
	// reset plugin status
	psList, err := svc.modelSvc.GetPluginStatusList(nil, nil)
	for _, ps := range psList {
		if ps.Status == constants.PluginStatusRunning {
			ps.Status = constants.PluginStatusError
			ps.Error = errors2.ErrorPluginMissingProcess.Error()
			ps.Pid = 0
			_ = svc.savePluginStatus(&ps)
		}
	}

	// plugins
	plugins, err := svc.modelSvc.GetPluginList(nil, nil)
	if err != nil {
		trace.PrintError(err)
		return
	}

	// restart plugins that need restart
	for _, p := range plugins {
		if p.AutoStart {
			if err := svc.StartPlugin(p.Id); err != nil {
				trace.PrintError(err)
				continue
			}
		}
	}
}

func (svc *Service) getPluginFromJson(pluginPath string) (p *models.Plugin, err error) {
	pluginJsonPath := filepath.Join(pluginPath, "plugin.json")
	if !utils.Exists(pluginJsonPath) {
		return nil, trace.TraceError(errors2.ErrorPluginPluginJsonNotExists)
	}
	pluginJsonData, err := ioutil.ReadFile(pluginJsonPath)
	if err != nil {
		return nil, trace.TraceError(err)
	}
	var _p models.Plugin
	if err := json.Unmarshal(pluginJsonData, &_p); err != nil {
		return nil, trace.TraceError(err)
	}
	return &_p, nil
}

func (svc *Service) getPluginById(id primitive.ObjectID) (p *models.Plugin, err error) {
	if svc.cfgSvc.IsMaster() {
		p, err = svc.modelSvc.GetPluginById(id)
		if err != nil {
			return nil, err
		}
		return p, nil
	} else {
		log.Debugf("[PluginService] get plugin by id[%s]", id.Hex())
		_p, err := svc.clientModelPluginSvc.GetPluginById(id)
		if err != nil {
			return nil, trace.TraceError(err)
		}
		log.Debugf("[PluginService] plugin: %v", _p)
		p, ok := _p.(*models.Plugin)
		if !ok {
			return nil, trace.TraceError(errors2.ErrorPluginInvalidType)
		}
		return p, nil
	}
}

func (svc *Service) getPluginStatus(pluginId primitive.ObjectID) (ps *models.PluginStatus, err error) {
	if svc.cfgSvc.IsMaster() {
		ps, err = svc.modelSvc.GetPluginStatus(bson.M{
			"plugin_id": pluginId,
			"node_id":   svc.n.Id,
		}, nil)
		if err != nil {
			// add if not exists
			if strings.Contains(err.Error(), mongo.ErrNoDocuments.Error()) {
				return svc.addPluginStatus(pluginId, svc.n.Id)
			}

			// error
			return nil, err
		}
		return ps, nil
	} else {
		_ps, err := svc.clientModelPluginStatusSvc.GetPluginStatus(bson.M{
			"plugin_id": pluginId,
			"node_id":   svc.n.Id,
		}, nil)
		if err != nil {
			log.Debugf("[PluginService] error: %v", err)
			// add if not exists
			if strings.Contains(err.Error(), mongo.ErrNoDocuments.Error()) {
				return svc.addPluginStatus(pluginId, svc.n.Id)
			}
		}
		log.Debugf("[PluginService] plugin status: %v", _ps)
		ps, ok := _ps.(*models.PluginStatus)
		if !ok {
			return nil, trace.TraceError(errors2.ErrorPluginInvalidType)
		}
		return ps, nil
	}
}

func (svc *Service) savePlugin(p *models.Plugin) (err error) {
	if svc.cfgSvc.IsMaster() {
		return delegate.NewModelDelegate(p).Save()
	} else {
		return client.NewModelDelegate(p).Save()
	}
}

func (svc *Service) savePluginStatus(ps interfaces.PluginStatus) (err error) {
	if svc.cfgSvc.IsMaster() {
		return delegate.NewModelDelegate(ps).Save()
	} else {
		return client.NewModelDelegate(ps).Save()
	}
}

func (svc *Service) addPluginStatus(pid primitive.ObjectID, nid primitive.ObjectID) (ps *models.PluginStatus, err error) {
	log.Debugf("[PluginService] add plugin status[pid: %s; nid: %s]", pid.Hex(), nid.Hex())
	ps = &models.PluginStatus{
		PluginId: pid,
		NodeId:   nid,
	}
	if svc.cfgSvc.IsMaster() {
		if err := delegate.NewModelDelegate(ps).Add(); err != nil {
			return nil, err
		}
	} else {
		psD := client.NewModelDelegate(ps)
		if err := psD.Add(); err != nil {
			return nil, err
		}
		ps = psD.GetModel().(*models.PluginStatus)
	}
	return ps, nil
}

func (svc *Service) getCurrentNode() (err error) {
	return backoff.RetryNotify(svc._getNode, backoff.NewExponentialBackOff(), utils.BackoffErrorNotify("plugin service get node"))
}

func (svc *Service) _getNode() (err error) {
	if svc.cfgSvc.IsMaster() {
		svc.n, err = svc.modelSvc.GetNodeByKey(svc.cfgSvc.GetNodeKey(), nil)
		if err != nil {
			return err
		}
	} else {
		_n, err := svc.clientModelNodeSvc.GetNodeByKey(svc.cfgSvc.GetNodeKey())
		if err != nil {
			return err
		}
		n, ok := _n.(*models.Node)
		if !ok {
			return trace.TraceError(errors2.ErrorPluginInvalidType)
		}
		svc.n = n
	}
	return nil
}

func (svc *Service) getGlobalSettings() (err error) {
	return backoff.RetryNotify(svc._getGlobalSettings, backoff.NewExponentialBackOff(), utils.BackoffErrorNotify("plugin service get global settings"))
}

func (svc *Service) getPublicPluginRepo(fullName string) (res bson.M, err error) {
	// http request
	url := fmt.Sprintf("https://api.github.com/repos/%s", fullName)
	r, err := req.Get(url)
	if err != nil {
		return nil, trace.TraceError(err)
	}

	// deserialize
	bytes := r.Bytes()
	if err := json.Unmarshal(bytes, &res); err != nil {
		return nil, trace.TraceError(err)
	}

	return res, nil
}

func (svc *Service) getPublicPluginPluginJson(fullName string) (res bson.M, err error) {
	// http request
	url := fmt.Sprintf("https://api.github.com/repos/%s/contents/plugin.json", fullName)
	headers := req.Header{
		"Accept": "application/vnd.github.VERSION.raw",
	}
	r, err := req.Get(url, headers)
	if err != nil {
		return nil, trace.TraceError(err)
	}

	// deserialize
	bytes := r.Bytes()
	if err := json.Unmarshal(bytes, &res); err != nil {
		return nil, trace.TraceError(err)
	}

	return res, nil
}

func (svc *Service) getPublicPluginReadme(fullName string) (res string, err error) {
	// http request
	url := fmt.Sprintf("https://api.github.com/repos/%s/contents/README.md", fullName)
	headers := req.Header{
		"Accept": "application/vnd.github.VERSION.raw",
	}
	r, err := req.Get(url, headers)
	if err != nil {
		return "", trace.TraceError(err)
	}

	return r.String(), nil
}

func (svc *Service) _getGlobalSettings() (err error) {
	if svc.cfgSvc.IsMaster() {
		s, err := svc.modelSvc.GetSettingByKey(constants.SettingPlugin, nil)
		if err != nil {
			if err.Error() == mongo.ErrNoDocuments.Error() {
				svc.ps = entity.PluginSetting{
					PluginBaseUrl:   constants.DefaultSettingPluginBaseUrlGitHub,
					GithubPublicOrg: constants.DefaultSettingPluginGithubPublicOrg,
					RepoPrefix:      constants.DefaultSettingPluginRepoPrefix,
				}
				s := &models.Setting{
					Key:   constants.SettingPlugin,
					Value: svc.ps.Value(),
				}
				if err := delegate.NewModelDelegate(s).Add(); err != nil {
					return err
				}
				return nil
			}
			return err
		}
		svc.ps = entity.NewPluginSetting(s.Value)
		return nil
	} else {
		var settingModelSvc interfaces.GrpcClientModelBaseService
		if err := backoff.Retry(func() error {
			if svc.clientModelSvc == nil {
				return errors.New("clientModelSvc is nil")
			}
			settingModelSvc, err = svc.clientModelSvc.NewBaseServiceDelegate(interfaces.ModelIdSetting)
			if err != nil {
				return err
			}
			return nil
		}, backoff.NewConstantBackOff(1*time.Second)); err != nil {
			return trace.TraceError(err)
		}
		_s, err := settingModelSvc.Get(bson.M{"key": constants.SettingPlugin}, nil)
		if err != nil {
			return err
		}
		s, ok := _s.(*models.Setting)
		if !ok {
			return trace.TraceError(errors2.ErrorPluginInvalidType)
		}
		svc.ps = entity.NewPluginSetting(s.Value)
		return nil
	}
}

func (svc *Service) _getPublicPluginList() (res string, err error) {
	// http request
	url := fmt.Sprintf("%s/plugins.json", constants.DefaultSettingPluginBaseUrl)
	r, err := req.Get(url)
	if err != nil {
		return "", trace.TraceError(err)
	}

	// deserialize
	var allRepos []entity.PublicPlugin
	bytes := r.Bytes()
	if err := json.Unmarshal(bytes, &allRepos); err != nil {
		return "", trace.TraceError(err)
	}

	// filter
	var repos []entity.PublicPlugin
	for _, repo := range allRepos {
		if strings.HasPrefix(repo.Name, svc.ps.RepoPrefix) {
			repos = append(repos, repo)
		}
	}

	// serialize
	resBytes, err := json.Marshal(repos)
	if err != nil {
		return "", trace.TraceError(err)
	}

	return string(resBytes), nil
}

func (svc *Service) _getPublicPluginInfo(fullName string) (res string, err error) {
	if err := svc.getGlobalSettings(); err != nil {
		return "", err
	}

	// wait group
	wg := sync.WaitGroup{}
	wg.Add(3)

	// errors
	var errs multierror.Errors

	// repo
	var repo interface{}
	go func() {
		var err error
		repo, err = svc.getPublicPluginRepo(fullName)
		if err != nil {
			errs = append(errs, err)
		}
		wg.Done()
	}()

	// plugin.json
	var pluginJson interface{}
	go func() {
		var err error
		pluginJson, err = svc.getPublicPluginPluginJson(fullName)
		if err != nil {
			errs = append(errs, err)
		}
		wg.Done()
	}()

	// readme
	var readme string
	go func() {
		var err error
		readme, err = svc.getPublicPluginReadme(fullName)
		if err != nil {
			errs = append(errs, err)
		}
		wg.Done()
	}()

	// wait
	wg.Wait()

	// has errors
	if len(errs) > 0 {
		return "", errs.Err()
	}

	// res data
	resData := bson.M{
		"repo":       repo,
		"pluginJson": pluginJson,
		"readme":     readme,
	}

	// serialize
	resBytes, err := json.Marshal(resData)
	if err != nil {
		return "", err
	}

	return string(resBytes), nil
}

func (svc *Service) sendStartPluginMessages(p *models.Plugin) (err error) {
	if !svc.cfgSvc.IsMaster() || svc.svr == nil {
		return trace.TraceError(errors2.ErrorPluginForbidden)
	}
	log.Infof("[PluginService] sending start plugin messages")
	ns, err := svc.modelSvc.GetNodeList(bson.M{
		"is_master": false,
		"active":    true,
	}, nil)
	if err != nil {
		return err
	}
	log.Infof(fmt.Sprintf("[PluginService] worker nodes: %d", len(ns)))
	for _, n := range ns {
		if err := svc.svr.SendStreamMessageWithData("node:"+n.Key, grpc.StreamMessageCode_START_PLUGIN, p); err != nil {
			trace.PrintError(err)
		}
	}
	return nil
}

func (svc *Service) syncToWorkspace(fsSvc interfaces.PluginFsService) (err error) {
	op := func() error {
		return svc._syncToWorkspace(fsSvc)
	}
	b := backoff.NewExponentialBackOff()
	b.MaxElapsedTime = 5 * time.Minute
	return backoff.Retry(op, b)
}

func (svc *Service) _syncToWorkspace(fsSvc interfaces.PluginFsService) (err error) {
	if err := fsSvc.GetFsService().SyncToWorkspace(); err != nil {
		return err
	}
	return nil
}

func (svc *Service) isAllowed(p *models.Plugin) (ok bool) {
	return p.DeployMode == constants.PluginDeployModeAll || svc.cfgSvc.IsMaster()
}

func (svc *Service) _buildPlugin(pluginPath string, p interfaces.Plugin) (err error) {
	// install command
	installCmd := p.GetInstallCmd()
	if installCmd == "" {
		// windows 系统支持
		if runtime.GOOS == "windows" {
			installCmd = DefaultWindowsPluginInstallCmd
		} else {
			installCmd = DefaultPluginInstallCmd
		}
	}

	// build on local
	cmd := sys_exec.BuildCmd(installCmd)

	// current directory
	cmd.Dir = pluginPath

	// run
	if err := cmd.Run(); err != nil {
		return err
	}

	// sync to fs
	fsSvc, err := GetPluginFsService(p.GetId())
	if err != nil {
		return err
	}
	if err := fsSvc.GetFsService().GetFs().SyncLocalToRemote(pluginPath, fsSvc.GetFsPath()); err != nil {
		return err
	}

	return nil
}

func NewPluginService(opts ...Option) (svc2 interfaces.PluginService, err error) {
	// service
	svc := &Service{
		fsPathBase:      DefaultPluginFsPathBase,
		monitorInterval: 15 * time.Second,
		daemonMap:       sync.Map{},
	}

	// apply options
	for _, opt := range opts {
		opt(svc)
	}

	// dependency injection
	c := dig.New()
	if err := c.Provide(config.NewNodeConfigService); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(service.GetService); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(client.NewNodeServiceDelegate); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(client.NewPluginServiceDelegate); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(client.NewPluginStatusServiceDelegate); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Invoke(func(
		cfgSvc interfaces.NodeConfigService,
		modelSvc service.ModelService,
		clientModelNodeSvc interfaces.GrpcClientModelNodeService,
		clientModelPluginSvc interfaces.GrpcClientModelPluginService,
		clientModelPluginStatusSvc interfaces.GrpcClientModelPluginStatusService,
	) {
		svc.cfgSvc = cfgSvc
		svc.modelSvc = modelSvc
		svc.clientModelNodeSvc = clientModelNodeSvc
		svc.clientModelPluginSvc = clientModelPluginSvc
		svc.clientModelPluginStatusSvc = clientModelPluginStatusSvc
	}); err != nil {
		return nil, err
	}

	// grpc server (master node only)
	if svc.cfgSvc.IsMaster() {
		svc.svr, err = server.GetServer(svc.cfgSvc.GetConfigPath())
		if err != nil {
			return nil, err
		}
	}

	// initialize
	if err := svc.Init(); err != nil {
		return nil, err
	}

	return svc, nil
}

var store = sync.Map{}

func GetPluginService(path string, opts ...Option) (svc interfaces.PluginService, err error) {
	// return if service exists
	res, ok := store.Load(path)
	if ok {
		svc, ok = res.(interfaces.PluginService)
		if ok {
			return svc, nil
		}
	}

	// service
	svc, err = NewPluginService(opts...)
	if err != nil {
		return nil, err
	}

	// save to cache
	store.Store(path, svc)

	return svc, nil
}

func ProvideGetPluginService(path string, opts ...Option) func() (svr interfaces.PluginService, err error) {
	return func() (svr interfaces.PluginService, err error) {
		return GetPluginService(path, opts...)
	}
}
