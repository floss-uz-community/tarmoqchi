GOOS=darwin     GOARCH=amd64    go build -ldflags="-s -w" -o bin/tarmoqchi-darwin-amd64
GOOS=darwin     GOARCH=arm64    go build -ldflags="-s -w" -o bin/tarmoqchi-darwin-arm64
GOOS=linux      GOARCH=386      go build -ldflags="-s -w" -o bin/tarmoqchi-linux-386
GOOS=linux      GOARCH=amd64    go build -ldflags="-s -w" -o bin/tarmoqchi-linux-amd64
GOOS=linux      GOARCH=arm      go build -ldflags="-s -w" -o bin/tarmoqchi-linux-arm
GOOS=linux      GOARCH=arm64    go build -ldflags="-s -w" -o bin/tarmoqchi-linux-arm64
GOOS=windows    GOARCH=386      go build -ldflags="-s -w" -o bin/tarmoqchi-windows-386.exe
GOOS=windows    GOARCH=amd64    go build -ldflags="-s -w" -o bin/tarmoqchi-windows-amd64.exe