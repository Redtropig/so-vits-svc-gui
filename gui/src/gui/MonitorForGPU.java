package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Locale;

import static gui.GUI.*;

/**
 * GPU Status Monitor
 * @responsibility Display & Auto-Refresh GPUs' status
 */
public class MonitorForGPU extends JFrame {
    private static final String FRAME_TITLE = "GPU Monitor";
    private static final int GPU_STATUS_SERVER_PORT = 43687;
    private static final long REFRESH_INTERVAL = 1000; // ms
    private static final long CONNECT_RETRY_INTERVAL = 1000; // ms

    private JTextArea displayArea;
    private JPanel monitorPanel;

    private Thread autoRefresh;

    protected MonitorForGPU() {

        /* Global Settings */
        java.util.Locale.setDefault(Locale.ENGLISH);

        /* UI Frame Settings */
        setTitle(FRAME_TITLE);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(new ImageIcon(ICON_PATH).getImage());
        setResizable(false);
        setVisible(true);
        setContentPane(monitorPanel);

        pack();

        // Schedule GPU status auto-refresh
        registerAutoRefresh();
    }

    /**
     * Register GPU-status auto-refresher in a new Thread.
     */
    private void registerAutoRefresh() {
        ProcessBuilder gpuQueryBuilder = new ProcessBuilder("nvidia-smi.exe");

        autoRefresh = new Thread(() -> {
            displayArea.setText("Attempting to retrieve GPU Info...");

            // Refresh Loop
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // At Foreground
                    if (isShowing()) {
                        Process gpuQuery;
                        Socket gpuSocket = null;
                        InputStream gpuStatusInputStream;

                        // Connected to Server?
                        if (remoteAgent != null) {
                            setTitle(FRAME_TITLE + " (remote)");
                            gpuSocket = new Socket(remoteAgent.getInetAddress(), GPU_STATUS_SERVER_PORT);
                            gpuStatusInputStream = gpuSocket.getInputStream();
                        } else {
                            setTitle(FRAME_TITLE + " (local)");
                            gpuQuery = gpuQueryBuilder.start();
                            gpuStatusInputStream = gpuQuery.getInputStream();
                        }

                        // get new GPU-status
                        StringBuilder displayBuffer = new StringBuilder();
                        BufferedReader in = new BufferedReader(new InputStreamReader(gpuStatusInputStream,
                                CHARSET_DISPLAY_DEFAULT));
                        String line;
                        while ((line = in.readLine()) != null) {
                            displayBuffer.append(line).append('\n');
                        }
                        in.close();

                        // End Connection
                        if (gpuSocket != null) {
                            gpuSocket.close();
                        }

                        // update display area
                        EventQueue.invokeLater(() -> {
                            int selectionStart = displayArea.getSelectionStart();
                            int selectionEnd = displayArea.getSelectionEnd();
                            displayArea.setText(displayBuffer.toString());
                            displayArea.setSelectionStart(selectionStart);
                            displayArea.setSelectionEnd(selectionEnd);
                            // Adjust Window Size
                            pack();
                        });
                    }
                    // Refresh Interval
                    Thread.sleep(REFRESH_INTERVAL);
                } catch (IOException ex) {
                    try {
                        EventQueue.invokeLater(() -> {
                            displayArea.setText("Re-attempting to connect Server GPU Monitor...");
                            pack();
                        });
                        Thread.sleep(CONNECT_RETRY_INTERVAL);
                    } catch (InterruptedException e) {
                        return;
                    }
                    continue;
                } catch (InterruptedException ex) {
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
