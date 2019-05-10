public class Receiver {
    public static void main(String[] args) throws Exception {
        TCPServerSocket tcpServerSocket = new TCPServerSocketImpl(12344);
        TCPSocket tcpSocket = tcpServerSocket.accept();
        tcpSocket.receive("received.txt");
        tcpSocket.close();
        tcpServerSocket.close();
    }
}