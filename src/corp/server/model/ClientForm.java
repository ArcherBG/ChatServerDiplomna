package corp.server.model;

public class ClientForm {

	public long id;
	public String email;
	public String password;
	public String accessToken;
	public String command;
	public int isAdmin;

	public ClientForm() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String type) {
		this.command = type;
	}

	public final int getIsAdmin() {
		return isAdmin;
	}

	public final void setIsAdmin(int isAdmin) {
		this.isAdmin = isAdmin;
	}

}
