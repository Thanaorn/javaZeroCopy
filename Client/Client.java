
package com.mycompany.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

public class Client {
    private String fileSave;
    private String ip = "";
    private String dir = "/home/";
    private String serverName = "";
    private String videoName = "";
    private String newFileLocal;
    private int NUM_THREADS;
    private boolean zero_mode;
    static MyFrame frame = new MyFrame();

    Client(String ip, String videoName, String fileSave, String serverName, String dir,boolean zero_mode) {
        this.ip = ip;
        this.videoName = videoName;
        this.fileSave = fileSave;
        this.serverName = serverName;
        this.dir = dir;
        this.zero_mode = zero_mode;
    }

    void connectToServer() {

        System.out.println("127.0.0.1: Server");

        System.out.println("ip address: ");

        System.out.println("Enter file location:"); // file location to download
        String filePath = dir + serverName + "/OSVideo/" + videoName;

        System.out.println(filePath);

        try {
            Socket s = SocketChannel.open().socket();
            s.connect(new InetSocketAddress(ip, 50003));
            System.out.println("connect Socket to Server IP " + ip);

            // true ใช้ zero , false ไม่ใช้ zero

            DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());

            dataOutputStream.writeInt(1);
            dataOutputStream.flush();

            String command = "Read" + filePath;
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();
            int buffer;
            long size = dataInputStream.readLong();

            dataInputStream.close();
            dataOutputStream.close();
            s.close();

            if (size == -1) {
                System.out.println("File " + filePath + " Not found");
            } else if (size == 0) {
                System.out.println("File " + filePath + " Empty");
            } else {
                if (size > 2000000000) {
                    System.out.println("Size File is Too Big");
                    buffer = 1024 * 1000;
                    if (!zero_mode) {
                        NUM_THREADS = 15;
                    }

                } else if (size > 500000000) {
                    System.out.println("Size File is nomal");
                    buffer = 1024 * 750;
                    if (!zero_mode) {
                        NUM_THREADS = 10;
                    }

                } else {
                    System.out.println("Size File is small");
                    buffer = 1024 * 500;
                    if (!zero_mode) {
                        NUM_THREADS = 5;
                    }
                }
                frame.setNt(NUM_THREADS);
                System.out.println("Size of file : " + size / 1048576 + " MB");
                System.out.println("Enter location to save:");
                newFileLocal = fileSave + "/" + videoName;
                System.out.println("Download To File Folder :" + newFileLocal);

                if (zero_mode) {

                    new ZeroCopyDownload(new InetSocketAddress(ip, 50003), size, filePath, newFileLocal, buffer,frame);

                } else {

                    long bufferSize = (size / NUM_THREADS) + 1;
                    DownloadThread[] threads = new DownloadThread[NUM_THREADS];
                    ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);

                    long startt = System.currentTimeMillis();
                    frame.setProgessBar(startt,0,true);
                    for (int i = 1; i <= NUM_THREADS; i++) {
                        threads[i - 1] = new DownloadThread(i, ip, 50003, bufferSize,
                                size, filePath,newFileLocal,buffer,frame,startt);

                        executorService.execute(threads[i - 1]);
                    }

                    executorService.shutdown();

                    for (int i = 1; i <= NUM_THREADS;) {
                        if (threads[i - 1].status) {
                            i++;

                        }
                    }
                    System.out.println("GET FILE SUCESS!!!");

                }

            }

        } catch (IOException ex) {
            MyFrame frame = new MyFrame();
            frame.popupConnectFall();
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void main(String[] args) throws IOException {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
        });
    }
}

class DownloadThread extends Thread {
    private Socket s;
    private int thread_id;
    private byte[] b;
    private long index_start;
    private long end_bytes;
    private String file_path_dowload;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String new_file;
    private int buffer_data;
    private MyFrame frame;
    private long startd;
    boolean status = false;

