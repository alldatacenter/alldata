package server

import (
	"encoding/json"
	"fmt"
	"github.com/apex/log"
	config2 "github.com/crawlab-team/crawlab-core/config"
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/entity"
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/grpc/middlewares"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/node/config"
	grpc2 "github.com/crawlab-team/crawlab-grpc"
	"github.com/crawlab-team/go-trace"
	"github.com/grpc-ecosystem/go-grpc-middleware"
	grpc_auth "github.com/grpc-ecosystem/go-grpc-middleware/auth"
	"github.com/grpc-ecosystem/go-grpc-middleware/recovery"
	"github.com/spf13/viper"
	"go.uber.org/dig"
	"go/types"
	"google.golang.org/grpc"
	"net"
	"sync"
)

var subs = sync.Map{}

type Server struct {
	// dependencies
	nodeCfgSvc          interfaces.NodeConfigService
	nodeSvr             *NodeServer
	taskSvr             *TaskServer
	pluginSvr           *PluginServer
	messageSvr          *MessageServer
	modelDelegateSvr    *ModelDelegateServer
	modelBaseServiceSvr *ModelBaseServiceServer

	// settings
	cfgPath string
	address interfaces.Address

	// internals
	svr     *grpc.Server
	l       net.Listener
	stopped bool
}

func (svr *Server) Init() (err error) {
	// register
	if err := svr.Register(); err != nil {
		return err
	}

	return nil
}

func (svr *Server) Start() (err error) {
	// grpc server binding address
	address := svr.address.String()

	// listener
	svr.l, err = net.Listen("tcp", address)
	if err != nil {
		_ = trace.TraceError(err)
		return errors.ErrorGrpcServerFailedToListen
	}
	log.Infof("grpc server listens to %s", address)

	// start grpc server
	go func() {
		if err := svr.svr.Serve(svr.l); err != nil {
			if err == grpc.ErrServerStopped {
				return
			}
			trace.PrintError(err)
			log.Error(errors.ErrorGrpcServerFailedToServe.Error())
		}
	}()

	return nil
}

func (svr *Server) Stop() (err error) {
	// skip if listener is nil
	if svr.l == nil {
		return nil
	}

	// graceful stop
	log.Infof("grpc server stopping...")
	svr.svr.Stop()

	// close listener
	log.Infof("grpc server closing listener...")
	_ = svr.l.Close()

	// mark as stopped
	svr.stopped = true

	// log
	log.Infof("grpc server stopped")

	return nil
}

func (svr *Server) Register() (err error) {
	grpc2.RegisterModelDelegateServer(svr.svr, *svr.modelDelegateSvr)       // model delegate
	grpc2.RegisterModelBaseServiceServer(svr.svr, *svr.modelBaseServiceSvr) // model base service
	grpc2.RegisterNodeServiceServer(svr.svr, *svr.nodeSvr)                  // node service
	grpc2.RegisterTaskServiceServer(svr.svr, *svr.taskSvr)                  // task service
	grpc2.RegisterPluginServiceServer(svr.svr, *svr.pluginSvr)              // plugin service
	grpc2.RegisterMessageServiceServer(svr.svr, *svr.messageSvr)            // message service

	return nil
}

func (svr *Server) SetAddress(address interfaces.Address) {
	svr.address = address
}

func (svr *Server) GetConfigPath() (path string) {
	return svr.cfgPath
}

func (svr *Server) SetConfigPath(path string) {
	svr.cfgPath = path
}

func (svr *Server) GetSubscribe(key string) (sub interfaces.GrpcSubscribe, err error) {
	res, ok := subs.Load(key)
	if !ok {
		return nil, trace.TraceError(errors.ErrorGrpcStreamNotFound)
	}
	sub, ok = res.(interfaces.GrpcSubscribe)
	if !ok {
		return nil, trace.TraceError(errors.ErrorGrpcInvalidType)
	}
	return sub, nil
}

func (svr *Server) SetSubscribe(key string, sub interfaces.GrpcSubscribe) {
	subs.Store(key, sub)
}

func (svr *Server) DeleteSubscribe(key string) {
	subs.Delete(key)
}

func (svr *Server) SendStreamMessage(key string, code grpc2.StreamMessageCode) (err error) {
	return svr.SendStreamMessageWithData(key, code, nil)
}

func (svr *Server) SendStreamMessageWithData(key string, code grpc2.StreamMessageCode, d interface{}) (err error) {
	var data []byte
	switch d.(type) {
	case types.Nil:
		// do nothing
	case []byte:
		data = d.([]byte)
	default:
		var err error
		data, err = json.Marshal(d)
		if err != nil {
			panic(err)
		}
	}
	sub, err := svr.GetSubscribe(key)
	if err != nil {
		return err
	}
	msg := &grpc2.StreamMessage{
		Code: code,
		Key:  svr.nodeCfgSvc.GetNodeKey(),
		Data: data,
	}
	return sub.GetStream().Send(msg)
}

