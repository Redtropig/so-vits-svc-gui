package models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class ExecutionAgentTest {
    private ExecutionAgent executionAgent;

    @BeforeEach
    void setUp() {
        executionAgent = ExecutionAgent.getExecutionAgent();
    }

    @Test
    void getExecutionAgentSingletonTest() {
        ExecutionAgent executionAgent2 = ExecutionAgent.getExecutionAgent();
        ExecutionAgent executionAgent3 = ExecutionAgent.getExecutionAgent();

        Assertions.assertSame(executionAgent, executionAgent2);
        Assertions.assertSame(executionAgent2, executionAgent3);
    }

    @Test
    void executeLaterScheduleTest() {
        String[] command = {"ping", "127.0.0.1"};

        Assertions.assertTrue(executionAgent.executeLater(Arrays.stream(command).toList()));
        Assertions.assertTrue(executionAgent.executeLater(Arrays.stream(command).toList()));
    }

}