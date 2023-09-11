package gui;

import models.ExecutionAgent;
import models.FileUsage;
import models.InstructionType;
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
    private static final File RESULTS_DIR = new File(".\\results");
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
    private static final ChangeListener minZeroSpinnerGuard = e -> {
        JSpinner minZeroSpinner = (JSpinner) e.getSource();
        minZeroSpinner.setValue(Math.max((Integer) minZeroSpinner.getValue(), 0));
    };
    private static final ChangeListener minOneSpinnerGuard = e -> {
        JSpinner minOneSpinner = (JSpinner) e.getSource();
        minOneSpinner.setValue(Math.max((Integer) minOneSpinner.getValue(), 1));
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
    private JSpinner gpuIdSpinnerTrain;
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
    private JSpinner gpuIdSpinnerInfer;
    private ButtonGroup floatPrecisionGroup;
    private JMenuItem connectItm;
    private JMenuItem disconnectItm;
    private JMenuBar menuBar;
    private JMenu remoteMenu;
    private JMenu currentConnection;

    private final ExecutionAgent executionAgent;

    private File[] voiceAudioFiles;
    private File[] vocalAudioFiles;
    protected static RemoteAgent remoteAgent;

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

        // Kill all sub-processes & Disconnect from Server on Frame closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                executionAgent.cancelAllTasks();
                if (remoteAgent != null) {
                    remoteAgent.close();
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
        menuBar = new JMenuBar();

        remoteMenu = new JMenu("Remote");
        currentConnection = new JMenu("@localhost");

        /* Remote */
        connectItm = new JMenuItem("Connect to...", KeyEvent.VK_C);
        disconnectItm = new JMenuItem("Disconnect", KeyEvent.VK_D);
        // Connect To
        connectItm.addActionListener((e) -> {
            // ip_port[0] -> hostname, ip_port[1] -> port
            String[] ip_port;
            try {
                ip_port = JOptionPane.showInputDialog(
                        this,
                        "Connect to <IP,Port>:",
                        "Connect To...",
                        JOptionPane.QUESTION_MESSAGE
                ).trim().split(",", 2);
            } catch (NullPointerException ex) { // User canceled IP:Port input
                return;
            }

            // Connect to Server
            new Thread(() -> {
                connectItm.setEnabled(false);

                // parse String[] to InetAddress
                InetSocketAddress address;
                try {
                    System.out.println("[INFO] Resolving Hostname...");
                    address = new InetSocketAddress(ip_port[0], Integer.parseInt(ip_port[1].trim()));
                    if (address.isUnresolved()) {
                        System.err.println("[ERROR] Cannot Resolve Hostname: \"" + ip_port[0] + "\"");
                        return;
                    }
                    System.out.println("[INFO] Connecting...");
                    remoteAgent = new RemoteAgent(address);
                    disconnectItm.setEnabled(true);
                    currentConnection.setText("@[" + remoteAgent.getInetAddress() + "]:" + remoteAgent.getPort());
                    System.out.println("[INFO] Connection Established.");
                } catch (ConnectException ex) {
                    System.err.println("[ERROR] " + ex.getMessage());
                } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException ex) { // Invalid input
                    System.err.println("[!] <IP,Port> address invalid.");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    connectItm.setEnabled(true);
                }
            }, "Connect").start();
        });

        // Disconnect
        disconnectItm.setEnabled(false);
        disconnectItm.addActionListener((e) -> {
            remoteAgent.close();
            resetDisconnectedState();
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

                // Slice Worker
                final String finalSpeakerName = speakerName;
                new SwingWorker<Void, Integer>() {
                    @Override
                    protected Void doInBackground() throws InterruptedException {
                        /* Transfer voice files */
                        System.out.println("[INFO] Uploading Voice File(s)...");
                        totalVoiceFilesTransProgress.setMaximum(voiceAudioFiles.length);
                        totalVoiceFilesTransProgress.setValue(0);
                        totalVoiceFilesTransProgress.setString("0/" + totalVoiceFilesTransProgress.getMaximum());

                        for (int i = 0; i < voiceAudioFiles.length; Thread.sleep(RemoteAgent.FILE_TRANSFER_INTERVAL)) {
                            try {
                                remoteAgent.transferFileToServer(
                                        FileUsage.TO_SLICE,
                                        voiceAudioFiles[i],
                                        currentVoiceFileTransProgress
                                );
                            } catch (IOException ex) {
                                resetDisconnectedState();
                                System.err.println("[ERROR] Failed to Upload File(s), please Check the Connection.");
                                // enable related interactions
                                voiceSlicerBtn.setEnabled(true);
                                clearSliceOutDirBtn.setEnabled(true);
                                return null;
                            }
                            publish(++i);
                        }
                        System.out.println("[INFO] All Voice File(s) are Uploaded to Server.");
                        /* End Transfer voice files */

                        /* Slice on Server */
                        // Construct Instruction
                        JSONObject instruction = new JSONObject();
                        instruction.put("INSTRUCTION", InstructionType.SLICE.name());
                        instruction.put("spk", finalSpeakerName);
                        instruction.put("min_interval", SLICING_MIN_INTERVAL_DEFAULT);

                        // Execute Instruction on Server
                        try {
                            remoteAgent.executeInstructionOnServer(instruction);
                        } catch (IOException ex) {
                            return null;
                        } finally {
                            // enable related interactions
                            voiceSlicerBtn.setEnabled(true);
                            clearSliceOutDirBtn.setEnabled(true);
                        }
                        /* End Slice on Server */

                        return null;
                    }

                    @Override
                    protected void process(List<Integer> completedCount) {
                        totalVoiceFilesTransProgress.setValue(completedCount.get(completedCount.size() - 1));
                        totalVoiceFilesTransProgress.setString(
                                totalVoiceFilesTransProgress.getValue() +"/"+ totalVoiceFilesTransProgress.getMaximum()
                        );
                    }
                }.execute();

                return;
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
                                System.out.println("[INFO] Slicing completed: \"" + voiceFile.getName() + "\"");
                            } else {
                                String errorMessage = buildTerminationErrorMessage(process, SLICER_PY);
                                System.err.println(errorMessage);
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
            /* Connected to Server */
            if (remoteAgent != null) {
                new SwingWorker<Void, Integer>() {
                    @Override
                    protected Void doInBackground() {
                        sendClearInstruction(InstructionType.SLICE);
                        return null;
                    }
                }.execute();
                return;
            }
            /* End Connected to Server */

            File[] subFiles = Objects.requireNonNull(SLICING_OUT_DIR_DEFAULT.listFiles(File::isDirectory));
            if (subFiles.length == 0) {
                System.out.println("[INFO] Slicing Out Directory is Clean, Nothing to Clear.");
                return;
            }
            for (File subDir : subFiles) {
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

            // disable related interactions before preprocess
            preprocessBtn.setEnabled(false);
            clearPreprocessOutDirBtn.setEnabled(false);

            /* Connected to Server */
            if (remoteAgent != null) {

                // Preprocess Worker
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {

                        /* Preprocess on Server */
                        // Construct Instruction
                        JSONObject instruction = new JSONObject();
                        instruction.put("INSTRUCTION", InstructionType.PREPROCESS.name());
                        instruction.put("encoder", speechEncoderCbBx.getSelectedItem());
                        instruction.put("f0_predictor", f0PredictorPreproCbBx.getSelectedItem());
                        instruction.put("loudness_embedding", loudnessEmbedCkBx.isSelected());

                        // Execute Instruction on Server
                        try {
                            remoteAgent.executeInstructionOnServer(instruction);
                        } catch (IOException ex) {
                            return null;
                        } finally {
                            // enable related interactions
                            preprocessBtn.setEnabled(true);
                            clearPreprocessOutDirBtn.setEnabled(true);
                        }
                        /* End Preprocess on Server */

                        return null;
                    }
                }.execute();

                return;
            }
            /* End Connected to Server */

            // dataset_raw: nothing is prepared
            if (Objects.requireNonNull(SLICING_OUT_DIR_DEFAULT.listFiles(File::isDirectory)).length == 0) {
                System.err.println("[!] Please SLICE at least 1 VOICE file.");
                return;
            }

            System.out.println("[INFO] Preprocessing Dataset...");

            resampleAudio();
            splitDatasetAndGenerateConfig();
            generateHubertAndF0();
        });

        /* Preprocess Out Dir Cleaner */
        clearPreprocessOutDirBtn.addActionListener(e -> {
            /* Connected to Server */
            if (remoteAgent != null) {
                new SwingWorker<Void, Integer>() {
                    @Override
                    protected Void doInBackground() {
                        sendClearInstruction(InstructionType.PREPROCESS);
                        return null;
                    }
                }.execute();
                return;
            }
            /* End Connected to Server */

            File[] subFiles = Objects.requireNonNull(PREPROCESS_OUT_DIR_DEFAULT.listFiles(File::isDirectory));
            if (subFiles.length == 0) {
                System.out.println("[INFO] Preprocess Out Directory is Clean, Nothing to Clear.");
                return;
            }
            for (File subDir : subFiles) {
                removeDirectory(subDir);
            }
        });

    }

    private void createTrainingArea() {

        /* GPU ID Train */
        gpuIdSpinnerTrain.setValue(GPU_ID_DEFAULT);
        gpuIdSpinnerTrain.addChangeListener(minZeroSpinnerGuard);

        /* Batch Size */
        batchSizeSpinner.setValue(BATCH_SIZE_DEFAULT);
        batchSizeSpinner.addChangeListener(minOneSpinnerGuard);

        /* Log Interval */
        logIntervalSpinner.setValue(LOG_INTERVAL_DEFAULT);
        logIntervalSpinner.addChangeListener(minOneSpinnerGuard);

        /* Eval Interval */
        evalIntervalSpinner.setValue(EVAL_INTERVAL_DEFAULT);
        evalIntervalSpinner.addChangeListener(minOneSpinnerGuard);

        /* Keep Last N Models */
        keepLastNModelSpinner.setValue(KEEP_LAST_N_MODEL_DEFAULT);
        keepLastNModelSpinner.addChangeListener(minZeroSpinnerGuard);

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

                /* Connected to Server */
                if (remoteAgent != null) {

                    // Train Worker
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            // Get train config from Server
                            JSONObject configJSONObject;
                            try {
                                configJSONObject = remoteAgent.getTrainConfig();
                            } catch (IOException ex) {
                                // enable related interactions
                                startTrainingBtn.setText(TRAINING_BTN_TEXT);
                                clearTrainLogDirBtn.setEnabled(true);
                                return null;
                            }

                            // display speaker names
                            JSONObject speakerJsonObject = configJSONObject.getJSONObject("spk");
                            speakerNameFld.setText(speakerJsonObject.keySet().toString());

                            /* Train on Server */
                            // Construct Instruction
                            JSONObject instruction = configJSONObject;
                            instruction.put("INSTRUCTION", InstructionType.TRAIN.name());
                            instruction.put("train", overwriteTrainingConfig().getJSONObject("train"));
                            instruction.put("gpu_id", (int) gpuIdSpinnerTrain.getValue());

                            // Execute Instruction on Server
                            try {
                                remoteAgent.executeInstructionOnServer(instruction);
                            } catch (IOException ex) {
                                return null;
                            } finally {
                                // enable related interactions
                                startTrainingBtn.setText(TRAINING_BTN_TEXT);
                                clearTrainLogDirBtn.setEnabled(true);
                            }
                            /* End Train on Server */

                            return null;
                        }
                    }.execute();

                    return;
                }
                /* End Connected to Server */

                // if resume training
                if (TRAINING_CONFIG_LOG.exists()) {
                    loadTrainingConfig();
                }

                overwriteTrainingConfig();
                displaySpeakersName();
                startTraining();

            } else { // Abort
                /* Connected to Server */
                if (remoteAgent != null) {

                    // Train Abort Worker
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {

                            /* Abort on Server */
                            // Construct Instruction
                            JSONObject instruction = new JSONObject();
                            instruction.put("INSTRUCTION", InstructionType.ABORT.name());

                            // Execute Instruction on Server
                            try {
                                remoteAgent.executeInstructionOnServer(instruction);
                            } catch (IOException ex) {
                                return null;
                            } finally {
                                // enable related interactions
                                startTrainingBtn.setText(TRAINING_BTN_TEXT);
                                clearTrainLogDirBtn.setEnabled(true);
                            }
                            /* End Abort on Server */

                            return null;
                        }
                    }.execute();

                    return;
                }
                /* End Connected to Server */

                executionAgent.cancelAllTasks();
            }
        });

        /* Train Log Cleaner */
        clearTrainLogDirBtn.addActionListener(e -> {
            /* Connected to Server */
            if (remoteAgent != null) {
                new SwingWorker<Void, Integer>() {
                    @Override
                    protected Void doInBackground() {
                        sendClearInstruction(InstructionType.TRAIN);
                        return null;
                    }
                }.execute();
                return;
            }
            /* End Connected to Server */

            File[] subFiles = Objects.requireNonNull(TRAINING_LOG_DIR_DEFAULT.listFiles((f) ->
                    !(f.getName().equals("diffusion") ||
                            f.getName().equals("D_0.pth") ||
                            f.getName().equals("G_0.pth"))));
            if (subFiles.length == 0) {
                System.out.println("[INFO] Train Log Directory is Clean, Nothing to Clear.");
                return;
            }
            for (File subFile : subFiles) {
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

        /* GPU ID Inference */
        gpuIdSpinnerInfer.setValue(GPU_ID_DEFAULT);
        gpuIdSpinnerInfer.addChangeListener(minZeroSpinnerGuard);

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
                                new File(INFERENCE_INPUT_DIR_DEFAULT, f.getName()).toPath(),
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
                JSONObject configJSONObject;
                /// Connected to Server?
                if (remoteAgent != null) {
                    try {
                        configJSONObject = remoteAgent.getTrainConfig();
                    } catch (IOException ex) {
                        resetDisconnectedState();
                        System.err.println("[ERROR] Connection Lost.");
                        return;
                    }
                } else {
                    configJSONObject = getConfigJsonObject();
                }
                configJSONObject.getJSONObject("spk").keySet().forEach((spk) -> {
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

                /* Connected to Server */
                if (remoteAgent != null) {

                    // Inference Worker
                    new SwingWorker<Void, Integer>() {
                        @Override
                        protected Void doInBackground() throws InterruptedException {
                            /* Transfer vocal files */
                            System.out.println("[INFO] Uploading Vocal File(s)...");
                            totalVocalFilesTransProgress.setMaximum(vocalAudioFiles.length);
                            totalVocalFilesTransProgress.setValue(0);
                            totalVocalFilesTransProgress.setString("0/" + totalVocalFilesTransProgress.getMaximum());

                            for (int i = 0; i < vocalAudioFiles.length; Thread.sleep(RemoteAgent.FILE_TRANSFER_INTERVAL)) {
                                try {
                                    remoteAgent.transferFileToServer(
                                            FileUsage.TO_INFER,
                                            vocalAudioFiles[i],
                                            currentVocalFileTransProgress
                                    );
                                } catch (IOException ex) {
                                    resetDisconnectedState();
                                    System.err.println("[ERROR] Failed to Upload File(s), please Check the Connection.");
                                    return null;
                                }
                                publish(++i);
                            }
                            System.out.println("[INFO] All Vocal File(s) are Uploaded to Server.");
                            /* End Transfer vocal files */

                            /* Inference on Server */
                            commitAllInferConfigInput();
                            // Construct Instruction
                            JSONObject instruction = new JSONObject();
                            instruction.put("INSTRUCTION", InstructionType.INFER.name());
                            instruction.put("gpu_id", (int) gpuIdSpinnerInfer.getValue());
                            instruction.put("spk", speakerPickCbBx.getSelectedItem());
                            instruction.put("f0_predictor", f0PredictorInferCbBx.getSelectedItem());
                            instruction.put("nsf_hifigan", nsfHiFiGanCkBx.isSelected());

                            System.out.println("[INFO] Inference Running... (this may take minutes without console output)");

                            // Execute Instruction on Server
                            try {
                                remoteAgent.executeInstructionOnServer(instruction);
                            } catch (IOException ex) {
                                resetInferenceState();
                                System.err.println("[ERROR] Inference Failed.");
                                return null;
                            }
                            /* End Inference on Server */

                            /* Get Results from Server */
                            System.out.println("[INFO] Retrieving Results from Server...");
                            try {
                                remoteAgent.getResultFiles(RESULTS_DIR);
                            } catch (IOException ex) {
                                System.err.println("[ERROR] Failed to Get Result Files from Server.");
                                return null;
                            } finally {
                                resetInferenceState();
                            }
                            /* End Get Results from Server */

                            System.out.println("[INFO] Output audios -> \"" + RESULTS_DIR + "\"");

                            return null;
                        }

                        @Override
                        protected void process(List<Integer> completedCount) {
                            totalVocalFilesTransProgress.setValue(completedCount.get(completedCount.size() - 1));
                            totalVocalFilesTransProgress.setString(
                                    totalVocalFilesTransProgress.getValue() +"/"+ totalVocalFilesTransProgress.getMaximum()
                            );
                        }
                    }.execute();

                    return;
                }
                /* End Connected to Server */

                // local inference below
                System.out.println("[INFO] Inference Running... (this may take minutes without console output)");
                startInference();

            } else { // Abort
                /* Connected to Server */
                if (remoteAgent != null) {

                    // Inference Abort Worker
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {

                            /* Abort on Server */
                            // Construct Instruction
                            JSONObject instruction = new JSONObject();
                            instruction.put("INSTRUCTION", InstructionType.ABORT.name());

                            // Execute Instruction on Server
                            try {
                                remoteAgent.executeInstructionOnServer(instruction);
                            } catch (IOException ex) {
                                return null;
                            } finally {
                                resetInferenceState();
                            }
                            /* End Abort on Server */

                            return null;
                        }
                    }.execute();

                    return;
                }
                /* End Connected to Server */

                executionAgent.cancelAllTasks();
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
                            System.err.println("[ERROR] \"" +
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
                        String errorMessage = buildTerminationErrorMessage(process, RESAMPLER_PY);
                        System.err.println(errorMessage);
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
                        String errorMessage = buildTerminationErrorMessage(process, FLIST_CONFIGER_PY);
                        System.err.println(errorMessage);
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
                        String errorMessage = buildTerminationErrorMessage(process, HUBERT_F0_GENERATOR_PY);
                        System.err.println(errorMessage);
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
    private JSONObject overwriteTrainingConfig() {
        // Get JSON Objects
        JSONObject configJsonObject = getConfigJsonObject();
        JSONObject trainJsonObject = configJsonObject.getJSONObject("train");

        // Commit values & Handle invalid user inputs (to previous valid setting)
        commitAllTrainConfigInput();

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
        try (FileWriter configJsonWriter = new FileWriter(TRAINING_CONFIG)) {
            configJsonWriter.write(configJsonObject.toString(JSON_STR_INDENT_FACTOR));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return configJsonObject;
    }

    /**
     * Commit all train config user input.
     */
    private void commitAllTrainConfigInput() {
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
        try {
            gpuIdSpinnerTrain.commitEdit();
        } catch (ParseException e) {
            gpuIdSpinnerTrain.updateUI();
        }
    }

    /**
     * Commit all inference config user input.
     */
    private void commitAllInferConfigInput() {
        try {
            gpuIdSpinnerInfer.commitEdit();
        } catch (ParseException e) {
            gpuIdSpinnerInfer.updateUI();
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
     * Build general termination error message about process's unexpected termination.
     * @param process the Process which ran into a unexpected termination.
     * @param executable the executable File associated with that process.
     * @return termination error message
     */
    private static String buildTerminationErrorMessage(Process process, File executable) {
        return "[ERROR] \"" +
                executable.getName() +
                "\" terminated unexpectedly, exit code: " +
                process.exitValue();
    }

    /**
     * Start Training with config.json
     */
    private void startTraining() {
        String[] command = {
                "cmd.exe",
                "/c",
                "set",
                "CUDA_VISIBLE_DEVICES=" + (int) gpuIdSpinnerTrain.getValue(),
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
                        System.err.println("[WARNING] \"" +
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
        command.add("CUDA_VISIBLE_DEVICES=" + (int) gpuIdSpinnerInfer.getValue());
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
                        System.out.println("[INFO] Output audios -> \"" + RESULTS_DIR + "\"");
                    } else {
                        System.err.println("[WARNING] \"" +
                                INFERENCE_PY.getName() +
                                "\" interrupted, exit code: " +
                                process.exitValue()
                        );
                    }

                    // delete all temp Files
                    Arrays.stream(vocalAudioFiles).forEach(File::delete);
                    resetInferenceState();
                }
        );
        executionAgent.invokeExecution();
    }

    /**
     * Reset Inference Components State -> Vacant
     */
    private void resetInferenceState() {
        vocalAudioFiles = null;
        vocalChosenFld.setEnabled(false);
        vocalChosenFld.setText("File name(s) should be in English");
        speakerPickCbBx.setEnabled(false);
        speakerPickCbBx.removeAllItems();
        inferenceBtn.setText(INFERENCE_BTN_TEXT);
    }

    /**
     * Update/Restore GUI to disconnected state.
     */
    private void resetDisconnectedState() {
        resetInferenceState();
        remoteAgent = null;
        disconnectItm.setEnabled(false);
        currentConnection.setText("@localhost");
    }

    /**
     * Send clear Instruction, targeted on designated type of Instruction's output directory.
     * @param type states which type of Instruction's output directory to be cleared
     */
    private void sendClearInstruction(InstructionType type) {
        // Construct Instruction
        JSONObject instruction = new JSONObject();
        instruction.put("INSTRUCTION", InstructionType.CLEAR.name());
        instruction.put("dir", type.name().toLowerCase());

        // Execute Instruction on Server
        try {
            remoteAgent.executeInstructionOnServer(instruction);
        } catch (IOException ex) {
            System.err.println("[ERROR] Failed to Clear " + type + " Output Directory.");
        }
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