    public DownloadThread(int i, String ip, int port, long bufferSize, long size, String filePath, String path_new,
                          int buffer,MyFrame frame,long startd) throws IOException {
        this.startd = startd;
        this.frame = frame;
        thread_id = i;
        file_path_dowload = filePath;
        new_file = path_new;
        buffer_data = buffer;

        s = SocketChannel.open().socket();
        s.connect(new InetSocketAddress(ip, port));

        index_start = bufferSize * (thread_id - 1);
        end_bytes = (index_start + bufferSize) - 1;
        if (end_bytes >= size) {
            end_bytes = size;
        }
    }

    @Override
    public void run() {
        try {
            dataOutputStream = new DataOutputStream(s.getOutputStream());
            dataOutputStream.writeInt(2);
            dataOutputStream.flush();
            dataOutputStream.writeUTF("Hello i'm Thread ID : " + thread_id);
            dataOutputStream.flush();

            dataOutputStream.writeInt(buffer_data);
            dataOutputStream.flush();
            dataOutputStream.writeLong(index_start);
            dataOutputStream.flush();
            dataOutputStream.writeLong(end_bytes);
            dataOutputStream.flush();
            dataOutputStream.writeUTF(file_path_dowload);
            dataOutputStream.flush();

            dataInputStream = new DataInputStream(s.getInputStream());

            downloadFile(startd);

            dataOutputStream.close();
            dataInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile(long startd) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(new_file, "rw");
        b = new byte[buffer_data];
        int bytesRead;
        long start = System.currentTimeMillis();
        while (index_start < end_bytes && (bytesRead = dataInputStream.read(b, 0,
                Math.min(b.length, (int) (end_bytes - index_start)))) != -1) {
            writeFile(randomAccessFile, bytesRead);
            index_start += bytesRead;
        }
        long end = System.currentTimeMillis();
        long timeuse = end-start;
        System.out.println("Thread ID : " + thread_id + " get file sucess at " + timeuse+ " millisecond");
        frame.countSS(startd);
        status = true;
        dataOutputStream.flush();
    }

    private synchronized void writeFile(RandomAccessFile randomAccessFile, int bytesRead) throws IOException {
        randomAccessFile.seek(index_start);
        randomAccessFile.write(b, 0, bytesRead);
        ;
    }
}

class ZeroCopyDownload {
    private long size;
    private int buffer;
    private Socket s;
    private String file_path_dowload;
    private String new_file;
    private DataOutputStream dataOutputStream;
    private MyFrame frame;

    public ZeroCopyDownload(SocketAddress socketAddress, long size, String filePath, String newFile, int buffer,MyFrame frame)
            throws IOException {
        this.size = size;
        this.frame = frame;
        file_path_dowload = filePath;
        new_file = newFile;
        this.buffer = buffer;
        s = SocketChannel.open().socket();
        s.connect(socketAddress);

        downloadFileByZeroCopy();
    }

    private void downloadFileByZeroCopy() throws IOException {
        dataOutputStream = new DataOutputStream(s.getOutputStream());
        dataOutputStream.writeInt(3);
        dataOutputStream.flush();
        dataOutputStream.writeUTF(file_path_dowload);
        dataOutputStream.flush();
        dataOutputStream.writeInt(buffer);
        dataOutputStream.flush();

        long total = 0;
        double present;
        double sizeF = size;
        FileChannel fileChannel = new RandomAccessFile(new_file, "rw").getChannel();
        long start = System.currentTimeMillis();
        while (total < size) {
            if (total + buffer >= size) {
                buffer = (int) (size - total);
            }
            fileChannel.transferFrom(s.getChannel(), total, buffer);
            total += buffer;
            present = (total/sizeF)*100;
            System.out.printf( "%.2f %% \n",present);

            int ps = (int) present;
            frame.setProgessBar(start,ps,false);
        }
        long end = System.currentTimeMillis();
        long timeuse = end-start;
        System.out.println("ZERO-COPY DOWNLOND FILE SUCESS AT " + timeuse/1000.0 + " SECOND");
        frame.showSS(timeuse,false);
        fileChannel.close();
        dataOutputStream.close();
        s.close();
    }

}