package notification

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/apex/log"
	"github.com/cenkalti/backoff/v4"
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/entity"
	"github.com/crawlab-team/crawlab-core/grpc/client"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/delegate"
	"github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-core/models/service"
	"github.com/crawlab-team/crawlab-core/node/config"
	"github.com/crawlab-team/crawlab-core/utils"
	mongo2 "github.com/crawlab-team/crawlab-db/mongo"
	grpc "github.com/crawlab-team/crawlab-grpc"
	"github.com/crawlab-team/go-trace"
	parser "github.com/crawlab-team/template-parser"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.uber.org/dig"
	"io"
	"os"
	"strings"
	"time"
)

type Service struct {
	cfgSvc   interfaces.NodeConfigService
	c        interfaces.GrpcClient
	col      *mongo2.Col // notification settings
	modelSvc service.ModelService
	stream   grpc.PluginService_SubscribeClient
}

func (svc *Service) Init() (err error) {
	// handle events
	go svc.handleEvents()

	return nil
}

func (svc *Service) Start() (err error) {
	// init plugin data
	if err := svc.initPluginData(); err != nil {
		return err
	}

	// start grpc client
	if !svc.c.IsStarted() {
		if err := svc.c.Start(); err != nil {
			return err
		}
	}

	// connect
	if err := svc.connect(); err != nil {
		return err
	}

	// register
	if err := svc.subscribe(); err != nil {
		return err
	}

	// initialize data
	if err := svc.initData(); err != nil {
		return err
	}

	return nil
}

func (svc *Service) Stop() (err error) {
	return nil
}

func (svc *Service) connect() (err error) {
	if err := backoff.Retry(svc._connect, backoff.NewExponentialBackOff()); err != nil {
		return err
	}
	return nil
}

func (svc *Service) _connect() (err error) {
	stream, err := svc.c.GetMessageClient().Connect(context.Background())
	if err != nil {
		return err
	}
	msg := &grpc.StreamMessage{
		Code:    grpc.StreamMessageCode_CONNECT,
		NodeKey: svc.cfgSvc.GetNodeKey(),
		Key:     svc.getStreamMessagePrefix() + svc.cfgSvc.GetNodeKey(),
	}
	if err := stream.Send(msg); err != nil {
		return err
	}
	svc.stream = stream
	return nil
}

func (svc *Service) subscribe() (err error) {
	if err := backoff.Retry(svc._subscribe, backoff.NewExponentialBackOff()); err != nil {
		return err
	}
	return nil
}

func (svc *Service) _subscribe() (err error) {
	log.Infof("subscribe events")

	// request request data
	data, err := json.Marshal(entity.GrpcEventServiceMessage{
		Type: constants.GrpcEventServiceTypeRegister,
	})
	if err != nil {
		return trace.TraceError(err)
	}

	// register request
	req := &grpc.PluginRequest{
		Name:    PluginName,
		NodeKey: svc.cfgSvc.GetNodeKey(),
		Data:    data,
	}

	// register
	_, err = svc.c.GetPluginClient().Register(context.Background(), req)
	if err != nil {
		return trace.TraceError(err)
	}

	return
}

func (svc *Service) initPluginData() (err error) {
	op := func() error {
		if _, err := svc.modelSvc.GetPluginByName(PluginName); err != nil {
			// error
			if err.Error() != mongo.ErrNoDocuments.Error() {
				return err
			}

			// not exists, add
			pluginData := []byte(`{
  "name": "notification",
  "short_name": "plugin-notification",
  "full_name": "crawlab-team/plugin-notification",
  "description": "A plugin for handling notifications",
  "proto": "http",
  "cmd": "sh ./bin/start.sh",
  "docker_cmd": "/app/plugins/bin/plugin-notification",
  "docker_dir": "/app/plugins/plugin-notification",
  "endpoint": "localhost:39999",
  "event_key": {
    "include": "^model:",
    "exclude": "artifact"
  },
  "install_url": "https://github.com/crawlab-team/plugin-notification",
  "deploy_mode": "master_only",
  "auto_start": true,
  "lang_url": "ui/lang",
  "ui_components": [
    {
      "name": "notification-list",
      "title": "Notifications",
      "src": "ui/src/NotificationList.vue",
      "type": "view",
      "path": "notifications"
    },
    {
      "name": "notification-detail",
      "title": "Notifications",
      "src": "ui/src/NotificationDetail.vue",
      "type": "view",
      "path": "notifications/:id"
    }
  ],
  "ui_sidebar_navs": [
    {
      "path": "/notifications",
      "title": "plugins.notification.ui_sidebar_navs.title.notifications",
      "icon": [
        "fa",
        "envelope"
      ]
    }
  ],
  "ui_assets": [
    {
      "path": "ui/public/simplemde/simplemde.js",
      "type": "js"
    },
    {
      "path": "ui/public/simplemde/simplemde.css",
      "type": "css"
    },
    {
      "path": "ui/public/css/style.css",
      "type": "css"
    }
  ]
}
`)
			var p models.Plugin
			_ = json.Unmarshal(pluginData, &p)
			if err := delegate.NewModelDelegate(&p).Add(); err != nil {
				return err
			}
		}

		// exists, skip
		return nil
	}
	return backoff.Retry(op, backoff.NewConstantBackOff(1*time.Second))
}

