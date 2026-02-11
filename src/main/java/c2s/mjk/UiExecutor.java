package c2s.mjk;

// Interface for executing tasks on the UI thread
public interface UiExecutor {
    void execute(Runnable runnable);
}