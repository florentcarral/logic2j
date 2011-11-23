/**
 * 
 */
package org.logic2j.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.sql.DataSource;

import org.apache.derby.impl.store.raw.log.LogAccessFile;
import org.logic2j.util.DynIterable.DynBuilder;

/**
 * @author vincent
 * 
 */
public class SqlRunnerAdHoc extends SqlRunner {

	public static int FETCH_SIZE = 1000;

	public SqlRunnerAdHoc(DataSource theDataSource) {
		super(theDataSource);
	}

	@Override
	public Iterable<Object[]> query(String theSelect, Object[] theParameters)
			throws SQLException {
		if (theParameters == null) {
			theParameters = EMPTY_PARAMS;
		}
		IterableResultSet itRes = null ;
		if (DEBUG_ENABLED) {
			logger.debug("SqlRunner SQL \"" + theSelect + '"');
			logger.debug(" parameters=" + Arrays.asList(theParameters));
		}
		try {
			Connection conn = this.dataSource.getConnection();
			stmt = this.prepareStatement(conn, theSelect);
			this.fillStatement(stmt, theParameters);
			itRes = new IterableResultSet(stmt);
		} catch (SQLException e) {
			if (DEBUG_ENABLED) {
				logger.debug("Caught exception \"" + e + "\", going to rethrow");
			}
			this.rethrow(e, theSelect, theParameters);
		} 
		return itRes;
	}

	@Override
	@Deprecated
	protected Iterable<Object[]> handle(ResultSet theResultSet)
			throws SQLException {
		/*
		 * List<Object[]> result = new ArrayList<Object[]>();
		 * Iterator<ResultSet> itRes = this.asIterable(theResultSet).iterator();
		 * while (itRes.hasNext()) { ResultSet rs = itRes.next();
		 * result.add(toArray(rs, rs.getMetaData().getColumnCount())); }
		 */
		return new IterableResultSet(stmt);
	}

	// *
	private DynBuilder<Object[], ResultSet> objectBuilder() {
		return new DynBuilder<Object[], ResultSet>() {
			@Override
			public Object[] build(ResultSet input) {
				try {
					return toArray(input, input.getMetaData().getColumnCount());
				} catch (SQLException e) {
					return null;
				}
			}
		};
	}

	/*/

	private Iterable<ResultSet> asIterable(final ResultSet rs,
			final PreparedStatement stm) {
		return new Iterable<ResultSet>() {
			@Override
			public Iterator<ResultSet> iterator() {
				return new Iterator<ResultSet>() {
					private boolean last = false;

					public boolean hasNext() {
						try {
							if (!last) {
								if (rs.isLast()) {
									rs.close();
									stm.close();
									last = true;
								}
							}
							return !last;
						} catch (SQLException e) {
							throw new RuntimeException(e);
						}
					}

					public ResultSet next() {
						try {
							rs.next();
							return rs;
						} catch (SQLException e) {
							throw new NoSuchElementException();
						}
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	// */

	@Override
	protected PreparedStatement prepareStatement(Connection conn, String sql)
			throws SQLException {
		conn.setAutoCommit(true);
		PreparedStatement stm = conn.prepareStatement(sql,
				java.sql.ResultSet.TYPE_SCROLL_SENSITIVE,
				java.sql.ResultSet.CONCUR_READ_ONLY);
		stm.setFetchSize(SqlRunnerAdHoc.FETCH_SIZE);
		return stm;
	}

}
