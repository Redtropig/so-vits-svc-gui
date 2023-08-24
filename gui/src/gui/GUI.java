package gui;

import models.ExecutionAgent;
import models.FileUsage;
import models.RemoteAgent;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.List;
import java.util.*;

import static models.ExecutionAgent.*;

public class GUI extends JFrame {

    private static final String PROGRAM_TITLE = "SoftVC VITS Singing Voice Conversion GUI";
    public static final Charset CHARSET_DISPLAY_DEFAULT = StandardCharsets.UTF_8;
    protected static final String ICON_PATH = ".\\gui\\data\\img\\GUI-Icon.png";
    private static final File SLICING_OUT_DIR_DEFAULT = new File(SO_VITS_SVC_DIR + "\\dataset_raw");
    private static final File PREPROCESS_OUT_DIR_DEFAULT = new File(SO_VITS_SVC_DIR + "\\dataset\\44k");
    private static final File INFERENCE_INPUT_DIR_DEFAULT = new File(SO_VITS_SVC_DIR + "\\raw");
    private static final File TRAINING_LOG_DIR_DEFAULT = new File(SO_VITS_SVC_DIR + "\\logs\\44k");
    private static final File TRAINING_CONFIG = new File(SO_VITS_SVC_DIR + "\\configs\\config.json");
    private static final File TRAINING_CONFIG_LOG = new File(TRAINING_LOG_DIR_DEFAULT + "\\config.json");
    private static final int JSON_STR_INDENT_FACTOR = 2;
    private static final int SLICING_MIN_INTERVAL_DEFAULT = 100; // ms
    private static final String AUDIO_FILE_OUT_FORMAT = "wav";
    private static final String[] AUDIO_FILE_EXTENSIONS_ACCEPTED = {"wav"};
    private static final String AUDIO_FILE_EXTENSIONS_DESCRIPTION = "Wave File(s)(*.wav)";
    private static final String SPEAKER_NAME_DEFAULT = "default-speaker";
    private static final int CONSOLE_LINE_COUNT_MAX = 512;
    private static final String SPEECH_ENCODER_DEFAULT = "vec768l12";
    private static final String[] SPEECH_ENCODERS = {
            "vec768l12",
            "vec256l9",
            "hubertsoft",
            "whisper-ppg",
            "cnhubertlarge",
            "dphubert",
            "whisper-ppg-large",
            "wavlmbase+"
    };
    private static final String F0_PREDICTOR_PREPROCESS_DEFAULT = "rmvpe";
    private static final String F0_PREDICTOR_INFER_DEFAULT = "pm";
    private static final String[] F0_PREDICTORS = {
            "crepe",
            "dio",
            "pm",
            "harvest",
            "rmvpe",
            "fcpe",
    };
    private static final int GPU_ID_DEFAULT = 0;
    private static final int BATCH_SIZE_DEFAULT = 4;
    private static final int LOG_INTERVAL_DEFAULT = 50;
    private static final int EVAL_INTERVAL_DEFAULT = 200;
    private static final int KEEP_LAST_N_MODEL_DEFAULT = 1;
    private static final String TRAINING_BTN_TEXT = "Start Training";
    private static final String INFERENCE_BTN_TEXT = "Start Inference";
    private static final String REGEX_TRAINED_MODEL_NAME = "^G_[1-9]\\d*\\.pth$";
    private static final float CLIP_INFER_DEFAULT = 0;
    private static final int PITCH_SHIFT_INFER_DEFAULT = 0;


    private JPanel mainPanel;
    private JPanel datasetPrepPanel;
    private JTextArea consoleArea;
    private JTextField voiceChosenFld;
    private JButton voiceFileChooserBtn;
    private JTextField sliceOutDirFld;
    private JButton voiceSlicerBtn;
    private JButton clearSliceOutDirBtn;
    private JScrollPane consolePanel;
    private JComboBox<String> speechEncoderCbBx;
    private JComboBox<String> f0PredictorPreproCbBx;
    private JCheckBox loudnessEmbedCkBx;
    private JButton preprocessBtn;
    private JTextField preprocessOutDirFld;
    private JButton clearPreprocessOutDirBtn;
    private JPanel trainingPanel;
    private JSpinner gpuIdSpinner;
    private JSpinner batchSizeSpinner;
    private JSpinner logIntervalSpinner;
    private JSpinner evalIntervalSpinner;
    private JSpinner keepLastNModelSpinner;
    private JCheckBox allInMemCkBx;
    private JRadioButton fp32Btn;
    private JRadioButton fp16Btn;
    private JRadioButton bf16Btn;
    private JButton gpuMonitorBtn;
    private JButton startTrainingBtn;
    private JTextField speakerNameFld;
    private JTextField trainLogDirFld;
    private JButton clearTrainLogDirBtn;
    private JComboBox<String> f0PredictorInferCbBx;
    private JComboBox<String> speakerPickCbBx;
    private JButton inferenceBtn;
    private JTextField vocalChosenFld;
    private JButton vocalChooserBtn;
    private JPanel inferencePanel;
    private JCheckBox nsfHiFiGanCkBx;
    private JProgressBar currentVoiceFileTransProgress;
    private JProgressBar totalVoiceFilesTransProgress;
    private JProgressBar currentVocalFileTransProgress;
    private JProgressBar totalVocalFilesTransProgress;
    private ButtonGroup floatPrecisionGroup;

