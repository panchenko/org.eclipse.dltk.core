/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.dltk.internal.core.index.sql.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.dltk.core.index.sql.File;
import org.eclipse.dltk.core.index.sql.IFileDao;

/**
 * Element file data access object
 * 
 * @author michael
 * 
 */
public class H2FileDao implements IFileDao {

	private static final String Q_INSERT = "INSERT INTO FILES(PATH,TIMESTAMP,CONTAINER_ID) VALUES(?,?,?);"; //$NON-NLS-1$
	private static final String Q_SELECT = "SELECT * FROM FILES WHERE PATH=? AND CONTAINER_ID=?;"; //$NON-NLS-1$
	private static final String Q_SELECT_BY_CONTAINER_ID = "SELECT * FROM FILES WHERE CONTAINER_ID=?;"; //$NON-NLS-1$
	private static final String Q_SELECT_BY_ID = "SELECT * FROM FILES WHERE ID=?;"; //$NON-NLS-1$
	private static final String Q_DELETE = "DELETE FROM FILES WHERE PATH=? AND CONTAINER_ID=?;"; //$NON-NLS-1$
	private static final String Q_DELETE_BY_ID = "DELETE FROM FILES WHERE ID=?;"; //$NON-NLS-1$

	public File insert(Connection connection, String path, long timestamp,
			int containerId) throws SQLException {

		PreparedStatement statement = connection.prepareStatement(Q_INSERT,
				Statement.RETURN_GENERATED_KEYS);
		try {
			int param = 0;
			statement.setString(++param, path);
			statement.setLong(++param, timestamp);
			statement.setInt(++param, containerId);
			statement.executeUpdate();

			ResultSet result = statement.getGeneratedKeys();
			try {
				result.next();
				File file = new File(result.getInt(1), path, timestamp,
						containerId);
				H2Cache.addFile(file);
				return file;
			} finally {
				result.close();
			}
		} finally {
			statement.close();
		}
	}

	public File select(Connection connection, String path, int containerId)
			throws SQLException {

		File file = H2Cache.selectFileByContainerIdAndPath(containerId, path);
		if (file == null) {
			PreparedStatement statement = connection.prepareStatement(Q_SELECT);
			try {
				int param = 0;
				statement.setString(++param, path);
				statement.setInt(++param, containerId);
				ResultSet result = statement.executeQuery();
				try {
					if (result.next()) {
						file = new File(result.getInt(1), result.getString(2),
								result.getLong(3), result.getInt(4));

						H2Cache.addFile(file);
					}
				} finally {
					result.close();
				}
			} finally {
				statement.close();
			}
		}
		return file;
	}

	public File[] selectByContainerId(Connection connection, int containerId)
			throws SQLException {

		Collection<File> files = H2Cache.selectFilesByContainerId(containerId);
		if (files == null) {
			files = new LinkedList<File>();

			PreparedStatement statement = connection
					.prepareStatement(Q_SELECT_BY_CONTAINER_ID);
			try {
				int param = 0;
				statement.setInt(++param, containerId);
				ResultSet result = statement.executeQuery();
				try {
					while (result.next()) {
						File file = new File(result.getInt(1),
								result.getString(2), result.getLong(3),
								result.getInt(4));

						files.add(file);
						H2Cache.addFile(file);
					}
				} finally {
					result.close();
				}
			} finally {
				statement.close();
			}
		}
		return (File[]) files.toArray(new File[files.size()]);
	}

	public File selectById(Connection connection, int id) throws SQLException {

		File file = H2Cache.selectFileById(id);
		if (file == null) {
			PreparedStatement statement = connection
					.prepareStatement(Q_SELECT_BY_ID);
			try {
				int param = 0;
				statement.setInt(++param, id);
				ResultSet result = statement.executeQuery();
				try {
					if (result.next()) {
						file = new File(result.getInt(1), result.getString(2),
								result.getLong(3), result.getInt(4));

						H2Cache.addFile(file);
					}
				} finally {
					result.close();
				}
			} finally {
				statement.close();
			}
		}
		return file;
	}

	public void delete(Connection connection, String path, int containerId)
			throws SQLException {

		PreparedStatement statement = connection.prepareStatement(Q_DELETE);
		try {
			int param = 0;
			statement.setString(++param, path);
			statement.setInt(++param, containerId);
			statement.executeUpdate();
		} finally {
			statement.close();
		}

		H2Cache.deleteFileByContainerIdAndPath(containerId, path);
	}

	public void deleteById(Connection connection, int id) throws SQLException {
		PreparedStatement statement = connection
				.prepareStatement(Q_DELETE_BY_ID);
		try {
			int param = 0;
			statement.setInt(++param, id);
			statement.executeUpdate();
		} finally {
			statement.close();
		}

		H2Cache.deleteFileById(id);
	}
}
