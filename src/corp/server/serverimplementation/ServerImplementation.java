package corp.server.serverimplementation;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;

import java.sql.*;

import corp.server.dao.ClientDAOImpl;
import corp.server.dao.MessageDAOImpl;
import corp.server.interfaces.ChatServer;
import corp.server.interfaces.ClientDAO;
import corp.server.interfaces.MessageDAO;
import corp.server.model.ClientForm;
import corp.server.model.FindForm;
import corp.server.model.MessageDTO;
import corp.server.model.MessageForm;
import corp.server.serverimplementation.ConnectionThread.ClientConnectionCallback;

public final class ServerImplementation implements ChatServer, ClientConnectionCallback {
	private static final int DEFAULT_PORT_NUMBER = 3456;
	private static final String SYSTEM_MESSAGE_COMMAND = "system";
	private static final String ADMIN_MODE = "adminmode";
	private static final String MESSAGE_INVALID_PARAMETERS = "invalid parameters";
	private static final String MESSAGE_LOGIN_ERROR = "login error";

	private static final String COMMAND_FIND = "findclient";
	private static final String COMMAND_FIND_RESPONSE = "findclientresponse";
	private static final String COMMAND_LOGIN_RESPONSE = "loginresponse";
	private static final String WHISPER_COMMAND = "whisper";

	private int portNum;
	private int maxConnections;

	private List<ConnectionThread> unvalidatedConnections;
	private ConcurrentMap<Long, ConnectionThread> validConnections;

	public ServerImplementation(int portNum, int maxConnections) {

		// Check if provided port num is between the min and max port range
		if (portNum > 0 && portNum < 65545) {
			this.portNum = portNum;
		} else {
			this.portNum = DEFAULT_PORT_NUMBER;
		}

		if (maxConnections > 0 && maxConnections < Integer.MAX_VALUE) {
			this.maxConnections = maxConnections;
		} else {
			printMessageOnConsole("Error. Max connections must be between 0 and ~ 2 000 000 000");
			System.exit(1);
		}

		// unvalidatedConnectionsCount = new AtomicInteger(0);
		unvalidatedConnections = Collections.synchronizedList(new ArrayList<>());
		validConnections = new ConcurrentHashMap<>();
	}

