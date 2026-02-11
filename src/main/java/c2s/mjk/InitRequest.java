package c2s.mjk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Example: Professional API initialization request using extended C2S pattern.
 *
 * This demonstrates the RECOMMENDED approach for production applications:
 *
 * FEATURES DEMONSTRATED:
 * - Custom callback interfaces for type-safe API
 * - Automatic JSON parsing and error handling
 * - Async processing (onResponseAsync) for heavy background work
 * - Method chaining for fluent API design
 * - Separation of concerns (network vs business logic)
 *
 * LIFECYCLE:
 * 1. onInit() - Setup request parameters and enable async processing
 * 2. HTTP request executes in background
 * 3. onResponseAsync() - Parse and cache data on background thread
 * 4. onResponse() - Call success callback (on UI thread if UiExecutor set)
 * 5. User's success/error callback is invoked
 *
 * USAGE:
 * new InitRequest(endpoint, path)
 *     .setLoggingEnabled(true)
 *     .onSuccess(config -> { handle success })
 *     .onError((code, msg) -> {  handle error })
 *     .start();
 */
public class InitRequest extends C2S {

    /**
     * Callback interface for successful initialization.
     * Provides parsed JSON configuration from server.
     */
    public interface OnInitSuccess {
        void onSuccess(JSONObject serverConfig);
    }

    /**
     * Callback interface for initialization errors.
     * Provides error code and message for handling.
     */
    public interface OnInitError {
        void onError(int code, String message);
    }

    private OnInitSuccess successCallback;
    private OnInitError errorCallback;

    /**
     * Constructor for InitRequest.
     *
     * @param endpoint the API endpoint URL
     * @param path the API path
     */
    public InitRequest(String endpoint, String path) {
        super(endpoint, path, C2S.POST);
    }

    /**
     * Sets the success callback.
     *
     * @param callback called when initialization succeeds
     * @return this instance for method chaining
     */
    public InitRequest onSuccess(OnInitSuccess callback) {
        this.successCallback = callback;
        return this;
    }

    /**
     * Sets the error callback.
     *
     * @param callback called when initialization fails
     * @return this instance for method chaining
     */
    public InitRequest onError(OnInitError callback) {
        this.errorCallback = callback;
        return this;
    }

    /**
     * Override setLoggingEnabled to return InitRequest for method chaining.
     *
     * @param enabled true to enable logging
     * @return this instance for method chaining
     */
    @Override
    public InitRequest setLoggingEnabled(boolean enabled) {
        super.setLoggingEnabled(enabled);
        return this;
    }

    /**
     * Override setConnectTimeout to return InitRequest for method chaining.
     *
     * @param connectTimeout timeout in milliseconds
     * @return this instance for method chaining
     */
    @Override
    public InitRequest setConnectTimeout(int connectTimeout) {
        super.setConnectTimeout(connectTimeout);
        return this;
    }

    /**
     * Override setReadTimeout to return InitRequest for method chaining.
     *
     * @param readTimeout timeout in milliseconds
     * @return this instance for method chaining
     */
    @Override
    public InitRequest setReadTimeout(int readTimeout) {
        super.setReadTimeout(readTimeout);
        return this;
    }

    /**
     * Initializes request with required parameters.
     * Called automatically before HTTP request starts.
     *
     * This is where you:
     * - Enable async processing if needed
     * - Add request parameters (become JSON body for POST)
     * - Add custom headers
     * - Configure authentication
     *
     * Override this in your own extended classes to customize behavior.
     */
    @Override
    public void onInit() {
        // Enable async processing - allows onResponseAsync to run before onResponse
        // Useful for heavy data parsing/caching that shouldn't block UI
        enableAsyncProcess();

        // Add initialization parameters (sent as JSON body)
        // In a real app, these might come from device info, user prefs, etc.
        addParameter("appID", "test-app-001")
                .addParameter("appVersion", "1.0.0")
                .addParameter("language", "en")
                .addParameter("device", "Java")
                .addParameter("platform", "Linux");
    }

    /**
     * Handles successful initialization (HTTP 200).
     * Parses server configuration and calls success callback.
     *
     * @param responseCode the HTTP response code
     * @param response the server response body
     */
    @Override
    public void onResponse(int responseCode, String response) {
        System.out.println("[InitRequest] Response received: " + responseCode);

        try {
            JSONObject json = new JSONObject(response);
            if (successCallback != null) {
                successCallback.onSuccess(json);
            }
        } catch (JSONException e) {
            System.out.println("[InitRequest] Failed to parse response: " + e.getMessage());
            if (errorCallback != null) {
                errorCallback.onError(responseCode, "Invalid JSON response");
            }
        }
    }

    /**
     * Handles initialization failures.
     *
     * @param responseCode the HTTP error code
     * @param response the error message or response
     */
    @Override
    public void onFailure(int responseCode, String response) {
        System.out.println("[InitRequest] Request failed: " + responseCode);

        // Handle 2xx responses that came through error path
        if (responseCode >= 200 && responseCode < 300) {
            onResponse(responseCode, response);
        } else if (errorCallback != null) {
            errorCallback.onError(responseCode, response);
        }
    }

    /**
     * Handles connection drops.
     */
    @Override
    public void onConnectionDropped() {
        System.out.println("[InitRequest] Connection dropped");
        if (errorCallback != null) {
            errorCallback.onError(-1, "Connection lost");
        }
    }

    /**
     * Processes response asynchronously in background thread.
     * Called BEFORE onResponse() if enableAsyncProcess() is enabled.
     *
     * USE THIS FOR:
     * - Heavy JSON parsing
     * - Database operations
     * - File I/O
     * - Caching data
     * - Any CPU-intensive work
     *
     * This keeps the UI thread responsive while processing large responses.
     * After this completes, onResponse() is called (on UI thread if UiExecutor set).
     *
     * @param responseCode the HTTP response code (200, 404, etc.)
     * @param response the raw server response body (JSON string)
     */
    @Override
    public void onResponseAsync(int responseCode, String response) {
        System.out.println("[InitRequest] Processing async response...");
        System.out.println("  Response size: " + response.length() + " bytes");

        try {
            // Parse JSON in background thread (doesn't block UI)
            JSONObject json = new JSONObject(response);
            System.out.println("  Parsed fields: " + json.keySet());

            // Extract and cache server configuration
            // In a real app, this might save to SharedPreferences, database, etc.
            if (json.has("serverTime")) {
                String serverTime = json.getString("serverTime");
                System.out.println("  Server time: " + serverTime);
                // Cache.set("server_time", serverTime);
            }

            if (json.has("config")) {
                JSONObject config = json.getJSONObject("config");
                System.out.println("  Configuration loaded: " + config.length() + " items");
                // Cache.set("app_config", config);
            }
        } catch (JSONException e) {
            System.out.println("  JSON parse error: " + e.getMessage());
        }
    }

    /**
     * Handles async failures.
     *
     * @param responseCode the HTTP error code
     * @param response the error message
     */
    @Override
    public void onFailureAsync(int responseCode, String response) {
        System.out.println("[InitRequest] Async failure: " + responseCode);
    }

    /**
     * Handles async connection drops.
     */
    @Override
    public void onConnectionDroppedAsync() {
        System.out.println("[InitRequest] Async connection dropped");
    }

    /**
     * Logs messages with custom formatting.
     *
     * @param log the message to log
     */
    @Override
    public void printLog(String log) {
        System.out.println("[LOG] " + log);
    }
}
