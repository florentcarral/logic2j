/**
 * 
 */
package org.logic2j.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author vincent
 * 
 */
public class IterableResultSet implements Iterable<Object[]> {
	private PreparedStatement stmt;

	public IterableResultSet(PreparedStatement stmt) {
		super();
		this.stmt = stmt;
	}

	@Override
	public Iterator<Object[]> iterator() {
		try {
			return new ResultSetIterator();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		this.stmt.close();
	}

	private class ResultSetIterator implements Iterator<Object[]> {
		private ResultSet rs;
		private Object[] current;
		private boolean hasNext = true;

		public ResultSetIterator() throws SQLException {
			super();
			this.rs = stmt.executeQuery();
			boolean notEmpty = rs.next();
			if(notEmpty){
				current = this.buildRow(rs, rs.getMetaData().getColumnCount());
			}
			else{
				this.hasNext = false;
			}
		}

		/**
		 * @param theResultSet
		 * @param theCols
		 * @return One row as array
		 */
		protected Object[] buildRow(ResultSet theResultSet, int theCols)
				throws SQLException {
			final Object[] result = new Object[theCols];
			for (int i = 0; i < theCols; i++) {
				result[i] = theResultSet.getObject(i + 1);
			}
			return result;
		}

		@Override
		public boolean hasNext() {
			return this.hasNext;
		}

		@Override
		public Object[] next() {
			try {
				Object[] obj = this.current;
				this.hasNext = this.rs.next();
				if(this.hasNext)
					this.current = this.buildRow(this.rs, this.rs.getMetaData().getColumnCount());
				else
					rs.close();
				return obj;
			} catch (SQLException e) {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void finalize() throws Throwable {
			rs.close();
			super.finalize();
		}

	}
}
