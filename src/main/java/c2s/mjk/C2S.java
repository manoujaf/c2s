package c2s.mjk;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

/**
 * HTTP/HTTPS request client for asynchronous network communication.
 * Extends the Async class to provide network communication capabilities with support for:
 * - GET, POST, PUT, DELETE HTTP methods
 * - JSON and form-urlencoded request bodies
 * - File uploads with multipart form data
 * - Proxy configuration with authentication
 * - Custom headers and request properties
 * - Basic HTTP authentication
 * - Asynchronous and synchronous operation modes
 *
 * Can be used directly by creating an instance and optionally overriding callback methods.
 * Default implementations provided for all callback methods.
 */
public class C2S extends Async {

    // Request configuration
    /** The complete API endpoint URL */
    private String api;
    /** Server response data (text, number, or JSON) */
    private String response = "127.0.0.1";
    /** The HTTP request method (GET, POST, PUT, DELETE) */
    private final String requestMethod;
    /** POST request data in UTF-8 format */
    private String postData;
    /** GET request query parameters */
    private String getData;
    /** PUT request data in UTF-8 format */
    private String putData;
    /** DELETE request query parameters */
    private String deleteData;
    /** Combined data field */
    private String data;
    /** JSON request body */
    private final JSONObject jsonData;

    // Proxy configuration
    /** Proxy server hostname or IP */
    private String proxyHost;
    /** Proxy server port */
    private int proxyPort;
    /** Proxy authentication username */
    private String proxyUsername;
    /** Proxy authentication password */
    private String proxyPassword;
    /** Flag indicating if proxy is configured */
    private boolean isProxySet;
    /** Flag indicating if proxy authentication is configured */
    private boolean isProxyAuthSet;

    // Connection settings
    /** Connection timeout in milliseconds */
    private int connectTimeout;
    /** Read timeout in milliseconds */
    private int readTimeout;
    /** HTTPS connection object */
    private HttpsURLConnection httpsURLConnection;
    /** List of request headers/properties */
    private final List<RequestProperty> requestPropertyList;
    /** HTTP response code from server */
    private int responseCode = -1;
    /** Flag to enable debug logging */
    private boolean showLog;

    // HTTP method constants
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String POST = "POST";
    public static final String GET = "GET";

    // Character encoding constant
    private final String UTF_8 = "UTF-8";

    // HTTP response code categories
    public static int HTTP_RESPONSE_INFORMATION = 100;
    public static int HTTP_RESPONSE_SUCCESS = 200;
    public static int HTTP_RESPONSE_REDIRECTION = 300;
    public static int HTTP_RESPONSE_CLIENT_ERROR = 400;
    public static int HTTP_RESPONSE_SERVER_ERROR = 500;
    public static final int NO_FILE = 48;

    // File upload configuration
    /** Multipart form data boundary separator */
    private static final String twoHyphens = "--";
    /** Multipart boundary identifier */
    private static final String boundary = "*****";
    /** HTTP line ending for multipart data */
    private static final String lineEnd = "\r\n";

    // File upload fields
    /** File to upload */
    private File file;
    /** Name of the file to upload */
    private String fileName;
    /** Maximum buffer size for file reading */
    private int maxBufferSize;
    /** Flag to enable asynchronous callbacks */
    private boolean isAsync;
    /** Flag to use JSON body format instead of form-urlencoded */
    private boolean isJsonBody;

    /**
     * Constructor for C2S HTTP request client.
     *
     * @param endPoint the base endpoint URL (e.g., "https://api.example.com")
     * @param path the API path to append to the endpoint (e.g., "/users")
     * @param requestMethod the HTTP method to use (GET, POST, PUT, DELETE)
     */
    public C2S(String endPoint, String path, String requestMethod) {

        // Create complete API URL from endpoint and path
        this.api = endPoint + path;
        this.requestMethod = requestMethod;

        // Initialize configuration
        requestPropertyList = new ArrayList<>();
        isProxySet = false;
        isProxyAuthSet = false;
        readTimeout = 0;
        connectTimeout = 0;
        maxBufferSize = 1024 * 1024;
        showLog = false;
        isAsync = false;
        jsonData = new JSONObject();
        isJsonBody = true;
    }

    @Override
    public void onPreExecute() {
        onInit();
    }

    @Override
    public void doInBackground() {

        if (showLog) {
            printLog("doInBackground: " + api);
        }

        if (file == null) {
            sendRequest();
        } else {
            sendFileRequest();
        }

    }

