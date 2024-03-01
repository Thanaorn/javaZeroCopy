
package com.mycompany.client;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;
import java.awt.event.ActionEvent;


public class MyFrame extends JFrame {
    String filePathDownload="";
    JProgressBar progressBar = new JProgressBar(0, 100);
    String dir = "C:/Users/thana/Documents/";
    boolean option;
    long timeMulti;
    long timeZero;
    public String ip="";
    private DefaultTableModel model;
    double nt;
    int count=0;
    private String serverName="";
    double present =0;
    void setProgessBar(long start,int current,boolean mode){
        progressBar.setValue(current);

        if(current==100&&mode){
            long end = System.currentTimeMillis();
            long time = end-start;
            timeMulti = time;
            showSS(time,mode);
            
            count=0;
        }
    }
    void showSS(long time,boolean mode){
        if(!mode){
            timeZero = time;
        }
        String message="Download Successful!! "+time/1000.0+" Second\nTime Multi-Thread : "+timeMulti/1000.0+"\nTime Zero copy : "+timeZero/1000.0;
        JOptionPane.showMessageDialog(null, message);
    }
    void countSS(long start)throws IOException {
        count++;

        present = (count/nt)*100;
        int ps = (int) present;
        setProgessBar(start,ps,true);

    }
    void setNt(int num){
        nt = num;
    }
    void setDir(String dir){
        this.dir = dir;
    }
    void setFilePath(String filename){
        filePathDownload = filename;
    }
    void popupConnectFall(){
        JOptionPane.showMessageDialog(null, "Connect Server Fall");
    }
    private static void openFile(File file) {
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.open(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public MyFrame() {

        // Set frame title
        setTitle("Client Request");

        // Set default close operation
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set frame size
        setSize(580, 550);

        // Create components
        JLabel label = new JLabel("Choose option :");
        //table
        model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Number", "File Name", "Type","Size","Time"});

        // สร้างตาราง
        JTable table = new JTable(model);


        JButton buttonConnect = new JButton("Connect");
        JButton openFileDownload = new JButton("Open File");

        buttonConnect.setBounds(50,100,95,30);




        progressBar.setStringPainted(true);

        JButton addButton = new JButton("Add Row");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // เพิ่มแถวใหม่ในตาราง
                model.addRow(new Object[]{"1", "New", "Row","MP$","124","255"});
            }
        });
        JButton removeButton = new JButton("Remove Selected Row");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // ลบแถวที่เลือก
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    model.removeRow(selectedRow);
                }
            }
        });
        JButton updateButton = new JButton("Update Selected Row");
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // อัปเดตข้อมูลในแถวที่เลือก
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    System.out.println(model.getValueAt( selectedRow, 1));
                }
            }
        });
//        JPanel buttonPanel = new JPanel();
//        buttonPanel.add(addButton);
//        buttonPanel.add(removeButton);
//        buttonPanel.add(updateButton);
        buttonConnect.addActionListener((ActionEvent ae) -> {
            SwingUtilities.invokeLater(() -> {
                String[] function = {"download multithread","Zero Copy"};
                int ipChoice = JOptionPane.showOptionDialog(
                        null,
                        "Select Option :",
                        "Download option",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        function,
                        function[0]
                );
                if (ipChoice==0) {
                    option = false;
                }else if(ipChoice==1){
                    option = true;
                }
                ip = "127.0.0.1";

                serverName = "Server";

                String[] videoName = {"Thailand", "Japan", "Europe", "Scotland"};

                int videoChoice = JOptionPane.showOptionDialog(
                        null,
                        "Select a video:",
                        "video City",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        videoName,
                        videoName[0]
                );

                // Store the chosen option
                String selectedVideo = videoName[videoChoice]+".pdf";

                // Prompt user to choose a file
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY);
                int fileChooserResult = fileChooser.showOpenDialog(null);
                String filePath = "";

                if (fileChooserResult == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    filePath = selectedFile.getAbsolutePath();
                }

                // Display the selected option and file path
                String message = "IP Address : "+ip+"\nSelected Video : " + selectedVideo  + "\nSave File Path : " + filePath+"\nOption : " ;
                if(option){
                    message = message.concat("Zero Copy!!");
                }else{
                    message = message.concat("MultiDownload thread!!");

                }
                JOptionPane.showMessageDialog(null, message);
                if(ip.equalsIgnoreCase("") || filePath.equalsIgnoreCase("") || serverName.equalsIgnoreCase("")){
                    message = "Please select data";
                    JOptionPane.showMessageDialog(null, message);
                }else {
                    connectServerT conT = new connectServerT(ip, selectedVideo, filePath, serverName,dir,option);

                    this.setFilePath(filePath+"/"+selectedVideo);
                    conT.start();

                }
            });
        });

        openFileDownload.addActionListener((ActionEvent e) -> {
            String f = filePathDownload;
            if(!f.equalsIgnoreCase("")){
                File filePath = new File(f);
                openFile(filePath);
            }else{

                JOptionPane.showMessageDialog(null, "You haven't downloaded yet.");
            }
        });



        JButton openDialogButton = new JButton("Exit");
        openDialogButton.setBounds(50,100,95,30);
        openDialogButton.addActionListener(e -> {
            int choice = showCustomDialog();
            if (choice == JOptionPane.YES_OPTION) {
                System.exit(0);
            } else if (choice == JOptionPane.NO_OPTION) {
                System.out.println("You clicked No");
            }
        });
        FlowLayout layout = new FlowLayout(FlowLayout.CENTER, 10, 30);
        setLayout(layout);

        // Add components to the frame
//        ImageIcon gifIcon = new ImageIcon("/home/pongsakorn/Pictures/giphy.gif");  // เปลี่ยนเป็นพาธของไฟล์ GIF
//        JLabel gifLabel = new JLabel(gifIcon);
        ImageIcon gifIcon2 = new ImageIcon("path gif");  // เปลี่ยนเป็นพาธของไฟล์ GIF
        JLabel gifLabel2 = new JLabel(gifIcon2);

        add(label);

        add(buttonConnect);


        add(progressBar);

        add(openFileDownload);
//        add(gifLabel);
        add(openDialogButton);
        add(gifLabel2);
//        add(buttonPanel, BorderLayout.SOUTH);
//        JScrollPane scrollPane = new JScrollPane(table);
//
//        // เพิ่ม JScrollPane ลงใน Frame
//        getContentPane().add(scrollPane, BorderLayout.CENTER);
//
//        // แสดง Frame
//        setVisible(true);
    }



    private static int showCustomDialog() {
        String[] options = {"Yes", "No"};
        return JOptionPane.showOptionDialog(
                null,
                "Do you want to exit the application?",
                "Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
    }
}

class connectServerT extends Thread{
    public String ip="";
    private String serverName="";
    private String videoName="";
    private String fileSave = "";
    private String dir="";
    private boolean option;
    connectServerT(String ip , String videoName,String fileSave,String serverName,String dir,boolean option){
        this.ip = ip;
        this.videoName = videoName;
        this.fileSave = fileSave;
        this.serverName = serverName;
        this.dir = dir;
        this.option = option;
    }

    @Override
    public void run() {
        Client cl = new Client(ip,videoName ,fileSave,serverName,dir,option);
        cl.connectToServer();


    }
}