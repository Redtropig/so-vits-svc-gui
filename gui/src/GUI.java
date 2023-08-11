import models.ExecutionAgent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GUI extends JFrame {

    private static final String PROGRAM_TITLE = "SoftVC VITS Singing Voice Conversion GUI";
    private static final String ICON_PATH = ".\\gui\\data\\img\\GUI-Icon.png";
    private static final String SLICING_OUT_DIR_DEFAULT = ".\\so-vits-svc-4.1-Stable\\dataset_raw";
    private static final String PREPROCESS_OUT_DIR_DEFAULT = ".\\so-vits-svc-4.1-Stable\\dataset\\44k";
    private static final int SLICING_MIN_INTERVAL_DEFAULT = 100; // ms
    private static final String[] VOCAL_FILE_EXTENSIONS_ACCEPTED = {"wav"};
    private static final String VOCAL_FILE_EXTENSIONS_DESCRIPTION = "Wave File(s)(*.wav)";
    private static final String VOICE_NAME_DEFAULT = "default-voice";
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

    private final ExecutionAgent executionAgent;
    private File[] vocalAudioFiles;

    public GUI() {

        /* Global Settings */
        java.util.Locale.setDefault(Locale.ENGLISH);
        // redirect System out & err PrintStream
        OutputStream outGUI = new OutputStream() {
            @Override
            public void write(int b) {
                updateConsole(String.valueOf((char)b));
            }
        };
        PrintStream printGUI = new PrintStream(outGUI, true, StandardCharsets.UTF_8);
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
        sliceOutDirFld.setText(SLICING_OUT_DIR_DEFAULT);
        vocalSlicerBtn.addActionListener(e -> {
            // No selected vocal file
            if (vocalAudioFiles == null || vocalAudioFiles.length == 0) {
                System.err.println("[!] Please select at least 1 vocal file.");
                return;
            }

            // user input voice name
            String voiceName = JOptionPane.showInputDialog(datasetPrepPanel,
                    "Set voice name:",
                    "Voice Name",
                    JOptionPane.QUESTION_MESSAGE).trim();
            if (voiceName.isEmpty()) {  // handle empty Name
                voiceName = VOICE_NAME_DEFAULT;
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
                        sliceOutDirFld.getText() + "/" + voiceName,
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
        clearSliceOutDirBtn.addActionListener(e -> emptyDirectory(new File(SLICING_OUT_DIR_DEFAULT)));
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
        preprocessOutDirFld.setText(PREPROCESS_OUT_DIR_DEFAULT);
        preprocessBtn.addActionListener(e -> {

            // disable related interactions before preprocess
            preprocessBtn.setEnabled(false);
            clearPreprocessOutDirBtn.setEnabled(false);

            resampleAudio();
            splitDatasetAndGenerateConfig();
            generateHubertAndF0();
        });

        /* Preprocess Out Dir Cleaner */
        clearPreprocessOutDirBtn.addActionListener(e -> emptyDirectory(new File(PREPROCESS_OUT_DIR_DEFAULT)));

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
     * Empty a directory.
     * @param directory directory to be emptied
     * @dependency Windows OS
     */
    private void emptyDirectory(File directory) {
        if (directory.isDirectory()) {
            String[] command = {"cmd", "/c", "rmdir", "/s", "/q", directory.getAbsolutePath()};

            // schedule a task
            executionAgent.executeLater(command, null, () -> {
                directory.mkdir();
                System.out.println("[INFO] \"" + directory.getName() + "\" Directory Cleared.");
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
