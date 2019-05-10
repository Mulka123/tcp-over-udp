import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class TCPServerSocketImpl extends TCPServerSocket {
    private static final int MAX_PAYLOAD_SIZE = EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES;
    private EnhancedDatagramSocket udpSocket;
    private InetAddress address = InetAddress.getByName("127.0.0.1");
    private int peerPort;
    private int port;
    private TCPState state;

    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.port = port;
        this.peerPort = 0;
        state = TCPState.CLOSED;
    }

    private TCPPacket receivePacket() throws Exception {
        byte[] buf = new byte[MAX_PAYLOAD_SIZE];
        DatagramPacket recvDp = new DatagramPacket(buf, buf.length);
        udpSocket.receive(recvDp);
        if (peerPort == 0)
            peerPort = recvDp.getPort();
        System.out.println("PEER PORT IS " + peerPort);
        return TCPPacket.toTCPPacket(recvDp.getData());
    }

    @Override
    public TCPSocket accept() throws Exception {
        if (state != TCPState.CLOSED) return null;

        state = TCPState.LISTEN;
        int mySeqNumber = -1;
        int myAckNumber = 0;
        udpSocket = new EnhancedDatagramSocket(port);
        udpSocket.setSoTimeout(10);

        boolean synReceived = false;
        while (!synReceived) {
            try {
                TCPPacket recvPkt = receivePacket();
                myAckNumber = recvPkt.getSeqNum() + 1;
                if (recvPkt.isSYN()) {
                    synReceived = true;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("TIMEOUT");
            }
        }
        state = TCPState.SYN_RECEIVED;

        // send SYN-ACK and expect ACK
        boolean ackReceived = false;

        TCPPacket packet = new TCPPacket(true, true);
        packet.setSeqNumber(mySeqNumber++);
        packet.setAckNumber(myAckNumber);
        while (!ackReceived) {
            sendPacket(packet);

            try {
                TCPPacket recvPkt = receivePacket();
                if (recvPkt.getAckNumber() != mySeqNumber) {
                    System.out.println("BAD PACKET IN SV :|");
                    //TODO: what now?
                }
                if (recvPkt.isACK()) {
                    ackReceived = true;
                }
            } catch (SocketTimeoutException e) {
                //TODO: increment a variable to prevent sticking in while?
                // age ACK miss shod?! :|
            }
        }

        state = TCPState.ESTABLISHED;
        System.out.println("Server Established.");
        TCPSocketImpl ret = new TCPSocketImpl("127.0.0.1", peerPort);
        ret.setUdpSocket(udpSocket); //TODO: is correct?
        ret.setSeqNum(mySeqNumber);
        return ret;
    }

    private void sendPacket(TCPPacket packet) throws Exception {
        byte[] buf = packet.toStream();
        DatagramPacket dp = new DatagramPacket(buf, buf.length, address, peerPort);
        udpSocket.send(dp);
    }

    @Override
    public void close() {
        //TODO: what to do with udpSocket? :|
    }
}
