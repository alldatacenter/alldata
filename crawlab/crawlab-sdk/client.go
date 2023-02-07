package sdk

import (
	"context"
	"github.com/apex/log"
	"github.com/cenkalti/backoff/v4"
	grpc2 "github.com/crawlab-team/crawlab-grpc"
	"github.com/crawlab-team/crawlab-sdk/entity"
	"github.com/crawlab-team/crawlab-sdk/interfaces"
	"github.com/crawlab-team/go-trace"
	"google.golang.org/grpc"
	"os"
	"time"
)

var C *Client

type Client struct {
	// settings
	address *entity.Address
	timeout time.Duration

	// internals
	conn *grpc.ClientConn

	// dependencies
	ModelDelegateClient    grpc2.ModelDelegateClient
	ModelBaseServiceClient grpc2.ModelBaseServiceClient
	NodeClient             grpc2.NodeServiceClient
	TaskClient             grpc2.TaskServiceClient
	PluginClient           grpc2.PluginServiceClient
}

func (c *Client) GetModelDelegateClient() grpc2.ModelDelegateClient {
	return c.ModelDelegateClient
}

func (c *Client) GetModelBaseServiceClient() grpc2.ModelBaseServiceClient {
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

func (c *Client) init() (err error) {
	// connect
	op := c.connect
	b := backoff.NewExponentialBackOff()
	notify := func(err error, duration time.Duration) {
		log.Errorf("init client error: %v, re-attempt in %.1f seconds", err, duration.Seconds())
	}
	if err := backoff.RetryNotify(op, b, notify); err != nil {
		return trace.TraceError(err)
	}

	// register
	if err := c.register(); err != nil {
		return err
	}

	return nil
}

func (c *Client) connect() (err error) {
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
	opts = append(opts, grpc.WithChainUnaryInterceptor(GetAuthTokenUnaryChainInterceptor()))
	opts = append(opts, grpc.WithChainStreamInterceptor(GetAuthTokenStreamChainInterceptor()))
	c.conn, err = grpc.DialContext(ctx, address, opts...)
	if err != nil {
		return trace.TraceError(err)
	}

	return nil
}

func (c *Client) register() (err error) {
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

	return nil
}

func GetClient(opts ...ClientOption) interfaces.Client {
	if C != nil {
		return C
	}

	// address
	address, err := entity.NewAddressFromString(os.Getenv("CRAWLAB_GRPC_ADDRESS"))
	if err != nil {
		address = entity.NewAddress(&entity.AddressOptions{
			Host: os.Getenv("CRAWLAB_GRPC_ADDRESS_HOST"),
			Port: os.Getenv("CRAWLAB_GRPC_ADDRESS_PORT"),
		})
	}

	// client
	client := &Client{
		address: address,
		timeout: 10 * time.Second,
	}

	// apply options
	for _, opt := range opts {
		opt(client)
	}

	// initialize
	if err := client.init(); err != nil {
		panic(err)
	}

	C = client

	return client
}
