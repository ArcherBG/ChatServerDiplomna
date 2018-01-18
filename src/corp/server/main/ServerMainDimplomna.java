package corp.server.main;

import corp.server.serverimplementation.ServerImplementation;

public class ServerMainDimplomna {

	private static String portNumStr;
	private static String maxConectionsStr;

	private static int portNum;
	private static int maxConnections;

	public static void main(String[] args) {
		getArguments(args);

		// Trim parameters.
		if (portNumStr != null && portNumStr.length() != 0 && !portNumStr.equals("")) {
			portNum = Integer.parseInt(portNumStr.trim());
		}
		if (maxConectionsStr != null && maxConectionsStr.length() != 0 && !maxConectionsStr.equals("")) {
			maxConnections = Integer.parseInt(maxConectionsStr.trim());
		}

		ServerImplementation chatServer = new ServerImplementation(portNum, maxConnections);
		chatServer.startServer();

	}

	private static void getArguments(String[] args) {
		final String portNumPrefix = "-p";
		final String maxConnectionsPrefix = "-n";

		boolean portNumArgFound = false;
		boolean maxConnectionsArgFound = false;

		for (int i = 0; i < args.length; i++) {

			// Check if current arg is port argument.
			if (args[i].startsWith(portNumPrefix)) {

				// Check if the argument does not contain twice.
				if (!portNumArgFound) {

					portNumStr = args[i].substring(portNumPrefix.length());
					portNumArgFound = true;
					continue;
				} else {
					System.out.println("Error port argument is set twice");
					System.exit(1);
				}

			}

			// Check if current arg is maxConnections argument.
			if (args[i].startsWith(maxConnectionsPrefix)) {

				// Check if the argument does not contain twice.
				if (!maxConnectionsArgFound) {
					
					maxConectionsStr = args[i].substring(maxConnectionsPrefix.length());
					maxConnectionsArgFound = true;
					continue;
				} else {
					System.out.println("Error max connection argument is set twice");
					System.exit(1);
				}
			}
		}
	}

}
