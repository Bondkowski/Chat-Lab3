import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class ChatServer {
    //Thread pool up to ten working threads in one moment
    private static ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    private static ServerSocket mainSocket;
    private static HashMap<Integer,ChatRoom> allChatRooms = new HashMap<Integer, ChatRoom>();
    private static HashMap<String, Integer> allRoomRefs = new HashMap<String, Integer>();
    private static int clientIdCounter = 0;
    private static int roomIdCounter = 0;
    private static boolean work = true;

    public ChatServer (ServerSocket socket){
        mainSocket = socket;
    }

    public HashMap<Integer,ChatRoom> getAllChatRooms(){
        return allChatRooms;
    }

    public void sendMessage(ArrayList<String> strings){
        Integer room_ref = Integer.parseInt(strings.get(0).substring(6));
        System.out.println(room_ref);
        strings.remove(1);
        for(Map.Entry<Integer, ChatClient> entry : allChatRooms.get(room_ref).getRoomClients().entrySet()){
            entry.getValue().setMessage(strings);
        }
    }

    public static void addChatRoom(ChatRoom c, Integer i){
        allChatRooms.put(c.getRoomRef(),c);
        allRoomRefs.put(c.getChatRoomName(), i);
    }
    public static ChatRoom isChatRoomExists(String s){
        if(allRoomRefs.containsKey(s)){
            return allChatRooms.get(allRoomRefs.get(s));
        }else {
            return null;
        }
    }
    public static void removeChatRoom(ChatRoom chatRoom){
        chatRoom.close();
    }
    public static void addChatClient(ChatClient c){
        executorService.submit(c);
        c.setId(++clientIdCounter);
    }
    public static void removeChatClient(ChatClient c){

    }
    public void joinChatRoom(ArrayList<String> strings, ChatClient chatClient){
        ChatRoom chatRoom;
        int clientId;
        String newChatRoomName = strings.get(0).substring(14);
        clientId = chatClient.getId();
        if((chatRoom = isChatRoomExists(newChatRoomName))!= null){
            chatRoom.addClient(clientId ,chatClient);
        }else{
            chatRoom = new ChatRoom(++roomIdCounter, newChatRoomName, this);
            addChatRoom(chatRoom, roomIdCounter);
            chatRoom.addClient(clientId, chatClient);
        }
        strings.clear();
        strings.add("JOINED_CHATROOM:"+chatRoom.getChatRoomName());
        strings.add("SERVER_IP:"+chatClient.getIp());
        strings.add("PORT:"+chatClient.getPort());
        strings.add("ROOM_REF:"+chatRoom.getRoomRef());
        strings.add("JOIN_ID:"+chatClient.getId());
        chatClient.setMessage(strings);
    }

    public void disconnectClient(ArrayList<String> strings, ChatClient c){
        removeChatClient(c);
    }
    public void shutdownServer(){
        try {
            executorService.shutdownNow();
            mainSocket.close();
            work = false;
            System.exit(1);
        } catch (IOException e){
            e.getMessage();
            System.err.println("Something wrong was happen...");
        }
    }

    public static void main(String[] args) throws IOException {
        try{
            //checking if we have an argument for port number
            if(args.length != 1) {
                System.err.println("Incorrect arguments! Type TCP port number as only argument");
                System.exit(1);
            }
            int portNumber = Integer.parseInt(args[0]);
            mainSocket = new ServerSocket(portNumber);
            ChatServer server = new ChatServer(mainSocket);

            while (work) {
                int queueSize = executorService.getQueue().size();
                Socket listeningSocket = mainSocket.accept();
                DataOutputStream responseToClient = new DataOutputStream(listeningSocket.getOutputStream());
                //limiting the queue of clients waiting for the thread by 10.
                //In total we will have 20: 10 working threads and 10 clients in the queue.
                //All other client will be discarded with message
                if(queueSize > 10) {
                    responseToClient.writeBytes("Server is overloaded... Sorry!\n");
                }else {
                    ChatClient c = new ChatClient(listeningSocket,server);
                    addChatClient(c);
                }
            }
        }catch(Exception e){
            System.err.println(e);
            e.printStackTrace();
        }

    }
}