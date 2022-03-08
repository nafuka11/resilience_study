# レジリエンスプログラミングハンズオン

```text
├── README.md
├── go
│   └── main.go
└── java
```

## Goによる遅いAPIエンドポイント

```text
cd go
go run main.go -c 2
```

`-c` で同時に接続することができるクライアント数を制限することができる。

```http
GET /api/v1/slow HTTP/1.1
Host: localhost:9081

HTTP/1.1 200 OK
Content-Type: application/json
Date: Tue, 08 Mar 2022 08:47:35 GMT
Content-Length: 65

[{"id":123,"name":"会議室A","description":"最大人数5人"}]
```

## Spring Boot から Go のサービスを呼び出す

