import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;
import java.io.*;

public class TCPSocketImpl extends TCPSocket {
    private static final int MAX_PAYLOAD_SIZE = EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES;
    private static final int receive_buffer_size = 1024; // num of available packets at first
    private EnhancedDatagramSocket udp_socket;
    private TCPState state;
    private CongestionControlState congestionState;
    private double windowSize;
    private long SSThreshold;
    private int my_next_sequence_number;
    private int my_last_ack_number;
    private int expected_sequence_number;
    private InetAddress address;
    private List<TCPPacket> sendpkt = new ArrayList<>();


    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        address = InetAddress.getByName(ip);
        this.peer_port = port;
        this.port = 8000;
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
        udp_socket = new EnhancedDatagramSocket(port); //TODO: how to choose port?
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
        while(true) { //TODO: chejori az send shodane ACK mitonim motmaen shim?
            sendPacket(packet);
            try {
                TCPPacket recv_pkt = receivePacket();
                if(recv_pkt.isSYN_ACK()){
                    continue;
                }
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

    private int sendpacketchunkbychunk(String pathToFile) throws Exception {
        File file = new File(pathToFile);
        FileInputStream f = new FileInputStream(file);        
        ArrayList<TCPPacket> window = new ArrayList<>();
        final int chunkSize = EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES - TCPPacket.overHeadSize();
        int EFS = 0;
        int highWater = 0;
        int last_ack = -1;
        int lastPacketSeqNum = -2;
        int maxTryForLastSegment = 5;
        int lastSentChunkNum = -1;

        int numdup = 0;

        boolean is_file_read = false;
        while(!is_file_read || (lastPacketSeqNum != last_ack && maxTryForLastSegment > 0)) { //TODO: check for last ack?

            // SENDING PART
            while(true) {
                System.out.println("WINDOW SIZE: " + windowSize);
                if(congestionState == CongestionControlState.FASTRECOVERY) {
                    if(EFS >= windowSize) break;
                } else {
                    if(lastSentChunkNum - last_ack >= windowSize) break;
                }
                byte[] chunk = new byte[chunkSize];
                if(f.read(chunk) > 0) {
                    TCPPacket tcpp = new TCPPacket(chunk);
                    tcpp.setSeqNumber(++lastSentChunkNum);

                    if(f.available() == 0){
                        tcpp.setAsLastSegment();
                        lastPacketSeqNum = lastSentChunkNum;
                        is_file_read = true;
                    }

                    window.add(tcpp);
                    sendpkt.add(tcpp);

                    sendPacket(tcpp);
                    System.out.println("READ FROM FILE: " + new String(chunk));
                    System.out.println("SENT PACKET " + tcpp.getSeqNum());
                    EFS++;
                } else {
                    System.out.println("BAD READ " + new String(chunk));
                    is_file_read = true;
                    break;
                }
            }
            System.out.println("-----------------");



            // RECEIVING PART
            boolean shouldRetransmit = false;

            try {
                TCPPacket recvpkt = receivePacket();

                System.out.println("RECEIVED ACK " + recvpkt.getAckNumber());

                CongestionControlState nextState = congestionState;
                boolean isDup = false;
                int rwnd = recvpkt.getRwndSize();
                int ackNumber = recvpkt.getAckNumber();

                EFS--;

                // CHECK FOR DUPLICATE ACK
                if (ackNumber == last_ack) {
                    isDup = true;
                    numdup++;
                    if (numdup == 3) {
                        EFS--;

                        if (congestionState != CongestionControlState.FASTRECOVERY) {
                            numdup = 0;
                            highWater = lastSentChunkNum;
                            SSThreshold = Math.max(Utils.toInt(windowSize / 2), 2);
//                            windowSize = SSThreshold + 3;
                            windowSize = SSThreshold;
                            onWindowChange();
                            nextState = CongestionControlState.FASTRECOVERY;
                        }

                    }
                } else numdup = 0;



                // CHANGE STATE, WINDOW SIZE AND SSTHRESHOLD
                switch (congestionState) {
                    case SLOWSTART:
                        if(!isDup){
                            windowSize++;
                            if (windowSize > SSThreshold)
                                nextState = CongestionControlState.CONGESTIONAVOIDANCE;
                        }
                        break;
                    case CONGESTIONAVOIDANCE:
                        if(!isDup) {
                            windowSize += 1 / windowSize;
                            onWindowChange();
                        }
                        break;
                    case FASTRECOVERY:
                        if(!isDup) {
                            if(ackNumber >= highWater) {
                                windowSize = SSThreshold;
                                nextState = CongestionControlState.CONGESTIONAVOIDANCE;
                            } else {
                                windowSize -= ackNumber - last_ack + 1; // num of data acked by this ack
                                shouldRetransmit = true;
                            }
                        } else {
                            windowSize++;
                        }
                        onWindowChange();
                        break;
                }

                // UPDATE LAST ACKNOWLEDGED PACKET
                if(ackNumber > last_ack) {
                    last_ack = ackNumber;
                    while (window.size() > 0 && window.get(0).getSeqNum() <= last_ack)
                        window.remove(0);
                }

                congestionState = nextState;

                windowSize = Math.min(windowSize, rwnd);

            } catch (SocketTimeoutException e) {

                SSThreshold = Math.max(Utils.toInt(windowSize / 2), 2);
                windowSize = 1;
                onWindowChange();
                congestionState = CongestionControlState.SLOWSTART;
                shouldRetransmit = true;
            }

            // RETRANSMIT LOST SEGMENT
            if(shouldRetransmit) {
                TCPPacket pkt = sendpkt.get(last_ack + 1);
                System.out.println("RETRANSMIT PACKET " + pkt.getSeqNum());
                if(pkt.getSeqNum() == lastPacketSeqNum)
                    maxTryForLastSegment--;
                sendPacket(pkt);
            }
        }
        return lastSentChunkNum;
    }

    @Override
    public void send(String pathToFile) throws Exception {
        congestionState = CongestionControlState.SLOWSTART;
        SSThreshold = Integer.MAX_VALUE;
        windowSize = 1;
        int numofchunk = sendpacketchunkbychunk(pathToFile);//numofchunk shaiad niaz shod :/
    }
    
    @Override
    public void receive(String pathToFile) throws Exception {
        ArrayList<TCPPacket> packets = new ArrayList<>();
        expected_sequence_number = 0;
        my_last_ack_number = -1;
        ArrayList<TCPPacket> receive_buffer = new ArrayList<>();
        ArrayList<Integer> receivedSeqNums = new ArrayList<>();
        udp_socket.setSoTimeout(Integer.MAX_VALUE);
        while (true) {
            TCPPacket recv_pkt = receivePacket();
            int seqNum = recv_pkt.getSeqNum();
            System.out.println("RECEIVED PACKET " + recv_pkt.getSeqNum());

            if (!receivedSeqNums.contains(seqNum)){
                Utils.addIfNotExists(receive_buffer, recv_pkt);
                receivedSeqNums.add(seqNum);
            }


            receive_buffer.sort(Comparator.comparing(TCPPacket::getSeqNum));
            System.out.println("RECV BUF: " + receive_buffer);
            System.out.println("EXPECTING " + expected_sequence_number);
            int inOrderBufferedItems = Utils.findAllInOrderItems(receive_buffer, expected_sequence_number);
            if(inOrderBufferedItems > 0) {
                my_last_ack_number = receive_buffer.get(inOrderBufferedItems-1).getSeqNum();
                packets.addAll(Utils.extractItemsInRange(receive_buffer, 0, inOrderBufferedItems));
                expected_sequence_number = my_last_ack_number + 1;

                System.out.println(inOrderBufferedItems);
                System.out.println(receive_buffer);
                System.out.println(packets);

                if(recv_pkt.isLastPacket()) //and if we've collected all data before it? and if receive buffer is not empty?
                    break;
            }

            // send ACK
            sendACK(my_last_ack_number, receive_buffer_size - receive_buffer.size());
            System.out.println("SENT ACK " + my_last_ack_number);
        }

        packets.sort(Comparator.comparing(TCPPacket::getSeqNum));
        TCPPacket.saveToFile(packets, "RECEIVED_" + pathToFile);
    }

    private void sendACK(int ack_number, int rwnd) throws Exception {
        TCPPacket pkt = new TCPPacket(ack_number, rwnd);
        sendPacket(pkt);
    }

    @Override
    public void close() throws Exception {
        udp_socket.close();
    }

    @Override
    public long getSSThreshold() {
        return SSThreshold;
    }

    @Override
    public long getWindowSize() {
        return (long)windowSize;
    }
}
