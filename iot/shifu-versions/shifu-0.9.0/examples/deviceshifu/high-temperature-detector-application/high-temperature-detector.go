package main

import (
	"io"
	"log"
	"net/http"
	"strconv"
	"time"
)

func main() {
	targetURL := "http://edgedevice-thermometer/read_value"
	req, _ := http.NewRequest("GET", targetURL, nil)
	for {
		res, _ := http.DefaultClient.Do(req)
		body, _ := io.ReadAll(res.Body)
		temperature, _ := strconv.Atoi(string(body))
		if temperature > 20 {
			log.Println("High temperature:", temperature)
		} else if temperature > 15 {
			log.Println("Normal temperature:", temperature)
		} else {
			log.Println("Low temperature:", temperature)
		}
		res.Body.Close()
		time.Sleep(2 * time.Second)
	}
}
