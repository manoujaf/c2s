package c2s.mjk;

/**
 * Abstract base class for asynchronous operations.
 * Simulates the deprecated Android AsyncTask pattern for Java environments.
 *
 * This class provides a thread-based execution model with three lifecycle phases:
 * - onPreExecute(): Executes before background work begins
 * - doInBackground(): Executes the main background work
 * - onPostExecute(): Executes after background work completes
 */
public abstract class Async extends Thread {

    /**
     * Constructor for Async class.
     * Initializes the asynchronous operation with default settings.
     */
    public Async() {
    }

    /**
     * Executes the async operation lifecycle.
     * Runs in a separate thread and executes the three lifecycle methods in sequence.
     */
    @Override
    public void run() {
        super.run();
        // Pre-execution phase: Initialize and prepare for background work
        onPreExecute();
        // Background execution phase: Perform main work in background thread
        doInBackground();
        // Post-execution phase: Handle results after background work completes
        onPostExecute();
    }

    /**
     * Called before background work begins.
     * Override to initialize UI elements, show progress indicators, etc.
     * Executes in the calling thread (not the background thread).
     */
    public abstract void onPreExecute();

    /**
     * Performs background work.
     * Override to implement the main work that should be done asynchronously.
     * Executes in the background thread.
     */
    public abstract void doInBackground();

    /**
     * Called after background work completes.
     * Override to update UI with results, handle completion tasks, etc.
     * Executes after doInBackground() completes.
     */
    public abstract void onPostExecute();

}