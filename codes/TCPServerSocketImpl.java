import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Objects;

public class TCPServerSocketImpl extends TCPServerSocket {
    private static final int MAX_PAYLOAD_SIZE = EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES;
    EnhancedDatagramSocket udp_socket;
    InetAddress address = InetAddress.getByName("127.0.0.1");
    int peer_port;
    int port;
    private TCPState state;
    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.port = port;
    }

    private TCPPacket receiveInHandshake() throws Exception{
        byte[] buf = new byte[MAX_PAYLOAD_SIZE];
        DatagramPacket recv_dp = new DatagramPacket(buf, buf.length);
        udp_socket.receive(recv_dp);
        TCPPacket recv_pkt = TCPPacket.toTCPPacket(recv_dp.getData());
        peer_port = recv_dp.getPort();
        return recv_pkt;
    }

    @Override
    public TCPSocket accept() throws Exception {
        state = TCPState.LISTEN;

        udp_socket = new EnhancedDatagramSocket(port);
        udp_socket.setSoTimeout(5000); //TODO: ?

        boolean syn_received = false;
        while(!syn_received) {
            try{
                TCPPacket recv_pkt = receiveInHandshake();
                if(recv_pkt.isSYN())
                    syn_received = true;
            } catch (SocketTimeoutException e) {
                continue;
            }
        }
        state = TCPState.SYN_RECEIVED;

        // send SYN-ACK and expect ACK
        boolean ack_received = false;
        while(!ack_received) {
            TCPPacket packet = new TCPPacket(false, true, false);
            byte[] buf = packet.toStream();
            DatagramPacket send_dp = new DatagramPacket(buf, buf.length, address, peer_port);
            udp_socket.send(send_dp);
            try {
                TCPPacket recv_pkt = receiveInHandshake();
                if(recv_pkt.isACK())
                    ack_received = true;
            } catch (SocketTimeoutException e) {
                //TODO: increment a variable to prevent sticking in while?
                // age ACK miss shod?! :|
            }
        }

        state = TCPState.ESTABLISHED;
        System.out.println("Server Established.");
        TCPSocketImpl ret = new TCPSocketImpl("127.0.0.1", peer_port);
        ret.setUdpSocket(udp_socket);
        return ret;
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
