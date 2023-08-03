package main

import (
	"log"
	"net"
)

func main() {
	addr := "0.0.0.0:11122"
	listener, err := net.Listen("tcp", addr)
	if err != nil {
		panic(err)
	}
	defer listener.Close()
	log.Println("listening at ", addr)
	for {
		conn, err := listener.Accept()
		log.Println(conn.RemoteAddr())
		if err != nil {
			break
		}

		go handleReq(conn)
	}
}
func handleReq(conn net.Conn) {
	for {
		data := make([]byte, 1024)
		_, err := conn.Read(data)
		log.Println(string(data), err)
		if err != nil {
			log.Println(err)
			return
		}

		conn.Write(data)
	}
}
