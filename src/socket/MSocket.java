package socket;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Class {@code MSocket}
 * Socket and ServerSocket are encapsulated
 * to connect and transmit information to players play online-game
 */

public class MSocket {
    private ServerSocket server;
    private Socket client;
    private DataInputStream input;
    private DataOutputStream output;

    /**
     * Whether or not connect has been successfully built
     */
    public boolean connect;

    public MSocket() {
        server = null;
        client = null;
        input = null;
        output = null;
        connect = false;
    }

    /**
     * create a game room and return room number
     * room number is a 8-byte string,
     * the HEX representation of server's IP address
     *
     * @return room number
     */
    public String createRoom() {
        if (connect) {
            return "Error";
        }
        try {
            server = new ServerSocket(8888);
            //server需要在外网或和client同一局域网网段
            String roomNumber;
            String ip = InetAddress.getLocalHost().getHostAddress();
            String num[] = ip.split("\\.");
            for (int i = 0; i < 4; i++) {
                num[i] = Integer.toHexString(Integer.parseInt(num[i])).toUpperCase();
                if (num[i].length() == 1) {
                    num[i] = "0" + num[i];
                }
            }
            roomNumber = num[0] + num[1] + num[2] + num[3];
            return roomNumber;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "Error";
        } catch (IOException e) {
            e.printStackTrace();
            return "Error";
        }
    }

    /**
     * enter a room
     * It may take a lot of time and should be executed in a sub thread
     * If the connection is not successfully built in 100 seconds,
     * it will stop and return {@code false}
     * If successfully receive message, it will return {@code true}
     *
     * @param roomNumber room number
     * @return Whether or not it has been successfully connected
     */
    public boolean enterRoom(String roomNumber) {
        if (connect) {
            return false;
        }
        try {
            String ip = "";
            for (int i = 0; i < 4; i++) {
                ip += Integer.valueOf(roomNumber.substring(i * 2, i * 2 + 2), 16) + ".";
            }
            ip = ip.substring(0, ip.length() - 1);
            client = new Socket(ip, 8888);

            input = new DataInputStream(client.getInputStream());
            output = new DataOutputStream(client.getOutputStream());
            output.writeUTF("OK");

            String result = input.readUTF();
            if ("OK".equals(result)) {
                connect = true;
                return true;
            } else {
                return false;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * After server building a game room,
     * it should start listening to whether there is a player to connect
     * It may take a lot of time and should be executed in a sub thread
     * If the connection is not successfully built in 100 seconds,
     * it will stop and return {@code false}
     * If successfully receive message, it will return {@code true}
     *
     * @return Whether or not it has been successfully connected
     */
    public boolean connect() {
        int count = 0;
        while (count < 100) {
            try {
                client = server.accept();

                input = new DataInputStream(client.getInputStream());
                output = new DataOutputStream(client.getOutputStream());
                String clientInputStr = input.readUTF();

                if (clientInputStr.equals("OK")) {
                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    String replyStr = "OK";
                    out.writeUTF(replyStr);
                    connect = true;
                    return true;
                }
                Thread.sleep(1000);
                System.out.println(count++);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public void send(String str) {
        try {
            output.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Listen if there is any message sent
     * It may take a lot of time and should be executed in a sub thread
     * If the connection is not successfully built in 100 seconds, it will stop and return {@code "Error"}
     * If successfully receive message, it will return the message.
     *
     * @return the message
     */
    public String receive() {
        int count = 0;
        while (count < 100) {
            try {
                String clientInputStr = input.readUTF();
                if (clientInputStr != null) {
                    return clientInputStr;
                }
                Thread.sleep(1000);
                count++;
            } catch (IOException e) {
                e.printStackTrace();
                return "Error";
            } catch (InterruptedException e) {
                e.printStackTrace();
                return "Error";
            }
        }
        return "Error";
    }

    public void disconnect() {
        try {
            input.close();
            output.close();
            if (server != null)
                server.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
