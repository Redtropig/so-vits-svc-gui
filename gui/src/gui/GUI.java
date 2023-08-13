package gui;

import models.ExecutionAgent;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.*;

public class GUI extends JFrame {

    private static final String PROGRAM_TITLE = "SoftVC VITS Singing Voice Conversion GUI";
    public static final Charset CHARSET_DISPLAY_DEFAULT = StandardCharsets.UTF_8;
    protected static final String ICON_PATH = ".\\gui\\data\\img\\GUI-Icon.png";
    private static final File SLICING_OUT_DIR_DEFAULT = new File(ExecutionAgent.SO_VITS_SVC_DIR + "\\dataset_raw");
    private static final File PREPROCESS_OUT_DIR_DEFAULT = new File(ExecutionAgent.SO_VITS_SVC_DIR + "\\dataset\\44k");
    private static final File TRAINING_LOG_DIR_DEFAULT = new File(ExecutionAgent.SO_VITS_SVC_DIR + "\\logs\\44k");
    private static final File TRAINING_CONFIG = new File(ExecutionAgent.SO_VITS_SVC_DIR + "\\configs\\config.json");
    private static final File TRAINING_CONFIG_LOG = new File(TRAINING_LOG_DIR_DEFAULT + "\\config.json");
    private static final int JSON_STR_INDENT_FACTOR = 2;
    private static final int SLICING_MIN_INTERVAL_DEFAULT = 100; // ms
    private static final String[] VOCAL_FILE_EXTENSIONS_ACCEPTED = {"wav"};
    private static final String VOCAL_FILE_EXTENSIONS_DESCRIPTION = "Wave File(s)(*.wav)";
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
    private static final String F0_PREDICTOR_DEFAULT = "rmvpe";
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


    private JPanel mainPanel;
    private JPanel datasetPrepPanel;
    private JTextArea consoleArea;
    private JTextField vocalInPathFld;
    private JButton vocalChooserBtn;
    private JTextField sliceOutDirFld;
    private JButton vocalSlicerBtn;
    private JButton clearSliceOutDirBtn;
    private JScrollPane consolePanel;
    private JComboBox<String> speechEncoderCbBx;
    private JComboBox<String> f0PredictorCbBx;
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
    private ButtonGroup floatPrecisionGroup;

    private final ExecutionAgent executionAgent;

    private File[] vocalAudioFiles;

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
        setResizable(true);
        setVisible(true);

        /* Field Assignments */
        executionAgent = ExecutionAgent.getExecutionAgent();

        /* Components */
        createUIComponents();
        setContentPane(mainPanel);

