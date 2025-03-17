package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"time"

	"github.com/gorilla/websocket"
)

const (
	wsDomain   = "wss://tarmoqchi.uz/server"
	httpDomain = "https://tarmoqchi.uz"
	version    = "Tarmoqchi CLI v1.0.0"
)

// RequestType enum
type RequestType string

const (
	Forward RequestType = "FORWARD"
	Created RequestType = "CREATED"
	Error   RequestType = "ERROR"
)

// ForwardInfo represents forwarding information
type ForwardInfo struct {
	Path   string `json:"path"`
	Method string `json:"method"`
	Body   string `json:"body,omitempty"`
}

// TunnelInfo represents tunnel information
type TunnelInfo struct {
	Message string `json:"message"`
}

// Request represents a request from the server
type Request struct {
	ID          string       `json:"id"`
	Type        RequestType  `json:"type"`
	ForwardInfo *ForwardInfo `json:"forwardInfo,omitempty"`
	TunnelInfo  *TunnelInfo  `json:"tunnelInfo,omitempty"`
	Error       string       `json:"error,omitempty"`
}

// Response represents a response to the server
type Response struct {
	ID         string `json:"requestId"`
	StatusCode int    `json:"status"`
	Body       string `json:"body"`
}

func main() {
	// Define command line flags
	portFlag := flag.String("port", "", "Local port to forward requests")
	authFlag := flag.String("auth", "", "Authentication token")
	helpFlag := flag.Bool("help", false, "Show help information")
	versionFlag := flag.Bool("version", false, "Show version information")

	// Parse command line arguments
	flag.Parse()

	// Check for help or version flags
	if *helpFlag {
		printHelp()
		return
	}

	if *versionFlag {
		fmt.Println(version)
		return
	}

	// Handle commands
	if *authFlag != "" {
		authorize(*authFlag)
	} else if *portFlag != "" {
		createTunnel(*portFlag)
	} else {
		printError("Invalid command. Use '--help' to see available commands.")
	}
}

func printHelp() {
	fmt.Println("Tarmoqchi CLI - A lightweight tunneling tool")
	fmt.Println("Usage:")
	fmt.Println("  tarmoqchi [options]")
	fmt.Println("\nOptions:")
	fmt.Println("  --port <port>    Local port to forward requests")
	fmt.Println("  --auth <token>   Authentication token")
	fmt.Println("  --help           Show help information")
	fmt.Println("  --version        Show version information")
}

func printTarmoqchi() {
	logo := `
  _______                                    _     _
 |__   __|                                  | |   (_)
    | | __ _ _ __ _ __ ___   ___   __ _  ___| |__  _
    | |/ _' | '__| '_ ' _ \ / _ \ / _' |/ __| '_ \| |
    | | (_| | |  | | | | | | (_) | (_| | (__| | | | |
    |_|\__,_|_|  |_| |_| |_|\___/ \__, |\___|_| |_|_|
                                     | |
                                     |_|
`
	fmt.Println(logo)
}

func authorize(auth string) {
	reqBody := map[string]string{"token": auth}
	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		printError("Failed to create request body: " + err.Error())
		return
	}

	resp, err := http.Post(httpDomain+"/auth", "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		printError("Failed to connect to server: " + err.Error())
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode == 200 {
		// Create directory if it doesn't exist
		homeDir, err := os.UserHomeDir()
		if err != nil {
			printError("Failed to get home directory: " + err.Error())
			return
		}

		tokenDir := filepath.Join(homeDir, ".tarmoqchi")
		err = os.MkdirAll(tokenDir, 0755)
		if err != nil {
			printError("Failed to create directory: " + err.Error())
			return
		}

		// Save token to file
		tokenPath := filepath.Join(tokenDir, "token")
		err = os.WriteFile(tokenPath, []byte(auth), 0600)
		if err != nil {
			printError("Failed to save token: " + err.Error())
			return
		}

		printSuccess("Successfully authenticated and token saved.")
	} else {
		body, _ := io.ReadAll(resp.Body)
		printError("Authentication failed. Server response: " + string(body))
	}
}