	@Override
	public void startServer() {
		ServerSocket serverSocket = null;
		boolean socketIsListening = true;

		try {
			serverSocket = new ServerSocket(portNum);
			printMessageOnConsole("Server is listening on port: " + portNum);

			while (socketIsListening) {

				// Because sum may overflow we cast to long
				if ((long) unvalidatedConnections.size() + validConnections.size() <= maxConnections) {
					ConnectionThread connection = new ConnectionThread(serverSocket.accept(), this);
					connection.start();

					// When first connect client has not validated itself
					unvalidatedConnections.add(connection);

					printMessageOnConsole("Number of unvalidated clients is: " + unvalidatedConnections.size()
							+ " and valid is: " + validConnections.size());
				} else {
					printMessageOnConsole("Max number of connections reached:  " + maxConnections);
					// Without accepting the client socket, his will time out and 
					// a connection will now be made.
				}
			}
		} catch (IOException e) {
			System.out.println("Error could not listen on port : " + portNum);
			e.printStackTrace();
		} finally {
			try {
				socketIsListening = false;
				serverSocket.close();
			} catch (IOException e) {
				System.out.println("Error closing serverSocket");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Generates access token for every client
	 */
	private String generateAccessToken() {
		int randomNum = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE - 1);
		return String.valueOf(randomNum);
	}

	private void printMessageOnConsole(String message) {
		System.out.println(message);
	}

	/** Callback methods are implemented below */

	@Override
	public synchronized void disconnectUnvalidatedClient(long threadId) {
		boolean isRemoved = false;

		for (int i = 0; i < unvalidatedConnections.size(); i++) {
			if (threadId == unvalidatedConnections.get(i).getId()) {
				unvalidatedConnections.remove(i);
				isRemoved = true;
				break;
			}
		}

		// Write log
		if (isRemoved) {
			printMessageOnConsole("Disconnected client with threadId: " + threadId);
		} else {
			printMessageOnConsole("Error disconnecting unvalidated client. Client was not found in list. ");
		}

		printMessageOnConsole("Size of unvaldiated list is: " + unvalidatedConnections.size());
	}

	@Override
	public synchronized void disconnectValidatedClient(long clientId) {

		ConnectionThread removedClient = validConnections.remove(clientId);

		if (removedClient != null) {
			printMessageOnConsole("Disconnected client with clientId: " + clientId);
			printMessageOnConsole("Valid clients count: " + validConnections.size());

		} else {
			printMessageOnConsole("Error disconnecting client. Client was not found in map. ");
			printMessageOnConsole("Valid clients count: " + validConnections.size());
		}
	}

	/** Client has passed validation and can now be moved to trusted clients */
	// TODO needs refractoring
	@Override
	public synchronized void registerClient(ClientForm form, ConnectionThread connectionThread) {

		// Validate required fields
		if (((form.email == null || form.email.trim().length() == 0)
				&& (form.password == null || form.password.trim().length() == 0))) {
			connectionThread.sendErrorMessageToMyself(MESSAGE_INVALID_PARAMETERS);
			return;
		}

		ClientDAO clientDao = new ClientDAOImpl();

		// Set access token on user to authenticate with it
		form.setAccessToken(generateAccessToken());

		// Save client in db
		long clientId = clientDao.insert(form);

		// Is save is successful
		if (clientId > 0) {

			// Find client in unvalidated list
			int index = findIndexInUnvalidatedConnections(connectionThread);
			if (index < 0) {
				printMessageOnConsole("Error finding client in unvalidated list");
				connectionThread.closeConnection();
				return;
			}

			// Move to valid connections
			ConnectionThread connection = unvalidatedConnections.remove(index);
			connection.setUserId(clientId);
			validConnections.put(clientId, connection);

			// Client is valid raise his flag in order to know and notify him.
			connectionThread.setValidated(true);
			connectionThread.setRegisterResponseReady(true);
			connectionThread.notify();

			// Send response to sender
			MessageForm messageForm = new MessageForm();
			messageForm.setCommand(SYSTEM_MESSAGE_COMMAND);
			connectionThread.sendMessageToMyself(messageForm);

			printMessageOnConsole("Client with id " + clientId + " has registered");
			printMessageOnConsole("Number of unvalidated clients is: " + unvalidatedConnections.size()
					+ " and valid is: " + validConnections.size());
		}

		else {
			connectionThread.sendErrorMessageToMyself(MESSAGE_LOGIN_ERROR);
		}
	}

	/** If client pass validation , will now be moved to trusted clients */
	@Override
	public synchronized void loginClient(ClientForm form, ConnectionThread connectionThread) {

		// Validate required fields
		if (!isLoginFormValid(form)) {
			// Client is valid raise his flag in order to know and notify
			// him.
			connectionThread.setValidated(false);
			connectionThread.setLoginResponseReady(true);
			connectionThread.notify();
			connectionThread.sendErrorMessageToMyself(MESSAGE_INVALID_PARAMETERS);
			return;
		}

		ClientDAO clientDao = new ClientDAOImpl();
		ClientForm clientFromDb = null;
		// Find client in db by what is provided
		if (form.getId() > 0) {
			clientFromDb = clientDao.findClient(form.getId());
		} else {
			clientFromDb = clientDao.findClient(form.getEmail());
		}

		// If client exists
		if (clientFromDb != null && clientFromDb.getId() > 0) {

			// printMessageOnConsole("Emails are: " + clientFromDb.getEmail()+ "
			// --- " + form.getEmail());

			// Validate credentials
			if ((clientFromDb.getEmail().equals(form.getEmail())
					&& clientFromDb.getPassword().equals(form.getPassword()))
					|| clientFromDb.getAccessToken().equals(form.getAccessToken())) {

				// Generate new access token for client and save it in db
				String accessToken = generateAccessToken();
				clientFromDb.setAccessToken(accessToken);
				long rowId = clientDao.update(clientFromDb);

				if (rowId <= 0) {
					printMessageOnConsole("Error saving client access token");
					// Client is valid raise his flag in order to know and
					// notify him.
					connectionThread.setValidated(false);
					connectionThread.setLoginResponseReady(true);
					connectionThread.notify();

					connectionThread.sendErrorMessageToMyself(MESSAGE_LOGIN_ERROR);
					return;
				}

				// Find client in unvalidated list
				int index = findIndexInUnvalidatedConnections(connectionThread);
				if (index < 0) {
					printMessageOnConsole("Error finding client in unvalidated list");
					connectionThread.closeConnection();
					return;
				}

				// Move client to valid connections
				long clientId = clientFromDb.getId();
				ConnectionThread connection = unvalidatedConnections.remove(index);
				connection.setUserId(clientId);
				validConnections.put(clientId, connection);

				// Client is valid raise his flag in order to know and notify
				// him.
				connectionThread.setValidated(true);
				connectionThread.setLoginResponseReady(true);
				connectionThread.notify();

				// Send response to sender
				MessageForm messageForm = new MessageForm();
				messageForm.setCommand(COMMAND_LOGIN_RESPONSE);
				messageForm.setIsAdmin(clientFromDb.getIsAdmin());
				messageForm.setAccessToken(accessToken);
				messageForm.setId(clientId);
				connectionThread.sendMessageToMyself(messageForm);

				// If there are unsent messages to client,
				// send them to him now
				sendAllUnsendMessagesToReceiver(connectionThread);

				printMessageOnConsole("Client with id " + clientId + " has loged in");
				printMessageOnConsole("Number of unvalidated clients is: " + unvalidatedConnections.size()
						+ " and valid is: " + validConnections.size());

			} else {

				// Client is valid raise his flag in order to know and notify
				// him.
				connectionThread.setValidated(false);
				connectionThread.setLoginResponseReady(true);
				connectionThread.notify();

				// Send response to sender
				MessageForm messageForm = new MessageForm();
				messageForm.setCommand(COMMAND_LOGIN_RESPONSE);
				messageForm.setMessage(MESSAGE_INVALID_PARAMETERS);
				messageForm.setAccessToken(null);
				messageForm.setId(0);
				connectionThread.sendMessageToMyself(messageForm);

				printMessageOnConsole("Client with thread id " + connectionThread.getId() + " has invalid credentials");
			}
		} else {
			printMessageOnConsole("Client with thread id " + connectionThread.getId() + " does not exist in db");

			// Client is valid raise his flag in order to know and notify
			// him.
			connectionThread.setValidated(false);
			connectionThread.setLoginResponseReady(true);
			connectionThread.notify();

			// Send response to sender
			MessageForm messageForm = new MessageForm();
			messageForm.setCommand(COMMAND_LOGIN_RESPONSE);
			messageForm.setMessage(MESSAGE_INVALID_PARAMETERS);
			messageForm.setAccessToken(null);
			messageForm.setId(0);
			connectionThread.sendMessageToMyself(messageForm);

			// connectionThread.sendErrorMessageToMyself(MESSAGE_LOGIN_ERROR);
		}
	}

	@Override
	public void findClientInDb(FindForm form, ConnectionThread connectionThread) {

		// Find client in db by email or id
		ClientDAO clientDao = new ClientDAOImpl();
		ClientForm clientFromDb = null;
		if (form.getEmail() != null && form.getEmail().trim().length() != 0) {
			clientFromDb = clientDao.findClient(form.getEmail());
		} else {
			clientFromDb = clientDao.findClient(form.getClientId());
		}

		// Response if client is found
		if (clientFromDb != null) {

			FindForm returnForm = new FindForm();
			returnForm.setCommand(COMMAND_FIND_RESPONSE);
			returnForm.setClientId(clientFromDb.getId());
			returnForm.setEmail(clientFromDb.getEmail());
			connectionThread.sendMessageToMyself(returnForm);
		} else {

			FindForm returnForm = new FindForm();
			returnForm.setCommand(COMMAND_FIND_RESPONSE);
			returnForm.setClientId(-1); // set id as invalid
			returnForm.setEmail(null);
			connectionThread.sendMessageToMyself(returnForm);
		}
	}

	@Override
	public boolean doesUserExists(long clientId) {

		ClientDAO clientDao = new ClientDAOImpl();
		ClientForm clientFromDb = clientDao.findClient(clientId);
		if (clientFromDb != null) {
			return true;
		}
		return false;
	}

	public boolean doesUserExists(String email) {

		ClientDAO clientDao = new ClientDAOImpl();
		ClientForm clientFromDb = clientDao.findClient(email);
		if (clientFromDb != null) {
			return true;
		}
		return false;
	}

	@Override
	public void sendMessageToReceiver(MessageForm form) {

		// If receiver is online send now the message else store in db and
		// wait to come online.
		ConnectionThread connectionThread = validConnections.get(form.getReceiverId());
		if (connectionThread != null) {

			sendMessageToOnlineReceiver(form);
		} else {

			// Store the message to send when he comes online
			storeUnsentMessageToReceiver(form);
		}
	}

	/** Helper methods */

	/**
	 * Store in db messages
	 */
	private void storeUnsentMessageToReceiver(MessageForm form) {
		MessageDAO messagesDao = new MessageDAOImpl();

		// Convert data to dto
		MessageDTO dto = new MessageDTO();
		dto.setSenderId(form.getSenderId());
		dto.setReceiverId(form.receiverId);
		dto.setMessage(form.getMessage());
		dto.setIsReceivedByClient(0); // 0 = false

		messagesDao.insert(dto);
	}

	private void sendAllUnsendMessagesToReceiver(final ConnectionThread client) {
		MessageDAO messageDao = new MessageDAOImpl();

		List<MessageDTO> list = messageDao.queryAllUnsendMessagesForReceiver(client.getUserId());

		for (MessageDTO dto : list) {

			// Convert data to MessageForm
			MessageForm messageForm = new MessageForm();
			messageForm.setMessageId(dto.getId());
			messageForm.setCommand(WHISPER_COMMAND);
			messageForm.setSenderId(dto.getSenderId());
			messageForm.setReceiverId(dto.getReceiverId());
			messageForm.setMessage(dto.getMessage());

			client.sendMessageToMyself(messageForm);
			
			
			// Update db that the msg was send to receiver
			dto.setIsReceivedByClient(1); // 1 = true
			messageDao.update(dto);
		}

	}

	private boolean isLoginFormValid(ClientForm form) {
		boolean isValid = false;

		if (!((form.email == null || form.email.trim().length() == 0)
				&& (form.password == null || form.password.trim().length() == 0))) {
			isValid = true;
		} else if (!(form.accessToken == null || form.accessToken.trim().length() == 0)) {
			isValid = true;
		}

		return isValid;
	}

	public void sendMessageToOnlineReceiver(MessageForm form) {
		ConnectionThread connection = validConnections.get(form.getReceiverId());
		connection.sendMessageToMyself(form);
	}

	/**
	 * Finds and return the id of element. It there is not such element it
	 * returns -1.
	 */
	private synchronized int findIndexInUnvalidatedConnections(ConnectionThread connectionThread) {
		for (int i = 0; i < unvalidatedConnections.size(); i++) {
			if (connectionThread.getId() == unvalidatedConnections.get(i).getId()) {
				return i;
			}
		}
		return -1;
	}

}
