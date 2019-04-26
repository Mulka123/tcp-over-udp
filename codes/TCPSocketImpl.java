import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {
    private EnhancedDatagramSocket udp_socket;
    private TCPState state;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        udp_socket = new EnhancedDatagramSocket(8000);

        byte[] buf = "SYN".getBytes();
        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramPacket dp = new DatagramPacket(buf, buf.length, address, 12344);
        udp_socket.send(dp);

        state = TCPState.SYN_SENT;

        buf = new byte[256];
        dp = new DatagramPacket(buf, buf.length);
        udp_socket.receive(dp);
        String data = new String(dp.getData());
//        if(!data.equals("SYN-ACK")){
//            System.out.println("Oops!");
//            return;
//        }

        state = TCPState.ESTABLISHED;

        buf = "ACK".getBytes();
        dp = new DatagramPacket(buf, buf.length, address, 12344);
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
