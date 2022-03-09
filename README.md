# レジリエンスプログラミングハンズオン

このリポジトリは、2022年3月9日に42Tokyoで開催したレジリエンスプログラミングに関するハンズオンイベントのために作成したものです。

## 環境について

windows/mac ともに、Go 1.17 / JDK17 で作成しました。

```text
% go version
go version go1.17.8 darwin/amd64
```

```text
% java -version
openjdk version "17.0.2" 2022-01-18 LTS
OpenJDK Runtime Environment Microsoft-30338 (build 17.0.2+8-LTS)
OpenJDK 64-Bit Server VM Microsoft-30338 (build 17.0.2+8-LTS, mixed mode, sharing)
```

## 実行方法

### Go

```
cd go
go get tidy
go run main.go
```

tcp/9081 で HTTP サーバーが立ち上がります。

エンドポイントとして以下の2つが存在します。

- /api/go/slow 遅いエンドポイント
- /api/go/test 内部で http://localhost:9082/api/java/slow を呼び出すエンドポイント

起動時に `-c 2` のように指定することでHTTPの並行性を制限することができます。小さな数字を指定するとサーバーが高負荷な状態をシミュレートすることができます。

### Java

```
cd java
./gradlew bootJar
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar
```

tcp/9082 で HTTP サーバーが立ち上がります。

エンドポイントとして以下の2つが存在します。

- /api/java/slow 遅いエンドポイント
- /api/java/test 内部で http://localhost:9081/api/go/slow を呼び出すエンドポイント
