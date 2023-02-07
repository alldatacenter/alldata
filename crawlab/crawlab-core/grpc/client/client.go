package client

import (
	"context"
	"encoding/json"
	"github.com/apex/log"
	"github.com/cenkalti/backoff/v4"
	config2 "github.com/crawlab-team/crawlab-core/config"
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/entity"
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/grpc/middlewares"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/node/config"
	"github.com/crawlab-team/crawlab-core/utils"
	grpc2 "github.com/crawlab-team/crawlab-grpc"
	"github.com/crawlab-team/go-trace"
	"github.com/spf13/viper"
	"go.uber.org/dig"
	"google.golang.org/grpc"
	"google.golang.org/grpc/connectivity"
	"io"
	"os"
	"sync"
	"time"
)

type Client struct {
	// dependencies
	nodeCfgSvc interfaces.NodeConfigService

	// settings
	cfgPath       string
	address       interfaces.Address
	timeout       time.Duration
	subscribeType string
	handleMessage bool

	// internals
	conn   *grpc.ClientConn
	stream grpc2.NodeService_SubscribeClient
	msgCh  chan *grpc2.StreamMessage
	err    error

	// grpc clients
	ModelDelegateClient    grpc2.ModelDelegateClient
	ModelBaseServiceClient grpc2.ModelBaseServiceClient
	NodeClient             grpc2.NodeServiceClient
	TaskClient             grpc2.TaskServiceClient
	PluginClient           grpc2.PluginServiceClient
	MessageClient          grpc2.MessageServiceClient
}

func (c *Client) Init() (err error) {
	// do nothing
	return nil
}

func (c *Client) Start() (err error) {
	// connect
	if err := c.connect(); err != nil {
		return err
	}

	// register rpc services
	if err := c.Register(); err != nil {
		return err
	}

	// subscribe
	if err := c.subscribe(); err != nil {
		return err
	}

	// handle stream message
	if c.handleMessage {
		go c.handleStreamMessage()
	}

	return nil
}

func (c *Client) Stop() (err error) {
	// skip if connection is nil
	if c.conn == nil {
		return nil
	}

	// grpc server address
	address := c.address.String()

	// unsubscribe
	if err := c.unsubscribe(); err != nil {
		return err
	}
	log.Infof("grpc client unsubscribed from %s", address)

	// close connection
	if err := c.conn.Close(); err != nil {
		return err
	}
	log.Infof("grpc client disconnected from %s", address)

	return nil
}

func (c *Client) Register() (err error) {
	// model delegate
	c.ModelDelegateClient = grpc2.NewModelDelegateClient(c.conn)

	// model base service
	c.ModelBaseServiceClient = grpc2.NewModelBaseServiceClient(c.conn)

	// node
	c.NodeClient = grpc2.NewNodeServiceClient(c.conn)

	// task
	c.TaskClient = grpc2.NewTaskServiceClient(c.conn)

	// plugin
	c.PluginClient = grpc2.NewPluginServiceClient(c.conn)

	// message
	c.MessageClient = grpc2.NewMessageServiceClient(c.conn)

	// log
	log.Infof("[GrpcClient] grpc client registered client services")
	log.Debugf("[GrpcClient] ModelDelegateClient: %v", c.ModelDelegateClient)
	log.Debugf("[GrpcClient] ModelBaseServiceClient: %v", c.ModelBaseServiceClient)
	log.Debugf("[GrpcClient] NodeClient: %v", c.NodeClient)
	log.Debugf("[GrpcClient] TaskClient: %v", c.TaskClient)
	log.Debugf("[GrpcClient] PluginClient: %v", c.PluginClient)
	log.Debugf("[GrpcClient] MessageClient: %v", c.MessageClient)

	return nil
}

func (c *Client) GetModelDelegateClient() (res grpc2.ModelDelegateClient) {
	return c.ModelDelegateClient
}

func (c *Client) GetModelBaseServiceClient() (res grpc2.ModelBaseServiceClient) {
	return c.ModelBaseServiceClient
}

func (c *Client) GetNodeClient() grpc2.NodeServiceClient {
	return c.NodeClient
}

func (c *Client) GetTaskClient() grpc2.TaskServiceClient {
	return c.TaskClient
}

func (c *Client) GetPluginClient() grpc2.PluginServiceClient {
	return c.PluginClient
}

func (c *Client) GetMessageClient() grpc2.MessageServiceClient {
	return c.MessageClient
}

func (c *Client) SetAddress(address interfaces.Address) {
	c.address = address
}

func (c *Client) SetTimeout(timeout time.Duration) {
	c.timeout = timeout
}

func (c *Client) SetSubscribeType(value string) {
	c.subscribeType = value
}

