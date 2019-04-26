import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Objects;

public class TCPServerSocketImpl extends TCPServerSocket {
    EnhancedDatagramSocket udp_socket;
    int port;
    private TCPState state;
    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.port = port;
    }

    @Override
    public TCPSocket accept() throws Exception {
        state = TCPState.LISTEN;

        udp_socket = new EnhancedDatagramSocket(port);
        byte[] b = new byte[256];
        DatagramPacket syn = new DatagramPacket(b, b.length);
        udp_socket.receive(syn);
        String s = new String(syn.getData());
//        if(!Objects.equals(s, "SYN")){
//            System.out.println("inja");
//            System.out.println(s.length() + " " + s);
//            return null;
//        }

        state = TCPState.SYN_RECEIVED;

        b = "SYN-ACK".getBytes();
        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramPacket ack = new DatagramPacket(b, b.length, address, syn.getPort());
        udp_socket.send(ack);
        DatagramPacket ack_syn = new DatagramPacket(b, b.length);
        udp_socket.receive(ack_syn);
        s = new String(ack_syn.getData());
//        if(!Objects.equals(s, "ACK")){
//            System.out.println("onja");
//            System.out.println(new String(syn.getData()));
//            return null;
//        }
        state = TCPState.ESTABLISHED;
        udp_socket.close();
        return null;
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
