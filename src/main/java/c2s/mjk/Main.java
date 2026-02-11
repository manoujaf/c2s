package c2s.mjk;

/**
 * Main entry point demonstrating C2S library usage.
 *
 * This class shows three progressively advanced ways to use the C2S HTTP client:
 * 1. Basic - inline anonymous class with minimal setup
 * 2. Intermediate - with custom callback handling
 * 3. Advanced - extending C2S with a dedicated request class
 *
 * Each example demonstrates different patterns you can use in your own applications.
 */
public class Main {
    /**
     * Main method - entry point of the application.
     * Runs three examples sequentially with delays to show async behavior.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {

        System.out.println("=== C2S Library Usage Examples ===\n");

        // EXAMPLE 1: Quick and simple - create request inline
        System.out.println("Example 1: Basic usage (inline, anonymous class)");


        /// ////////////////

        basicExample();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n" + "=".repeat(40) + "\n");

        // EXAMPLE 2: With callback override for response handling
        System.out.println("Example 2: With response handling");
        callbackExample();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n" + "=".repeat(40) + "\n");

        // EXAMPLE 3: Advanced - Using custom InitRequest class
        System.out.println("Example 3: Extended C2S class (InitRequest)");
        extendedExample();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n=== All Examples Complete ===");
    }

    /**
     * Example 1: Simplest possible usage - inline anonymous class.
     *
     * Perfect for quick one-off requests or prototyping.
     * No need to create separate classes or complex setup.
     *
     * Pattern: Create → Configure → Start
     */
    static void basicExample() {
        // Create request with inline anonymous class implementation
        // Endpoint: https://jsonplaceholder.typicode.com (free test API)
        C2S request = new C2S("https://jsonplaceholder.typicode.com", "/posts", C2S.POST) {
            @Override
            public void onResponse(int responseCode, String response) {
                super.onResponse(responseCode, response);
                System.out.println("✓ Response received:");
                System.out.println(response);
                System.exit(0);
            }
        };

        // Add parameters using fluent API (method chaining)
        // These become JSON body for POST requests
        request.addParameter("title", "Basic Request")
               .addParameter("body", "Using C2S without any customization")
               .addParameter("userId", "1");

        // Start the async HTTP request (runs in background thread)
        request.start();
    }

    /**
     * Example 2: Custom callback handling and timeout configuration.
     *
     * Shows how to:
     * - Override specific callbacks (onFailure, onResponse, etc.)
     * - Configure connection timeouts
     * - Handle different response codes
     *
     * Override only the callbacks you need - others use default behavior.
     */
    static void callbackExample() {
        C2S request = new C2S("https://jsonplaceholder.typicode.com", "/posts", C2S.POST) {
            @Override
            public void onFailure(int code, String response) {
                // Custom error handling logic
                if (code >= 200 && code < 300) {
                    // Sometimes successful responses come through onFailure
                    System.out.println("✓ Success (HTTP " + code + ")");
                    System.out.println("  Data: " + response.substring(0, Math.min(60, response.length())) + "...");
                } else {
                    System.out.println("✗ Error (HTTP " + code + ")");
                }
            }
        };

        // Configure timeouts and request parameters
        request.setConnectTimeout(5000);  // 5 second connection timeout
        request.addParameter("title", "Custom Response")
               .addParameter("body", "With callback handling");

        request.start();
    }

    /**
     * Example 3: Professional pattern - dedicated request class (InitRequest).
     *
     * This is the recommended approach for production applications because it:
     * - Encapsulates request logic in a reusable class
     * - Provides type-safe callback interfaces
     * - Enables async processing with onResponseAsync
     * - Keeps business logic separate from network code
     * - Supports method chaining for clean API
     *
     * See InitRequest.java for the full implementation.
     */
    static void extendedExample() {
        // Create instance of custom request class
        // InitRequest extends C2S and adds initialization-specific logic
        InitRequest request = new InitRequest("https://jsonplaceholder.typicode.com", "/posts")
                .setLoggingEnabled(true)         // Enable debug logging
                .setConnectTimeout(5000);        // 5 second timeout

        // Set custom success and error handlers using lambda expressions
        // These provide clean, type-safe callback APIs
        request.onSuccess(serverConfig -> {
                    System.out.println("✓ Initialization successful!");
                    System.out.println("  Config: " + serverConfig.toString());
                })
                .onError((code, message) -> {
                    System.out.println("✗ Initialization failed!");
                    System.out.println("  Error code: " + code);
                    System.out.println("  Message: " + message);
                });

        // Start the async request
        // onInit() → HTTP request → onResponseAsync() → onResponse() → callback
        request.start();
    }
}