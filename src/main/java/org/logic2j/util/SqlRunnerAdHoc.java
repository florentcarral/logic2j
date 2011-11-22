/**
 * 
 */
package org.logic2j.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

/**
 * @author vincent
 *
 */
public class SqlRunnerAdHoc extends SqlRunner {

	public static int FETCH_SIZE = 500;
	
	public SqlRunnerAdHoc(DataSource theDataSource) {
		super(theDataSource);
	}

	@Override
	protected List<Object[]> handle(ResultSet theResultSet) throws SQLException {
		// TODO Auto-generated method stub
		
		return super.handle(theResultSet);
	}

	@Override
	protected PreparedStatement prepareStatement(Connection conn, String sql)
			throws SQLException {
		PreparedStatement stm = conn.prepareStatement(sql, java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
		stm.setFetchSize(SqlRunnerAdHoc.FETCH_SIZE);
		return stm;
	}

	
	
}
