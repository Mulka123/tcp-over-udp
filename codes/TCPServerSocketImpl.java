import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

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
        this.peer_port = 0;
    }

    private TCPPacket receivePacket() throws Exception {
        byte[] buf = new byte[MAX_PAYLOAD_SIZE];
        DatagramPacket recv_dp = new DatagramPacket(buf, buf.length);
        udp_socket.receive(recv_dp);
        if(peer_port == 0)
            peer_port = recv_dp.getPort();
        System.out.println("PEER PORT IS " + peer_port);
        return TCPPacket.toTCPPacket(recv_dp.getData());
    }

    @Override
    public TCPSocket accept() throws Exception {
        state = TCPState.LISTEN;
        int my_seq_number = Utils.randomInRange(50, 200); //havijoori :\
        int my_ack_number = 0;
        udp_socket = new EnhancedDatagramSocket(port);
        udp_socket.setSoTimeout(1000); //TODO: ?

        boolean syn_received = false;
        while(!syn_received) {
            try {
                TCPPacket recv_pkt = receivePacket();
                my_ack_number = recv_pkt.getSeqNum() + 1;
                if(recv_pkt.isSYN()){
                    syn_received = true;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("TIMEOUT");
                continue;
            }
        }
        state = TCPState.SYN_RECEIVED;

        // send SYN-ACK and expect ACK
        boolean ack_received = false;

        TCPPacket packet = new TCPPacket(true, true);
        packet.setSeqNumber(my_seq_number++);
        packet.setAckNumber(my_ack_number);
        while(!ack_received) {
            sendPacket(packet);

            try {
                TCPPacket recv_pkt = receivePacket();
                if(recv_pkt.getAckNumber() != my_seq_number){
                    System.out.println("BAD PACKET IN SV :|");
                    //TODO: what now?
                }
                if(recv_pkt.isACK()){
                    ack_received = true;
                }
            } catch (SocketTimeoutException e) {
                //TODO: increment a variable to prevent sticking in while?
                // age ACK miss shod?! :|
            }
        }

        state = TCPState.ESTABLISHED;
        System.out.println("Server Established.");
        TCPSocketImpl ret = new TCPSocketImpl("127.0.0.1", peer_port);
        ret.setUdpSocket(udp_socket); //TODO: is correct?
        ret.setSeqNum(my_seq_number);
        return ret;
    }

    private void sendPacket(TCPPacket packet) throws Exception {
        byte[] buf = packet.toStream();
        DatagramPacket dp = new DatagramPacket(buf, buf.length, address, peer_port);
        udp_socket.send(dp);
    }

    @Override
    public void close() throws Exception {
        //TODO: what to do with udp_socket
    }
}
