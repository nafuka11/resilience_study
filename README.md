# resilience_study

このリポジトリは、2022年3月9日に開催された「実例で学ぶレジリエンスプログラミング教室」の [ハンズオン用リポジトリ](https://github.com/nebosuke/resilience_study) のForkです。

オリジナルのREADME.mdは [README_original.md](./README_original.md) にあります。

## 変更点

`go/` に以下の変更を加えています。

- CircuitBreakerのstate変更時のログ出力を追加
- CircuitBreakerを無効化したhttp.Getのwrapper関数、 `weak_get` を追加

## 確認手順

### 必要物

- [k6](https://k6.io/)
  - 負荷テストツールです。JavaScriptで負荷テストのシナリオを記述できます。
  - ハンズオンでどのようなツールを使っていたか失念したため、自分で選定しました。

### goでのCircuitBreaker発動確認

1. 必要なパッケージのインストール
   ```bash
   cd go
   go mod tidy
   ```
1. 並行性を制限してHTTPサーバを起動する
   ```bash
   go run main.go -c 10
   ```
1. 別Terminalを立ち上げ、負荷テストを実施する
   ```bash
   k6 run ./k6_test.js --vus 10 --duration 10s
   ```
   - `--vus 10` は同時接続数10、`--duration 10s` はテストの実行時間10秒の指定です。
1. HTTPサーバが起動されているTerminalで以下のようなログ出力がされ、  
   CircuitBreakerのstateがOpenになっていることを確認する
   ```json
   {"level":"info","ts":1647323079.09885,"caller":"go/main.go:48","msg":"gobreaker state changed","from":"closed","to":"open"}
   ```

今度は、CircuitBreakerが無効な状態で負荷テストを実施します。

1. main.go を書き換え、CircuitBreakerを無効にする
   ```diff
   func testEndpoint(w http.ResponseWriter, r *http.Request) {
   -       body, err := get("http://localhost:9082/api/java/slow")
   +       body, err := weak_get("http://localhost:9082/api/java/slow")
   ```
   - 本当はコマンドライン引数等で切り替えられた方がいいのですが、コード書き換えで対応しています。
1. 並行性を制限してHTTPサーバを起動する
   ```bash
   go run main.go -c 10
   ```
1. 別Terminalを立ち上げ、負荷テストを実施する
   ```bash
   k6 run ./k6_test.js --vus 10 --duration 10s
   ```
   - 先程よりもiterations（シナリオの実行回数）が少ないことが確認できると思います。
1. HTTPサーバが起動されているTerminalで、state変更のログ出力がされていないことを確認する

## 参考URL

[Secure integrations with Circuit Breaker in Go \- DEV Community](https://dev.to/he110/circuitbreaker-pattern-in-go-43cn)

[k6の使い方 シンプル&軽快な負荷試験ツールを試す \| フューチャー技術ブログ](https://future-architect.github.io/articles/20210324/)
