package corp.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.omg.PortableInterceptor.SUCCESSFUL;

import corp.server.interfaces.MessageDAO;
import corp.server.model.ClientForm;
import corp.server.model.MessageDTO;

public class MessageDAOImpl implements MessageDAO {

	private static final String CONNECTION_STRING = "jdbc:mysql://localhost/chat?"
			+ "user=root&password=root&useSSL=false";

	private static final String TABLE_NAME = "messages";
	private static final String COLUMN_ID = "id";
	private static final String COLUMN_SENDER_ID = "sender_id";
	private static final String COLUMN_RECEIVER_ID = "receiver_id";
	private static final String COLUMN_MESSAGE = "message";
	private static final String COLUMN_IS_MESSAGE_RECEIVED = "is_message_received";
	private static final String COLUMN_TIMESTAMP = "timestamp";

	/**
	 * 
	 * @param dto
	 * @return - return 1 if insert was successful or -1 when it was not
	 */
	@Override
	public long insert(MessageDTO dto) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		long status = -1;

		try {
			connection = DriverManager.getConnection(CONNECTION_STRING);
			preparedStatement = connection
					.prepareStatement("INSERT " + TABLE_NAME + " SET " + COLUMN_SENDER_ID + " =?, " + COLUMN_RECEIVER_ID
							+ " =?, " + COLUMN_MESSAGE + " =?, " + COLUMN_IS_MESSAGE_RECEIVED + " =?");

			preparedStatement.setLong(1, dto.getSenderId());
			preparedStatement.setLong(2, dto.getReceiverId());
			preparedStatement.setString(3, dto.getMessage());
			preparedStatement.setInt(4, dto.getIsReceivedByClient());

			preparedStatement.executeUpdate();

			status = 1;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				preparedStatement.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

				try {
					connection.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		return status;
	}

	/**
	 * 
	 * @param dto
	 * @return - return 1 if update was successful or -1 when it was not
	 */
	@Override
	public long update(MessageDTO dto) {

		Connection connection = null;
		PreparedStatement preparedStatement = null;
		long status = 1;

		try {
			connection = DriverManager.getConnection(CONNECTION_STRING);

			preparedStatement = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET " + COLUMN_SENDER_ID
					+ " =?, " + COLUMN_RECEIVER_ID + " =?, " + COLUMN_MESSAGE + " =?, " + COLUMN_IS_MESSAGE_RECEIVED
					+ " =? " + " WHERE " + COLUMN_ID + " = ? ");

			preparedStatement.setLong(1, dto.getSenderId());
			preparedStatement.setLong(2, dto.getReceiverId());
			preparedStatement.setString(3, dto.getMessage());
			preparedStatement.setInt(4, dto.getIsReceivedByClient());
			preparedStatement.setLong(5, dto.getId());

			preparedStatement.executeUpdate();

			status = 1;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				preparedStatement.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

				try {
					connection.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		}

		return status;
	}

	@Override
	public void delete(MessageDTO dto) {
		// TODO Implement

	}

	@Override
	public List<MessageDTO> query(MessageDTO dto) {
		// Implement
		return null;
	}

	@Override
	public List<MessageDTO> queryAllUnsendMessagesForReceiver(long receiverId) {

		List<MessageDTO> list = new ArrayList<MessageDTO>();
		Connection connection = null;

		try {
			connection = DriverManager.getConnection(CONNECTION_STRING);
			Statement statement = connection.createStatement();

			ResultSet resultSet = statement
					.executeQuery("SELECT * FROM " + TABLE_NAME + " WHERE " +
			COLUMN_RECEIVER_ID + " = " + receiverId + " AND " + COLUMN_IS_MESSAGE_RECEIVED + " = 0");

			while (resultSet.next()) {

				MessageDTO tempDto = new MessageDTO();

				tempDto.setId(resultSet.getLong(COLUMN_ID));
				tempDto.setSenderId(resultSet.getLong(COLUMN_SENDER_ID));
				tempDto.setReceiverId(resultSet.getLong(COLUMN_RECEIVER_ID));
				tempDto.setMessage(resultSet.getString(COLUMN_MESSAGE));
				tempDto.setIsReceivedByClient(resultSet.getInt(COLUMN_IS_MESSAGE_RECEIVED));
				tempDto.setTimestamp(resultSet.getString(COLUMN_TIMESTAMP));
			
				list.add(tempDto);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return list;
	}

}