func (svc *Service) initData() (err error) {
	total, err := svc.col.Count(nil)
	if err != nil {
		return err
	}
	if total > 0 {
		return nil
	}

	// data to initialize
	settings := []Setting{
		{
			Id:          primitive.NewObjectID(),
			Type:        TypeMail,
			Enabled:     true,
			Name:        "Task Change (Mail)",
			Description: "This is the default mail notification. You can edit it with your own settings",
			Triggers: []string{
				"model:tasks:change",
			},
			Title: "[Crawlab] Task Update: {{$.status}}",
			Template: `Dear {{$.user.username}},

Please find the task data as below.

|Key|Value|
|:-:|:--|
|Task Status|{{$.status}}|
|Task Priority|{{$.priority}}|
|Task Mode|{{$.mode}}|
|Task Command|{{$.cmd}}|
|Task Params|{{$.params}}|
|Error Message|{{$.error}}|
|Node|{{$.node.name}}|
|Spider|{{$.spider.name}}|
|Project|{{$.spider.project.name}}|
|Schedule|{{$.schedule.name}}|
|Result Count|{{$.:task_stat.result_count}}|
|Wait Duration (sec)|{#{{$.:task_stat.wait_duration}}/1000#}|
|Runtime Duration (sec)|{#{{$.:task_stat.runtime_duration}}/1000#}|
|Total Duration (sec)|{#{{$.:task_stat.total_duration}}/1000#}|
|Result Count|{{$.:task_stat.result_count}}|
|Avg Results / Sec|{#{{$.:task_stat.result_count}}/({{$.:task_stat.total_duration}}/1000)#}|
`,
			Mail: SettingMail{
				Server:         "smtp.163.com",
				Port:           "465",
				User:           os.Getenv("CRAWLAB_PLUGIN_NOTIFICATION_MAIL_USER"),
				Password:       os.Getenv("CRAWLAB_PLUGIN_NOTIFICATION_MAIL_PASSWORD"),
				SenderEmail:    os.Getenv("CRAWLAB_PLUGIN_NOTIFICATION_MAIL_SENDER_EMAIL"),
				SenderIdentity: os.Getenv("CRAWLAB_PLUGIN_NOTIFICATION_MAIL_SENDER_IDENTITY"),
				To:             "{{$.user[create].email}}",
				Cc:             os.Getenv("CRAWLAB_PLUGIN_NOTIFICATION_MAIL_CC"),
			},
		},
		{
			Id:          primitive.NewObjectID(),
			Type:        TypeMobile,
			Enabled:     true,
			Name:        "Task Change (Mobile)",
			Description: "This is the default mobile notification. You can edit it with your own settings",
			Triggers: []string{
				"model:tasks:change",
			},
			Title: "[Crawlab] Task Update: {{$.status}}",
			Template: `Dear {{$.user.username}},

Please find the task data as below.

- **Task Status**: {{$.status}}
- **Task Priority**: {{$.priority}}
- **Task Mode**: {{$.mode}}
- **Task Command**: {{$.cmd}}
- **Task Params**: {{$.params}}
- **Error Message**: {{$.error}}
- **Node**: {{$.node.name}}
- **Spider**: {{$.spider.name}}
- **Project**: {{$.spider.project.name}}
- **Schedule**: {{$.schedule.name}}
- **Result Count**: {{$.:task_stat.result_count}}
- **Wait Duration (sec)**: {#{{$.:task_stat.wait_duration}}/1000#}
- **Runtime Duration (sec)**: {#{{$.:task_stat.runtime_duration}}/1000#}
- **Total Duration (sec)**: {#{{$.:task_stat.total_duration}}/1000#}
- **Result Count**: {{$.:task_stat.result_count}}
- **Avg Results / Sec**: {#{{$.:task_stat.result_count}}/({{$.:task_stat.total_duration}}/1000)#}`,
			Mobile: SettingMobile{
				Webhook: os.Getenv("CRAWLAB_PLUGIN_NOTIFICATION_MOBILE_WEBHOOK"),
			},
		},
	}
	var data []interface{}
	for _, s := range settings {
		data = append(data, s)
	}
	_, err = svc.col.InsertMany(data)
	if err != nil {
		return err
	}
	return nil
}

