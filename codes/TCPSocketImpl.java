import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {
    EnhancedDatagramSocket udp_socket;
    InetAddress address;

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
        File f = new File(pathToFile);
        udp_socket = new EnhancedDatagramSocket(8000);
        if(f.length()<udp_socket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES){
            TCPPacket tcpp = new TCPPacket(f);
            address = InetAddress.getByName("127.0.0.1");
            DatagramPacket dp = new DatagramPacket(tcpp.toStream(), tcpp.length(),address, 12344);
            udp_socket.setSoTimeout();
            udp_socket.send(dp);
            DatagramPacket recdp;
            while(udp_socket.receive(recdp)){

            }
        }

        
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
