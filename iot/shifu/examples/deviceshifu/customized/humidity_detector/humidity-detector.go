package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
)

func getHumidity(w http.ResponseWriter, req *http.Request) {
	dat, _ := os.ReadFile("raw_data")
	log.Println("get a request, return raw_data")
	fmt.Fprint(w, string(dat))
}

func main() {
	http.HandleFunc("/humidity", getHumidity)
	http.HandleFunc("/humidity_custom", getHumidity)

	http.ListenAndServe(":11111", nil)
}