func (svr *Server) IsStopped() (res bool) {
	return svr.stopped
}

func (svr *Server) recoveryHandlerFunc(p interface{}) (err error) {
	err = errors.NewError(errors.ErrorPrefixGrpc, fmt.Sprintf("%v", p))
	trace.PrintError(err)
	return err
}

func NewServer(opts ...Option) (svr2 interfaces.GrpcServer, err error) {

	// server
	svr := &Server{
		cfgPath: config2.DefaultConfigPath,
		address: entity.NewAddress(&entity.AddressOptions{
			Host: constants.DefaultGrpcServerHost,
			Port: constants.DefaultGrpcServerPort,
		}),
	}

	// options
	for _, opt := range opts {
		opt(svr)
	}

	// dependency injection
	c := dig.New()
	if err := c.Provide(config.ProvideConfigService(svr.GetConfigPath())); err != nil {
		return nil, err
	}
	if err := c.Provide(NewModelDelegateServer); err != nil {
		return nil, err
	}
	if err := c.Provide(NewModelBaseServiceServer); err != nil {
		return nil, err
	}
	if err := c.Provide(ProvideNodeServer(svr)); err != nil {
		return nil, err
	}
	if err := c.Provide(ProvideTaskServer(svr)); err != nil {
		return nil, err
	}
	if err := c.Provide(ProvidePluginServer(svr)); err != nil {
		return nil, err
	}
	if err := c.Provide(ProvideMessageServer(svr)); err != nil {
		return nil, err
	}
	if err := c.Invoke(func(
		nodeCfgSvc interfaces.NodeConfigService,
		modelDelegateSvr *ModelDelegateServer,
		modelBaseServiceSvr *ModelBaseServiceServer,
		nodeSvr *NodeServer,
		taskSvr *TaskServer,
		pluginSvr *PluginServer,
		messageSvr *MessageServer,
	) {
		svr.nodeCfgSvc = nodeCfgSvc
		svr.modelDelegateSvr = modelDelegateSvr
		svr.modelBaseServiceSvr = modelBaseServiceSvr
		svr.nodeSvr = nodeSvr
		svr.taskSvr = taskSvr
		svr.pluginSvr = pluginSvr
		svr.messageSvr = messageSvr
	}); err != nil {
		return nil, err
	}

	// recovery options
	recoveryOpts := []grpc_recovery.Option{
		grpc_recovery.WithRecoveryHandler(svr.recoveryHandlerFunc),
	}

	// grpc server
	svr.svr = grpc.NewServer(
		grpc_middleware.WithUnaryServerChain(
			grpc_recovery.UnaryServerInterceptor(recoveryOpts...),
			grpc_auth.UnaryServerInterceptor(middlewares.GetAuthTokenFunc(svr.nodeCfgSvc)),
		),
		grpc_middleware.WithStreamServerChain(
			grpc_recovery.StreamServerInterceptor(recoveryOpts...),
			grpc_auth.StreamServerInterceptor(middlewares.GetAuthTokenFunc(svr.nodeCfgSvc)),
		),
	)

	// initialize
	if err := svr.Init(); err != nil {
		return nil, err
	}

	return svr, nil
}

func ProvideServer(path string, opts ...Option) func() (res interfaces.GrpcServer, err error) {
	if path == "" {
		path = config2.DefaultConfigPath
	}
	opts = append(opts, WithConfigPath(path))
	return func() (res interfaces.GrpcServer, err error) {
		return NewServer(opts...)
	}
}

var serverStore = sync.Map{}

func GetServer(path string, opts ...Option) (svr interfaces.GrpcServer, err error) {
	if path == "" {
		path = config2.DefaultConfigPath
	}
	opts = append(opts, WithConfigPath(path))

	viperServerAddress := viper.GetString("grpc.server.address")
	if viperServerAddress != "" {
		address, err := entity.NewAddressFromString(viperServerAddress)
		if err != nil {
			return nil, err
		}
		opts = append(opts, WithAddress(address))
	}

	res, ok := serverStore.Load(path)
	if ok {
		svr, ok = res.(interfaces.GrpcServer)
		if ok {
			return svr, nil
		}
	}
	svr, err = NewServer(opts...)
	if err != nil {
		return nil, err
	}
	serverStore.Store(path, svr)
	return svr, nil
}

func ProvideGetServer(path string, opts ...Option) func() (svr interfaces.GrpcServer, err error) {
	return func() (svr interfaces.GrpcServer, err error) {
		return GetServer(path, opts...)
	}
}
