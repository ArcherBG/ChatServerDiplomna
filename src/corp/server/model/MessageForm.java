package corp.server.model;

public class MessageForm extends ClientForm {

    public long senderId;
    public long receiverId;
    public long messageId;
    public String username;
    public String message;


    public long getSenderId() {
        return senderId;
    }

    public long getReceiverId() {
        return receiverId;
    }


    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public String getUsername() {
        return username;
    }


    public String getMessage() {
        return message;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(long receiverId) {
        this.receiverId = receiverId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