        pack();
    }

    private void createUIComponents() {
        createDatasetPrepArea();
        createPreprocessArea();
        createTrainingArea();
        createConsoleArea();
    }

    private void createDatasetPrepArea() {

        /* Vocal File Chooser */
        vocalChooserBtn.addActionListener(e -> {
            JFileChooser vocalFileChooser = new JFileChooser();
            vocalFileChooser.setAcceptAllFileFilterUsed(false);
            vocalFileChooser.setMultiSelectionEnabled(true);
            vocalFileChooser.setFileFilter(new FileNameExtensionFilter(VOCAL_FILE_EXTENSIONS_DESCRIPTION,
                    VOCAL_FILE_EXTENSIONS_ACCEPTED));

            // Choose vocal file(s)
            if (vocalFileChooser.showOpenDialog(datasetPrepPanel) == JFileChooser.APPROVE_OPTION) {
                vocalAudioFiles = vocalFileChooser.getSelectedFiles();

                // Update file-path text field
                vocalInPathFld.setText(String.join(";", Arrays.stream(vocalAudioFiles).map(File::getName).toList()));
            }
        });

        /* Vocal File Slicer */
        sliceOutDirFld.setText(SLICING_OUT_DIR_DEFAULT.getPath());
        vocalSlicerBtn.addActionListener(e -> {
            // No selected vocal file
            if (vocalAudioFiles == null || vocalAudioFiles.length == 0) {
                System.err.println("[!] Please select at least 1 vocal file.");
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
            vocalSlicerBtn.setEnabled(false);
            clearSliceOutDirBtn.setEnabled(false);

            // slice each vocal file
            for (int i = vocalAudioFiles.length - 1; i >= 0; i--) {
                File vocalFile = vocalAudioFiles[i];

                // Command construction
                String[] command = {
                        ExecutionAgent.PYTHON_EXE.getAbsolutePath(),
                        ExecutionAgent.SLICER_PY.getAbsolutePath(),
                        vocalFile.getPath(),
                        "--out",
                        sliceOutDirFld.getText() + "/" + speakerName,
                        "--min_interval",
                        String.valueOf(SLICING_MIN_INTERVAL_DEFAULT)
                };

                // schedule a task
                int finalI = i;
                executionAgent.executeLater(command, null, () -> {
                    System.out.println("[INFO] Slicing completed: " + vocalFile.getName());
                    if (finalI == 0) {
                        // enable related interactions after batch execution
                        vocalSlicerBtn.setEnabled(true);
                        clearSliceOutDirBtn.setEnabled(true);
                    }
                });
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
            f0PredictorCbBx.addItem(predictor);
        }
        f0PredictorCbBx.setSelectedItem(F0_PREDICTOR_DEFAULT);

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

                overwriteTrainingConfig();
                loadTrainingConfig();
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
                            f.getName().equals("G_0.pth")))))
            {
                if (subFile.isDirectory()) {
                    removeDirectory(subFile);
                } else {
                    subFile.delete();
                    System.out.println("[INFO] File Removed: \"" + subFile.getPath() + "\"");
                }
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
    }

    /**
     * Remove a directory.
     * @param directory directory to be removed
     * @dependency Windows OS
     */
    private void removeDirectory(File directory) {
        if (directory.isDirectory()) {
            String[] command = {"cmd", "/c", "rmdir", "/s", "/q", directory.getAbsolutePath()};

            // schedule a task
            executionAgent.executeLater(command, null, () -> {
                System.out.println("[INFO] Directory Removed: \"" + directory.getPath() + "\"");
            });

            // execute ASAP
            executionAgent.invokeExecution();
        }
    }

    /**
     * Resample audios @src -> @dest, to 44100Hz mono.
     * @src .\dataset_raw
     * @dest .\dataset\44k
     */
    private void resampleAudio() {
        String[] command = {
                ExecutionAgent.PYTHON_EXE.getAbsolutePath(),
                ExecutionAgent.RESAMPLER_PY.getAbsolutePath(),
        };

        executionAgent.executeLater(command,
                ExecutionAgent.SO_VITS_SVC_DIR,
                () -> System.out.println("[INFO] Resampled to 44100Hz mono.")
        );
        executionAgent.invokeExecution();
    }

    /**
     * Split the dataset into training and validation sets, and generate configuration files.
     */
    private void splitDatasetAndGenerateConfig() {
        List<String> command = new ArrayList<>();
        command.add(ExecutionAgent.PYTHON_EXE.getAbsolutePath());
        command.add(ExecutionAgent.FLIST_CONFIGER_PY.getAbsolutePath());
        command.add("--speech_encoder");
        command.add((String) speechEncoderCbBx.getSelectedItem());
        if (loudnessEmbedCkBx.isSelected()) {
            command.add("--vol_aug");
        }

        executionAgent.executeLater(
                command,
                ExecutionAgent.SO_VITS_SVC_DIR,
                () -> System.out.println("[INFO] Training Set, Validation Set, Configuration Files Created.")
        );
        executionAgent.invokeExecution();
    }

    /**
     * Generate hubert and f0.
     */
    private void generateHubertAndF0() {
        String[] command = {
                ExecutionAgent.PYTHON_EXE.getAbsolutePath(),
                ExecutionAgent.HUBERT_F0_GENERATOR_PY.getAbsolutePath(),
                "--f0_predictor",
                (String) f0PredictorCbBx.getSelectedItem()
        };

        executionAgent.executeLater(command, ExecutionAgent.SO_VITS_SVC_DIR, () -> {
            System.out.println("[INFO] Hubert & F0 Predictor Generated.");
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
        if(fp32Btn.isSelected()) {
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

    private void displaySpeakersName() {
        JSONObject speakerJsonObject = getConfigJsonObject().getJSONObject("spk");
        speakerNameFld.setText(speakerJsonObject.keySet().toString());
    }

    /**
     * Get Training Config JSONObject.
     * @return Config JSONObject from TRAINING_CONFIG_LOG if it exists, otherwise from TRAINING_CONFIG.
     */
    private static JSONObject getConfigJsonObject() {
        File loadSource = TRAINING_CONFIG_LOG.exists() ? TRAINING_CONFIG_LOG : TRAINING_CONFIG;
        StringBuilder configJsonStrBuilder = new StringBuilder();

        // Load JSON String from loadSource
        try (Scanner in = new Scanner(loadSource)) {
            while (in.hasNext()) {
                configJsonStrBuilder.append(in.nextLine());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Parse JSON String to JSONObject
        return new JSONObject(configJsonStrBuilder.toString());
    }

    /**
     * Start Training with config.json
     */
    private void startTraining() {
        String[] command = {
                ExecutionAgent.PYTHON_EXE.getAbsolutePath(),
                ExecutionAgent.TRAIN_PY.getAbsolutePath(),
                "-c",
                TRAINING_CONFIG.getAbsolutePath(),
                "-m",
                "44k"
        };

        executionAgent.executeLater(command, ExecutionAgent.SO_VITS_SVC_DIR, () -> {
            startTrainingBtn.setText(TRAINING_BTN_TEXT);
            clearTrainLogDirBtn.setEnabled(true);
            System.out.println("[INFO] Training Stopped.");
        });
        executionAgent.invokeExecution();
    }

    /**
     * Build & Get GUI Console PrintStream
     * @return PrintStream to GUI Console
     */
    private PrintStream getPrintStream() {
        OutputStream outGUI = new OutputStream() {
            @Override
            public void write(int b) {
                updateConsole(String.valueOf((char)b)); // 1 Byte Char only (Unused)
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
     * @param out new output stream to be set.
     * @param err new error stream to be set.
     */
    private void redirectSystemOutErrStream(PrintStream out, PrintStream err) {
        System.setOut(out);
        System.setErr(err);
    }

    /**
     * Append output -> console text area.
     * @param output the text to be displayed in console area.
     */
    private void updateConsole(String output) {
        consoleArea.append(output);

        // console auto scroll to bottom
        JScrollBar verticalScrollBar = consolePanel.getVerticalScrollBar();
        EventQueue.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
    }

}
