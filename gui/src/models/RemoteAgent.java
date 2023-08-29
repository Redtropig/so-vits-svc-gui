package models;

import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import static gui.GUI.CHARSET_DISPLAY_DEFAULT;

/**
 * Remote Agent
 * @responsibility Control the remote server to execute instructions.
 * @feature One RemoteAgent bounds one Server(address & port), eternally.
 */
public class RemoteAgent {

    private static final int FILE_TRANSFER_FRAGMENT_SIZE = 1024; // bytes
    private static final int FILE_TRANSFER_SERVER_PORT = 23333;
    public static final int FILE_TRANSFER_INTERVAL = 5; // ms

    private final Socket probeSocket; // closed after probing

    /**
     * Probe connectivity to the Server (does NOT hold the probing connection) & Create RemoteAgent if connectable.
     * @param address Server address to connect
     * @throws IOException failed to connect to Server
     */
    public RemoteAgent(InetSocketAddress address) throws IOException {
        probeSocket = new Socket(address.getAddress(), address.getPort());
        probeSocket.close();
    }

    /**
     * Close the socket connection.
     */
    public void close() {
        try {
            probeSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Notify Server the FileUsage & Transfer a Single File to the Server. (in current Thread, may Block)
     * @param usage Enum indicates the usage of the file to be transferred.
     * @param file File to be transferred.
     * @param progressBar JProgressBar to be updated.
     */
    public void transferFileToServer(FileUsage usage, File file, JProgressBar progressBar) throws IOException {

        Socket fileTransferSocket = new Socket(getInetAddress(), FILE_TRANSFER_SERVER_PORT);
        DataOutputStream serverOutputStream = new DataOutputStream(fileTransferSocket.getOutputStream());

        // Notify server InstructionType & FileUsage
        serverOutputStream.writeUTF(usage.name());
        serverOutputStream.writeUTF(file.getName());
        serverOutputStream.flush();

        // Transfer File
        DataInputStream fileInputStream = new DataInputStream(new FileInputStream(file));

        {
            byte[] fragment = new byte[FILE_TRANSFER_FRAGMENT_SIZE];
            long totalLengthTransferred = 0;
            int acturalFragmentLength;
            while ((acturalFragmentLength = fileInputStream.read(fragment)) != -1) {
                serverOutputStream.write(fragment, 0, acturalFragmentLength);
                serverOutputStream.flush();
                totalLengthTransferred += acturalFragmentLength;
                // update progressBar
                if (progressBar != null) {
                    progressBar.setValue((int) (totalLengthTransferred / file.length() * 100));
                }
            }
        }

        // Closures
        fileInputStream.close();
        fileTransferSocket.close();
    }

    /**
     * Transfer a Single Instruction to the Server, and Retrieve Feedback from the Server. (in current Thread, may Block)
     * @param instruction Instruction to be transferred.
     */
    public void executeInstructionOnServer(JSONObject instruction) throws IOException {

        Socket instructionSocket = new Socket(getInetAddress(), getPort());
        DataOutputStream serverOutputStream = new DataOutputStream(instructionSocket.getOutputStream());

        // send Instruction
        serverOutputStream.writeUTF(instruction.toString());
        serverOutputStream.flush();

        // retrieve Feedback from Server
        BufferedReader in = new BufferedReader(new InputStreamReader(instructionSocket.getInputStream(),
                CHARSET_DISPLAY_DEFAULT));
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println(line);
            System.out.flush();
        }
        instructionSocket.close();
    }

    /**
     * Get train config from the Server.
     * @return train config JSONObject
     */
    public JSONObject getTrainConfig() throws IOException {

        Socket instructionSocket = new Socket(getInetAddress(), getPort());
        DataOutputStream serverOutputStream = new DataOutputStream(instructionSocket.getOutputStream());

        // construct GET config Instruction
        JSONObject instruction = new JSONObject();
        instruction.put("INSTRUCTION", InstructionType.GET_CONF.name());
        instruction.put("config", InstructionType.TRAIN.name().toLowerCase());

        // send Instruction
        serverOutputStream.writeUTF(instruction.toString());
        serverOutputStream.flush();

        // retrieve configJSONString from Server
        BufferedReader in = new BufferedReader(new InputStreamReader(instructionSocket.getInputStream(),
                CHARSET_DISPLAY_DEFAULT));
        String configJSONString = in.readLine();
        instructionSocket.close();

        return new JSONObject(configJSONString);
    }

    /**
     * Get result Files from the Server and Write them into local resultDir directory.
     * @param resultDir the directory to store the received result Files.
     */
    public void getResultFiles(File resultDir) throws IOException {

        Socket instructionSocket = new Socket(getInetAddress(), getPort());
        DataInputStream serverInputStream = new DataInputStream(instructionSocket.getInputStream());
        DataOutputStream serverOutputStream = new DataOutputStream(instructionSocket.getOutputStream());

        // construct GET results Instruction
        JSONObject instruction = new JSONObject();
        instruction.put("INSTRUCTION", InstructionType.GET_RESULTS.name());

        // send Instruction
        serverOutputStream.writeUTF(instruction.toString());
        serverOutputStream.flush();

        // get result Files from Server
        while (true) {
            File resultFile;
            try {
                resultFile = new File(resultDir, serverInputStream.readUTF());
            } catch (IOException ex) { // Server side closed File transfer socket pipe
                break;
            }
            FileOutputStream fileOutStream = new FileOutputStream(resultFile);

            // Write to File
            int fileLen = serverInputStream.readInt();
            byte[] buffer = new byte[fileLen];
            serverInputStream.readFully(buffer, 0, fileLen);
            fileOutStream.write(buffer, 0, fileLen);
            fileOutStream.close();

            System.out.println("[INFO] File Received: \"" + resultFile + "\"");
        }

        instructionSocket.close();
    }

    /* Getters */
    public InetAddress getInetAddress() {
        return probeSocket.getInetAddress();
    }
    public int getPort() {
        return probeSocket.getPort();
    }
}
