package main

import (
	"encoding/json"
	"flag"
	"github.com/sony/gobreaker"
	"go.uber.org/zap"
	"golang.org/x/net/netutil"
	"io/ioutil"
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

type Temperature struct {
	Celsius float64 `json:"celsius"`
}

var circuitBreaker *gobreaker.CircuitBreaker

func init() {
	rand.Seed(time.Now().UnixNano())

	logger, _ = zap.NewProduction()

	var settings gobreaker.Settings
	settings.Name = "java/slow"
	settings.Timeout = time.Duration(10 * time.Second)
	settings.ReadyToTrip = func(counts gobreaker.Counts) bool {
		failureRatio := float64(counts.TotalFailures) / float64(counts.Requests)
		return counts.Requests >= 10 && failureRatio >= 0.5
	}
	circuitBreaker = gobreaker.NewCircuitBreaker(settings)
}

func testEndpoint(w http.ResponseWriter, r *http.Request) {
	body, err := get("http://localhost:9082/api/java/slow")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	var temperature Temperature
	if err := json.Unmarshal(body, &temperature); err != nil {
		logger.Error("cannot unmarshal json string", zap.String("json", string(body)))
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	res, err := json.Marshal(temperature)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(res)
}

func slowEndpoint(w http.ResponseWriter, r *http.Request) {
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

	sleep := time.Duration(erlangKL(0.5, 2) * 200.0)
	time.Sleep(sleep * time.Millisecond)

	logger.Info("returns available rooms", zap.Int("size", len(rooms)), zap.Int64("sleep", int64(sleep)))

	w.Header().Set("Content-Type", "application/json")
	w.Write(res)
}

func erlangKL(lambda float64, k int) float64 {
	g := 0.0
	for i := 0; i < k; i++ {
		g += rand.ExpFloat64() / lambda
	}
	return g
}

func get(url string) ([]byte, error) {
	body, err := circuitBreaker.Execute(func() (interface{}, error) {
		resp, err := http.Get(url)
		if err != nil {
			return nil, err
		}

		defer resp.Body.Close()
		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return nil, err
		}

		return body, nil
	})
	if err != nil {
		return nil, err
	}

	return body.([]byte), nil
}

func main() {
	var (
		maxConn uint64
	)

	flag.Uint64Var(&maxConn, "c", defaultMaxConn, "maximum number of client connections the server will accept, 0 means unlimited")
	flag.Parse()

	logger.Info("start http server")

	router := http.NewServeMux()
	router.HandleFunc("/api/go/slow", slowEndpoint)
	router.HandleFunc("/api/go/test", testEndpoint)

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
