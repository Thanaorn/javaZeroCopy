
package com.mycompany.server;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) {
        int port = 50003; // FTP default port
        stopServer stop = new stopServer();
        stop.start();

        try {
            ServerSocket serverSocket = ServerSocketChannel.open().socket();
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.setReuseAddress(true);

            System.out.println("Server is turn on");
            System.out.println("\tlistening on port " + port);
            System.out.println("\ncommand to turn off Server = \"exit\" or \"stop\"");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Handle client in a separate thread or process
                ServerHandler handler = new ServerHandler(clientSocket);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class stopServer extends Thread {
    @Override
    public void run() {
        Scanner scan = new Scanner(System.in);
        String exitString = scan.next();
        if (exitString.equalsIgnoreCase("exit")
                || exitString.equalsIgnoreCase("stop")) {
            System.out.println("Server has been shut down." +
                    "\nThank you for using the service.");
            System.exit(0);
        } else {
            System.out.println("Invalid order.");
            run();
        }

    }
}

class ServerHandler extends Thread {
    private Socket clientSocket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private File f;

    public ServerHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

            int mode = dataInputStream.readInt();
            if (mode == 1) {
                sendSizeFile();
                System.out.println("Mode sendSizeFile");

            } else if (mode == 2) {
                sendFile();
                System.out.println("Mode sendFile");
            } else if (mode == 3) {
                sendFileByZeroCopy();
                System.out.println("Mode sendzeroFile");

            }
            dataInputStream.close();
            dataOutputStream.close();
            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendSizeFile() throws IOException {
        String command = dataInputStream.readUTF();
        System.out.println(command);
        String fileName = command.substring(4);

        f = new File(fileName);

        if (f.exists() && f.isFile()) {
            long size = f.length();
            System.out.println(size / 1048576 + " MB");
            dataOutputStream.writeLong(size);
            dataOutputStream.flush();

        } else {
            System.out.println("Nooo");
            dataOutputStream.writeInt(-1);
            dataOutputStream.flush();
        }
    }

    private void sendFile() throws IOException {
        System.out.println(dataInputStream.readUTF());

        int buffer_data = dataInputStream.readInt();
        long index_start = dataInputStream.readLong();
        long end_bytes = dataInputStream.readLong();
        String filePath = dataInputStream.readUTF();
        f = new File(filePath);

        byte[] b = new byte[buffer_data];
        RandomAccessFile randomAccessFile = new RandomAccessFile(f, "r");
        int bytesRead;
        randomAccessFile.seek(index_start);
        long start = System.currentTimeMillis();
        while (index_start < end_bytes && (bytesRead = randomAccessFile.read(b, 0,
                Math.min(b.length, (int) (end_bytes - index_start)))) != -1) {
            dataOutputStream.write(b, 0, bytesRead);
            dataOutputStream.flush();
            index_start += bytesRead;
            randomAccessFile.seek(index_start);
        }
        long end = System.currentTimeMillis();
        System.out.println("SEND SUCESS AT " + (end - start) + " MILLISECOND");

        randomAccessFile.close();
    }

    private void sendFileByZeroCopy() throws IOException {
        String filePath = dataInputStream.readUTF();
        int buffer_data = dataInputStream.readInt();
        f = new File(filePath);

        long total = 0;
        FileChannel fileChannel = new RandomAccessFile(f, "r").getChannel();
        long end_bytes = fileChannel.size();
        long start = System.currentTimeMillis();
        while (total < end_bytes) {
            if (total + buffer_data >= end_bytes) {
                buffer_data = (int) (end_bytes - total);
            }
            fileChannel.transferTo(total, buffer_data, clientSocket.getChannel());
            total += buffer_data;
        }
        long end = System.currentTimeMillis();
        System.out.println("ZERO-COPY SEND SUCESS AT " + (end - start) + " MILLISECOND");

        fileChannel.close();
    }
}