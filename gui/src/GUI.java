import models.ExecutionAgent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GUI extends JFrame {

    private static final String PROGRAM_TITLE = "SoftVC VITS Singing Voice Conversion GUI";
    private static final String ICON_PATH = ".\\gui\\data\\img\\GUI-Icon.png";
    private static final String SLICING_DIRECTORY_DEFAULT = ".\\so-vits-svc-4.1-Stable\\dataset_raw";
    private static final String[] VOCAL_FILE_EXTENSIONS_ACCEPTED = {"wav"};
    private static final String VOCAL_FILE_EXTENSIONS_DESCRIPTION = "Wave File(s)(*.wav)";
    private static final String VOICE_NAME_DEFAULT = "default-voice";

    private JPanel mainPanel;
    private JPanel datasetPrepPanel;
    private JPanel consolePanel;
    private JTextArea consoleArea;
    private JTextField vocalInPathFld;
    private JButton vocalChooserBtn;
    private JTextField sliceOutDirFld;
    private JButton vocalSlicerBtn;
    private JButton clearSliceOutDirBtn;

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
        PrintStream printGUI = new PrintStream(outGUI, true);
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
    }


    private void createDatasetPrepArea() {

        // Vocal File Chooser
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
                vocalInPathFld.setText(String.join(";",
                                            Arrays.stream(vocalAudioFiles).map(File::getName).toList()));
            }
        });

        // Vocal File Slicer
        sliceOutDirFld.setText(SLICING_DIRECTORY_DEFAULT);
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

            // disable slicer btn during slicing
            vocalSlicerBtn.setEnabled(false);
            clearSliceOutDirBtn.setEnabled(false);

            // slice each vocal file
            for (int i = vocalAudioFiles.length - 1; i >= 0; i--) {
                File vocalFile = vocalAudioFiles[i];

                // Command construction
                List<String> command = new ArrayList<>();
                command.add(ExecutionAgent.PYTHON_EXE_PATH);
                command.add(ExecutionAgent.SLICER_PATH);
                command.add(vocalFile.getPath());
                command.add("--out");
                command.add(sliceOutDirFld.getText() + "/" + voiceName);

                // schedule a task
                int finalI = i;
                executionAgent.executeLater(command, () -> {
                    System.out.println("[INFO] Slicing completed: " + vocalFile.getName());
                    if (finalI == 0) {
                        vocalSlicerBtn.setEnabled(true);
                        clearSliceOutDirBtn.setEnabled(true);
                    }
                });
            }

            // execute ASAP
            executionAgent.invokeExecution();
        });

        // Slice Out Dir Cleaner
        clearSliceOutDirBtn.addActionListener(e -> {
            File sliceOutDir = new File(SLICING_DIRECTORY_DEFAULT);
            String[] command = {"cmd", "/c", "rmdir", "/s", "/q", sliceOutDir.getAbsolutePath()};

            // schedule a task
            executionAgent.executeLater(Arrays.stream(command).toList(), () -> {
                assert sliceOutDir.mkdir();
                System.out.println("[INFO] Slice Output Directory Cleared.");
            });

            // execute ASAP
            executionAgent.invokeExecution();
        });
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
    }

}
