package models;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Remote Agent
 * @responsibility Control the remote server to execute instructions.
 */
public class RemoteAgent {

    private Socket socket;

    public RemoteAgent(InetSocketAddress address) throws IOException {
        socket = new Socket(address.getAddress(), address.getPort());
        socket.setKeepAlive(true);
    }

    /**
     * Close the socket connection.
     */
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            socket = null;
        }
    }

    /* Getters */
    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }
    public int getPort() {
        return socket.getPort();
    }
    public InputStream getInputStream() {
        try {
            return socket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public OutputStream getOutputStream() {
        try {
            return socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
