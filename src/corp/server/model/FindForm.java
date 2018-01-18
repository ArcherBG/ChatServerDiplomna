package corp.server.model;

public class FindForm {

    // Meta data
    public long formId;
    public String command;

    // Payload
    public long clientId;
    public String email;
    public String message;


    public long getFormId() {
        return formId;
    }

    public void setFormId(long formId) {
        this.formId = formId;
    }

    public String getCommand() {
        return command;
    }


    public void setCommand(String command) {
        this.command = command;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
