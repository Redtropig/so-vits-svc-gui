import javax.swing.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

public class GUI extends JFrame {

    private static final String PROGRAM_TITLE = "SoftVC VITS Singing Voice Conversion GUI";
    private static final String ICON_PATH = "./gui/data/img/GUI-Icon.png";

    private JPanel mainPanel;
    private JPanel datasetPrepPanel;
    private JPanel consolePanel;
    private JTextArea consoleArea;

    private ExecutionAgent executionAgent;

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

    }


    /**
     * Redirect System output & error stream to designated PrintStream(s)
     * @param out new output stream to be set
     * @param err new error stream to be set
     */
    private void redirectSystemOutErrStream(PrintStream out, PrintStream err) {
        System.setOut(out);
        System.setErr(err);
    }

    /**
     * Append output -> console area
     * @param output text to be displayed in console area
     */
    private void updateConsole(String output) {
        consoleArea.append(output);
    }

}
