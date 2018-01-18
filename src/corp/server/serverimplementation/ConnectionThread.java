package corp.server.serverimplementation;

import java.awt.event.WindowStateListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import corp.server.model.ClientForm;
import corp.server.model.FindForm;
import corp.server.model.MessageForm;

public final class ConnectionThread extends Thread {

	private static final String COMMAND = "command";
	private static final String COMMAND_PREFIX = ":";
	private static final String CLIENT_IDENTIFICATION_COMMAND = ":meet";
	private static final String GET_ALL_CLIENTS_COMMAND = ":who";
	private static final String COMMAND_QUIT = "quit";
	private static final String SYSTEM_MESSAGE_COMMAND = "system";

	private static final String COMMAND_FIND = "findclient";
	private static final String COMMAND_FIND_RESPONSE = "findclientresponse";
	private static final String COMMAND_LOGIN = "login";

	private static final String WHISPER_COMMAND = "whisper";
	private static final String REGISTER_COMMAND = "register";
	private static final String INFO_COMMAND = "ack";
	private static final String PING_COMMAND = "ping";
	private static final String SUCCESS = "success";
	private static final String DEFAULT_THREAD_NAME = "Guest";
	private static final int INCORRECT_ATTEMPTS_ÀLLOWED = 10;

	private ClientConnectionCallback listener;
	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;
	private LinkedBlockingQueue<String> messagesToSend = new LinkedBlockingQueue<>();

	private Long userId = 0L;

	private boolean isConnected;
	private AtomicBoolean hasClientValidated;
	private boolean isLoginResponseReady;
	private boolean isRegisterResponseReady;

	/**
	 * Set userId after client has logged in or registered and is now thrusted
	 * connection
	 */
	public synchronized void setUserId(Long userId) {
		this.userId = userId;
	}

	
	public synchronized long getUserId(){
		return this.userId;
	}
	
	public void setValidated(boolean isValidated) {
		this.hasClientValidated.set(isValidated);
	}

	/** Returns weather the client has connected successfully */
	public boolean isConnected() {
		return isConnected;
	}

	/**
	 * Set this to true then the main method wants to notify thread about login
	 * result
	 */
	public synchronized void setLoginResponseReady(boolean isLoginResponseReady) {
		this.isLoginResponseReady = isLoginResponseReady;
	}

	public synchronized void setRegisterResponseReady(boolean isRegisterResponseReady) {
		this.isRegisterResponseReady = isRegisterResponseReady;
	}

	public ConnectionThread(Socket clientSocket, ClientConnectionCallback listener) {
		// Change the build in name of thread to default name.
		this.setName(DEFAULT_THREAD_NAME);
		this.clientSocket = clientSocket;
		this.listener = listener;
		this.isConnected = false;
		this.hasClientValidated = new AtomicBoolean(false);
		isLoginResponseReady = false;
		isRegisterResponseReady = false;

	}

	public void run() {
		printMessageOnConsole(
				"A new client with thread id: " + Thread.currentThread().getId() + " has connected to the Server");

		try {
			// Open send and receive streams to client.
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			isConnected = true;

			runOutInWorkerThread();

			// Wait client to login or register before he can do anything else
			waitForClientToLoginOrRegister();

			String msgFromClient;
			try {
				// gson = new Gson();
				while (isConnected) {
					if ((msgFromClient = in.readLine()) != null) {
						printMessageOnConsole("\t Read msg: " + msgFromClient);

						// MessageForm messageForm =
						// gson.fromJson(msgFromClient, MessageForm.class);
						// Check what type of command the user send
						handleCommand(msgFromClient);
					}
				}
			} catch (IOException e) {
				// Suppress exception if trying to read from closed stream
			}
		} catch (IOException | InterruptedException e) {
			printMessageOnConsole("Error opening send and receive streams to client or Inerrupeted exception!");
			e.printStackTrace();
		} finally {
			//printMessageOnConsole(this.getId() + " thread is in finally");

			try {
				in.close();
			} catch (Exception e) {
				printMessageOnConsole("Error closing input stream to client.");
				e.printStackTrace();
			}

			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// If the client has been validated this thread is given given
			// userId.
			// If not use the build in thread id to remove the connection
			if (userId <= 0) {
				listener.disconnectUnvalidatedClient(this.getId());
			} else {
				listener.disconnectValidatedClient(userId);
			}
		}
	}

