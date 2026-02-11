package c2s.mjk;

// Simulates the deprecated AsyncTask pattern
public abstract class Async extends Thread {

    private UiExecutor uiExecutor;

    public Async() {
    }

    // Set executor to run onPostExecute on UI thread
    public void setUiExecutor(UiExecutor executor) {
        this.uiExecutor = executor;
    }

    @Override
    public void run() {
        super.run();
        doInBackground();
        if (uiExecutor != null) {
            uiExecutor.execute(this::onPostExecute);
        } else {
            onPostExecute();
        }
    }

    // Called to perform background work
    public abstract void doInBackground();

    // Called after background work completes
    public abstract void onPostExecute();
}