func (svc *Service) sendMail(s *Setting, entity bson.M) (err error) {
	// to
	to, err := parser.Parse(s.Mail.To, entity)
	if err != nil {
		log.Warnf("parsing 'to' error: %v", err)
	}
	if to == "" {
		return nil
	}

	// cc
	cc, err := parser.Parse(s.Mail.Cc, entity)
	if err != nil {
		log.Warnf("parsing 'cc' error: %v", err)
	}

	// title
	title, err := parser.Parse(s.Title, entity)
	if err != nil {
		log.Warnf("parsing 'title' error: %v", err)
	}

	// content
	content, err := parser.Parse(s.Template, entity)
	if err != nil {
		log.Warnf("parsing 'content' error: %v", err)
	}

	// send mail
	if err := SendMail(s, to, cc, title, content); err != nil {
		return err
	}

	return nil
}

func (svc *Service) sendMobile(s *Setting, entity bson.M) (err error) {
	// webhook
	webhook, err := parser.Parse(s.Mobile.Webhook, entity)
	if err != nil {
		log.Warnf("parsing 'webhook' error: %v", err)
	}
	if webhook == "" {
		return nil
	}

	// title
	title, err := parser.Parse(s.Title, entity)
	if err != nil {
		log.Warnf("parsing 'title' error: %v", err)
	}

	// content
	content, err := parser.Parse(s.Template, entity)
	if err != nil {
		log.Warnf("parsing 'content' error: %v", err)
	}

	// send
	if err := SendMobileNotification(webhook, title, content); err != nil {
		return err
	}

	return nil
}

func (svc *Service) GetTriggerList() (res []string, total int, err error) {
	modelList := []string{
		interfaces.ModelColNameTag,
		interfaces.ModelColNameNode,
		interfaces.ModelColNameProject,
		interfaces.ModelColNameSpider,
		interfaces.ModelColNameTask,
		interfaces.ModelColNameJob,
		interfaces.ModelColNameSchedule,
		interfaces.ModelColNameUser,
		interfaces.ModelColNameSetting,
		interfaces.ModelColNameToken,
		interfaces.ModelColNameVariable,
		interfaces.ModelColNameTaskStat,
		interfaces.ModelColNamePlugin,
		interfaces.ModelColNameSpiderStat,
		interfaces.ModelColNameDataSource,
		interfaces.ModelColNameDataCollection,
		interfaces.ModelColNamePasswords,
	}
	actionList := []string{
		interfaces.ModelDelegateMethodAdd,
		interfaces.ModelDelegateMethodChange,
		interfaces.ModelDelegateMethodDelete,
		interfaces.ModelDelegateMethodSave,
	}

	var triggers []string
	for _, m := range modelList {
		for _, a := range actionList {
			triggers = append(triggers, fmt.Sprintf("model:%s:%s", m, a))
		}
	}

	return triggers, len(triggers), nil
}

func (svc *Service) GetSettingList(query bson.M, pagination *entity.Pagination, sort bson.D) (res []Setting, total int, err error) {
	// get list
	var list []Setting
	if err := svc.col.Find(query, &mongo2.FindOptions{
		Sort:  sort,
		Skip:  pagination.Size * (pagination.Page - 1),
		Limit: pagination.Size,
	}).All(&list); err != nil {
		if err.Error() == mongo.ErrNoDocuments.Error() {
			return nil, 0, nil
		} else {
			return nil, 0, err
		}
	}

	// total count
	total, err = svc.col.Count(query)
	if err != nil {
		return nil, 0, err
	}

	return list, total, nil
}

func (svc *Service) GetSetting(id primitive.ObjectID) (res *Setting, err error) {
	var s Setting
	if err := svc.col.FindId(id).One(&s); err != nil {
		return nil, err
	}
	return &s, nil
}