func (c *Client) SetHandleMessage(handleMessage bool) {
	c.handleMessage = handleMessage
}

func (c *Client) Context() (ctx context.Context, cancel context.CancelFunc) {
	return context.WithTimeout(context.Background(), c.timeout)
}

func (c *Client) NewRequest(d interface{}) (req *grpc2.Request) {
	return &grpc2.Request{
		NodeKey: c.nodeCfgSvc.GetNodeKey(),
		Data:    c.getRequestData(d),
	}
}

func (c *Client) NewPluginRequest(d interface{}) (req *grpc2.PluginRequest) {
	return &grpc2.PluginRequest{
		Name:    os.Getenv("CRAWLAB_PLUGIN_NAME"),
		NodeKey: c.nodeCfgSvc.GetNodeKey(),
		Data:    c.getRequestData(d),
	}
}

func (c *Client) GetConfigPath() (path string) {
	return c.cfgPath
}

func (c *Client) SetConfigPath(path string) {
	c.cfgPath = path
}

func (c *Client) NewModelBaseServiceRequest(id interfaces.ModelId, params interfaces.GrpcBaseServiceParams) (req *grpc2.Request, err error) {
	data, err := json.Marshal(params)
	if err != nil {
		return nil, trace.TraceError(err)
	}
	msg := &entity.GrpcBaseServiceMessage{
		ModelId: id,
		Data:    data,
	}
	return c.NewRequest(msg), nil
}

func (c *Client) GetMessageChannel() (msgCh chan *grpc2.StreamMessage) {
	return c.msgCh
}

func (c *Client) Restart() (err error) {
	if c.needRestart() {
		return c.Start()
	}
	return nil
}

func (c *Client) IsStarted() (res bool) {
	return c.conn != nil
}

func (c *Client) IsClosed() (res bool) {
	if c.conn != nil {
		return c.conn.GetState() == connectivity.Shutdown
	}
	return false
}

func (c *Client) Err() (err error) {
	return c.err
}

func (c *Client) GetStream() (stream grpc2.NodeService_SubscribeClient) {
	return c.stream
}

func (c *Client) connect() (err error) {
	return backoff.RetryNotify(c._connect, backoff.NewExponentialBackOff(), utils.BackoffErrorNotify("grpc client connect"))
}

func (c *Client) _connect() (err error) {
	// grpc server address
	address := c.address.String()

	// timeout context
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	// connection
	// TODO: configure dial options
	var opts []grpc.DialOption
	opts = append(opts, grpc.WithInsecure())
	opts = append(opts, grpc.WithBlock())
	opts = append(opts, grpc.WithChainUnaryInterceptor(middlewares.GetAuthTokenUnaryChainInterceptor(c.nodeCfgSvc)))
	opts = append(opts, grpc.WithChainStreamInterceptor(middlewares.GetAuthTokenStreamChainInterceptor(c.nodeCfgSvc)))
	c.conn, err = grpc.DialContext(ctx, address, opts...)
	if err != nil {
		_ = trace.TraceError(err)
		return errors.ErrorGrpcClientFailedToStart
	}
	log.Infof("[GrpcClient] grpc client connected to %s", address)

	return nil
}

func (c *Client) subscribe() (err error) {
	var op func() error
	switch c.subscribeType {
	case constants.GrpcSubscribeTypeNode:
		op = c._subscribeNode
	case constants.GrpcSubscribeTypePlugin:
		op = c._subscribePlugin
	default:
		return errors.ErrorGrpcInvalidType
	}
	return backoff.RetryNotify(op, backoff.NewExponentialBackOff(), utils.BackoffErrorNotify("grpc client subscribe"))
}

func (c *Client) _subscribeNode() (err error) {
	req := c.NewRequest(&entity.NodeInfo{
		Key:      c.nodeCfgSvc.GetNodeKey(),
		IsMaster: false,
	})
	c.stream, err = c.GetNodeClient().Subscribe(context.Background(), req)
	if err != nil {
		return trace.TraceError(err)
	}

	// log
	log.Infof("[GrpcClient] grpc client subscribed to remote server")

	return nil
}

func (c *Client) _subscribePlugin() (err error) {
	req := c.NewPluginRequest(nil)
	c.stream, err = c.GetPluginClient().Subscribe(context.Background(), req)
	if err != nil {
		return trace.TraceError(err)
	}

	// log
	log.Infof("[GrpcClient] grpc client subscribed to remote server")

	return nil
}

func (c *Client) unsubscribe() (err error) {
	req := c.NewRequest(&entity.NodeInfo{
		Key:      c.nodeCfgSvc.GetNodeKey(),
		IsMaster: false,
	})
	if _, err = c.GetNodeClient().Unsubscribe(context.Background(), req); err != nil {
		return trace.TraceError(err)
	}
	return nil
}

