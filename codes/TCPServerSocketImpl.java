import java.net.DatagramPacket;

public class TCPServerSocketImpl extends TCPServerSocket {
    EnhancedDatagramSocket udp_socket;
    int port;
    int state = 0;
    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.port = port;
    }

    @Override
    public TCPSocket accept() throws Exception {
        udp_socket = new EnhancedDatagramSocket(port);
        byte[] b = new byte[256];
        DatagramPacket syn = new DatagramPacket(b, b.length);
        udp_socket.receive(syn);
        if(!(new String(syn.getData()).equals("SYN"))){
            System.println("as");
            return;
        }
        state = 1;
        b = "SYN-ACK";
        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramPacket ack = new DatagramPacket(b, b.length, address, syn.getPort());
        udp_socket.send(ack);
        state = 2;
        DatagramPacket ack-syn = new DatagramPacket(b, b.length);
        udp_socket.receive(ack-syn);
        if(!(new String(ack-syn.getData()).equals("ACK"))){
            System.println("aaa");
            return;
        }
        state = 3;
        udp_socket.close();
        return null;
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
