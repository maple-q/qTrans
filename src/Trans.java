import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class Trans {
    // 监听端口
    private final Integer listeningPort;
    // 转发到的地址
    private final String host;
    private final Integer port;

    // 是否和目的地建立连接
    private boolean isConnect = false;
    // Local -> Victim
    private Queue<Byte> queue1 = new LinkedList<>();
    // Victim -> Local
    private Queue<Byte> queue2 = new LinkedList<>();

    public Trans(Integer listeningPort, String host, Integer port) {
        this.listeningPort = listeningPort;
        this.host = host;
        this.port = port;
    }

    public void start() {
        // 开启监听，如果有数据传过来，就和目的地址建立连接，然后传过去
        try {
            ServerSocket serverSocket = new ServerSocket(this.listeningPort);
            System.out.println("Server listening at: " + this.listeningPort);
            // 等待客户端连接，只支持一个连接
            Socket socket = serverSocket.accept();
            System.out.println("Client " + socket.getRemoteSocketAddress() + " connected...");
            // 获取输入输出流
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            // 等待用户输入
            byte[] arr = new byte[1024];
            while (true) {
                // 返回的是读取的字节数
                int nums = inputStream.read(arr);
                if (nums != -1) {
                    // 建立连接
                    if (!this.isConnect) {
                        Socket toVictimSocket = new Socket(this.host, this.port);
                        System.out.println("Connect to " + this.host + ":" + this.port + " success...");
                        OutputStream toVictimOutputStream = toVictimSocket.getOutputStream();
                        InputStream victimInputStream = toVictimSocket.getInputStream();
                        // 开启两个线程监听和Victim Socket的输入输出流
                        new Thread(new HandleInputWithVictim(this.queue2, victimInputStream)).start();
                        new Thread(new HandleOutputWithVictim(this.queue1, toVictimOutputStream)).start();
                        this.isConnect = true;
                    }

                    synchronized (this.queue1) {
                        for (int i = 0; i < nums; i++) {
                            this.queue1.add(arr[i]);
                        }
                    }

                    break;
                }
            }
            // 开启两个线程监听和控制端Socket的输入输出流
            new Thread(new HandleInputWithController(this.queue1, inputStream)).start();
            new Thread(new HandleOutputWithController(this.queue2, outputStream)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// queue1  controller  ->   victim
// 和controller通信的socket，从input读取内容，写入queue1
// 和victim通信的socket，从queue1读取内容，写入output

// queue2

class HandleInputWithController implements Runnable {

    // Local -> Victim
    private Queue<Byte> queue;
    private InputStream inputStream;

    public HandleInputWithController(Queue<Byte> queue, InputStream inputStream) {
        this.queue = queue;
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        byte[] arr = new byte[1024];

        while (true) {
            try {
                int read_num = this.inputStream.read(arr);

                if (read_num != -1) {
                    // 有数据可以读取
                    synchronized (this.queue) {
                        for (int i = 0; i < read_num; i++) {
                            this.queue.add(arr[i]);
                        }
                    }

                }
                System.out.println("1 :" + this.queue.isEmpty());
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}


class HandleOutputWithController implements Runnable {

    // Victim -> Local
    private Queue<Byte> queue;
    private OutputStream outputStream;

    public HandleOutputWithController(Queue<Byte> queue, OutputStream outputStream) {
        this.queue = queue;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        byte[] arr = new byte[1024];
        while (true) {
            int i = 0;
            for (i = 0; i < arr.length; i++) {
                Byte b = this.queue.poll();
                if (b != null) {
                    arr[i] = b;
                } else {
                    break;
                }
            }
            // 真正有用的数据：0 ~ i
            byte[] arr2 = new byte[i];
            for (int j = 0; j < i; j++) {
                arr2[j] = arr[j];
            }
            try {
                this.outputStream.write(arr2);
                this.outputStream.flush();

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}


class HandleInputWithVictim implements Runnable {

    // Victim -> Local
    private Queue<Byte> queue;
    private InputStream inputStream;

    public HandleInputWithVictim(Queue<Byte> queue, InputStream inputStream) {
        this.queue = queue;
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        byte[] arr = new byte[1024];

        while (true) {
            try {
                int read_num = this.inputStream.read(arr);
                if (read_num != -1) {
                    // 有数据可以读取
                    for (int i = 0; i < read_num; i++) {
                        this.queue.add(arr[i]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}


class HandleOutputWithVictim implements Runnable {

    // Local -> Victim
    private Queue<Byte> queue;
    private OutputStream outputStream;

    public HandleOutputWithVictim(Queue<Byte> queue, OutputStream outputStream) {
        this.queue = queue;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        byte[] arr = new byte[1024];
        while (true) {
            int i;
            synchronized (this.queue) {
                for (i = 0; i < arr.length; i++) {
                    Byte b = this.queue.poll();
                    if (b != null) {
                        arr[i] = b;
                    } else {
                        break;
                    }
                }
            }
            // 真正有用的数据：0 ~ i
            byte[] arr2 = new byte[i];
            for (int j = 0; j < i; j++) {
                arr2[j] = arr[j];
            }
            try {
                this.outputStream.write(arr2);
                this.outputStream.flush();

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}