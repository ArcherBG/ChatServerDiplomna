package corp.server.interfaces;

import java.util.List;

import corp.server.model.MessageDTO;


public interface MessageDAO {
	
	long insert(MessageDTO dto);
	
	long   update(MessageDTO dto);
	
	void delete(MessageDTO dto);
	
	List<MessageDTO> query(MessageDTO dto);

	List<MessageDTO> queryAllUnsendMessagesForReceiver(long receiverId);

}
