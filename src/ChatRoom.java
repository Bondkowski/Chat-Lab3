
import java.util.ArrayList;
import java.util.HashMap;
 
public class ChatRoom {
    private ChatServer server;
    private String chatRoomName;
    private int roomRef;
    private HashMap<Integer,ChatClient> allRoomClients = new HashMap<Integer, ChatClient>();

   

    public ChatRoom(int id, String n, ChatServer s){
        chatRoomName = n;
        roomRef = id;
        server = s;
    }
    public HashMap<Integer, ChatClient> getRoomClients(){
        return allRoomClients;
    }
    public int getRoomRef(){
        return roomRef;
    }
    public String getChatRoomName(){
        return chatRoomName;
    }
    public boolean isEmpty(){
        return allRoomClients.isEmpty();
    }
    public void close(){
        System.exit(0);
    }
    public void addClient(Integer s, ChatClient c){
        allRoomClients.put(s,c);
    }
    public void removeClient(String c){

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
            allRoomClients.get(clientJOIN_ID).setMessage(strings);
        } catch (Exception e) {
            e.getMessage();

        }
    }
}
