/**
 * 
 */
package org.logic2j.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.sql.DataSource;

import org.logic2j.util.DynIterable.DynBuilder;

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
	protected Iterable<Object[]> handle(ResultSet theResultSet)
			throws SQLException {
		/*List<Object[]> result = new ArrayList<Object[]>();
		Iterator<ResultSet> itRes = this.asIterable(theResultSet).iterator();
		while (itRes.hasNext()) {
			 ResultSet rs =  itRes.next();
			 result.add(toArray(rs, rs.getMetaData().getColumnCount()));
		}*/
		return new DynIterable<Object[], ResultSet>(this.objectBuilder(), this.asIterable(theResultSet));
	}
//*
	private DynBuilder<Object[], ResultSet> objectBuilder(){
		return new DynBuilder<Object[], ResultSet>() {
			@Override
			public Object[] build(ResultSet input) {
				try {
					return toArray(input, input.getMetaData()
							.getColumnCount());
				} catch (SQLException e) {
					return null;
				}
			}
		};
	}
	//*/
	
	private Iterable<ResultSet> asIterable(final ResultSet rs) {
		return new Iterable<ResultSet>() {
			@Override
			public Iterator<ResultSet> iterator() {
				return new Iterator<ResultSet>() {
					private boolean last = false;
					public boolean hasNext() {
						try {
							if(!last){
								if(rs.isLast()){
									rs.close();
									last = true;
								}
							}
							return !last;
						}
						catch (SQLException e) {
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
	//*/

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
