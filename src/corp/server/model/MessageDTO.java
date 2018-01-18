package corp.server.model;

public class MessageDTO  extends MessageForm{
	
	public int isReceivedByClient;	
	public String timestamp;
	
		
	public final int getIsReceivedByClient() {
		return isReceivedByClient;
	}
	public final void setIsReceivedByClient(int isReceivedByClient) {
		this.isReceivedByClient = isReceivedByClient;
	}
	public final String getTimestamp() {
		return timestamp;
	}
	public final void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

}
