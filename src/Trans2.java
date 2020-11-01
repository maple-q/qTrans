import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Trans2 {
    // 监听端口
    private final Integer listeningPort;
    // 转发到的地址
    private final String host;
    private final Integer port;

    public Trans2(Integer listeningPort, String host, Integer port) {
        this.listeningPort = listeningPort;
        this.host = host;
        this.port = port;
    }

    public void start() {
        try {
            // 监听本地端口
            ServerSocket serverSocket = new ServerSocket(this.listeningPort);
            System.out.println("Server listening at: " + this.listeningPort);

            while (true) {
                // 阻塞，等待连接
                Socket socket = serverSocket.accept();
                // 创建新线程处理连接
                new Thread(new Handle(socket, this.host, this.port)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


class Handle implements Runnable {

    private Socket socket;
    private String host;
    private Integer port;

    public Handle(Socket socket, String host, Integer port) {
        this.socket = socket;
        this.host = host;
        this.port = port;
    }

    public void run() {
        try {
            // 主动连接对方
            Socket clientSocket = new Socket(this.host, this.port);
            System.out.println("Client:  " + clientSocket.getRemoteSocketAddress() + " connected...");
            // 创建两个线程，把controller传的东西转发到victim以及把victim传得东西转给controller
            new Thread(new Forward(socket, clientSocket)).start();
            new Thread(new Forward(clientSocket, socket)).start();

        } catch (UnknownHostException e) {
            System.out.println("unknown host!");
        } catch (SocketException e) {
            System.out.println("设置超时时间失败");
        } catch (IOException e) {
            System.out.println("can't connect to the remote host:port!");
        }
    }
}


class Forward implements Runnable {

    private Socket socket1;
    private Socket socket2;

    private InputStream inputStream;
    private OutputStream outputStream;

    private static final Integer BUFFER = 1024;

    public Forward(Socket socket1, Socket socket2) throws SocketException {
        this.socket1 = socket1;
        this.socket2 = socket2;
        // 设置读取数据超时时间为3min
        this.socket1.setSoTimeout(180000);
    }

    public void run() {
        try {
            this.inputStream = this.socket1.getInputStream();
        } catch (IOException e) {
            System.out.println("获取输入流失败");
            System.exit(-1);
        }

        try {
            this.outputStream = this.socket2.getOutputStream();
        } catch (IOException e) {
            System.out.println("获取输出流失败");
            System.exit(-1);
        }
        // 一直等待socket1发送数据，然后发送给socket2
        byte[] data = new byte[BUFFER];
        while (true) {
            try {
                int readNums = this.inputStream.read(data);
                if (BUFFER == readNums) {
                    this.outputStream.write(data);
                } else {
                    // 发了多少就传多少
                    byte[] readData = new byte[readNums];
                    for (int i = 0; i < readNums; i++) {
                        readData[i] = data[i];
                    }
                    this.outputStream.write(readData);
                }
            } catch (IOException e) {
                continue;
            }
        }
    }
}