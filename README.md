# C2S - Client to Server HTTP Library

A lightweight, asynchronous HTTP/HTTPS client library for Java that simplifies network communication with REST APIs. C2S provides a clean, chainable API for making HTTP requests with support for JSON, file uploads, proxy configuration, and custom headers.

## Features

- **Multiple HTTP Methods**: GET, POST, PUT, DELETE
- **Request Formats**: JSON (default) and form-urlencoded
- **File Uploads**: Multipart form data support
- **Async/Sync Operations**: Built-in threading with customizable callbacks
- **Proxy Support**: HTTP proxy with authentication
- **Custom Headers**: Easy header management and basic authentication
- **Timeout Configuration**: Configurable connection and read timeouts
- **Response Handling**: Separate callbacks for success, failure, and connection drops
- **Cryptographic Utilities**: Built-in support for MD5, SHA1, SHA256, HMAC, and RSA operations

## Installation

### Gradle

Add the following to your `build.gradle`:

```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.json:json:20231013'
    // Add your C2S library dependency here
}
```

### Maven

```xml
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20231013</version>
</dependency>
```

## Quick Start

### Basic POST Request

```java
// Create a request with endpoint, path, and method
C2S request = new C2S("https://api.example.com", "/users", C2S.POST) {
    @Override
    public void onResponse(int responseCode, String response) {
        System.out.println("Success: " + response);
    }

    @Override
    public void onFailure(int responseCode, String response) {
        System.out.println("Error: " + response);
    }
};

// Add parameters and send
request.addParameter("name", "John Doe")
       .addParameter("email", "john@example.com")
       .start();
```

### GET Request with Query Parameters

```java
C2S request = new C2S("https://api.example.com", "/users", C2S.GET) {
    @Override
    public void onResponse(int responseCode, String response) {
        System.out.println("Users: " + response);
    }
};

request.addParameter("page", "1")
       .addParameter("limit", "10")
       .start();
```

## Usage Examples

### 1. Simple Request (No Customization)

```java
C2S request = new C2S("https://jsonplaceholder.typicode.com", "/posts", C2S.POST) {
    @Override
    public void onResponse(int responseCode, String response) {
        System.out.println(response);
    }
};

request.addParameter("title", "My Post")
       .addParameter("body", "Post content")
       .addParameter("userId", "1")
       .start();
```

### 2. Request with Configuration

```java
C2S request = new C2S("https://api.example.com", "/data", C2S.POST);

request.setConnectTimeout(5000)
       .setReadTimeout(10000)
       .setLoggingEnabled(true)
       .addRequestProperty("Authorization", "Bearer YOUR_TOKEN")
       .addRequestProperty("Custom-Header", "value")
       .addParameter("key", "value")
       .start();
```

### 3. File Upload

```java
C2S request = new C2S("https://api.example.com", "/upload", C2S.POST) {
    @Override
    public void onResponse(int responseCode, String response) {
        System.out.println("Upload successful: " + response);
    }
};

File file = new File("/path/to/file.jpg");
request.setFile(file);
request.setFileName("image.jpg");
request.setMaxBufferSize(1024 * 1024); // 1MB buffer
request.start();
```

### 4. Using Proxy

```java
C2S request = new C2S("https://api.example.com", "/data", C2S.GET);

// Configure proxy
request.setProxyHost("proxy.example.com", 8080);
request.setProxyAuth("username", "password");

request.start();
```

### 5. Form-Urlencoded Instead of JSON

```java
C2S request = new C2S("https://api.example.com", "/form", C2S.POST);

request.enablePostMultiPart(); // Use form-urlencoded instead of JSON
request.addParameter("username", "user")
       .addParameter("password", "pass")
       .start();
```

### 6. Extended Request Class

Create a custom request class for reusable API calls:

```java
public class LoginRequest extends C2S {

    public interface OnLoginSuccess {
        void onSuccess(JSONObject userData);
    }

    private OnLoginSuccess successCallback;

    public LoginRequest(String endpoint) {
        super(endpoint, "/auth/login", C2S.POST);
    }

    public LoginRequest onSuccess(OnLoginSuccess callback) {
        this.successCallback = callback;
        return this;
    }

    @Override
    public void onResponse(int responseCode, String response) {
        try {
            JSONObject json = new JSONObject(response);
            if (successCallback != null) {
                successCallback.onSuccess(json);
            }
        } catch (JSONException e) {
            onFailure(responseCode, "Invalid JSON response");
        }
    }

    public LoginRequest login(String username, String password) {
        addParameter("username", username);
        addParameter("password", password);
        return this;
    }
}

// Usage
new LoginRequest("https://api.example.com")
    .login("john@example.com", "password123")
    .onSuccess(userData -> {
        System.out.println("Logged in as: " + userData.getString("name"));
    })
    .start();
```

## API Reference

### Constructor

```java
C2S(String endPoint, String path, String requestMethod)
```

- `endPoint`: Base URL (e.g., "https://api.example.com")
- `path`: API path (e.g., "/users")
- `requestMethod`: HTTP method (`C2S.GET`, `C2S.POST`, `C2S.PUT`, `C2S.DELETE`)

### Request Configuration