	private void runOutInWorkerThread() {
		Thread outThread = new Thread(new Runnable() {
			@Override
			public void run() {

				// Send message to server
				while (isConnected) {
					try {
						String msg = messagesToSend.take();
						//printMessageOnConsole("\t\t Sending to client msg: " + msg);
						out.println(msg);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (out != null) {
					out.close();
				}

			}
		});
		outThread.start();
	}

	/**
	 * This method is executing in a worker thread
	 */
	private void handleCommand(final String messageFromClient) {

		// String commandValue =parseCommandValue (messageFromClient);

		// Check what type the command is
		if (messageFromClient.contains(WHISPER_COMMAND)) {

			handleWhisperCommand(messageFromClient);
		} else if (messageFromClient.contains(COMMAND_FIND)) {

			handleFindCommand(messageFromClient);
		} else if (messageFromClient.contains(SYSTEM_MESSAGE_COMMAND)) {

			printMessageOnConsole("System message from client : " + messageFromClient);
		} else if (messageFromClient.contains(COMMAND_QUIT)) {

			handleQuitCommand();
		} else {

			sendErrorMessageToMyself("Invalid command");
		}
	}

	private String parseCommandValue(String messageFromClient) {
		// TODO make parser
		return null;
	}

	private void handleFindCommand(String messageFromClient) {
		Gson json = new Gson();
		FindForm findForm = json.fromJson(messageFromClient, FindForm.class);

		// Validate required fields
		if ((findForm.getEmail() == null || findForm.getEmail().trim().length() == 0) && findForm.getClientId() <= 0) {
			sendErrorMessageToMyself("Invalid  parameters. You shoud provide client id or client email.");
			return;
		}

		listener.findClientInDb(findForm, this);
	}

	private void handleWhisperCommand(String messageFromClient) {

		Gson json = new Gson();
		MessageForm receivedForm = json.fromJson(messageFromClient, MessageForm.class);

		// Send message only if it is valid. If not inform the sender.
		if (!isInputValid(WHISPER_COMMAND, receivedForm)) {
			sendErrorMessageToMyself("Invalid  parameters");
			return;
		}

		if (listener.doesUserExists(receivedForm.getReceiverId())) {
			// The server sets the client id of the sender
			// explicitly, not the client.
			receivedForm.setSenderId(this.userId);

			// Send info msg to server to know that his msg was received.
			MessageForm infoForm = new MessageForm();
			infoForm.setCommand(INFO_COMMAND);
			infoForm.setMessageId(receivedForm.getMessageId());
			infoForm.setMessage(SUCCESS);
			sendMessageToMyself(infoForm);

			listener.sendMessageToReceiver(receivedForm);
		} else {
			sendErrorMessageToMyself("Receiver does not exist");
		}
	}

	private void waitForClientToLoginOrRegister() throws IOException, InterruptedException {

		// TODO Start timeout timer and count the incorrect attempts and if time
		// runs out close the connection
		int incorrectAttempts = 0;
		int disconnectAfter = 10 * 1000; // 10 seconds
		long timeEnd = Calendar.getInstance().getTimeInMillis() + disconnectAfter;

		String lineRead;

		try {
			// while (timeEnd > Calendar.getInstance().getTimeInMillis()) {
			Gson json = new Gson();
			while (!hasClientValidated.get() && incorrectAttempts < INCORRECT_ATTEMPTS_ÀLLOWED) {

				if ((lineRead = in.readLine()) != null) {

					printMessageOnConsole("Client  thread id: " + this.getId() + " login msg: " + lineRead);

					// Trim the string in order to remove tailing characters
					// that break the parsing
					ClientForm clientForm = json.fromJson(lineRead.trim(), ClientForm.class);

					// Check if there is such user.
					if (clientForm.getCommand().equals(COMMAND_LOGIN)) {
						synchronized (this) {

							listener.loginClient(clientForm, this);

							// wait for response from the main thread
							while (!isLoginResponseReady) {
								this.wait();
							}
							isLoginResponseReady = false; // reset the flag
						}
					} else if (clientForm.getCommand().equals(REGISTER_COMMAND)) {
						synchronized (this) {
							listener.registerClient(clientForm, this);

							// wait for response from the main thread
							while (!isRegisterResponseReady) {
								this.wait();
							}
							// reset the flag
							isRegisterResponseReady = false;
						}

					} else {
						// Handle invalid command
						incorrectAttempts++;

						MessageForm errorForm = new MessageForm();
						errorForm.setCommand(SYSTEM_MESSAGE_COMMAND);
						errorForm.setMessage("Invalid command. User must first login or register");
						sendMessageToMyself(errorForm);
					}
					
				} else {
					printMessageOnConsole("Input is null");
					if (out == null){
						printMessageOnConsole("Output is null");
					}
					
					// closeConnection();
					// listener.disconnectUnvalidatedClient(this.getId());
					break;
				}
			} // end while

			// }

		} catch (JsonSyntaxException jsonException) {
			// Handle invalid command
			incorrectAttempts++;
			sendErrorMessageToMyself("Error. Expecting json, received string.");
		}

		if (!hasClientValidated.get()) {
			closeConnection();
			listener.disconnectUnvalidatedClient(this.getId());
		}

	}

	/**
	 * Closes socket and tells the server to remove him from collection
	 * 
	 */
	private void handleQuitCommand() {
		printMessageOnConsole("Thread id  " + this.getId() + " handle quit command");

		closeConnection();
	}

	private boolean isInputValid(String messageType, MessageForm form) {

		if (messageType.equals(WHISPER_COMMAND)) {
			// Validate we have receiver to send to
			if (form.receiverId <= 0) {
				sendErrorMessageToMyself("Reveicer ID id null or 0");
				return false;
			}
			// Validate that the form has msg to send
			if (form.message == null || form.message.trim().length() == 0) {
				sendErrorMessageToMyself("Message is null or empty");
				return false;
			}
			// Validate that the message has msg id
			if (form.messageId <= 0) {
				sendErrorMessageToMyself("Message ID is null or empty");
				return false;
			}

			return true;
		}

		return false;
	}

	public void printMessageOnConsole(String message) {
		System.out.println(message);
	}

	/**
	 * @param errorText
	 *            - Add only the text you want to send and this method handles
	 *            the rest.
	 */
	public void sendErrorMessageToMyself(String errorText) {

		Gson json = new Gson();
		MessageForm errorForm = new MessageForm();
		errorForm.setCommand(SYSTEM_MESSAGE_COMMAND);
		errorForm.setMessage(errorText);
		String response = json.toJson(errorForm);
		messagesToSend.add(response);
	}

	/**
	 *
	 * @param form
	 *            - the form to be populated with values is, responsibility of
	 *            the person who called this method.
	 */
	public void sendMessageToMyself(MessageForm form) {

		Gson json = new Gson();
		String response = json.toJson(form);
		//printMessageOnConsole("sendMessageToMyself: " + response);
		messagesToSend.add(response);
	}

	/**
	 *
	 * @param form
	 *            - the form to be populated with values is, responsibility of
	 *            the person who called this method.
	 */
	public void sendMessageToMyself(ClientForm form) {

		Gson json = new Gson();
		String response = json.toJson(form);
		messagesToSend.add(response);
	}

	/**
	 *
	 * @param form
	 *            - the form to be populated with values is, responsibility of
	 *            the person who called this method.
	 */
	public void sendMessageToMyself(FindForm form) {

		Gson json = new Gson();
		String response = json.toJson(form);
		messagesToSend.add(response);
	}

	public void closeConnection() {
		isConnected = false;
	}

	/*
	 * Callback interface
	 */
	public interface ClientConnectionCallback {

		void registerClient(ClientForm client, ConnectionThread connectionThread);

		void loginClient(ClientForm client, ConnectionThread connectionThread);

		void findClientInDb(FindForm form, ConnectionThread connectionThread);

		boolean doesUserExists(long clientId);

		void disconnectValidatedClient(long userId);

		void disconnectUnvalidatedClient(long threadId);

		/**
		 * Pass message to server who is responsible for sending it to the
		 * receiving client
		 */
		void sendMessageToReceiver(MessageForm form);

	}

}