func createTunnel(port string) {
	printTarmoqchi()
	printInfo("Attempting to establish a tunnel...")

	// Get token from file
	homeDir, err := os.UserHomeDir()
	if err != nil {
		printError("Failed to get home directory: " + err.Error())
		return
	}

	tokenPath := filepath.Join(homeDir, ".tarmoqchi", "token")
	tokenBytes, err := os.ReadFile(tokenPath)
	if err != nil {
		printError("Authentication token is missing. Please authenticate first.")
		return
	}

	token := strings.TrimSpace(string(tokenBytes))

	// Create header for WebSocket connection
	header := http.Header{}
	header.Add("Authorization", "Bearer "+token)

	// Connect to WebSocket
	c, _, err := websocket.DefaultDialer.Dial(wsDomain, header)
	if err != nil {
		printError("Error connecting to the server: " + err.Error())
		return
	}
	defer c.Close()

	printWarning("The tunnel will automatically close in 4 hours.")

	// Setup ping ticker
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	// Setup signal handling for graceful shutdown
	interrupt := make(chan os.Signal, 1)
	signal.Notify(interrupt, os.Interrupt)

	// Create a done channel to signal when to exit
	done := make(chan struct{})

	// Read messages from server
	go func() {
		defer close(done)
		for {
			_, message, err := c.ReadMessage()
			if err != nil {
				if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
					printError("Connection closed unexpectedly: " + err.Error())
				}
				return
			}

			var request Request
			if err := json.Unmarshal(message, &request); err != nil {
				printError("Failed to parse message: " + err.Error())
				continue
			}

			switch request.Type {
			case Forward:
				{
					if request.ForwardInfo != nil {
						go requestSender(&request, c, port)
					} else {
						printError("Invalid forward request: forwardInfo is missing.")
					}
				}
			case Created:
				{
					if request.TunnelInfo != nil {
						printInfo("Tunnel created: " + request.TunnelInfo.Message + " -> " + "http://localhost:" + port)
					}
				}
			default:
				{
					if request.Error != "" {
						printError("Error from server: " + request.Error)
					}
				}
			}

		}
	}()

	// Main loop
	for {
		select {
		case <-done:
			return
		case <-ticker.C:
			// Send ping message
			err := c.WriteMessage(websocket.PingMessage, []byte("ping"))
			if err != nil {
				printError("Failed to send ping: " + err.Error())
				return
			}
		case <-interrupt:
			printWarning("Connection closed.")

			// Cleanly close the connection by sending a close message
			err := c.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseNormalClosure, ""))
			if err != nil {
				printError("Error during closing websocket: " + err.Error())
				return
			}

			select {
			case <-done:
			case <-time.After(time.Second):
			}
			return
		case <-time.After(4 * time.Hour):
			printWarning("Tunnel timeout reached (4 hours). Closing connection.")
			return
		}
	}
}

func requestSender(request *Request, conn *websocket.Conn, localPort string) {
	forwardInfo := request.ForwardInfo
	targetURL := fmt.Sprintf("http://localhost:%s%s", localPort, forwardInfo.Path)

	// Create HTTP client and request
	client := &http.Client{}
	var reqBody io.Reader

	if forwardInfo.Body != "" {
		reqBody = strings.NewReader(forwardInfo.Body)
	}

	req, err := http.NewRequest(forwardInfo.Method, targetURL, reqBody)
	if err != nil {
		printError("Error creating request: " + err.Error())
		return
	}

	// Set default headers
	req.Header.Set("User-Agent", "Go HttpClient Bot")
	req.Header.Set("Accept", "*/*")

	// Send the request
	resp, err := client.Do(req)
	if err != nil {
		if strings.Contains(err.Error(), "connection refused") {
			printError(fmt.Sprintf("Connection refused. Make sure the local server is running on port %s", localPort))
		} else {
			printError("Error sending request: " + err.Error())
		}
		return
	}
	defer resp.Body.Close()

	// Read response body
	bodyBytes, err := io.ReadAll(resp.Body)

	if err != nil {
		printError("Error reading response body: " + err.Error())
		return
	}

	responseBody := string(bodyBytes)

	// Log the forward
	currentTime := time.Now().Format("15:04:05")

	printForward(fmt.Sprintf("[%s] %s %d %s", currentTime, forwardInfo.Method, resp.StatusCode, forwardInfo.Path))

	// Check response body size
	if len(responseBody) > 10000 {
		printError("Response body is too large. Truncating to 10000 characters.")
		return
	}

	// Send response back to server
	response := Response{
		ID:         request.ID,
		StatusCode: resp.StatusCode,
		Body:       responseBody,
	}

	responseJSON, err := json.Marshal(response)

	if err != nil {
		printError("Error marshaling response: " + err.Error())
		return
	}

	err = conn.WriteMessage(websocket.TextMessage, responseJSON)
	if err != nil {
		printError("Error sending response to server: " + err.Error())
		return
	}
}

// Print functions
func printError(msg string) {
	fmt.Println("[ERROR] " + msg)
}

func printInfo(msg string) {
	fmt.Println("[INFO] " + msg)
}

func printSuccess(msg string) {
	fmt.Println("[SUCCESS] " + msg)
}

func printWarning(msg string) {
	fmt.Println("[WARNING] " + msg)
}

func printForward(msg string) {
	fmt.Println("[FORWARD] " + msg)
}
