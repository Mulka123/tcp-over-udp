import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;

public class TCPSocketImpl extends TCPSocket {
    private static final int MAX_PAYLOAD_SIZE = EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES;
    private EnhancedDatagramSocket udp_socket;
    private TCPState state;
    private int my_next_sequence_number;
    private int my_last_ack_number;
    private int expected_sequence_number;
    private InetAddress address;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        address = InetAddress.getByName(ip);
        this.peer_port = port;
        this.state = TCPState.CLOSED; //TODO: check
    }

    void setSeqNum(int seqNum) { my_next_sequence_number = seqNum; }

    void setAckNum(int ackNum) { my_last_ack_number = ackNum; }

    void setUdpSocket(EnhancedDatagramSocket sock) {
        this.udp_socket = sock;
    }

    @Override
    public void connect() throws Exception {
        //TODO: check for state! don't connect if already connected
        udp_socket = new EnhancedDatagramSocket(8000); //TODO: how to choose port?
        udp_socket.setSoTimeout(5000); //TODO: how to set timeout?

        my_next_sequence_number = Utils.randomInRange(50, 200); //havijoori :\

        int synack_seq_num = 0;
        int synack_ack_num;
        // send ACK and expect SYN-ACK
        TCPPacket packet = new TCPPacket(true, false);
        packet.setSeqNumber(my_next_sequence_number++);
        boolean syn_ack_received = false;
        while(!syn_ack_received) {
            sendPacket(packet);

            state = TCPState.SYN_SENT;
            try {
                TCPPacket recv_pkt = receivePacket();
                synack_seq_num = recv_pkt.getSeqNum();
                synack_ack_num = recv_pkt.getAckNumber();
                if(synack_ack_num != my_next_sequence_number) {
                    System.out.println("BAD PACKET :|");
                    //TODO: what now?
                }
                if(recv_pkt.isSYN_ACK())
                    syn_ack_received = true;
            } catch (SocketTimeoutException e) {
                //TODO: limite maximum try? ye counter bezarim vase datagramSocketemon ke tedade try hasho beshmare
                // age ziad shod kolan timeout bede nasaze socketo?
                continue;
            }
        }

        // send ACK
        packet = new TCPPacket(false, true);
        packet.setSeqNumber(my_next_sequence_number++);
        packet.setAckNumber(++synack_seq_num);
        while(true){ //TODO: condition?
            sendPacket(packet);

            try {
                TCPPacket recv_pkt = receivePacket();
                if(recv_pkt.isSYN_ACK())
                    continue;
            } catch (SocketTimeoutException e) {
                break;
            }
        }

        System.out.println("Client Established.");
        state = TCPState.ESTABLISHED;
    }

    private TCPPacket receivePacket() throws Exception {
        byte[] buf = new byte[MAX_PAYLOAD_SIZE];
        DatagramPacket recv_dp = new DatagramPacket(buf, buf.length);
        udp_socket.receive(recv_dp);
        return TCPPacket.toTCPPacket(recv_dp.getData());
    }

    private void sendPacket(TCPPacket packet) throws Exception {
        byte[] buf = packet.toStream();
        DatagramPacket dp = new DatagramPacket(buf, buf.length, address, peer_port);
        udp_socket.send(dp);
    }

    @Override
    public void send(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        ArrayList<TCPPacket> packets = new ArrayList<>();

        while (true) {

            TCPPacket recv_pkt = receivePacket();
            packets.add(recv_pkt);

            if(recv_pkt.getSeqNum() == expected_sequence_number) {
                expected_sequence_number = recv_pkt.getSeqNum() + 1;
                my_last_ack_number = recv_pkt.getSeqNum();
            }

            // send ACK
            sendPacket(new TCPPacket(my_last_ack_number));

            if(recv_pkt.isLastPacket())
                break;
        }
        packets.sort(Comparator.comparing(TCPPacket::getSeqNum));
        TCPPacket.saveToFile(packets, pathToFile);
    }

    @Override
    public void close() throws Exception {
        udp_socket.close();
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
