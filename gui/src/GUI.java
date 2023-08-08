import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import models.ExecutionAgent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;

public class GUI extends JFrame {

    private static final String PROGRAM_TITLE = "SoftVC VITS Singing Voice Conversion GUI";
    private static final String ICON_PATH = "./gui/data/img/GUI-Icon.png";
    private static final String SLICING_DIRECTORY_DEFAULT = "./so-vits-svc-4.1-Stable/dataset_raw";
    private static final String[] VOCAL_FILE_EXTENSION_ACCEPTED = {"wav"};
    private static final String VOCAL_FILE_EXTENSION_DESCRIPTION = "Wave File(*.wav)";

    private JPanel mainPanel;
    private JPanel datasetPrepPanel;
    private JPanel consolePanel;
    private JTextArea consoleArea;
    private JTextField vocalInputPathField;
    private JButton vocalAudioChooserBtn;
    private JTextField sliceOutputDirectoryField;
    private JButton vocalAudioSlicerBtn;

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

        vocalAudioChooserBtn.addActionListener(e -> {
            JFileChooser vocalAudioFileChooser = new JFileChooser();
            vocalAudioFileChooser.setAcceptAllFileFilterUsed(false);
            vocalAudioFileChooser.setMultiSelectionEnabled(true);
            vocalAudioFileChooser.setFileFilter(new FileNameExtensionFilter(VOCAL_FILE_EXTENSION_DESCRIPTION, VOCAL_FILE_EXTENSION_ACCEPTED));

            if (vocalAudioFileChooser.showOpenDialog(datasetPrepPanel) == JFileChooser.APPROVE_OPTION) {
                vocalAudioFiles = vocalAudioFileChooser.getSelectedFiles();

                // Update file path text field
                vocalInputPathField.setText(String.join(";", Arrays.stream(vocalAudioFiles).map(File::getName).toList()));
            }
        });

        sliceOutputDirectoryField.setText(SLICING_DIRECTORY_DEFAULT);

        vocalAudioSlicerBtn.addActionListener(e -> {
            // TODO
            JOptionPane.showInputDialog(datasetPrepPanel, "Set voice name:", "Voice Name", JOptionPane.QUESTION_MESSAGE);
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
