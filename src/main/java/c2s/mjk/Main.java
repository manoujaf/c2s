package c2s.mjk;

/**
 * Main entry point for the C2S (Command & Control Server) application.
 * Demonstrates simple usage of C2S class for making HTTP requests.
 */
public class Main {
    /**
     * Main method - entry point of the application.
     * Shows 3 ways to use C2S - from simplest to most customized.
     *
     * @param args command line arguments (not used in this implementation)
     */
    public static void main(String[] args) {

        System.out.println("=== C2S Usage Examples ===\n");

        // EXAMPLE 1: Simplest - just create and use C2S directly
        System.out.println("Example 1: Basic usage (no customization)");


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
     * Example 1: Basic C2S usage without any customization.
     * Just create, configure, and send - that's it!
     */
    static void basicExample() {
        // Create request directly - no extending needed
        C2S request = new C2S("https://jsonplaceholder.typicode.com", "/posts", C2S.POST) {
            @Override
            public void onResponse(int responseCode, String response) {
                super.onResponse(responseCode, response);
                System.out.println(response);
                System.exit(0);
            }
        };

        // Add parameters with method chaining
        request.addParameter("title", "Basic Request")
               .addParameter("body", "Using C2S without any customization")
               .addParameter("userId", "1");

        // Start request
        request.start();
    }

    /**
     * Example 2: C2S with callback customization.
     * Override only the callbacks you need.
     */
    static void callbackExample() {
        C2S request = new C2S("https://jsonplaceholder.typicode.com", "/posts", C2S.POST) {
            @Override
            public void onFailure(int code, String response) {
                if (code >= 200 && code < 300) {
                    System.out.println("✓ Success (HTTP " + code + ")");
                    System.out.println("  Data: " + response.substring(0, Math.min(60, response.length())) + "...");
                } else {
                    System.out.println("✗ Error (HTTP " + code + ")");
                }
            }
        };

        // Configure and send
        request.setConnectTimeout(5000);
        request.addParameter("title", "Custom Response")
               .addParameter("body", "With callback handling");

        request.start();
    }

    /**
     * Example 3: Extended C2S class with full customization.
     * Shows how to create a dedicated request class for complex scenarios.
     */
    static void extendedExample() {
        InitRequest request = new InitRequest("https://jsonplaceholder.typicode.com", "/posts")
                .setLoggingEnabled(true)
                .setConnectTimeout(5000);

        // Set custom success and error handlers
        request.onSuccess(serverConfig -> {
                    System.out.println("✓ Initialization successful!");
                    System.out.println("  Config: " + serverConfig.toString());
                })
                .onError((code, message) -> {
                    System.out.println("✗ Initialization failed!");
                    System.out.println("  Error: " + message);
                });

        // Start the request
        request.start();
    }
}