    @Override
    public void onPostExecute() {

        if (showLog) {
            printLog("onPostExecute: (" +getResponseCode() + ") " + api + "\nResponse: " + response);
        }

        if (getResponseCode() == HTTP_RESPONSE_SUCCESS) {
            onResponse(responseCode, response);
        } else {
            if (response.equals("127.0.0.1")) {
                onConnectionDropped();
            } else {
                onFailure(responseCode, response);
            }
        }

    }

    /**
     * Appends a path segment to the API URL with proper slash handling.
     * Ensures a slash exists between the current API path and the new segment.
     *
     * @param urlData the path segment to append (e.g., "users", "123")
     */
    public void appendToPath(String urlData) {
        if (!urlData.isEmpty() && urlData.charAt(urlData.length() - 1) != '/') {
            api = api + "/";
        }
        api = api + urlData;
    }

    /**
     * Appends a path segment to the API URL with simple concatenation.
     * Removes trailing slash from current API path if present.
     *
     * @param urlData the path segment to append
     */
    public void appendPathSegment(String urlData) {
        if (!api.isEmpty() && api.charAt(api.length() - 1) == '/') {
            api = api.substring(0, api.length() - 1);
        }
        api = api + urlData;
    }

    /**
     *
     */
    private void sendRequest() {

        final URL url;

        try {

            // add get data
            if (getData != null) {
                api += getData;
            }

            if (deleteData != null) {
                api += deleteData;
            }

            url = new URL(api);

            // set proxy
            if (isProxySet) {
                final InetSocketAddress proxyInet = new InetSocketAddress(proxyHost, proxyPort);
                final Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyInet);
                httpsURLConnection = (HttpsURLConnection) url.openConnection(proxy);
            } else {
                httpsURLConnection = (HttpsURLConnection) url.openConnection();
            }

            // Works for HTTP and HTTPS, but sets a global default!
            if (isProxyAuthSet) {
                Authenticator.setDefault(new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
                    }
                });
            }

            // set request method
            httpsURLConnection.setRequestMethod(requestMethod);

            if (readTimeout != 0) {
                httpsURLConnection.setReadTimeout(readTimeout);
            }
            if (connectTimeout != 0) {
                httpsURLConnection.setConnectTimeout(connectTimeout);
            }

            // add request property
            if (!requestPropertyList.isEmpty()) {
                for (RequestProperty property : requestPropertyList) {
                    addRequestProperty(property.getKey(), property.getValue());
                }
            }

            if (isJsonBody) {
                httpsURLConnection.setRequestProperty("Content-Type", "application/json");
                httpsURLConnection.setRequestProperty("Accept", "application/json");
            } else {
                httpsURLConnection.setRequestProperty("contentType", "application/x-www-form-urlencoded");
            }

            // write data
            if (requestMethod.equals(PUT) || requestMethod.equals(POST)) {
                if (postData != null || putData != null) {

                    if (postData == null) {
                        postData = putData;
                    }

                    httpsURLConnection.setDoInput(true);
                    httpsURLConnection.setDoOutput(true);

                    if (isJsonBody) {
                        try(OutputStream os = httpsURLConnection.getOutputStream()) {
                            byte[] input = jsonData.toString().getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);
                        }
                    } else {
                        httpsURLConnection.getOutputStream().write(postData.getBytes(StandardCharsets.UTF_8));
                    }

                    httpsURLConnection.getOutputStream().flush();
                    httpsURLConnection.getOutputStream().close();
                }
            }

            //httpsURLConnection.connect();
            responseCode = httpsURLConnection.getResponseCode();

            final StringBuilder stringBuilder;
            final BufferedReader bufferedReader;
            final InputStreamReader inputStreamReader;

            if(getHttpResponseType() == HTTP_RESPONSE_SUCCESS) {
                inputStreamReader = new InputStreamReader(httpsURLConnection.getInputStream());
            } else {
                inputStreamReader = new InputStreamReader(httpsURLConnection.getErrorStream());
            }

            bufferedReader = new BufferedReader(inputStreamReader);

            stringBuilder = new StringBuilder();

            // read from server response and convert it to string
            String string;
            while ((string = bufferedReader.readLine()) != null) {
                stringBuilder.append(string);
            }

            inputStreamReader.close();
            bufferedReader.close();
            response = stringBuilder.toString();

            if (isAsync) {
                // in background
                if (getHttpResponseType() == HTTP_RESPONSE_SUCCESS) {
                    onResponseAsync(responseCode, response);
                } else {
                    if (response.equals("127.0.0.1")) {
                        onConnectionDroppedAsync();
                    } else {
                        onFailureAsync(responseCode, response);
                    }
                }
            }

        } catch (IOException e) {
            if (showLog) {
                printLog("IOException: " + api);
                e.printStackTrace();
            }
            responseCode = 400;
            onFailureAsync(responseCode, response);
        }

    }

    /**
     * Enables asynchronous callback processing.
     * When enabled, onResponseAsync, onFailureAsync, and onConnectionDroppedAsync callbacks
     * will be called during the background request execution instead of waiting for completion.
     */
    public void enableAsyncProcess() {
        isAsync = true;
    }

    /**
     * Enables multipart/form-data request format instead of JSON.
     * Useful for form submissions and file uploads.
     */
    public void enablePostMultiPart() {
        isJsonBody = false;
    }

    /**
     * send request function
     */
    private void sendFileRequest() {

        // disable async process
        enableAsyncProcess();

        final DataOutputStream dos;
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;

        if (!file.isFile()) {
            onFailure(NO_FILE, "No file!");
        } else {

            try {

                final URL url = new URL(api);

                // open file to read and send
                final FileInputStream fileInputStream = new FileInputStream(file);

                // Open a HTTP  connection to  the URL
                httpsURLConnection = (HttpsURLConnection) url.openConnection();
                httpsURLConnection.setDoInput(true); // Allow Inputs
                httpsURLConnection.setDoOutput(true); // Allow Outputs
                httpsURLConnection.setUseCaches(false); // Don't use a Cached Copy

                httpsURLConnection.setRequestMethod(requestMethod);
                httpsURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpsURLConnection.setRequestProperty("ENCTYPE", "multipart/form-data");
                httpsURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                httpsURLConnection.setRequestProperty("uploaded_file", fileName);

                // add request property
                if (!requestPropertyList.isEmpty()) {
                    for (RequestProperty property : requestPropertyList) {
                        addRequestProperty(property.getKey(), property.getValue());
                    }
                }

                dos = new DataOutputStream(httpsURLConnection.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data necessary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                responseCode = httpsURLConnection.getResponseCode();

                final StringBuilder stringBuilder;
                final BufferedReader bufferedReader;
                final InputStreamReader inputStreamReader;

                if(getHttpResponseType() == HTTP_RESPONSE_SUCCESS) {
                    inputStreamReader = new InputStreamReader(httpsURLConnection.getInputStream());
                } else {
                    inputStreamReader = new InputStreamReader(httpsURLConnection.getErrorStream());
                }

                bufferedReader = new BufferedReader(inputStreamReader);
                stringBuilder = new StringBuilder();

                // read from server response and convert it to string
                String string;
                while ((string = bufferedReader.readLine()) != null) {
                    stringBuilder.append(string);
                }

                inputStreamReader.close();
                bufferedReader.close();
                response = stringBuilder.toString();

                //close the streams
//                sourceFile.delete();
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (Exception e) {
                e.printStackTrace();
                responseCode = 0;
            }
        } // End else block

    }

    /**
     * Adds a key-value pair to the request body or query parameters.
     * The destination (POST body, GET query, PUT body, or DELETE query) depends on the request method.
     * Supports method chaining for fluent API usage.
     *
     * @param key the parameter key/name
     * @param value the parameter value
     * @return this C2S object for method chaining
     */
    public C2S addParameter(String key, String value) {

        // if post has been set
        String AND_CHAR = "&";
        String EQUAL_CHAR = "=";
        String QUESTION_MARK = "?";
        if (POST.equals(requestMethod)) {

            // add Json
            try {
                jsonData.put(key, value.trim());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            if (postData == null || postData.isEmpty()) {
                postData = urlEncode(key) + EQUAL_CHAR + urlEncode(value);
            } else {
                postData = postData + AND_CHAR + urlEncode(key) + EQUAL_CHAR + urlEncode(value);
            }
            data = postData;

        } else if (PUT.equals(requestMethod)) {

            if (putData == null || putData.isEmpty()) {
                putData = urlEncode(key) + EQUAL_CHAR + urlEncode(value);
            } else {
                putData = putData + AND_CHAR + urlEncode(key) + EQUAL_CHAR + urlEncode(value);
            }
            data = putData;

        } else if (GET.equals(requestMethod)) {// set GET method data
            if (getData == null || getData.isEmpty()) {
                getData = QUESTION_MARK + urlEncode(key) + EQUAL_CHAR + urlEncode(value);
            } else {
                getData = getData + AND_CHAR + urlEncode(key) + EQUAL_CHAR + urlEncode(value);
            }
            data = getData;

        } else if (DELETE.equals(requestMethod)) {// set GET method data
            if (deleteData == null || deleteData.isEmpty()) {
                deleteData = QUESTION_MARK + urlEncode(key) + EQUAL_CHAR + urlEncode(value);
            } else {
                deleteData = deleteData + AND_CHAR + urlEncode(key) + EQUAL_CHAR + urlEncode(value);
            }
            data = deleteData;

        }

        return this;
    }

    /**
     * Converts a string to UTF-8 URL-encoded format.
     * Replaces special characters with their percent-encoded representations.
     *
     * @param string the string to encode
     * @return the URL-encoded string in UTF-8 format, or empty string on error
     */
    private String urlEncode(String string) {
        if (string == null) {
            return "";
        } else {
            try {
                return URLEncoder.encode(string, UTF_8);
            } catch (UnsupportedEncodingException e) {
                if (showLog) {
                    printLog("UnsupportedEncodingException: " + api);
                    e.printStackTrace();
                }
                return "";
            }
        }
    }

    /***********************************************/

    /**
     * Adds a custom header property to the HTTP request.
     * If the connection hasn't been established yet, the property is queued.
     * If already connected, the property is added directly to the connection.
     * Supports method chaining.
     *
     * @param key the header name (e.g., "Authorization", "Content-Type")
     * @param value the header value
     * @return this C2S object for method chaining
     */
    public final C2S addRequestProperty(String key, String value) {

        if (httpsURLConnection == null) {
            requestPropertyList.add(new RequestProperty(key, value));
        } else {
            httpsURLConnection.addRequestProperty(key, value);
        }

        return this;
    }

    /**
     * Callback executed when the request succeeds (HTTP 2xx response).
     * Called after the request completes, in the main thread context.
     * Override to handle successful responses.
     *
     * @param responseCode the HTTP response code (e.g., 200)
     * @param response the server response body (can be text, JSON, or any string data)
     */
    public void onResponse(int responseCode, String response) {
        // Default implementation - override to handle response
    }

    /**
     * Callback executed before the request is sent.
     * Use this to initialize UI elements, prepare data, or show progress indicators.
     * Executes in the main thread.
     * Override to add custom initialization logic.
     */
    public void onInit() {
        // Default implementation - override to add custom initialization
    }

    /**
     * Callback executed when the request fails (non-2xx response or error).
     * Called after the request completes with a failure status.
     * Override to handle errors.
     *
     * @param responseCode the HTTP response code indicating the error (e.g., 404, 500)
     * @param response the server response or error message
     */
    public void onFailure(int responseCode, String response) {
        // Default implementation - override to handle failures
    }

    /**
     * Callback executed when the connection is dropped or unavailable.
     * Indicates a network connectivity issue rather than an HTTP error.
     * Override to handle connection issues.
     */
    public void onConnectionDropped() {
        // Default implementation - override to handle connection drops
    }

    /**
     * Async callback executed during request processing when the response is successful.
     * Only called if enableAsyncProcess() was invoked.
     * Executes in the background thread.
     * Override to process responses asynchronously.
     *
     * @param responseCode the HTTP response code
     * @param response the server response body
     */
    public void onResponseAsync(int responseCode, String response) {
        // Default implementation - override to handle async responses
    }

    /**
     * Async callback executed during request processing when a failure occurs.
     * Only called if enableAsyncProcess() was invoked.
     * Executes in the background thread.
     * Override to handle async failures.
     *
     * @param responseCode the HTTP response code indicating the error
     * @param response the server response or error message
     */
    public void onFailureAsync(int responseCode, String response) {
        // Default implementation - override to handle async failures
    }

    /**
     * Async callback executed during request processing when connection is dropped.
     * Only called if enableAsyncProcess() was invoked.
     * Executes in the background thread.
     * Override to handle async connection drops.
     */
    public void onConnectionDroppedAsync() {
        // Default implementation - override to handle async connection drops
    }

    /**
     * Retrieves all request data (parameters) combined and encoded in Base64.
     * Decodes URL-encoded data and re-encodes it as Base64 without line breaks.
     *
     * @return Base64-encoded request data, or empty string if no data set or on error
     */
    public String getData() {
        if (data == null) {
            return "";
        }
        try {
            String _data1 = URLDecoder.decode(data, UTF_8);
            String _data = Base64.getEncoder().encodeToString(_data1.getBytes());
            _data = _data.replace("\n", "");
            return _data;
        } catch (UnsupportedEncodingException e) {
            if (showLog) {
                printLog("UnsupportedEncodingException: " + api);
                e.printStackTrace();
            }
            return "";
        }
    }

    /**
     * Determines the HTTP response type category based on the response code.
     * Returns the first digit multiplied by 100 (e.g., 200 for 201, 300 for 304).
     *
     * @return the response type: 100, 200, 300, 400, or 500
     */
    public final int getHttpResponseType() {
        return (responseCode / 100) * 100;
    }

    /**
     * Encodes text to Base64 format using a specific character set.
     * Uses Base64 encoding without padding characters.
     *
     * @param text the text to encode
     * @param charset the character set to use for encoding
     * @return the Base64-encoded string without padding
     */
    public final String toBase64(String text, Charset charset) {
        return Base64.getEncoder().withoutPadding().encodeToString(text.getBytes(charset));
    }

    /**
     * Enables or disables debug logging for this request.
     * Supports method chaining.
     *
     * @param enabled true to enable logging, false to disable
     * @return this C2S object for method chaining
     */
    public C2S setLoggingEnabled(boolean enabled) {
        this.showLog = enabled;
        return this;
    }

    /**
     * Configures proxy settings for the HTTP request.
     *
     * @param proxyHost the proxy server hostname or IP address
     * @param proxyPort the proxy server port number
     */
    public void setProxyHost(String proxyHost, int proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        isProxySet = true;
    }

    /**
     * Sets proxy authentication credentials.
     *
     * @param proxyUsername the proxy username
     * @param proxyPassword the proxy password
     */
    public void setProxyAuth(String proxyUsername, String proxyPassword) {
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
        isProxyAuthSet = true;
    }

    /**
     * Retrieves the HTTP response code from the server.
     *
     * @return the HTTP response code (e.g., 200, 404, 500), or -1 if not yet set
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Sets the read timeout for the HTTP connection.
     * A read timeout occurs if no data is received within the specified time.
     * Supports method chaining.
     *
     * @param readTimeout timeout in milliseconds (0 means no timeout)
     * @return this C2S object for method chaining
     */
    public C2S setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * Sets the connection timeout for establishing the HTTP connection.
     * Supports method chaining.
     *
     * @param connectTimeout timeout in milliseconds (0 means no timeout)
     * @return this C2S object for method chaining
     */
    public C2S setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Retrieves the complete API URL (endpoint + path).
     *
     * @return the full API URL
     */
    public String getApi() {
        return api;
    }

    // File upload configuration methods

    /**
     * Sets the filename for file upload operations.
     *
     * @param fileName the name of the file to upload
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Sets the file object to upload.
     *
     * @param file the File object to upload
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Sets the maximum buffer size for reading files during upload.
     * Larger buffers are more efficient for large files but use more memory.
     *
     * @param maxBufferSize the buffer size in bytes (default 1MB)
     */
    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    /**
     * Checks if the application is running.
     * In a Java environment, always returns true as there's no concept of app foreground state.
     *
     * @return true if the app is running
     */
    public static boolean isForegrounded() {
        return true;
    }

    /**
     * Generates a Basic Authentication header property.
     * Encodes credentials in Base64 format as per HTTP Basic Auth specification.
     *
     * @param key the username/key for basic authentication
     * @param secret the password/secret for basic authentication
     * @return a RequestProperty object with Authorization header ready for use
     */
    public RequestProperty getBasicAuthentication(String key, String secret) {
        String auth = Base64.getEncoder().encodeToString((key+":"+secret).getBytes());
        auth = stripLineBreaks(auth);
        return new RequestProperty("Authorization", "Basic " + auth);
    }

    /**
     * Removes newlines from Base64 encoded strings.
     * Base64 encoding may include line breaks which need to be removed for HTTP headers.
     *
     * @param base64 the Base64 encoded string with potential newlines
     * @return the cleaned Base64 string without newlines
     */
    private String stripLineBreaks(String base64) {
        return base64.replace("\n", "");
    }

    /**
     * Retrieves the JSON request body object.
     * Use this to add JSON fields when using JSON request format.
     *
     * @return the JSONObject containing request data
     */
    public JSONObject getJsonData() {
        return jsonData;
    }

    /**
     * Logs a message for debugging purposes.
     * Override to define custom logging behavior.
     * Default implementation prints to System.out.
     *
     * @param log the message to log
     */
    public void printLog(String log) {
        System.out.println(log);
    }

    /**
     * Retrieves the POST request data.
     *
     * @return the POST data string, or null if not set
     */
    public String getPostData() {
        return postData;
    }

}
