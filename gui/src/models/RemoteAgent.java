package models;

import gui.GUI;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Remote Agent
 * @responsibility Control the remote server to execute instructions.
 */
public class RemoteAgent {

    private static final int FILE_TRANSFER_FRAGMENT_SIZE = 1024; // bytes
    private static final int FILE_TRANSFER_SERVER_PORT = 23333;
    public static final int FILE_TRANSFER_INTERVAL = 1; // ms

    private Socket controlSocket;

    public RemoteAgent(InetSocketAddress address) throws IOException {
        controlSocket = new Socket(address.getAddress(), address.getPort());
        controlSocket.setKeepAlive(true);
    }

    /**
     * Close the socket connection.
     */
    public void close() {
        try {
            controlSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Notify Server the FileUsage & Transfer a Single File to the Server.
     * @param usage Enum indicates the usage of the file to be transferred.
     * @param file File to be transferred.
     * @param progressBar JProgressBar to be updated.
     */
    public synchronized void transferFileToServer(FileUsage usage, File file, JProgressBar progressBar) throws IOException {

        Socket fileTransferSocket = new Socket(controlSocket.getInetAddress(), FILE_TRANSFER_SERVER_PORT);
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
        serverOutputStream.close();
        fileTransferSocket.close();
    }

    /**
     * Transfer a Single Instruction to the Server, and Retrieve Feedback from Server.
     * @param instruction instruction to be transferred.
     */
    public synchronized void executeInstructionOnServer(JSONObject instruction) throws IOException {
        if (controlSocket.isClosed()) {
            controlSocket = new Socket(controlSocket.getInetAddress(), getPort());
        }
        DataOutputStream serverOutputStream = new DataOutputStream(controlSocket.getOutputStream());

        // send Instruction
        serverOutputStream.writeUTF(instruction.toString());
        serverOutputStream.flush();

        // retrieve feedback from Server
        BufferedReader in = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(),
                GUI.CHARSET_DISPLAY_DEFAULT));
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println(line);
            System.out.flush();
        }
        controlSocket.close();
    }

    /* Getters */
    public InetAddress getInetAddress() {
        return controlSocket.getInetAddress();
    }
    public int getPort() {
        return controlSocket.getPort();
    }
    /**
     * Echo this RemoteAgent reference if its connection is currently alive.
     * @return this RemoteAgent if its connection is still alive, otherwise null.
     */
    public RemoteAgent echoIfAlive() {
        return controlSocket.isClosed()? null : this;
    }
    private InputStream getInputStream() {
        try {
            return controlSocket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private OutputStream getOutputStream() {
        try {
            return controlSocket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
