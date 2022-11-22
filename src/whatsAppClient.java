import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.event.MouseEvent;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class whatsAppClient extends Application {
    public static Stage stg;
    private String ip;
    private int port = 123;
    whatsAppClient wc;
    //==============================Login fxml element=======================================================
    @FXML
    private Button button;
    @FXML
    private Label wrongLogin;
    @FXML
    private TextField username;
    @FXML
    private TextField password;
    //=================================================================================================
    static String userInputName;
    static String userInputPassword;
    public static Socket socket = null;
    public static DataInputStream in = null;
    public static DataOutputStream out = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
//        wc = new whatsAppClient();
        stg = primaryStage;
        primaryStage.setResizable(false);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("loginForm.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root, 600, 450);
        primaryStage.setTitle("Welcome");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public void userLogin() throws Exception {
        if (socket == null)
            socket = new Socket(ip, port);

        if (checkLogin()) {
            try {

//
//                //===================================JUMP TO  CHAT ROOM=================================================
                changeScene("afterLogin.fxml");
                //======================================================================================================
            } catch (Exception e) {
                System.out.println("error " + e.getMessage());
            }

        } else
            wrongLogin.setText("Wrong info");

    }

    private boolean checkLogin() throws Exception {
        userInputName = username.getText().toString();
        userInputPassword = password.getText().toString();
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        sendString(userInputName, out);
        sendString(userInputPassword, out);
        Boolean isValid = in.readBoolean();
//        System.out.println(isValid);

        return (isValid);
//        if (userInputName.equals(userTest) && userInputPassword.equals(userPasswordTest)) {
//            wrongLogin.setText("Success!");
//            return true;
//        } else if (username.getText().isEmpty() && password.getText().isEmpty()) {
//            wrongLogin.setText("Please enter user data");
//        } else {
//            wrongLogin.setText("wrong user info");
//        }

    }


    public void changeScene(String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));

        stg.getScene().setRoot(root);

    }

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

    @FXML
    protected void initialize() throws IOException {
        //===================================get the local machine ip address==========================================
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        }
//===================================get the local machine ip address==========================================


    }


    public static void print(String str) {
        System.out.print(str);
    }

    public static void println(String str) {
        System.out.println(str);
    }
}