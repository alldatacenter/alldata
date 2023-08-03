package main

import (
	"fmt"
	"net/http"
)

func processHello(w http.ResponseWriter, req *http.Request) {
	fmt.Fprintln(w, "Hello_world from device via shifu!")
}

func headers(w http.ResponseWriter, req *http.Request) {
	for name, headers := range req.Header {
		for _, header := range headers {
			fmt.Fprintf(w, "%v: %v\n", name, header)
		}
	}
}

func main() {
	http.HandleFunc("/hello", processHello)
	http.HandleFunc("/headers", headers)

	http.ListenAndServe(":11111", nil)
}
