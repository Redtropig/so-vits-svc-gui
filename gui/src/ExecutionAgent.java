/**
 * Execution Agent
 * @responsibility Compose & Execute commands
 * @feature run in new Thread
 * @design SINGLETON
 */
public class ExecutionAgent {
    private static ExecutionAgent executionAgent;

    private ExecutionAgent() {

    }

    /**
     * Get the singleton ExecutionAgent instance
     * @return the ExecutionAgent instance if present, otherwise create new.
     */
    public static ExecutionAgent getExecutionAgent() {
        return (executionAgent == null) ? (executionAgent = new ExecutionAgent()) : (executionAgent);
    }


}
