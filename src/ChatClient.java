import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;


public class ChatClient implements Runnable {
    private Socket connectionSocket = null;
    private ChatServer server;
    private boolean work = true;
    private int id;
    private String name;
    private ArrayList<String> messageInArray;

    public ChatClient(Socket socket, ChatServer server) {
        new Thread("ClientThread");
        this.connectionSocket = socket;
        //to be able to close the server from the thread we have reference on it in the thread
        this.server = server;
    }
    // In this method we check if chatRoom exist and if not, we create it

    public int getId(){
        return id;
    }
    public void setId(int i){
        id = i;
    }
    public String getName(){
        return name;
    }
    public String getIp(){
        return connectionSocket.getInetAddress().toString().substring(1);
    }
    public int getPort(){
        return connectionSocket.getPort();
    }
    public void setMessage(ArrayList<String> s){
        messageInArray = s;
    }

    //next method transforms message from Array to a string, so it is easy to send with printwriter

    public String getMessage(){
        String message = "Something was wrong...";
        String identity = messageInArray.get(0);
        if(identity.startsWith("CHAT:")){
           message = (messageInArray.remove(0)+"\n"+messageInArray.remove(0)+"\n"+messageInArray.remove(0)+"\n\n");
        } else if(identity.startsWith("JOINED_CHATROOM:")){
            message = (messageInArray.remove(0)+"\n"+messageInArray.remove(0)+"\n"+messageInArray.remove(0)+"\n"+messageInArray.remove(0)+"\n"+messageInArray.remove(0)+"\n");
        } else if (identity.startsWith("LEAVE_CHATROOM:")){
            message = (messageInArray.remove(0)+"\n"+messageInArray.remove(0)+"\n");

        }
        return message;
    }
    // In this method we organize message from client into arrayList.
    // It also checks the length of array to prevent "stealing" of strings from the next message.
    public ArrayList<String> getArrayFromClient(String sentence, BufferedReader buffer) throws IOException{
        ArrayList<String> arrayFromClient = new ArrayList<String>();
        int arrayLength;

        if(sentence.startsWith("CHAT")||sentence.startsWith("DISCONNECT"))
            arrayLength=4;
        else
            arrayLength=5;

        arrayFromClient.add(sentence);
        while (buffer.ready() && arrayFromClient.size() < arrayLength) {
            sentence = buffer.readLine();
            arrayFromClient.add(sentence);
        }

        return arrayFromClient;
    }

    public void leaveChatRoom(ArrayList<String> strings) {
        try {
            ChatRoom chatRoom;
            Integer clientJOIN_ID;
            Integer chatROOM_REF = Integer.parseInt(strings.get(0).substring(14));
            clientJOIN_ID = Integer.parseInt(strings.get(1).substring(9));

            chatRoom = server.getAllChatRooms().get(chatROOM_REF);

            if (chatRoom.isEmpty()) {
                ChatServer.removeChatRoom(chatRoom);

            }
            strings.clear();
            strings.add("LEFT_CHATROOM:"+chatROOM_REF);
            strings.add("JOIN_ID:"+clientJOIN_ID);
            setMessage(strings);
        } catch (Exception e) {
            e.getMessage();

        }
    }

    public void run() {
        try {
            //reading input from client
            BufferedReader messageFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            PrintWriter responseToClient = new PrintWriter(connectionSocket.getOutputStream(), true);


            while(work) {

                String sentence = " ";
                if (messageFromClient.ready()) {
                    // reading input line by line
                    sentence = messageFromClient.readLine();
                }
                if (sentence.startsWith("CHAT")) {
                    try {
                    	System.out.println("CHAT message received");
                        server.sendMessage(getArrayFromClient(sentence, messageFromClient));
                    } catch (Exception e) {
                        responseToClient.println("ERROR_CODE:" + 1 + "\n" + "ERROR_DESCRIPTION:" + e.getMessage());
                    }
                } else if (sentence.startsWith("JOIN_CHATROOM:")) {
                    try {
                        if (name == null) {
                            name = getArrayFromClient(sentence, messageFromClient).get(3).substring(13);
                        }
                        server.joinChatRoom(getArrayFromClient(sentence, messageFromClient), this);
                        //responseToClient.println();
                    } catch (Exception e) {
                        responseToClient.println("ERROR_CODE:" + 2 + "\n" + "ERROR_DESCRIPTION:" + e.getMessage());
                    }
                } else if (sentence.startsWith("LEAVE_CHATROOM:")) {
                    try {
                        leaveChatRoom(getArrayFromClient(sentence, messageFromClient));
                    } catch (Exception e) {
                        responseToClient.println("ERROR_CODE:" + 3 + "\n" + "ERROR_DESCRIPTION:" + e.getMessage());
                    }
                } else if (sentence.startsWith("DISCONNECT:")) {
                    try {
                        server.disconnectClient(getArrayFromClient(sentence, messageFromClient), this);
                    } catch (Exception e) {
                        responseToClient.println("ERROR_CODE:" + 4 + "\n" + "ERROR_DESCRIPTION:" + e.getMessage());
                    }
                } else if (sentence.startsWith("HELO")) {
                    responseToClient.println(sentence + "\nIP:" + connectionSocket.getLocalAddress().toString().substring(1) + "\nPort: " + connectionSocket.getLocalPort() + "\nStudentID:13312345\n");
                } else if (sentence.equals("KILL_SERVICE")) {
                    work = false;
                    connectionSocket.close();
                    server.shutdownServer();
                    //for testing server
                    System.out.println("Received from client: " + sentence);
                } else if (!sentence.equals(" ")) {
                    //for testing server
                    System.out.println("Received from client: " + sentence);
                }

                //Sending a message to the client

                if (!messageInArray.isEmpty())
                    responseToClient.println(getMessage());
                    System.out.println("Another message sent");
                }
                System.out.println("idle");
        
        
        } catch (IOException e) {
            System.err.println("Can not listen to the socket:  " + connectionSocket.getLocalPort());
            e.getMessage();
            e.printStackTrace();
        }
    }
}