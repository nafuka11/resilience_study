package main

import (
	"encoding/json"
	"flag"
	"go.uber.org/zap"
	"golang.org/x/net/netutil"
	"math/rand"
	"net"
	"net/http"
	"time"
)

const (
	defaultMaxConn = 0
)

var logger *zap.Logger

type Room struct {
	ID          int    `json:"id"`
	Name        string `json:"name"`
	Description string `json:"description"`
}

func getAvailableRooms(w http.ResponseWriter, r *http.Request) {
	var rooms []Room
	rooms = append(rooms, Room{
		123,
		"会議室A",
		"最大人数5人",
	})

	res, err := json.Marshal(rooms)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	sleep := int(erlangKL(0.5, 2) * 200.0)
	time.Sleep(time.Millisecond * time.Duration(sleep))

	logger.Info("returns available rooms", zap.Int("size", len(rooms)), zap.Int("sleep", sleep))

	w.Header().Set("Content-Type", "application/json")
	w.Write(res)
}

func erlangKL(lambda float64, k int) float64 {
	g := 0.0
	for i := 0; i < k; i++ {
		g += (rand.ExpFloat64() / lambda)
	}
	return g
}

func main() {
	var (
		maxConn uint64
	)

	flag.Uint64Var(&maxConn, "c", defaultMaxConn, "maximum number of client connections the server will accept, 0 means unlimited")
	flag.Parse()

	rand.Seed(time.Now().UnixNano())

	logger, _ = zap.NewProduction()
	logger.Info("start http server")

	router := http.NewServeMux()
	router.HandleFunc("/api/go/slow", getAvailableRooms)

	srv := http.Server{
		ReadHeaderTimeout: time.Second * 5,
		ReadTimeout:       time.Second * 10,
		Handler:           router,
	}

	listener, err := net.Listen("tcp", ":9081")
	if err != nil {
		logger.Fatal(err.Error())
	}

	if maxConn > 0 {
		listener = netutil.LimitListener(listener, int(maxConn))
	}
	defer listener.Close()

	srv.Serve(listener)
}
