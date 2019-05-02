import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.io.*;
import java.nio.file.Files;
import java.util.* ;



public class TCPSocketImpl extends TCPSocket {
    private static final int MAX_PAYLOAD_SIZE = EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES;
    private EnhancedDatagramSocket udp_socket;
    private TCPState state;
    private int next_sequence_number;
    private int next_ack_number;

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
        TCPPacket packet = new TCPPacket(true, false);
        byte[] buf = packet.toStream();
        boolean syn_ack_received = false;
        DatagramPacket dp = new DatagramPacket(buf, buf.length, address, port);
        while(!syn_ack_received) {
            udp_socket.send(dp);

            state = TCPState.SYN_SENT;

            try {
                TCPPacket recv_pkt = receivePacket();
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
        buf = packet.toStream();
        dp = new DatagramPacket(buf, buf.length, address, port);
        while(true){ //TODO: condition?
            udp_socket.send(dp);
            try {
                TCPPacket recv_pkt = this.receivePacket();
                if(recv_pkt.isSYN_ACK())
                    continue;
            } catch (SocketTimeoutException e) {
                break;
            }
        }

        System.out.println("Client Established.");
        this.next_sequence_number = Utils.randomInRange(50, 200); //havijoori :\
        state = TCPState.ESTABLISHED;
    }

    private TCPPacket receivePacket() throws Exception {
        byte[] buf = new byte[MAX_PAYLOAD_SIZE];
        DatagramPacket recv_dp = new DatagramPacket(buf, buf.length);
        udp_socket.receive(recv_dp);
        return TCPPacket.toTCPPacket(recv_dp.getData());
    }

    private void resendwindow(List<byte[]> arrays ) throws Exception{
        InetAddress address = InetAddress.getByName("127.0.0.1");
        for(int i=0;i<arrays.size();i++){
            DatagramPacket dp = new DatagramPacket(arrays.get(i), udp_socket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES,address, 12344);
            udp_socket.setSoTimeout(1000);//ms?? eachtime??
      //      System.out.println(arrays.size());
            udp_socket.send(dp);  
        }          
    }

    private void gobackN(List<byte[]> arrays ) throws Exception{
        TCPPacket recvpkt;
        while(true){
            try{
                recvpkt = this.receivePacket();
                break;
            }
            catch(SocketTimeoutException sktexp){
                this.resendwindow(arrays);
           //     System.out.println("go back");
            }
        }    
    }

    private int sendpacketchunkbychunk(String pathToFile) throws Exception{
        File file = new File(pathToFile);
        FileInputStream f = new FileInputStream(file);        
        byte[] chunk = new byte[udp_socket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES];        
        List<byte[]> arrays = new ArrayList<byte[]>();
        int numofchunk = 0;
        long chunks = file.length()/udp_socket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES;
        while(f.read(chunk)>0){
            TCPPacket tcpp = new TCPPacket(chunk);
            byte[] bufsnd = tcpp.toStream();
            arrays.add(bufsnd);            
            this.resendwindow(arrays);
            numofchunk++;
            if(numofchunk%5 == 0){//this.getwindowsize()????
                this.gobackN(arrays);
                arrays.clear();    
            }
        }
        if(arrays.size()!=0){
            this.gobackN(arrays);
        }
        return numofchunk;
    }

    
    @Override
    public void send(String pathToFile) throws Exception {
        
        udp_socket = new EnhancedDatagramSocket(8112);
        int numofchunk = sendpacketchunkbychunk(pathToFile);//numofchunk shaiad niaz shod :/
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        byte[] data = "salam".getBytes();
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
