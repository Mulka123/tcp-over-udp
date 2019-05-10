public class Sender {
    public static void main(String[] args) throws Exception {
        TCPSocket tcpSocket = new TCPSocketImpl("127.0.0.1", 12344);
        tcpSocket.connect();
        tcpSocket.send("1MB.txt");
        tcpSocket.close();
        tcpSocket.saveCongestionWindowPlot();
    }
}