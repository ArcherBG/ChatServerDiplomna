package corp.server.dao;

import java.awt.Cursor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import corp.server.interfaces.ClientDAO;
import corp.server.model.ClientForm;

public class ClientDAOImpl implements ClientDAO {

	private static final String CONNECTION_STRING = "jdbc:mysql://localhost/chat?"
			+ "user=root&password=root&useSSL=false";
	private static final String TABLE_NAME = "clients";
	private static final String COLUMN_ID = "id";
	private static final String COLUMN_EMAIL = "email";
	private static final String COLUMN_PASSWORD = "password";
	private static final String COLUMN_ACCESS_TOKEN = "access_token";
	private static final String COLUMN_IS_ADMIN = "is_admin";

	public ClientDAOImpl() {

	}

	@Override
	public long insert(ClientForm form) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		long rowId = -1;

		try {
			connection = DriverManager.getConnection(CONNECTION_STRING);

			preparedStatement = connection.prepareStatement(
					"INSERT " + TABLE_NAME + " SET " + COLUMN_EMAIL + " = ? , "  + COLUMN_PASSWORD + " = ? , "
							+ COLUMN_ACCESS_TOKEN + " = ?, " + COLUMN_IS_ADMIN + " = ? ");

			preparedStatement.setString(1, form.getEmail());
			preparedStatement.setString(2, form.getPassword());
			preparedStatement.setString(3, form.getAccessToken());
			preparedStatement.setInt(4, form.getIsAdmin());
			preparedStatement.executeQuery();
			connection.commit();

			ClientForm savedForm = findClient(form.getEmail());
			rowId = savedForm.getId();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();

				try {
					connection.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		}

		return rowId;
	}

	@Override
	public long update(ClientForm form) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		long rowId = -1;

		try {
			connection = DriverManager.getConnection(CONNECTION_STRING);

			preparedStatement = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET " + COLUMN_EMAIL + " = ? , "
					+ COLUMN_PASSWORD + " = ? , " + COLUMN_ACCESS_TOKEN + " = ? , " + COLUMN_IS_ADMIN + " = ? "
					+ " WHERE " + COLUMN_ID + " = ? ");

			preparedStatement.setString(1, form.getEmail());
			preparedStatement.setString(2, form.getPassword());
			preparedStatement.setString(3, form.getAccessToken());
			preparedStatement.setInt(4, form.getIsAdmin());
			preparedStatement.setLong(5, form.getId());

			preparedStatement.executeUpdate();

			ClientForm savedForm = findClient(form.getEmail());
			rowId = savedForm.getId();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();

				try {
					connection.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		}

		return rowId;

	}

	@Override
	public void delete(ClientForm form) {
		// TODO implement

	}

	@Override
	public List<ClientForm> query(ClientForm form) {
		// TODO implement
		return null;
	}

	@Override
	public ClientForm findClient(long id) {
		
		ClientForm returnForm = null;
		Connection connection = null;

		try {
			connection = DriverManager.getConnection(CONNECTION_STRING);
			Statement statement = connection.createStatement();
			ClientForm tempForm = new ClientForm();

			if (id > 0) {
				ResultSet resultSet = statement
						.executeQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = " + id);
				if (resultSet.next()) {
					tempForm.setId(resultSet.getLong(COLUMN_ID));
					tempForm.setEmail(resultSet.getString(COLUMN_EMAIL));
					tempForm.setPassword(resultSet.getString(COLUMN_PASSWORD));
					tempForm.setAccessToken(resultSet.getString(COLUMN_ACCESS_TOKEN));
					tempForm.setIsAdmin(resultSet.getInt(COLUMN_IS_ADMIN));

					returnForm = tempForm;
				}
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

		return returnForm;
	}

	@Override
	public ClientForm findClient(String email) {
		ClientForm returnForm = null;
		Connection connection = null;

		try {
			connection = DriverManager.getConnection(CONNECTION_STRING);
			Statement statement = connection.createStatement();
			ClientForm tempForm = new ClientForm();

			if (email != null) {
				ResultSet resultSet = statement
						.executeQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_EMAIL + " = '" + email + "'");
				if (resultSet.next()) {
					tempForm.setId(resultSet.getLong(COLUMN_ID));
					tempForm.setEmail(resultSet.getString(COLUMN_EMAIL));
					tempForm.setPassword(resultSet.getString(COLUMN_PASSWORD));
					tempForm.setAccessToken(resultSet.getString(COLUMN_ACCESS_TOKEN));
					tempForm.setIsAdmin(resultSet.getInt(COLUMN_IS_ADMIN));

					returnForm = tempForm;
				}
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

		return returnForm;
	}

}