    private final ExecutionAgent executionAgent;

    private File[] voiceAudioFiles;
    private File[] vocalAudioFiles;
    private RemoteAgent remoteAgent;

    public GUI() {

        /* Global Settings */
        java.util.Locale.setDefault(Locale.ENGLISH);
        // redirect System out & err PrintStream
        PrintStream printGUI = getPrintStream();
        redirectSystemOutErrStream(printGUI, printGUI);

        /* UI Frame Settings */
        setTitle(PROGRAM_TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(new ImageIcon(ICON_PATH).getImage());
        setResizable(false);
        setVisible(true);

        /* Field Assignments */
        executionAgent = getExecutionAgent();

        /* Components */
        createUIComponents();
        setContentPane(mainPanel);

        // Kill all sub-processes on Frame closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Process currentProcess = executionAgent.getCurrentProcess();
                if (currentProcess != null) {
                    currentProcess.descendants().forEach(ProcessHandle::destroy);
                    currentProcess.destroy();
                }
            }
        });

        pack();
    }

    private void createUIComponents() {
        createMenuBar();
        createDatasetPrepArea();
        createPreprocessArea();
        createTrainingArea();
        createInferenceArea();
        createConsoleArea();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu remoteMenu = new JMenu("Remote");
        JMenu currentConnection = new JMenu("@localhost");

        /* Remote */
        JMenuItem connectItm = new JMenuItem("Connect to...", KeyEvent.VK_C);
        JMenuItem disconnectItm = new JMenuItem("Disconnect", KeyEvent.VK_D);
        // Connect To
        connectItm.addActionListener((e) -> {
            // ip_port[0] -> hostname, ip_port[1] -> port
            String[] ip_port;
            try {
                ip_port = JOptionPane.showInputDialog(
                        this,
                        "Connect to <IP:Port>:",
                        "Connect To...",
                        JOptionPane.QUESTION_MESSAGE
                ).split(":", 2);
            } catch (NullPointerException ex) { // User canceled IP:Port input
                return;
            }

            // Connect to Server
            new Thread(() -> {
                // parse String[] to InetAddress
                InetSocketAddress address;
                try {
                    address = new InetSocketAddress(ip_port[0], Integer.parseInt(ip_port[1]));
                    System.out.println("[INFO] Connecting...");
                    remoteAgent = new RemoteAgent(address);
                    disconnectItm.setEnabled(true);
                    currentConnection.setText("@" + remoteAgent.getInetAddress() + ":" + remoteAgent.getPort());
                    System.out.println("[INFO] Connection Established.");
                } catch (ConnectException ex) {
                    System.out.println("[INFO] " + ex.getMessage());
                } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | IOException ex) { // Invalid input
                    System.err.println("[!] Connect: <IP:Port> address invalid.");
                }
            }).start();
        });

        // Disconnect
        disconnectItm.setEnabled(false);
        disconnectItm.addActionListener((e) -> {
            remoteAgent.close();
            remoteAgent = null;
            disconnectItm.setEnabled(false);
            currentConnection.setText("@localhost");
            System.out.println("[INFO] Disconnected from the server.");
        });

        remoteMenu.setMnemonic(KeyEvent.VK_R);
        remoteMenu.add(connectItm);
        remoteMenu.add(disconnectItm);
        /* End Remote */

        /* Current Connection */
        currentConnection.setEnabled(false);
        /* End Current Connection */

        menuBar.add(remoteMenu);
        menuBar.add(currentConnection);
        setJMenuBar(menuBar);
    }

    private void createDatasetPrepArea() {

        /* Voice File Chooser */
        voiceFileChooserBtn.addActionListener(e -> {
            JFileChooser voiceFileChooser = new JFileChooser();
            voiceFileChooser.setAcceptAllFileFilterUsed(false);
            voiceFileChooser.setMultiSelectionEnabled(true);
            voiceFileChooser.setFileFilter(new FileNameExtensionFilter(AUDIO_FILE_EXTENSIONS_DESCRIPTION,
                    AUDIO_FILE_EXTENSIONS_ACCEPTED));

            // Choose voice file(s)
            if (voiceFileChooser.showOpenDialog(datasetPrepPanel) == JFileChooser.APPROVE_OPTION) {
                voiceAudioFiles = voiceFileChooser.getSelectedFiles();

                // Update file-chosen text field
                voiceChosenFld.setEnabled(true);
                voiceChosenFld.setText(String.join(";", Arrays.stream(voiceAudioFiles).map(File::getName).toList()));
            }
        });

        /* Voice File Slicer */
        sliceOutDirFld.setText(SLICING_OUT_DIR_DEFAULT.getPath());
        voiceSlicerBtn.addActionListener(e -> {
            // No selected voice file
            if (voiceAudioFiles == null || voiceAudioFiles.length == 0) {
                System.err.println("[!] Please SELECT at least 1 VOICE file.");
                return;
            }

            // user input speaker name
            String speakerName = JOptionPane.showInputDialog(
                    datasetPrepPanel,
                    "Set speaker name:",
                    "Speaker",
                    JOptionPane.QUESTION_MESSAGE
            );
            // if user canceled the speaker-name input dialog
            if (speakerName == null) {
                return;
            }
            speakerName = speakerName.trim();
            if (speakerName.isBlank()) {  // handle empty Name
                speakerName = SPEAKER_NAME_DEFAULT;
            }

            // disable related interactions before slicing
            voiceSlicerBtn.setEnabled(false);
            clearSliceOutDirBtn.setEnabled(false);

            /* Connected to Server */
            if (remoteAgent != null) {

                // Transfer voice files
                new SwingWorker<Void, Integer>() {
                    @Override
                    protected Void doInBackground() throws IOException {
                        System.out.println("[INFO] Uploading Voice File(s)...");
                        totalVoiceFilesTransProgress.setMaximum(voiceAudioFiles.length);
                        totalVoiceFilesTransProgress.setString("0/" + totalVoiceFilesTransProgress.getMaximum());

                        for (int i = 0; i < voiceAudioFiles.length; ) {
                            remoteAgent.transferFileToServer(
                                    FileUsage.TO_SLICE,
                                    voiceAudioFiles[i],
                                    currentVoiceFileTransProgress
                            );
                            publish(++i);
                        }

                        done();
                        return null;
                    }

                    @Override
                    protected void process(List<Integer> completedCount) {
                        totalVoiceFilesTransProgress.setValue(completedCount.get(completedCount.size() - 1));
                        totalVoiceFilesTransProgress.setString(
                                completedCount + "/" + totalVoiceFilesTransProgress.getMaximum()
                        );
                    }

                    @Override
                    protected void done() {
                        System.out.println("[INFO] All Voice File(s) are Uploaded to Server.");
                    }
                }.execute();

                // Slice on Server
                // TODO


                // enable related interactions after batch execution
                voiceSlicerBtn.setEnabled(true);
                clearSliceOutDirBtn.setEnabled(true);
            }
            /* End Connected to Server */

            System.out.println("[INFO] Slicing Audio(s)...");
            // slice each voice file
            for (int i = voiceAudioFiles.length - 1; i >= 0; i--) {
                File voiceFile = voiceAudioFiles[i];

                // Command construction
                String[] command = {
                        PYTHON_EXE.getAbsolutePath(),
                        SLICER_PY.getAbsolutePath(),
                        voiceFile.getPath(),
                        "--out",
                        sliceOutDirFld.getText() + "\\" + speakerName,
                        "--min_interval",
                        String.valueOf(SLICING_MIN_INTERVAL_DEFAULT)
                };

                // schedule a task
                int finalI = i;
                executionAgent.executeLater(
                        command,
                        null,
                        (process) -> {
                            if (process.exitValue() == 0) {
                                System.out.println("[INFO] Slicing completed: " + voiceFile.getName());
                            } else {
                                System.out.println("[ERROR] \"" +
                                        SLICER_PY.getName() +
                                        "\" terminated unexpectedly, exit code: " +
                                        process.exitValue()
                                );
                            }

                            if (finalI == 0) {
                                System.out.println("[INFO] All Slicing Done.");
                                // enable related interactions after batch execution
                                voiceSlicerBtn.setEnabled(true);
                                clearSliceOutDirBtn.setEnabled(true);
                            }
                        }
                );
            }

            // execute ASAP
            executionAgent.invokeExecution();
        });

        /* Slice Out Dir Cleaner */
        clearSliceOutDirBtn.addActionListener(e -> {
            for (File subDir : Objects.requireNonNull(SLICING_OUT_DIR_DEFAULT.listFiles(File::isDirectory))) {
                removeDirectory(subDir);
            }
        });
    }

    private void createPreprocessArea() {

        /* Speech Encoder */
        // add entries
        for (String encoder : SPEECH_ENCODERS) {
            speechEncoderCbBx.addItem(encoder);
        }
        speechEncoderCbBx.setSelectedItem(SPEECH_ENCODER_DEFAULT);

        /* F0 Predictor */
        // add entries
        for (String predictor : F0_PREDICTORS) {
            f0PredictorPreproCbBx.addItem(predictor);
        }
        f0PredictorPreproCbBx.setSelectedItem(F0_PREDICTOR_PREPROCESS_DEFAULT);

        /* Loudness Embedding */
        loudnessEmbedCkBx.addActionListener(e -> {
            if (loudnessEmbedCkBx.isSelected()) {
                speechEncoderCbBx.setEnabled(false);
                speechEncoderCbBx.setSelectedItem("vec768l12");
            } else {
                speechEncoderCbBx.setEnabled(true);
            }
        });

        /* Preprocessor */
        preprocessOutDirFld.setText(PREPROCESS_OUT_DIR_DEFAULT.getPath());
        preprocessBtn.addActionListener(e -> {

            // dataset_raw: nothing is prepared
            if (Objects.requireNonNull(SLICING_OUT_DIR_DEFAULT.listFiles(File::isDirectory)).length == 0) {
                System.err.println("[!] Please SLICE at least 1 VOICE file.");
                return;
            }

            System.out.println("[INFO] Preprocessing Dataset...");
            // disable related interactions before preprocess
            preprocessBtn.setEnabled(false);
            clearPreprocessOutDirBtn.setEnabled(false);

            resampleAudio();
            splitDatasetAndGenerateConfig();
            generateHubertAndF0();
        });

        /* Preprocess Out Dir Cleaner */
        clearPreprocessOutDirBtn.addActionListener(e -> {
            for (File subDir : Objects.requireNonNull(PREPROCESS_OUT_DIR_DEFAULT.listFiles(File::isDirectory))) {
                removeDirectory(subDir);
            }
        });

    }

    private void createTrainingArea() {
        ChangeListener minZeroGuard = e -> {
            JSpinner minZeroSpinner = (JSpinner) e.getSource();
            minZeroSpinner.setValue(Math.max((Integer) minZeroSpinner.getValue(), 0));
        };
        ChangeListener minOneGuard = e -> {
            JSpinner minOneSpinner = (JSpinner) e.getSource();
            minOneSpinner.setValue(Math.max((Integer) minOneSpinner.getValue(), 1));
        };

        /* GPU ID */
        gpuIdSpinner.setValue(GPU_ID_DEFAULT);
        gpuIdSpinner.addChangeListener(minZeroGuard);

        /* Batch Size */
        batchSizeSpinner.setValue(BATCH_SIZE_DEFAULT);
        batchSizeSpinner.addChangeListener(minOneGuard);

        /* Log Interval */
        logIntervalSpinner.setValue(LOG_INTERVAL_DEFAULT);
        logIntervalSpinner.addChangeListener(minOneGuard);

        /* Eval Interval */
        evalIntervalSpinner.setValue(EVAL_INTERVAL_DEFAULT);
        evalIntervalSpinner.addChangeListener(minOneGuard);

        /* Keep Last N Models */
        keepLastNModelSpinner.setValue(KEEP_LAST_N_MODEL_DEFAULT);
        keepLastNModelSpinner.addChangeListener(minZeroGuard);

        /* GPU Monitor */
        gpuMonitorBtn.addActionListener(e -> new MonitorForGPU());

        /* Trainer */
        trainLogDirFld.setText(TRAINING_LOG_DIR_DEFAULT.getPath());
        startTrainingBtn.setText(TRAINING_BTN_TEXT);
        startTrainingBtn.addActionListener(e -> {
            // Train or Abort
            if (startTrainingBtn.getText().equals(TRAINING_BTN_TEXT)) {
                startTrainingBtn.setText("Abort");
                clearTrainLogDirBtn.setEnabled(false);

                // if resume training
                if (TRAINING_CONFIG_LOG.exists()) {
                    loadTrainingConfig();
                }

                overwriteTrainingConfig();
                displaySpeakersName();
                startTraining();
            } else { // Abort
                executionAgent.getCurrentProcess().descendants().forEach(ProcessHandle::destroy);
            }
        });

        /* Train Log Cleaner */
        clearTrainLogDirBtn.addActionListener(e -> {
            for (File subFile : Objects.requireNonNull(TRAINING_LOG_DIR_DEFAULT.listFiles((f) ->
                    !(f.getName().equals("diffusion") ||
                            f.getName().equals("D_0.pth") ||
                            f.getName().equals("G_0.pth"))))) {
                if (subFile.isDirectory()) {
                    removeDirectory(subFile);
                } else {
                    subFile.delete();
                    System.out.println("[INFO] File Removed: \"" + subFile.getPath() + "\"");
                }
            }
        });
    }

    private void createInferenceArea() {

        /* Vocal File Chooser */
        vocalChooserBtn.addActionListener(e -> {
            JFileChooser vocalFileChooser = new JFileChooser();
            vocalFileChooser.setAcceptAllFileFilterUsed(false);
            vocalFileChooser.setMultiSelectionEnabled(true);
            vocalFileChooser.setFileFilter(new FileNameExtensionFilter(AUDIO_FILE_EXTENSIONS_DESCRIPTION,
                    AUDIO_FILE_EXTENSIONS_ACCEPTED));

            // Choose vocal file(s)
            if (vocalFileChooser.showOpenDialog(inferencePanel) == JFileChooser.APPROVE_OPTION) {
                vocalAudioFiles = vocalFileChooser.getSelectedFiles();

                // Update file-chosen text field
                vocalChosenFld.setEnabled(true);
                vocalChosenFld.setText(String.join(";", Arrays.stream(vocalAudioFiles).map(File::getName).toList()));

                // Copy vocal files into INFERENCE_INPUT_DIR_DEFAULT
                // & vocalAudioFiles follows the file[] reference as copied
                vocalAudioFiles = Arrays.stream(vocalAudioFiles).map((f) -> {
                    try {
                        File copied = Files.copy(
                                f.toPath(),
                                new File(INFERENCE_INPUT_DIR_DEFAULT.getPath() + "\\" + f.getName()).toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                        ).toFile();

                        // register copied file as temporary
                        copied.deleteOnExit();

                        return copied;
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }).toList().toArray(vocalAudioFiles);

                // Unlock Speaker picking ComboBox
                speakerPickCbBx.removeAllItems();
                getConfigJsonObject().getJSONObject("spk").keySet().forEach((spk) -> {
                    speakerPickCbBx.addItem(spk);
                });
                speakerPickCbBx.setEnabled(true);
            }
        });

        /* F0 Predictor for Inference */
        // add entries
        for (String predictor : F0_PREDICTORS) {
            f0PredictorInferCbBx.addItem(predictor);
        }
        f0PredictorInferCbBx.setSelectedItem(F0_PREDICTOR_INFER_DEFAULT);

        /* Inference */
        inferenceBtn.addActionListener(e -> {
            // raw: no vocal file is chosen
            if (vocalAudioFiles == null) {
                System.err.println("[!] Please SELECT at least 1 VOCAL file.");
                return;
            }

            // Infer or Abort
            if (inferenceBtn.getText().equals(INFERENCE_BTN_TEXT)) {
                inferenceBtn.setText("Abort");
                System.out.println("[INFO] Inference Running... (this may take minutes without console output)");

                startInference();
            } else { // Abort
                executionAgent.getCurrentProcess().destroy();
            }
        });
    }

    private void createConsoleArea() {
        JScrollBar verticalScrollBar = consolePanel.getVerticalScrollBar();

        /* Console Vertical Scroll Bar */
        verticalScrollBar.addAdjustmentListener(e -> {
            // limit the maximum line count of console history
            long exceededLineCount = consoleArea.getLineCount() - CONSOLE_LINE_COUNT_MAX;
            if (exceededLineCount > 0) {
                consoleArea.setText(
                        String.join("\n", consoleArea.getText().lines().skip(exceededLineCount).toList()) + '\n'
                );
            }
        });

        /* Console Popup Menu */
        JPopupMenu consoleMenu = new JPopupMenu("console popup menu");
        JMenuItem clearConsoleItm = new JMenuItem("clear");
        clearConsoleItm.addActionListener(e -> consoleArea.setText(""));
        consoleMenu.add(clearConsoleItm);
        consoleArea.setComponentPopupMenu(consoleMenu);
    }

    /**
     * Remove a directory.
     *
     * @param directory directory to be removed
     * @dependency Windows OS
     */
    private void removeDirectory(File directory) {
        if (directory.isDirectory()) {
            String[] command = {"cmd.exe", "/c", "rmdir", "/s", "/q", directory.getAbsolutePath()};

            // schedule a task
            executionAgent.executeLater(
                    command,
                    null,
                    (process) -> {
                        if (process.exitValue() == 0) {
                            System.out.println("[INFO] Directory Removed: \"" + directory.getPath() + "\"");
                        } else {
                            System.out.println("[ERROR] \"" +
                                    command[0] +
                                    "\" terminated unexpectedly, exit code: " +
                                    process.exitValue()
                            );
                        }
                    });

            // execute ASAP
            executionAgent.invokeExecution();
        }
    }

    /**
     * Resample audios @src -> @dest, to 44100Hz mono.
     *
     * @src .\dataset_raw
     * @dest .\dataset\44k
     */
    private void resampleAudio() {
        String[] command = {
                PYTHON_EXE.getAbsolutePath(),
                RESAMPLER_PY.getAbsolutePath(),
        };

        executionAgent.executeLater(
                command,
                SO_VITS_SVC_DIR,
                (process) -> {
                    if (process.exitValue() == 0) {
                        System.out.println("[INFO] Resampled to 44100Hz mono.");
                    } else {
                        System.out.println("[ERROR] \"" +
                                RESAMPLER_PY +
                                "\" terminated unexpectedly, exit code: " +
                                process.exitValue()
                        );
                    }
                }
        );
        executionAgent.invokeExecution();
    }

    /**
     * Split the dataset into training and validation sets, and generate configuration files.
     */
    private void splitDatasetAndGenerateConfig() {
        List<String> command = new ArrayList<>();
        command.add(PYTHON_EXE.getAbsolutePath());
        command.add(FLIST_CONFIGER_PY.getAbsolutePath());
        command.add("--speech_encoder");
        command.add((String) speechEncoderCbBx.getSelectedItem());
        if (loudnessEmbedCkBx.isSelected()) {
            command.add("--vol_aug");
        }

        executionAgent.executeLater(
                command,
                SO_VITS_SVC_DIR,
                (process) -> {
                    if (process.exitValue() == 0) {
                        System.out.println("[INFO] Training Set, Validation Set, Configuration Files Created.");
                    } else {
                        System.out.println("[ERROR] \"" +
                                FLIST_CONFIGER_PY.getName() +
                                "\" terminated unexpectedly, exit code: " +
                                process.exitValue()
                        );
                    }
                }
        );
        executionAgent.invokeExecution();
    }

    /**
     * Generate hubert and f0.
     */
    private void generateHubertAndF0() {
        String[] command = {
                PYTHON_EXE.getAbsolutePath(),
                HUBERT_F0_GENERATOR_PY.getAbsolutePath(),
                "--f0_predictor",
                (String) f0PredictorPreproCbBx.getSelectedItem()
        };

        executionAgent.executeLater(
                command,
                SO_VITS_SVC_DIR,
                (process) -> {
                    if (process.exitValue() == 0) {
                        System.out.println("[INFO] Hubert & F0 Predictor Generated.");
                    } else {
                        System.out.println("[ERROR] \"" +
                                HUBERT_F0_GENERATOR_PY.getName() +
                                "\" terminated unexpectedly, exit code: " +
                                process.exitValue()
                        );
                    }

                    System.out.println("[INFO] Preprocessing Done.");
                    // enable related interactions after batch execution
                    preprocessBtn.setEnabled(true);
                    clearPreprocessOutDirBtn.setEnabled(true);
                });
        executionAgent.invokeExecution();
    }

    /**
     * Overwrite training config to TRAINING_CONFIG file
     */
    private void overwriteTrainingConfig() {
        // Get JSON Objects
        JSONObject configJsonObject = getConfigJsonObject();
        JSONObject trainJsonObject = configJsonObject.getJSONObject("train");

        // Commit values & Handle invalid user inputs (to previous valid setting)
        try {
            logIntervalSpinner.commitEdit();
        } catch (ParseException e) {
            logIntervalSpinner.updateUI();
        }
        try {
            evalIntervalSpinner.commitEdit();
        } catch (ParseException e) {
            evalIntervalSpinner.updateUI();
        }
        try {
            batchSizeSpinner.commitEdit();
        } catch (ParseException e) {
            batchSizeSpinner.updateUI();
        }
        try {
            keepLastNModelSpinner.commitEdit();
        } catch (ParseException e) {
            keepLastNModelSpinner.updateUI();
        }

        // Modify train JSON Object
        trainJsonObject.put("log_interval", (int) logIntervalSpinner.getValue());
        trainJsonObject.put("eval_interval", (int) evalIntervalSpinner.getValue());
        trainJsonObject.put("batch_size", (int) batchSizeSpinner.getValue());
        if (fp32Btn.isSelected()) {
            trainJsonObject.put("fp16_run", false);
        } else {
            trainJsonObject.put("fp16_run", true);
            String halfType;
            if (fp16Btn.isSelected()) {
                halfType = fp16Btn.getText();
            } else {
                halfType = bf16Btn.getText();
            }
            trainJsonObject.put("half_type", halfType);
        }
        trainJsonObject.put("keep_ckpts", (int) keepLastNModelSpinner.getValue());
        trainJsonObject.put("all_in_mem", allInMemCkBx.isSelected());

        // Write config JSON back to TRAINING_CONFIG
        configJsonObject.put("train", trainJsonObject);
        try (FileWriter configJsonWriter = new FileWriter(TRAINING_CONFIG)) {
            configJsonWriter.write(configJsonObject.toString(JSON_STR_INDENT_FACTOR));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load config from TRAINING_CONFIG_LOG if it exists, otherwise from TRAINING_CONFIG.
     */
    private void loadTrainingConfig() {
        // Get JSON Objects
        JSONObject configJsonObject = getConfigJsonObject();
        JSONObject trainJsonObject = configJsonObject.getJSONObject("train");

        // Load train JSON Object
        logIntervalSpinner.setValue(trainJsonObject.getInt("log_interval"));
        evalIntervalSpinner.setValue(trainJsonObject.getInt("eval_interval"));
        batchSizeSpinner.setValue(trainJsonObject.getInt("batch_size"));
        if (trainJsonObject.getBoolean("fp16_run")) {
            switch (trainJsonObject.getString("half_type")) {
                case "fp16" -> {
                    fp16Btn.setSelected(true);
                }
                case "bf16" -> {
                    bf16Btn.setSelected(true);
                }
            }
        } else {
            fp32Btn.setSelected(true);
        }
        keepLastNModelSpinner.setValue(trainJsonObject.getInt("keep_ckpts"));
        allInMemCkBx.setSelected(trainJsonObject.getBoolean("all_in_mem"));
    }

    /**
     * Display Speaker Names on speakerNameFld
     */
    private void displaySpeakersName() {
        JSONObject speakerJsonObject = getConfigJsonObject().getJSONObject("spk");
        speakerNameFld.setText(speakerJsonObject.keySet().toString());
    }

    /**
     * Get Training Config JSONObject.
     *
     * @return Config JSONObject from TRAINING_CONFIG_LOG if it exists, otherwise from TRAINING_CONFIG.
     */
    private static JSONObject getConfigJsonObject() {
        File loadSource = TRAINING_CONFIG_LOG.exists() ? TRAINING_CONFIG_LOG : TRAINING_CONFIG;

        // Load JSON String from loadSource
        try (BufferedReader in = Files.newBufferedReader(loadSource.toPath())) {
            // Parse JSON String to JSONObject
            return new JSONObject(in.lines().reduce("", String::concat));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Start Training with config.json
     */
    private void startTraining() {
        String[] command = {
                "cmd.exe",
                "/c",
                "set",
                "CUDA_VISIBLE_DEVICES=" + (int) gpuIdSpinner.getValue(),
                "&&",
                PYTHON_EXE.getAbsolutePath(),
                TRAIN_PY.getAbsolutePath(),
                "-c",
                TRAINING_CONFIG.getAbsolutePath(),
                "-m",
                "44k"
        };

        executionAgent.executeLater(
                command,
                SO_VITS_SVC_DIR,
                (process) -> {
                    if (process.exitValue() == 0) {
                        System.out.println("[INFO] Training Complete.");
                    } else {
                        System.out.println("[WARNING] \"" +
                                TRAIN_PY.getName() +
                                "\" interrupted, exit code: " +
                                process.exitValue()
                        );
                    }

                    startTrainingBtn.setText(TRAINING_BTN_TEXT);
                    clearTrainLogDirBtn.setEnabled(true);
                });
        executionAgent.invokeExecution();
    }

    /**
     * Start Inference
     */
    private void startInference() {
        // Get JSON Objects
        JSONObject configJsonObject = getConfigJsonObject();
        JSONObject modelJsonObject = configJsonObject.getJSONObject("model");

        // Construct command
        List<String> command = new ArrayList<>();
        command.add("cmd.exe");
        command.add("/c");
        command.add("set");
        command.add("CUDA_VISIBLE_DEVICES=" + (int) gpuIdSpinner.getValue());
        command.add("&&");
        command.add(PYTHON_EXE.getAbsolutePath());
        command.add(INFERENCE_PY.getAbsolutePath());

        command.add("--model_path");
        File[] trainedModels = TRAINING_LOG_DIR_DEFAULT.listFiles((dir, name) ->
                name.matches(REGEX_TRAINED_MODEL_NAME));
        assert trainedModels != null;
        // logs: no trained model
        if (trainedModels.length == 0) {
            System.out.println("[ERROR] Model not Trained.");
            inferenceBtn.setText(INFERENCE_BTN_TEXT);
            return;
        }
        assert trainedModels.length == 1;
        command.add(trainedModels[0].getAbsolutePath());

        command.add("--config_path");
        command.add(TRAINING_CONFIG_LOG.getAbsolutePath());

        command.add("--wav_format");
        command.add(AUDIO_FILE_OUT_FORMAT);

        command.add("--trans");
        command.add(String.valueOf(PITCH_SHIFT_INFER_DEFAULT));

        command.add("--spk_list");
        command.add((String) speakerPickCbBx.getSelectedItem());

        command.add("--clean_names");
        command.addAll(Arrays.stream(vocalAudioFiles).map(File::getName).toList());

        command.add("--f0_predictor");
        command.add((String) f0PredictorInferCbBx.getSelectedItem());

        if (nsfHiFiGanCkBx.isSelected()) {
            command.add("--enhance");
        }

        // whisper-ppg speech encoder need to set --clip to 25 and -lg to 1
        if ("whisper-ppg".equals(modelJsonObject.getString("speech_encoder"))) {
            command.add("--clip");
            command.add(String.valueOf(25));
            command.add("-lg");
            command.add(String.valueOf(1));
        } else {
            command.add("--clip");
            command.add(String.valueOf(CLIP_INFER_DEFAULT));
        }

        // Schedule inference task
        executionAgent.executeLater(
                command,
                SO_VITS_SVC_DIR,
                (process) -> {
                    if (process.exitValue() == 0) {
                        System.out.println("[INFO] Inference Complete.");
                        System.out.println("[INFO] Output audios -> \".\\results\"");
                    } else {
                        System.out.println("[WARNING] \"" +
                                INFERENCE_PY.getName() +
                                "\" interrupted, exit code: " +
                                process.exitValue()
                        );
                    }

                    // reset Inference State -> Vacant
                    vocalAudioFiles = null;
                    vocalChosenFld.setEnabled(false);
                    vocalChosenFld.setText("File name(s) should be in English");
                    speakerPickCbBx.setEnabled(false);
                    speakerPickCbBx.removeAllItems();
                    inferenceBtn.setText(INFERENCE_BTN_TEXT);
                }
        );
        executionAgent.invokeExecution();
    }

    /**
     * Build & Get GUI Console PrintStream
     *
     * @return PrintStream to GUI Console
     */
    private PrintStream getPrintStream() {
        OutputStream outGUI = new OutputStream() {
            @Override
            public void write(int b) {
                updateConsole(String.valueOf((char) b)); // 1 Byte Char only (Unused)
            }

            @Override
            public void write(byte[] b, int off, int len) {
                updateConsole(new String(b, off, len, CHARSET_DISPLAY_DEFAULT));
            }
        };
        return new PrintStream(outGUI, true, CHARSET_DISPLAY_DEFAULT);
    }

    /**
     * Redirect System output & error stream to designated PrintStream(s).
     *
     * @param out new output stream to be set.
     * @param err new error stream to be set.
     */
    private void redirectSystemOutErrStream(PrintStream out, PrintStream err) {
        System.setOut(out);
        System.setErr(err);
    }

    /**
     * Append output -> console text area.
     *
     * @param output the text to be displayed in console area.
     */
    private void updateConsole(String output) {
        consoleArea.append(output);

        // console auto scroll to bottom
        JScrollBar verticalScrollBar = consolePanel.getVerticalScrollBar();
        EventQueue.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
    }

}
