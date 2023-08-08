package models;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Execution Agent
 * @responsibility Execute commands in a new Thread.
 * @design SINGLETON
 */
public class ExecutionAgent {
    private static ExecutionAgent executionAgent;

    private final Queue<Runnable> taskQueue;

    private ExecutionAgent() {
        taskQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Get the singleton ExecutionAgent instance.
     * @return the ExecutionAgent instance if present, otherwise create new.
     */
    public static ExecutionAgent getExecutionAgent() {
        return (executionAgent == null) ? (executionAgent = new ExecutionAgent()) : (executionAgent);
    }

    /**
     * Schedule a task to execute the command.
     * @param command command with its arguments.
     * @return true -> task scheduled, false otherwise.
     */
    public boolean executeLater(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();

        return taskQueue.offer(() -> {
            try {
                processBuilder.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Execute all scheduled tasks orderly.
     * When this method is in RUNNABLE_STATE -> this ExecutionAgent is in EXECUTABLE_STATE.
     */
    private synchronized void execute() {
        while (!taskQueue.isEmpty()) {
            taskQueue.remove().run();
        }
    }

    /**
     * Turn this ExecutionAgent into EXECUTABLE_STATE.
     * EXECUTABLE_STATE: execute all scheduled tasks orderly ASAP.
     */
    public void invokeExecution() {
        new Thread(this::execute).start();
    }

}
