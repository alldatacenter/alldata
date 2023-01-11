package main

/*
mockHTTPServer and MQTT Client
mock MQTT Client to connect to mosquitto and save latestData in memory
mock HTTP Server is to get latestData and return it in response Body
*/
import (
	"fmt"
	"net/http"
	"os"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	"github.com/edgenesis/shifu/pkg/logger"
)

var (
	serverAddress = os.Getenv("MQTT_SERVER_ADDRESS")
	clientAddress = os.Getenv("HTTP_SRVER_ADDRESS")
	tmpMessage    string
)

func main() {
	client, err := connectToMQTT(serverAddress)
	if err != nil {
		logger.Errorf("Error when connect to mqtt server")
	}
	subTopic(client)

	mux := http.NewServeMux()

	mux.HandleFunc("/", GetLatestData)

	logger.Infof("Client listening at %v", clientAddress)
	err = http.ListenAndServe(clientAddress, mux)
	if err != nil {
		logger.Errorf("Error when server running, errors: %v", err)
	}
}

func GetLatestData(w http.ResponseWriter, r *http.Request) {
	if tmpMessage == "" {
		http.Error(w, "empty", http.StatusInternalServerError)
		return
	}
	fmt.Fprintf(w, "%s", string(tmpMessage))
}

func subTopic(client *mqtt.Client) {
	token := (*client).Subscribe("/test", 1, nil)
	token.Wait()
	if token.Error() != nil {
		logger.Fatalf("Error when sub to topic,error: %v", token.Error().Error())
	}
	logger.Infof("Subscribed to topic: /test")
}

var messageSubHandler mqtt.MessageHandler = func(client mqtt.Client, msg mqtt.Message) {
	logger.Infof("%v", string(msg.Payload()))
	tmpMessage = string(msg.Payload())
}

func connectToMQTT(address string) (*mqtt.Client, error) {
	var client mqtt.Client
	ticker := time.NewTicker(time.Second).C
	for i := 0; i < 20; i++ {
		<-ticker
		opts := mqtt.NewClientOptions()
		opts.AddBroker(fmt.Sprintf("tcp://%s", address))
		opts.SetClientID("mockServer")
		opts.SetDefaultPublishHandler(messageSubHandler)
		opts.OnConnect = connectHandler
		opts.OnConnectionLost = connectLostHandler
		client = mqtt.NewClient(opts)
		if token := client.Connect(); token.Wait() && token.Error() != nil {
			logger.Errorf("Error when connect to server error: %v", token.Error())
			continue
		}
		break
	}

	return &client, nil
}

var connectHandler mqtt.OnConnectHandler = func(client mqtt.Client) {
	logger.Infof("Connected")
}

var connectLostHandler mqtt.ConnectionLostHandler = func(client mqtt.Client, err error) {
	logger.Infof("Connect lost: %v", err)
}