func (svc *Service) PosSetting(s *Setting) (err error) {
	s.Id = primitive.NewObjectID()
	if _, err := svc.col.Insert(s); err != nil {
		return err
	}
	return nil
}

func (svc *Service) PutSetting(id primitive.ObjectID, s Setting) (err error) {
	if err := svc.col.ReplaceId(id, s); err != nil {
		return err
	}

	return nil
}

func (svc *Service) DeleteSetting(id primitive.ObjectID) (err error) {
	if err := svc.col.DeleteId(id); err != nil {
		return err
	}

	return nil
}

func (svc *Service) EnableSetting(id primitive.ObjectID) (err error) {
	return svc._toggleSettingFunc(true)(id)
}

func (svc *Service) DisableSetting(id primitive.ObjectID) (err error) {
	return svc._toggleSettingFunc(false)(id)
}

func (svc *Service) handleEvents() {
	log.Infof("[NotificationService] start handling events")

	// get stream
	log.Infof("[NotificationService] attempt to obtain grpc stream...")
	var stream grpc.PluginService_SubscribeClient
	for {
		stream = svc.stream
		if stream == nil {
			time.Sleep(1 * time.Second)
			continue
		}
		break
	}
	log.Infof("[NotificationService] obtained grpc stream, start receiving messages...")

	for {
		// receive stream message
		msg, err := stream.Recv()

		if err != nil {
			// end
			if strings.HasSuffix(err.Error(), io.EOF.Error()) {
				// TODO: implement
				log.Infof("[NotificationService] received EOF signal, re-connecting...")
				//svc.GetGrpcClient().Restart()
			}

			trace.PrintError(err)
			time.Sleep(1 * time.Second)
			continue
		}

		// log
		utils.LogDebug(fmt.Sprintf("[NotificationService] received message: %v", msg))

		var data entity.GrpcEventServiceMessage
		switch msg.Code {
		case grpc.StreamMessageCode_SEND_EVENT:
			// data
			if err := json.Unmarshal(msg.Data, &data); err != nil {
				return
			}
			if len(data.Events) < 1 {
				continue
			}

			// event name
			eventName := data.Events[0]

			// settings
			var settings []Setting
			if err := svc.col.Find(bson.M{
				"enabled":  true,
				"triggers": eventName,
			}, nil).All(&settings); err != nil || len(settings) == 0 {
				continue
			}

			// handle events
			if err := svc._handleEventModel(settings, data.Data); err != nil {
				trace.PrintError(err)
			}
		default:
			continue
		}
	}
}

func (svc *Service) _handleEventModel(settings []Setting, data []byte) (err error) {
	var doc bson.M
	if err := json.Unmarshal(data, &doc); err != nil {
		return err
	}

	for _, s := range settings {
		switch s.Type {
		case TypeMail:
			err = svc.sendMail(&s, doc)
		case TypeMobile:
			err = svc.sendMobile(&s, doc)
		}
		if err != nil {
			trace.PrintError(err)
		}
	}

	return nil
}

func (svc *Service) _toggleSettingFunc(value bool) func(id primitive.ObjectID) error {
	return func(id primitive.ObjectID) (err error) {
		var s Setting
		if err := svc.col.FindId(id).One(&s); err != nil {
			return err
		}
		s.Enabled = value
		if err := svc.col.ReplaceId(id, s); err != nil {
			return err
		}
		return nil
	}
}

func (svc *Service) getStreamMessagePrefix() (prefix string) {
	return "plugin:" + PluginName + ":"
}

func NewService() *Service {
	// service
	svc := &Service{
		col: mongo2.GetMongoCol(SettingsColName),
	}

	c := dig.New()
	if err := c.Provide(config.NewNodeConfigService); err != nil {
		panic(err)
	}
	if err := c.Provide(client.ProvideGetClient("")); err != nil {
		panic(err)
	}
	if err := c.Invoke(func(
		cfgSvc interfaces.NodeConfigService,
		client interfaces.GrpcClient,
	) {
		svc.cfgSvc = cfgSvc
		svc.c = client
	}); err != nil {
		panic(err)
	}

	// model service
	modelSvc, err := service.GetService()
	if err != nil {
		panic(err)
	}
	svc.modelSvc = modelSvc

	if err := svc.Init(); err != nil {
		panic(err)
	}

	return svc
}

var _service *Service

func GetService() *Service {
	if _service == nil {
		_service = NewService()
	}
	return _service
}
