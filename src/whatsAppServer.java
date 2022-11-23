import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.StringTokenizer;

public class whatsAppServer {
    private HashMap<String, String> UserInfo = new HashMap<String, String>();
    private HashMap<String, Socket> socketList = new HashMap<String, Socket>();
    private HashMap<String, String> groupList = new HashMap<String, String>();
    private HashMap<String, Integer> clientStatus = new HashMap<String, Integer>();
    private HashMap<String, File> fileWaitToSend = new HashMap<String, File>();
    private String receiverId;
    private String userId;
    private String fileName;

    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    public whatsAppServer(int port) throws IOException {
        this.UserInfo = HashMapFromTextFile("UserInfo");
        this.groupList = HashMapFromTextFile("groupList");
        ServerSocket srvSocket = new ServerSocket(port);


        while (true) {
            print("Listening at port %d...\n", port);
            Socket clientSocket = srvSocket.accept();
            userId = Login(clientSocket);

            synchronized (socketList) {
                socketList.put(userId, clientSocket);
                clientStatus.put(userId, 0);


            }

            Thread t = new Thread(() -> {
                try {
                    serve(userId, clientSocket);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                synchronized (socketList) {
                    socketList.remove(userId);
                }
            });
            t.start();
        }
    }

    private void serve(String userId, Socket clientSocket) throws IOException {
        print("Established a connection to host %s:%d\n\n",
                clientSocket.getInetAddress(), clientSocket.getPort());
        DataInputStream in;
        DataOutputStream out;
        in = new DataInputStream(clientSocket.getInputStream());
        out = new DataOutputStream(clientSocket.getOutputStream());

        // send the number of user except himself
        out.writeInt((UserInfo.size() - 1));
        out.flush();
        //================sending user list except the current user====================================
        UserInfo.forEach((key, value) -> {
            try {
                if (!key.equals(userId)) {
                    sendString(key, out);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        //==================================================================================================
        //if this user login has a file wait to him


        while (true) {
            int command = in.readInt();
            System.out.println("Thread " + userId + ": " + command);
            //1->message
            //2->forward img file
            //3->createGroup
            //4->forward  file
            switch (command) {
                case 1:
                    forwardMessage(userId, in, out);
                    break;
                case 2:
                    forwardImageFile(in);
                    break;
                case 3:
                    createGroup();
                    break;
                case 4:
                    forwardFile(in);
                    break;
            }

        }

    }

    //    private void checkUnreadMsg(String userId,DataOutputStream out) throws IOException {
//        if (fileWaitToSend.containsKey(userId)){
//            System.out.println("check");
//        }
//    }
    private void createGroup() {
    }

    private File storeFile(DataInputStream in) throws IOException {
        //=======================store===========================================
        receiverId = receiveString(in);
        byte[] buffer = new byte[1024];
        int remain = in.readInt();
        String filename = "";
        while (remain > 0) {
            int len = in.read(buffer, 0, Math.min(remain, buffer.length));
            filename += new String(buffer, 0, len);
            remain -= len;
        }

        File file = new File(System.getProperty("user.dir") + "/src/File/" + System.currentTimeMillis() + "_" + receiverId + "_" + filename);
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
        return file;
    }

    private void forwardFile(DataInputStream in) throws IOException {
        //=======================store===========================================
        File file = storeFile(in);
        //===========================Send File==================================
        if (socketList.containsKey(receiverId)) {
            DataOutputStream toReceiver = new DataOutputStream(socketList.get(receiverId).getOutputStream());
            toReceiver.writeInt(4);
            sendString(userId,toReceiver);
            toReceiver.flush();
//            File fileToReceiver = new File(System.getProperty("user.dir") + "/src/File/" + System.currentTimeMillis() + "_" + receiverId + "_" + filename);
            if (!file.exists() && file.isDirectory())
                throw new IOException("Invalid path!");
            FileInputStream fis = new FileInputStream(file);
            byte[] filenameToReceiver = file.getName().getBytes();
            toReceiver.writeInt(filenameToReceiver.length);
            toReceiver.write(filenameToReceiver, 0, filenameToReceiver.length);
            long sizeToReceiver = file.length();
            toReceiver.writeLong(sizeToReceiver);
            print("Uploading %s (%d bytes)", file.getName(), sizeToReceiver);
            byte[] bufferToReceiver = new byte[1024];
            while (sizeToReceiver > 0) {
                int len = fis.read(bufferToReceiver, 0, (int) Math.min(sizeToReceiver, bufferToReceiver.length));
                toReceiver.write(bufferToReceiver, 0, len);
                sizeToReceiver -= len;
                print(".");
            }
            toReceiver.flush();
            print("Complete!");
            System.out.println("Delete status: " + file.delete());

        } else {
            //TODO-> when the user login again send the image immediately
            System.out.println(receiverId + " is put to the wait to send list");
            fileWaitToSend.put(receiverId, file);
        }

  /*      TODO ->
        If the receiver is offline, the files will be stored on the server side
        If the receiver is online, image files will be transferred immediately, and the
        delivered images will be displayed immediately
        */
    }

    private void forwardImageFile(DataInputStream in) throws IOException {
        //=======================store===========================================
        File file = storeFile(in);
        //===========================Send File==================================
        if (socketList.containsKey(receiverId)) {
            DataOutputStream toReceiver = new DataOutputStream(socketList.get(receiverId).getOutputStream());
            toReceiver.writeInt(2);
            toReceiver.flush();
//            sendString(userId, toReceiver);
//            File fileToReceiver = new File(System.getProperty("user.dir") + "/src/File/" + System.currentTimeMillis() + "_" + receiverId + "_" + filename);
            if (!file.exists() && file.isDirectory())
                throw new IOException("Invalid path!");
            FileInputStream fis = new FileInputStream(file);
            byte[] filenameToReceiver = file.getName().getBytes();
            toReceiver.writeInt(filenameToReceiver.length);
            toReceiver.write(filenameToReceiver, 0, filenameToReceiver.length);
            long sizeToReceiver = file.length();
            toReceiver.writeLong(sizeToReceiver);
            print("Uploading %s (%d bytes)", file.getName(), sizeToReceiver);
            byte[] bufferToReceiver = new byte[1024];
            while (sizeToReceiver > 0) {
                int len = fis.read(bufferToReceiver, 0, (int) Math.min(sizeToReceiver, bufferToReceiver.length));
                toReceiver.write(bufferToReceiver, 0, len);
                sizeToReceiver -= len;
                print(".");
            }
            toReceiver.flush();
            print("Complete!");
            System.out.println("Delete status: " + file.delete());
        } else {
            //TODO-> when the user login again send the image immediately
            System.out.println(receiverId + " is put to the wait to send list");
            fileWaitToSend.put(receiverId, file);
        }

  /*      TODO ->
        If the receiver is offline, the files will be stored on the server side
        If the receiver is online, image files will be transferred immediately, and the
        delivered images will be displayed immediately
        */
    }

    private void forwardMessage(String userId, DataInputStream in, DataOutputStream out) throws IOException {
        String msg = receiveString(in);
        System.out.println(msg);
        // break the string into message and recipient part
        StringTokenizer str = new StringTokenizer(msg, ":");
        String receiverId = str.nextToken();
        String text = userId + ":" + str.nextToken();
//            String text =  str.nextToken();
        if (UserInfo.containsKey(receiverId)) {
            forwardSingle(receiverId, text);
        } else if (groupList.containsKey(receiverId)) {
//                forwardGroup(receiverId, text);
        }
    }

    public void forwardSingle(String receiverId, String msg) {
        //sending msg to a particular user
        synchronized (socketList) {
            try {
                //check if the person online
                if (socketList.containsKey(receiverId)) {
                    DataOutputStream toReceiver = new DataOutputStream(socketList.get(receiverId).getOutputStream());
                    toReceiver.writeInt(1);
                    sendString(msg, toReceiver);
                }
            } catch (IOException ex) {
                print("Unable to forward message to %s:%d\n",
                        socketList.get(receiverId).getInetAddress().getHostName(), socketList.get(receiverId).getPort());
            }
        }
    }

    public void forwardGroup(String group, String msg) throws IOException {
        //sending msg to a group
        //get the group members from the group list
        String members = groupList.get(group);
        String[] member = members.split(",");
        synchronized (socketList) {
            for (int i = 0; i < member.length; i++) {
                try {
                    //loop over every member to send msg one by one
                    //check if they are online
                    if (UserInfo.containsKey(member[i])) {
                        if (socketList.containsKey(member[i])) {
                            DataOutputStream toReceiver = new DataOutputStream(socketList.get(member[i]).getOutputStream());
                            sendString(msg, toReceiver);
                        }
                    }
                } catch (IOException ex) {
                    print("Unable to forward message to %s:%d\n",
                            socketList.get(member[i]).getInetAddress().getHostName(), socketList.get(member[i]).getPort());
                }
            }
        }
    }
//    private void forward(String msg){
//        synchronized (socketList) {
//            for (Socket socket : socketList) {
//                try {
//                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//                    out.writeInt(msg.length());
//                    out.write(msg.getBytes(), 0, msg.length());
//                } catch (IOException ex) {
//                    print("Unable to forward message to %s:%d\n",
//                            socket.getInetAddress().getHostName(), socket.getPort());
//                }
//            }
//        }
//    }

    private String Login(Socket clientSocket) throws IOException {
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        String userId = "";
        String userPw = "";
        boolean valid = false;
        while (!valid) {
            try {
                userId = receiveString(in);
                userPw = receiveString(in);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //offline and
            if (!socketList.containsKey(userId) && UserInfo.containsKey(userId)) {
                valid = UserInfo.get(userId).equals(userPw);
            }
            out.writeBoolean(valid);
            out.flush();
        }
        return userId;
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

    public static HashMap<String, String> HashMapFromTextFile(String filename) {
        HashMap<String, String> map
                = new HashMap<String, String>();
        BufferedReader br = null;
        try {
            // create file object
            File file = new File(filename);
            // create BufferedReader object from the File
            br = new BufferedReader(new FileReader(file));
            String line = null;
            // read file line by line
            while ((line = br.readLine()) != null) {
                // split the line by :
                String[] parts = line.split(":");
                // first part is name, second is number
                String name = parts[0].trim();
                String number = parts[1].trim();
                // put name, number in HashMap if they are not empty
                if (!name.equals("") && !number.equals(""))
                    map.put(name, number);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Always close the BufferedReader
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
                ;
            }
        }
        return map;
    }


    public static void main(String[] args) throws Exception {
        int port = 123;
        new whatsAppServer(port);
    }


}