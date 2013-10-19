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

import org.eclipse.dltk.core.index.sql.Container;
import org.eclipse.dltk.core.index.sql.IContainerDao;

/**
 * Container path data access object
 * 
 * @author michael
 * 
 */
public class H2ContainerDao implements IContainerDao {

	private static final String Q_INSERT = "INSERT INTO CONTAINERS(PATH) VALUES(?);"; //$NON-NLS-1$
	private static final String Q_SELECT_BY_PATH = "SELECT * FROM CONTAINERS WHERE PATH=?;"; //$NON-NLS-1$
	private static final String Q_SELECT_BY_ID = "SELECT * FROM CONTAINERS WHERE ID=?;"; //$NON-NLS-1$
	private static final String Q_DELETE_BY_PATH = "DELETE FROM CONTAINERS WHERE PATH=?;"; //$NON-NLS-1$
	private static final String Q_DELETE_BY_ID = "DELETE FROM CONTAINERS WHERE ID=?;"; //$NON-NLS-1$

	public Container insert(Connection connection, String path)
			throws SQLException {

		Container container = selectByPath(connection, path);
		if (container != null) {
			return container;
		}

		PreparedStatement statement = connection.prepareStatement(Q_INSERT,
				Statement.RETURN_GENERATED_KEYS);
		try {
			int param = 0;
			statement.setString(++param, path);
			statement.executeUpdate();

			ResultSet result = statement.getGeneratedKeys();
			try {
				result.next();
				container = new Container(result.getInt(1), path);
				H2Cache.addContainer(container);
			} finally {
				result.close();
			}
		} finally {
			statement.close();
		}
		return container;
	}

	public Container selectByPath(Connection connection, String path)
			throws SQLException {

		Container container = H2Cache.selectContainerByPath(path);
		if (container != null) {
			return container;
		}

		PreparedStatement statement = connection
				.prepareStatement(Q_SELECT_BY_PATH);
		try {
			int param = 0;
			statement.setString(++param, path);
			ResultSet result = statement.executeQuery();
			try {
				if (result.next()) {
					container = new Container(result.getInt(1),
							result.getString(2));

					H2Cache.addContainer(container);
				}
			} finally {
				result.close();
			}
		} finally {
			statement.close();
		}
		return container;
	}

	public Container selectById(Connection connection, int id)
			throws SQLException {

		Container container = H2Cache.selectContainerById(id);
		if (container != null) {
			return container;
		}

		PreparedStatement statement = connection
				.prepareStatement(Q_SELECT_BY_ID);
		try {
			int param = 0;
			statement.setInt(++param, id);
			ResultSet result = statement.executeQuery();
			try {
				if (result.next()) {
					container = new Container(result.getInt(1),
							result.getString(2));

					H2Cache.addContainer(container);
				}
			} finally {
				result.close();
			}
		} finally {
			statement.close();
		}
		return container;
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

		H2Cache.deleteContainerById(id);
	}

	public void deleteByPath(Connection connection, String path)
			throws SQLException {
		PreparedStatement statement = connection
				.prepareStatement(Q_DELETE_BY_PATH);
		try {
			int param = 0;
			statement.setString(++param, path);
			statement.executeUpdate();
		} finally {
			statement.close();
		}

		H2Cache.deleteContainerByPath(path);
	}
}