func (c *Client) handleStreamMessage() {
	log.Infof("[GrpcClient] start handling stream message...")
	for {
		// resubscribe if stream is set to nil
		if c.stream == nil {
			if err := backoff.RetryNotify(c.subscribe, backoff.NewExponentialBackOff(), utils.BackoffErrorNotify("grpc client subscribe")); err != nil {
				log.Errorf("subscribe")
				return
			}
		}

		// receive stream message
		msg, err := c.stream.Recv()
		log.Debugf("[GrpcClient] received message: %v", msg)
		if err != nil {
			// set error
			c.err = err

			// end
			if err == io.EOF {
				log.Infof("[GrpcClient] received EOF signal, disconnecting")
				return
			}

			// connection closed
			if c.IsClosed() {
				return
			}

			// error
			trace.PrintError(err)
			c.stream = nil
			time.Sleep(1 * time.Second)
			continue
		}

		// send stream message to channel
		c.msgCh <- msg

		// reset error
		c.err = nil
	}
}

func (c *Client) needRestart() bool {
	switch c.conn.GetState() {
	case connectivity.Shutdown, connectivity.TransientFailure:
		return true
	case connectivity.Idle, connectivity.Connecting, connectivity.Ready:
		return false
	default:
		return false
	}
}

func (c *Client) getRequestData(d interface{}) (data []byte) {
	if d == nil {
		return data
	}
	switch d.(type) {
	case []byte:
		data = d.([]byte)
	default:
		var err error
		data, err = json.Marshal(d)
		if err != nil {
			panic(err)
		}
	}
	return data
}

func NewClient(opts ...Option) (res interfaces.GrpcClient, err error) {
	// client
	client := &Client{
		address: entity.NewAddress(&entity.AddressOptions{
			Host: constants.DefaultGrpcClientRemoteHost,
			Port: constants.DefaultGrpcClientRemotePort,
		}),
		timeout:       10 * time.Second,
		msgCh:         make(chan *grpc2.StreamMessage),
		subscribeType: constants.GrpcSubscribeTypeNode,
		handleMessage: true,
	}

	// apply options
	for _, opt := range opts {
		opt(client)
	}

	// dependency injection
	c := dig.New()
	if err := c.Provide(config.ProvideConfigService(client.GetConfigPath())); err != nil {
		return nil, err
	}
	if err := c.Invoke(func(nodeCfgSvc interfaces.NodeConfigService) {
		client.nodeCfgSvc = nodeCfgSvc
	}); err != nil {
		return nil, err
	}

	// init
	if err := client.Init(); err != nil {
		return nil, err
	}

	return client, nil
}

func ProvideClient(path string, opts ...Option) func() (res interfaces.GrpcClient, err error) {
	if path == "" {
		path = config2.DefaultConfigPath
	}
	opts = append(opts, WithConfigPath(path))
	return func() (res interfaces.GrpcClient, err error) {
		return NewClient(opts...)
	}
}

var clientStore = sync.Map{}

func GetClient(path string, opts ...Option) (c interfaces.GrpcClient, err error) {
	// normalize path
	if path == "" {
		path = config2.DefaultConfigPath
	}

	log.Debugf("[GetClient] path: %s", path)
	res, ok := clientStore.Load(path)
	if !ok {
		return createClient(path, opts...)
	}
	c, ok = res.(interfaces.GrpcClient)
	if !ok {
		return createClient(path, opts...)
	}
	return c, nil
}

func ForceGetClient(path string, opts ...Option) (p interfaces.GrpcClient, err error) {
	return createClient(path, opts...)
}

func createClient(path string, opts ...Option) (client2 interfaces.GrpcClient, err error) {
	viperAddress := viper.GetString("grpc.address")
	if viperAddress != "" {
		address, err := entity.NewAddressFromString(viperAddress)
		if err != nil {
			return nil, err
		}
		opts = append(opts, WithAddress(address))
	}

	viperCfgPath := viper.GetString("config.path")
	if viperCfgPath != "" {
		opts = append(opts, WithConfigPath(viperCfgPath))
	}

	c := dig.New()
	if err := c.Provide(ProvideClient(path, opts...)); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Invoke(func(client interfaces.GrpcClient) {
		client2 = client
	}); err != nil {
		return nil, trace.TraceError(err)
	}
	clientStore.Store(path, client2)
	return client2, nil
}

func ProvideGetClient(path string, opts ...Option) func() (res interfaces.GrpcClient, err error) {
	return func() (res interfaces.GrpcClient, err error) {
		return GetClient(path, opts...)
	}
}
