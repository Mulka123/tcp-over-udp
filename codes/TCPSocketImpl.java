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
    private long windowSize;
    private long SSThreshold;
    private int my_next_sequence_number;
    private int my_last_ack_number;
    private int expected_sequence_number;
    private InetAddress address;
    private List<TCPPacket> dups = new ArrayList<TCPPacket>();
    private List<TCPPacket> received = new ArrayList<TCPPacket>();
    private List<TCPPacket> sendpkt = new ArrayList<TCPPacket>();
    private int numdup = 1;
    private int MSS = 10;
    private long ocwnd = 0;

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

    private void resendwindow(List<byte[]> arrays ) throws Exception{
        for (byte[] item : arrays) {
            DatagramPacket dp = new DatagramPacket(item, item.length, address, peer_port);
            // size balayi elzaman maximum size nis
//            udp_socket.setSoTimeout(1000);//ms?? eachtime??
            //      System.out.println(arrays.size());
            udp_socket.send(dp);
        }
    }

    private TCPPacket findinchunks(int seqnum){
        for(int i=0;i<sendpkt.size();i++){
            if(sendpkt.get(i).getSeqNum() == seqnum + 1){
                return sendpkt.get(i);
            }
        }
        return null;
    }

    private int gobackN(List<byte[]> arrays) throws Exception{
        TCPPacket recvpkt;
        while(true){
            try{
                recvpkt = this.receivePacket();
                if(received.size() == 0 || recvpkt.getAckNumber() > received.get(received.size()-1).getAckNumber()){
                    received.add(recvpkt);
                    return 0;
                }
                else if(recvpkt.getAckNumber() == received.get(received.size()-1).getAckNumber()){
                    numdup++;
                    if(numdup == 3){
                        ocwnd = windowSize/2;
                        SSThreshold = ocwnd;
                        windowSize = SSThreshold + 3;
                        TCPPacket losspkt = findinchunks(recvpkt.getAckNumber());
                        this.sendPacket(losspkt);
                        numdup = 0;
                        congestionState = CongestionControlState.FASTRECOVERY;
                        return 0;
                    }
                    return -1;
                }
                else if(congestionState == CongestionControlState.FASTRECOVERY){
                    if(recvpkt.getAckNumber() == received.get(received.size()-1).getAckNumber()){
                        windowSize ++;
                    }
                    else {
                        windowSize /= 2;
                        congestionState = CongestionControlState.CONGESTIONAVOIDANCE;
                    }
                }
                return 0;
            }
            catch(SocketTimeoutException sktexp){

                SSThreshold = windowSize / 2;
                windowSize = 1;
                congestionState = CongestionControlState.SLOWSTART;

//                byte[] first_pkt = arrays.get(0);
//                arrays.clear();
//                sendPacket(new TCPPacket(first_pkt));
//                arrays.add(first_pkt);
//                this.resendwindow(arrays);
           //     System.out.println("go back");
            }
        }    
    }

    private int sendpacketchunkbychunk(String pathToFile) throws Exception {
        File file = new File(pathToFile);
        FileInputStream f = new FileInputStream(file);        
        byte[] chunk = new byte[EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES];
        List<byte[]> arrays = new ArrayList<>();
        List<byte[]> tmp = new ArrayList<>();
        int numofchunk = 0;
        int duporpar = 0;
//        long chunks = file.length()/udp_socket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES;
        while(f.read(chunk)>0) {
            if(duporpar == 0) {
                numofchunk++;
                TCPPacket tcpp = new TCPPacket(chunk);
                tcpp.setSeqNumber(numofchunk);
                sendpkt.add(tcpp);
                byte[] bufsnd = tcpp.toStream();
                tmp.add(bufsnd);
                arrays.add(bufsnd);
                this.resendwindow(tmp);
                tmp.clear();
            }
            if(arrays.size()>=5 || duporpar == -1){//this.getwindowsize()????
                duporpar = this.gobackN(arrays);
                arrays.remove(0);//should check seq#?:/
            }
        }
        return numofchunk;
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
        ArrayList<TCPPacket> receive_buffer = new ArrayList<>();

        while (true) {
            TCPPacket recv_pkt = receivePacket();
//            int sequence_number = recv_pkt.getSeqNum();

//            if(sequence_number < expected_sequence_number) {
//                if(not_received_packets.contains(sequence_number)){
//                    not_received_packets.remove(sequence_number);
//                    my_last_ack_number = Math.min(Collections.min(not_received_packets), sequence_number);
//                }
//                // else drop packet
//                continue;
//            } else if (sequence_number > expected_sequence_number) {
//                // fast recovery
//                for (int x = expected_sequence_number; x < sequence_number; ++x) {
//                    if(!not_received_packets.contains(x))
//                        not_received_packets.add(x); // alan 2 ro inja add mikone :| ke nabas bokone
//                }
//            } else {
//                expected_sequence_number = sequence_number + 1;
//                my_last_ack_number = sequence_number;
//            }
//
//            last_received_seq_number = sequence_number;

            Utils.addIfNotExists(receive_buffer, recv_pkt);
//            receive_buffer.add(recv_pkt);

//            // send ACK
//            sendACK(my_last_ack_number, receive_buffer_size - receive_buffer.size());

            receive_buffer.sort(Comparator.comparing(TCPPacket::getSeqNum));
            int inOrderBufferedItems = Utils.findAllInOrderItems(receive_buffer, expected_sequence_number);
            if(inOrderBufferedItems > 0) {
                packets.addAll(Utils.extractItemsInRange(receive_buffer, 0, inOrderBufferedItems));
                my_last_ack_number = receive_buffer.get(inOrderBufferedItems-1).getSeqNum();
                expected_sequence_number = my_last_ack_number + 1;

                if(recv_pkt.isLastPacket()) //and if we've collected all data before it? and if receive buffer is not empty?
                    break;
            }

            // send ACK
            sendACK(my_last_ack_number, receive_buffer_size - receive_buffer.size());

//            if(recv_pkt.isLastPacket()) //and if we've collected all data before it? and if receive buffer is not empty?
//                break;
        }

        packets.sort(Comparator.comparing(TCPPacket::getSeqNum));
        TCPPacket.saveToFile(packets, pathToFile);
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
        return windowSize;
    }
}
