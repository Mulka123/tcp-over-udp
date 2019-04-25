import java.net.DatagramPacket;

public class TCPServerSocketImpl extends TCPServerSocket {
    EnhancedDatagramSocket udp_socket;
    int port;
    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.port = port;
    }

    @Override
    public TCPSocket accept() throws Exception {
        udp_socket = new EnhancedDatagramSocket(port);
        byte[] b = new byte[256];
        DatagramPacket dp = new DatagramPacket(b, b.length);
        udp_socket.receive(dp);
        udp_socket.close();

        System.out.println("HEY" + new String(b));
        return null;
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
