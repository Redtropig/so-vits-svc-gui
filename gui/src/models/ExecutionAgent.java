package models;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Execution Agent
 * @responsibility Execute sequence of commands in a new Thread.
 * @design SINGLETON
 */
public class ExecutionAgent {
    public static final String PYTHON_EXE_PATH = ".\\workenv\\python.exe";
    public static final String SLICER_PATH = ".\\audio-slicer-main\\slicer2.py";

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
     * Schedule a task to execute the command and then its afterExecution.
     * if afterExecution == null, the same effect as afterExecution is ()->{}.
     * @param command NON-EMPTY command with its arguments.
     * @param afterExecution to run AFTER the command execution
     * @return true -> task scheduled, false otherwise.
     */
    public boolean executeLater(List<String> command, Runnable afterExecution) {
        // check if command is empty
        if (command.isEmpty()) {
            return false;
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();

        // Schedule in Queue
        return taskQueue.offer(() -> {
            try {
                // Run the process, then do after-task execution
                processBuilder.start().onExit().thenRun(afterExecution == null ? ()->{} : afterExecution);
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
