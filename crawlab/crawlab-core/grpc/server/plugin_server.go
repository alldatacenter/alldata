package server

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/apex/log"
	"github.com/cenkalti/backoff/v4"
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/entity"
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/event"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/service"
	"github.com/crawlab-team/crawlab-core/node/config"
	"github.com/crawlab-team/crawlab-core/utils"
	grpc "github.com/crawlab-team/crawlab-grpc"
	"github.com/crawlab-team/go-trace"
	"go.uber.org/dig"
	"io"
	"strings"
)

type PluginServer struct {
	grpc.UnimplementedPluginServiceServer

	// dependencies
	modelSvc service.ModelService
	cfgSvc   interfaces.NodeConfigService
	eventSvc interfaces.EventService

	// internals
	server interfaces.GrpcServer
}

func (svr PluginServer) Register(ctx context.Context, req *grpc.PluginRequest) (res *grpc.Response, err error) {
	// unmarshall data
	var msg entity.GrpcEventServiceMessage
	if req.Data != nil {
		if err := json.Unmarshal(req.Data, &msg); err != nil {
			return HandleError(err)
		}
	}

	switch msg.Type {
	case constants.GrpcEventServiceTypeRegister:
		ch := make(chan interfaces.EventData)
		p, err := svr.modelSvc.GetPluginByName(req.Name)
		if err != nil {
			return nil, trace.TraceError(err)
		}
		svr.eventSvc.Register("plugin:"+req.Name+":"+req.NodeKey, p.EventKey.Include, p.EventKey.Exclude, &ch)
		go svr.handleEvent(req.Name, req.NodeKey, &ch)
	default:
		return nil, trace.TraceError(errors.ErrorEventUnknownAction)
	}

	return HandleSuccess()
}

func (svr PluginServer) Subscribe(req *grpc.PluginRequest, stream grpc.PluginService_SubscribeServer) (err error) {
	log.Infof("[PluginServer] master received subscribe req from plugin[%s]", req.Name)

	// finished channel
	finished := make(chan bool)

	// set subscribe
	svr.server.SetSubscribe("plugin:"+req.Name+":"+req.NodeKey, &entity.GrpcSubscribe{
		Stream:   stream,
		Finished: finished,
	})
	ctx := stream.Context()

	log.Infof("[PluginServer] master subscribed plugin[%s]", req.Name)

	// Keep this scope alive because once this scope exits - the stream is closed
	for {
		select {
		case <-finished:
			log.Infof("[PluginServer] closing stream for plugin[%s]", req.Name)
			svr.eventSvc.Unregister("plugin:" + req.Name)
			return nil
		case <-ctx.Done():
			log.Infof("[PluginServer] plugin[%s] has disconnected", req.Name)
			svr.eventSvc.Unregister("plugin:" + req.Name)
			return nil
		}
	}
}

func (svr PluginServer) Poll(stream grpc.PluginService_PollServer) (err error) {
	finished := make(chan bool)
	for {
		msg, err := stream.Recv()
		if err == io.EOF {
			return nil
		}
		if err != nil {
			return err
		}
		switch msg.Code {
		case grpc.StreamMessageCode_CONNECT:
			svr.server.SetSubscribe("plugin:"+":"+msg.NodeKey, &entity.GrpcSubscribe{
				Stream:   stream,
				Finished: finished,
			})
		}
	}
}

func (svr PluginServer) deserialize(msg *grpc.StreamMessage) (data entity.StreamMessageTaskData, err error) {
	if err := json.Unmarshal(msg.Data, &data); err != nil {
		return data, trace.TraceError(err)
	}
	if data.TaskId.IsZero() {
		return data, trace.TraceError(errors.ErrorGrpcInvalidType)
	}
	return data, nil
}

// handleEvent receives events from channel and send to plugins
func (svr PluginServer) handleEvent(pluginName, nodeKey string, ch *chan interfaces.EventData) {
	op := func() error {
		sub, err := svr.server.GetSubscribe("plugin:" + pluginName + ":" + nodeKey)
		if err != nil {
			return trace.TraceError(err)
		}
		for {
			// model data
			eventData := <-*ch
			vData, err := json.Marshal(eventData.GetData())
			if err != nil {
				trace.PrintError(err)
				continue
			}

			// service message
			svcMsg := &entity.GrpcEventServiceMessage{
				Type:   constants.GrpcEventServiceTypeSend,
				Events: []string{eventData.GetEvent()},
				Data:   vData,
			}

			// serialize
			data, err := json.Marshal(svcMsg)
			if err != nil {
				trace.PrintError(err)
				continue
			}

			// stream message
			msg := &grpc.StreamMessage{
				Code: grpc.StreamMessageCode_SEND_EVENT,
				Data: data,
			}

			// send
			if err := sub.GetStream().Send(msg); err != nil {
				if strings.HasSuffix(err.Error(), "transport is closing") {
					return trace.TraceError(err)
				}
				trace.PrintError(err)
			}
			utils.LogDebug(fmt.Sprintf("msg: %v", msg))
		}
	}
	if err := backoff.Retry(op, backoff.NewExponentialBackOff()); err != nil {
		trace.PrintError(err)
	}
}

func NewPluginServer(opts ...PluginServerOption) (res *PluginServer, err error) {
	// plugin server
	svr := &PluginServer{}

	// apply options
	for _, opt := range opts {
		opt(svr)
	}

	// dependency injection
	c := dig.New()
	if err := c.Provide(service.NewService); err != nil {
		return nil, err
	}
	if err := c.Provide(config.ProvideConfigService(svr.server.GetConfigPath())); err != nil {
		return nil, err
	}
	if err := c.Provide(event.NewEventService); err != nil {
		return nil, err
	}
	if err := c.Invoke(func(
		modelSvc service.ModelService,
		cfgSvc interfaces.NodeConfigService,
		eventSvc interfaces.EventService,
	) {
		svr.modelSvc = modelSvc
		svr.cfgSvc = cfgSvc
		svr.eventSvc = eventSvc
	}); err != nil {
		return nil, err
	}

	return svr, nil
}

func ProvidePluginServer(server interfaces.GrpcServer, opts ...PluginServerOption) func() (res *PluginServer, err error) {
	return func() (*PluginServer, error) {
		opts = append(opts, WithServerPluginServerService(server))
		return NewPluginServer(opts...)
	}
}
