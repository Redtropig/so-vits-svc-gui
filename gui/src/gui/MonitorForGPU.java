package gui;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * GPU Status Monitor
 * @responsibility Display & Auto-Refresh GPUs' status
 */
public class MonitorForGPU extends JFrame {
    private static final String FRAME_TITLE = "GPU Monitor";
    private static final long REFRESH_INTERVAL = 1000; // ms

    private JTextArea displayArea;
    private JPanel monitorPanel;

    private Thread autoRefresh;

    protected MonitorForGPU() {

        /* Global Settings */
        java.util.Locale.setDefault(Locale.ENGLISH);

        /* UI Frame Settings */
        setTitle(FRAME_TITLE);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(new ImageIcon(GUI.ICON_PATH).getImage());
        setResizable(false);
        setVisible(true);
        setContentPane(monitorPanel);

        // Schedule GPU status auto-refresh
        registerAutoRefresh();
    }

    /**
     * Register GPU-status auto-refresher in a new Thread.
     */
    private void registerAutoRefresh() {
        ProcessBuilder gpuQueryBuilder = new ProcessBuilder("nvidia-smi.exe");

        autoRefresh = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // At Foreground
                    if (isShowing()) {
                        Process gpuQuery = gpuQueryBuilder.start();

                        // get new GPU-status
                        StringBuilder displayBuffer = new StringBuilder();
                        BufferedReader in = new BufferedReader(new InputStreamReader(gpuQuery.getInputStream(),
                                GUI.CHARSET_DISPLAY_DEFAULT));
                        String line;
                        while ((line = in.readLine()) != null) {
                            displayBuffer.append(line).append('\n');
                        }
                        in.close();

                        // update display area
                        int selectionStart = displayArea.getSelectionStart();
                        int selectionEnd = displayArea.getSelectionEnd();
                        displayArea.setText(displayBuffer.toString());
                        displayArea.setSelectionStart(selectionStart);
                        displayArea.setSelectionEnd(selectionEnd);

                        // Adjust Window Size
                        Thread.sleep(1); // yield
                        pack();
                    }
                    // Refresh Interval
                    Thread.sleep(REFRESH_INTERVAL);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "GPU-Monitor");

        // Kill auto-refresher Thread on Frame closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                autoRefresh.interrupt();
            }
        });

        autoRefresh.start();
    }

}
