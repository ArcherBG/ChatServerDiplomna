package corp.server.interfaces;

import java.util.List;

import corp.server.model.ClientForm;
import corp.server.model.MessageDTO;

public interface ClientDAO {
	
	long insert(ClientForm form);
	
	long   update(ClientForm form);
	
	void delete(ClientForm form);
	
	List<ClientForm> query(ClientForm form);


	ClientForm findClient(long id);

	ClientForm findClient(String email);
}
