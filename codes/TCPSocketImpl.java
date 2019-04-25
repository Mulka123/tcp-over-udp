import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {
    EnhancedDatagramSocket udp_socket;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        udp_socket = new EnhancedDatagramSocket(8000);
        byte[] b = "salam".getBytes();
        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramPacket dp = new DatagramPacket(b, b.length, address, 12344);
        udp_socket.send(dp);
        udp_socket.close();
    }

    @Override
    public void send(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }
}
