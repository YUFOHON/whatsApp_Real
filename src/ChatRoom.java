import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.awt.*;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.StringTokenizer;

public class ChatRoom implements Initializable {

    ObservableList<Node> children;
    //==============================After Login fxml element=======================================================
    @FXML
    private Label showReceiver;
    @FXML
    private Button closeButton;
    @FXML
    private Button buttonCreateGroup;
    @FXML
    private VBox userList;
    @FXML
    public Button buttonSend;
    @FXML
    public TextField txtInput;
    @FXML
    public BorderPane borderPane;
    @FXML
    private VBox messagePane;
    @FXML
    private MenuItem Cat;
    @FXML
    private MenuItem Girl;
    @FXML
    private Label showUserName;
    @FXML
    private TextField groupInput;
    @FXML
    private ScrollPane scrollPane;
    private String receiverName;
    @FXML
    private Button sendFileButton;
    private String senderName;
    private File chatHistory;
    int lock = 1;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean fileTransferring = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println(whatsAppClient.userInputName);
        whatsAppClient.stg.setHeight(640);
        whatsAppClient.stg.setWidth(1050);

        children = messagePane.getChildren();
        whatsAppClient wc = new whatsAppClient();
        in = whatsAppClient.in;
        out = whatsAppClient.out;
        try {
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                String s = wc.receiveString(in);
                Button user = new Button(s);
                user.setMinSize(228, 40);
                user.setStyle("-fx-font-size: 2em;");
                user.setTextFill(Color.BLACK);
                user.setUserData(s);
                user.setOnAction(event -> {
                    messagePane.getChildren().clear();
                    receiverName = (String) user.getUserData();
                    children.add(messageNode("You are connected with " + receiverName, true));
                    String currentPath = System.getProperty("user.dir") + "/src/chatHistory";
                   /*
                     if chatHistory dont have any database create a one
                    such as AB or BA it depen on which client send the ms first
                    */
                    File dir = new File(currentPath);
                    String fileNameNormal = whatsAppClient.userInputName + receiverName;
                    String fileNameReverse = receiverName + whatsAppClient.userInputName;
                    String[] list = dir.list();
                    if (list.length == 0) {
                        File file = new File(currentPath + "/" + fileNameNormal + ".txt");

                        try {
                            file.createNewFile();
                            chatHistory = file;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //========================Search====================================================================
                        boolean isFound = false;
                        for (int a = 0; a < list.length; a++) {
                            String filename = list[a];
                            if (filename.equals(fileNameNormal + ".txt") || filename.equals(fileNameReverse + ".txt")) {
                                isFound = true;

                                File file;
                                if (filename.equals(fileNameNormal + ".txt"))
                                    file = new File(currentPath + "/" + fileNameNormal + ".txt");
                                else
                                    file = new File(currentPath + "/" + fileNameReverse + ".txt");
                                //get the current chat History
                                chatHistory = file;
                                //read the file
                                try {
                                    Scanner scan = new Scanner(file);
                                    while (scan.hasNextLine()) {
                                        String data = scan.nextLine();
                                        String[] split = data.split(":");
                                        if (split[0].equals(receiverName))
                                            children.add(messageNode(split[1], false));
                                        else
                                            children.add(messageNode(split[1], true));
                                    }
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                        // if search result null-> file doesn't exist -> creat a new one
                        if (isFound == false) {
                            File file = new File(currentPath + "/" + fileNameNormal + ".txt");
                            try {
                                file.createNewFile();
                                chatHistory = file;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
//========================================================================================================================================

                    scrollPane.setVvalue(0);
                    showReceiver.setText(receiverName);

                });
                userList.getChildren().add(user);
                showUserName.setText(whatsAppClient.userInputName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread receive = new Thread(() -> {
            while (true)
                try {
                    if (in.available() != 0 && lock == 1 && !fileTransferring) {
                        lock++;

                        int command = in.readInt();
                        System.out.println("Thread: command: " + command);
                        if (command == 1) {
                            String msg = receiveString(in);
                            String[] s = msg.split(":");
                            // if A is not connect to B
                            if (!isConnect(msg)) {
                                lock--;
                                continue;
                            }
                            receivedMessage(wc, in, s[1]);
                            Thread.sleep(10);
                        } else if (command == 2) {
                            System.out.println("start receiveFile");
                            fileTransferring = true;
                            receiveImageFile();
                            fileTransferring = false;
                            System.out.println("completed");
                            lock--;
                            Thread.sleep(10);
                        } else if (command == 3) {

                        } else if (command == 4) {
                            receiveFile();
                        }
                    }
//                    System.out.println("start sleeping");
//                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
//                    Thread.currentThread().interrupt();
                }
        });
        receive.start();
        messagePane.heightProperty().addListener(event -> {
            scrollPane.setVvalue(1);
        });
        txtInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                try {
                    display_send_Message();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        buttonSend.setOnMouseClicked(event -> {
            try {
                display_send_Message();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        children.add(messageNode("Wellcome " + whatsAppClient.userInputName, true));
        closeButton.setOnAction(event -> {
            System.exit(0);
        });
        Cat.setOnAction(event -> {
            if (receiverName == null) {
                children.add(messageNode("Please select the receiver", true));
                return;
            }
            String currentPath = System.getProperty("user.dir") + "/src/img/cat.jpg";
            try {
                sendFile(currentPath, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            displayEmoji(currentPath, true);
        });
        Girl.setOnAction(event -> {
            if (receiverName == null) {
                children.add(messageNode("Please select the receiver", true));
                return;
            }
            String currentPath = System.getProperty("user.dir") + "/src/img/girl.jpg";
            try {
                sendFile(currentPath, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            displayEmoji(currentPath, true);
        });
        scrollPane.setVvalue(0);
        sendFileButton.setOnAction(event -> {

            if (receiverName == null) {
                children.add(messageNode("Please select the receiver", true));
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Resource File");
            File file =fileChooser.showOpenDialog(whatsAppClient.stg);
            if (file != null) {
                System.out.println(file.getAbsolutePath());
                try {
                    sendFile(file.getAbsolutePath(),false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private boolean isConnect(String msg) throws IOException {
        StringTokenizer str = new StringTokenizer(msg, ":");
        senderName = str.nextToken();
        System.out.println("I am " + whatsAppClient.userInputName + " and " + senderName + " want to send me msg " + msg);
        return senderName.equals(receiverName);
    }

    //============================String==========================================================
    public void sendString(String string, DataOutputStream out) throws IOException {
        int len = string.length();
        out.writeInt(len);
        out.write(string.getBytes(), 0, len);
        out.flush();
    }

    public String receiveString(DataInputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        String str = "";
        int len = in.readInt();
        while (len > 0) {
            int l = in.read(buffer, 0, Math.min(len, buffer.length));
            str += new String(buffer, 0, l);
            len -= l;
        }
        return str;
    }

    //============================Display==========================================================
    private void display_send_Message() throws IOException {

        String text = txtInput.getText();
        try {
            if (receiverName == null) {
                children.add(messageNode("Please select the receiver", true));
                txtInput.clear();
                return;
            } else {
                out.writeInt(1);
                sendString(receiverName + ":" + text, out);
                FileWriter fw = new FileWriter(chatHistory.getAbsoluteFile(), true);
                PrintWriter out = new PrintWriter(fw);
                out.println(whatsAppClient.userInputName + ":" + text);
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        txtInput.clear();

        Platform.runLater(() -> {
            children.add(messageNode(text, true));
        });
    }

    public void receivedMessage(whatsAppClient wc, DataInputStream in, String s) throws IOException {
        FileWriter f = new FileWriter(chatHistory.getName(), true);
        PrintWriter out = new PrintWriter(f);
        out.flush();
        out.close();
        Platform.runLater(() -> {
            children.add(messageNode(s, false));
            lock--;
        });
    }

    private void displayEmoji(String path, Boolean isRight) {
//        File file = new File(path);
//        whatsAppClient.out.


        Platform.runLater(() -> {
            children.add(imageNode(path, isRight));

        });
    }

    //============================Node==========================================================
    private Node messageNode(String text, boolean alignToRight) {
        HBox box = new HBox();
        box.paddingProperty().setValue(new Insets(10, 10, 10, 10));

        if (alignToRight)
            box.setAlignment(Pos.BASELINE_RIGHT);
        javafx.scene.control.Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-border-color: black;");
        label.setMaxWidth(809 / 2);
        label.setMinWidth(50);
        label.setMinHeight(50);
        box.getChildren().add(label);
        return box;
    }


    private Node imageNode(String imagePath, boolean alignToRight) {
        try {
            HBox box = new HBox();
            box.paddingProperty().setValue(new Insets(10, 10, 10, 10));
            if (alignToRight)
                box.setAlignment(Pos.BASELINE_RIGHT);
            FileInputStream in = new FileInputStream(imagePath);
            ImageView imageView = new ImageView(new Image(in));
            imageView.setFitWidth(300);
            imageView.setFitHeight(300);
            imageView.setPreserveRatio(true);
            box.getChildren().add(imageView);
            return box;
        } catch (IOException ex) {
            ex.printStackTrace();
            return messageNode("!!! Fail to display an image !!!", alignToRight);
        }
    }

    //============================File==========================================================
    private void sendFile(String path, boolean isImage) throws IOException {
        if (isImage) {
            fileTransferring = true;
            out.writeInt(2);
            out.flush();
            sendString(receiverName, out);
            File file = new File(path);
            if (!file.exists() && file.isDirectory())
                throw new IOException("Invalid path!");
            FileInputStream in = new FileInputStream(file);
            byte[] filename = file.getName().getBytes();
            out.writeInt(filename.length);
            out.write(filename, 0, filename.length);
            long size = file.length();
            out.writeLong(size);
            print("Uploading %s (%d bytes)", path, size);
            byte[] buffer = new byte[1024];
            while (size > 0) {
                int len = in.read(buffer, 0, (int) Math.min(size, buffer.length));
                out.write(buffer, 0, len);
                size -= len;
                print(".");
            }
            out.flush();
            in.close();
            print("Complete!");
            fileTransferring = false;
        } else {
            fileTransferring = true;
            out.writeInt(4);
            out.flush();
//            out.writeBoolean(true);
            sendString(receiverName, out);
            File file = new File(path);
            if (!file.exists() && file.isDirectory())
                throw new IOException("Invalid path!");
            FileInputStream in = new FileInputStream(file);
            byte[] filename = file.getName().getBytes();
            out.writeInt(filename.length);
            out.write(filename, 0, filename.length);
            long size = file.length();
            out.writeLong(size);
            print("Uploading %s (%d bytes)", path, size);
            byte[] buffer = new byte[1024];
            while (size > 0) {
                int len = in.read(buffer, 0, (int) Math.min(size, buffer.length));
                out.write(buffer, 0, len);
                size -= len;
                print(".");
            }
            out.flush();
            in.close();
            print("Complete!");
            fileTransferring = false;
            children.add(messageNode("file sended", true));
        }
    }

    private void receiveImageFile() throws IOException {
        byte[] buffer = new byte[1024];
        int remain = in.readInt();
        String filename = "";
        while (remain > 0) {
            int len = in.read(buffer, 0, Math.min(remain, buffer.length));
            filename += new String(buffer, 0, len);
            remain -= len;
        }
        String currentPath = System.getProperty("user.dir") + "/src/File/clientReceivedFiles";
        File directory = new File(currentPath);
        if (!directory.exists()) directory.mkdir();
        String filePath = currentPath + "/" + System.currentTimeMillis() + "_" + filename;
        File file = new File(filePath);
        if (!file.exists()) file.createNewFile();
        FileOutputStream fout = new FileOutputStream(file);
        long size = in.readLong();
        print("Downloading %s ...\n", filename);
        while (size > 0) {
            int len = in.read(buffer, 0, (int) Math.min(size, buffer.length));
            fout.write(buffer, 0, len);
            size -= len;
            print(".");
        }
        print("Completed!\n");
        fout.flush();
        fout.close();
//        String fileName = receiveString(in);
//        String currentPath = System.getProperty("user.dir") + "/src/File/clientReceivedFiles" + whatsAppClient.userInputName;
//        File directory = new File(currentPath);
//        if (!directory.exists()) {
//            directory.mkdir();
//        }
//        Files.copy(in, Paths.get(currentPath + "/" + fileName), StandardCopyOption.REPLACE_EXISTING);
        Platform.runLater(() -> {
            children.add(imageNode(filePath, false));
        });
    }

    private void receiveFile() throws IOException {
        byte[] buffer = new byte[1024];
        senderName = receiveString(in);
        int remain = in.readInt();

        String filename = "";
        while (remain > 0) {
            int len = in.read(buffer, 0, Math.min(remain, buffer.length));
            filename += new String(buffer, 0, len);
            remain -= len;
        }
        String currentPath = System.getProperty("user.dir") + "/src/File/clientReceivedFiles";
        File directory = new File(currentPath);
        if (!directory.exists()) directory.mkdir();
        String filePath = currentPath + "/" + System.currentTimeMillis() + "_" + filename;
        File file = new File(filePath);
        if (!file.exists()) file.createNewFile();
        FileOutputStream fout = new FileOutputStream(file);
        long size = in.readLong();
        print("Downloading %s ...\n", filename);
        while (size > 0) {
            int len = in.read(buffer, 0, (int) Math.min(size, buffer.length));
            fout.write(buffer, 0, len);
            size -= len;
            print(".");
        }
        print("Completed!\n");
        fout.flush();
        fout.close();
//        String fileName = receiveString(in);
//        String currentPath = System.getProperty("user.dir") + "/src/File/clientReceivedFiles" + whatsAppClient.userInputName;
//        File directory = new File(currentPath);
//        if (!directory.exists()) {
//            directory.mkdir();
//        }
//        Files.copy(in, Paths.get(currentPath + "/" + fileName), StandardCopyOption.REPLACE_EXISTING);
        Platform.runLater(() -> {
            children.add(messageNode("you got a file from " + senderName + " store in " + file.getAbsolutePath(), false));
        });
    }

    public static void print(String str, Object... o) {
        System.out.printf(str, o);
    }

}