| Method | Description |
|--------|-------------|
| `addParameter(String key, String value)` | Add request parameter (query or body) |
| `addRequestProperty(String key, String value)` | Add custom header |
| `setConnectTimeout(int timeout)` | Set connection timeout (ms) |
| `setReadTimeout(int timeout)` | Set read timeout (ms) |
| `setLoggingEnabled(boolean enabled)` | Enable/disable debug logging |
| `appendToPath(String segment)` | Append path segment with `/` |
| `appendPathSegment(String segment)` | Append path segment directly |

### Proxy Configuration

| Method | Description |
|--------|-------------|
| `setProxyHost(String host, int port)` | Configure HTTP proxy |
| `setProxyAuth(String username, String password)` | Set proxy authentication |

### File Upload

| Method | Description |
|--------|-------------|
| `setFile(File file)` | Set file to upload |
| `setFileName(String name)` | Set filename for upload |
| `setMaxBufferSize(int size)` | Set upload buffer size (default 1MB) |

### Request Formats

| Method | Description |
|--------|-------------|
| `enablePostMultiPart()` | Use form-urlencoded instead of JSON |
| `getJsonData()` | Get JSONObject for manual JSON manipulation |

### Authentication

| Method | Description |
|--------|-------------|
| `getBasicAuthentication(String key, String secret)` | Generate Basic Auth header |

### Callbacks (Override These)

```java
// Main callbacks (after request completes)
void onInit()                                    // Before request starts
void onResponse(int code, String response)       // On success (2xx)
void onFailure(int code, String response)        // On error (non-2xx)
void onConnectionDropped()                       // On connection loss

// Async callbacks (during request, if enableAsyncProcess() called)
void onResponseAsync(int code, String response)
void onFailureAsync(int code, String response)
void onConnectionDroppedAsync()
```

### Response Methods

| Method | Description |
|--------|-------------|
| `getResponseCode()` | Get HTTP response code |
| `getHttpResponseType()` | Get response category (100, 200, 300, 400, 500) |
| `getData()` | Get Base64-encoded request data |

### Constants

```java
// HTTP Methods
C2S.GET, C2S.POST, C2S.PUT, C2S.DELETE

// HTTP Response Categories
C2S.HTTP_RESPONSE_INFORMATION  // 100
C2S.HTTP_RESPONSE_SUCCESS      // 200
C2S.HTTP_RESPONSE_REDIRECTION  // 300
C2S.HTTP_RESPONSE_CLIENT_ERROR // 400
C2S.HTTP_RESPONSE_SERVER_ERROR // 500
```

## Cryptographic Utilities (Hash Class)

The library includes cryptographic utilities via the `Hash` class:

```java
// Hashing
String md5 = Hash.getHash("text", Hash.MD5);
String sha1 = Hash.getHash("text", Hash.SHA1);
String sha256 = Hash.getHash("text", Hash.SHA256);

// HMAC
String hmac = Hash.hmacDigest("data", "secret", Hash.HMAC_SHA256);

// Base64
String encoded = Hash.toBase64("text");
String decoded = Hash.fromBase64(encoded);

// RSA Signing
String signature = Hash.signWithRSA("data", "base64PrivateKey");

// File MD5
String fileMd5 = Hash.calculateFileMd5("/path/to/file");

// Random String Generation
String random = Hash.generateRandomString(32, Hash.NORMAL_CHARS);
String randomSpecial = Hash.generateRandomString(32, Hash.SPECIAL_CHARS);
```

## Architecture

### Class Hierarchy

```
Thread
  └── Async (Abstract threading base)
       └── C2S (HTTP client)
            └── InitRequest (Example extension)
```

### Lifecycle

1. **onPreExecute()** → `onInit()` - Initialization before request
2. **doInBackground()** → HTTP request execution
   - Optional: `onResponseAsync()` / `onFailureAsync()` (if `enableAsyncProcess()`)
3. **onPostExecute()** → `onResponse()` / `onFailure()` / `onConnectionDropped()`

## Thread Safety

- Each C2S instance creates its own background thread
- Callbacks are executed in the background thread context
- For UI updates, dispatch to the main thread manually

## Error Handling

```java
C2S request = new C2S("https://api.example.com", "/data", C2S.GET) {
    @Override
    public void onResponse(int responseCode, String response) {
        // Handle success (2xx responses)
        System.out.println("Success: " + response);
    }

    @Override
    public void onFailure(int responseCode, String response) {
        // Handle HTTP errors (4xx, 5xx)
        System.err.println("HTTP Error " + responseCode + ": " + response);
    }

    @Override
    public void onConnectionDropped() {
        // Handle network connectivity issues
        System.err.println("Network connection lost");
    }
};

request.start();
```

## Best Practices

1. **Extend for Reusability**: Create custom request classes for frequently used endpoints
2. **Handle All Callbacks**: Override `onFailure` and `onConnectionDropped` for robust error handling
3. **Set Timeouts**: Always configure appropriate timeouts for production use
4. **Use Logging**: Enable logging during development with `setLoggingEnabled(true)`
5. **Close Resources**: The library handles connection cleanup automatically
6. **Thread Management**: Each request creates a new thread; avoid creating too many simultaneous requests

## Examples

See the `Main.java` file for complete working examples:

1. **basicExample()** - Simple request without customization
2. **callbackExample()** - Request with response handling
3. **extendedExample()** - Using a custom request class (InitRequest)

## Requirements

- Java 8 or higher
- org.json library (for JSON parsing)

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues.

## License

[Specify your license here]

## Author

[Your name/organization]

## Support

For issues, questions, or contributions, please visit the GitHub repository.
