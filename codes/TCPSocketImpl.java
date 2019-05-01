import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class TCPSocketImpl extends TCPSocket {
    private static final int MAX_PAYLOAD_SIZE = EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES;
    private EnhancedDatagramSocket udp_socket;
    private TCPState state;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.port = port;
        this.state = TCPState.CLOSED; //TODO: check
    }

    void setUdpSocket(EnhancedDatagramSocket sock) {
        this.udp_socket = sock;
    }

    @Override
    public void connect() throws Exception {
        //TODO: check for state! don't connect if already connected
        udp_socket = new EnhancedDatagramSocket(8000); //TODO: how to choose port?
        udp_socket.setSoTimeout(5000); //TODO: how to set timeout?
        InetAddress address = InetAddress.getByName("127.0.0.1");

        // send ACK and expect SYN-ACK
        TCPPacket packet = new TCPPacket(true, false, false);
        byte[] buf = packet.toStream();
        boolean syn_ack_received = false;
        DatagramPacket dp = new DatagramPacket(buf, buf.length, address, port);
        while(!syn_ack_received) {
            udp_socket.send(dp);

            state = TCPState.SYN_SENT;

            try {
                TCPPacket recv_pkt = receiveInHandshake();
                if(recv_pkt.isSYN_ACK()){
                    syn_ack_received = true;

                }
            } catch (SocketTimeoutException e) {
                //TODO: limite maximum try? ye counter bezarim vase datagramSocketemon ke tedade try hasho beshmare
                // age ziad shod kolan timeout bede nasaze socketo?
                continue;
            }
        }

        // send ACK
        packet = new TCPPacket(false, false, true);
        buf = packet.toStream();
        dp = new DatagramPacket(buf, buf.length, address, port);
        while(true){ //TODO: condition?
            udp_socket.send(dp);
            try {
                TCPPacket recv_pkt = this.receiveInHandshake();
                if(recv_pkt.isSYN_ACK())
                    continue;
            } catch (SocketTimeoutException e) {
                break;
            }
        }

        System.out.println("Client Established.");
        state = TCPState.ESTABLISHED;
    }

    private TCPPacket receiveInHandshake() throws Exception{
        byte[] buf = new byte[MAX_PAYLOAD_SIZE];
        DatagramPacket recv_dp = new DatagramPacket(buf, buf.length);
        udp_socket.receive(recv_dp);
        TCPPacket recv_pkt = TCPPacket.toTCPPacket(recv_dp.getData());
        return recv_pkt;
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
