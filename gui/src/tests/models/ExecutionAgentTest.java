package models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

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

        Assertions.assertTrue(executionAgent.executeLater(Arrays.stream(command).toList(), null));
        Assertions.assertTrue(executionAgent.executeLater(Arrays.stream(command).toList(), null));
    }


    /**
     * This is a UNSTABLE test that may fail on poor CPU performance allocated to this process
     */
    @Test
    void invokeExecutionTest() throws InterruptedException {
        String[] command = {"help"};
        AtomicBoolean valid1 = new AtomicBoolean(false);
        AtomicBoolean valid2 = new AtomicBoolean(false);

        executionAgent.executeLater(Arrays.stream(command).toList(), () -> valid1.set(true));
        executionAgent.executeLater(Arrays.stream(command).toList(), () -> valid2.set(true));

        // wait for no execution occur
        Thread.sleep(100);

        Assertions.assertFalse(valid1.get());
        Assertions.assertFalse(valid2.get());

        // invoke execution
        executionAgent.invokeExecution();

        // wait for execution done
        Thread.sleep(100);

        Assertions.assertTrue(valid1.get());
        Assertions.assertTrue(valid2.get());
    }

}