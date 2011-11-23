/**
 * 
 */
package org.logic2j.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * @author vincent
 *
 */
public class IterableResultSet implements Iterable<Object[]> {
	private PreparedStatement stmt;	
	private int nbIterator = 0;
	
	public IterableResultSet(PreparedStatement stmt) {
		super();
		this.stmt = stmt;
	}
	
	@Override
	public Iterator<Object[]> iterator() {
		nbIterator++;
		return new ResultSetIterator();
	}
	
	

	@Override
	protected void finalize() throws Throwable {
		this.stmt.close();
	}



	private class ResultSetIterator implements Iterator<Object[]>{
		private Object[][] buffer = new Object[SqlRunnerAdHoc.FETCH_SIZE][];
		private int currentIndex = 0;
		private int offset = 0;
		private int lastObject = 0;
		boolean isEnd = false;
		
		private void updateBuffer() throws SQLException{
			ResultSet rs = stmt.executeQuery();
			rs.absolute(offset+currentIndex+1);
			offset+=currentIndex;
			currentIndex = 0;
			int bufferPost = 0;
			while(!rs.isAfterLast()&&bufferPost<SqlRunnerAdHoc.FETCH_SIZE){
				this.buffer[bufferPost] = this.rowBuilder(rs, rs.getMetaData().getColumnCount());
				rs.next();
				bufferPost++;
			}
			if(rs.isAfterLast()){
				this.isEnd = true;
			}
			this.lastObject = bufferPost-1;
			rs.close();
		}

		private Object[] rowBuilder(ResultSet theResultSet, int theCols) throws SQLException{
			final Object[] result = new Object[theCols];
		    for (int i = 0; i < theCols; i++) {
		      result[i] = theResultSet.getObject(i + 1);
		    }
		    return result;
		}
		
		public ResultSetIterator() {
			super();
			try {
				this.updateBuffer();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		@Override
		public boolean hasNext() {
			return (this.currentIndex <= this.lastObject)||(!this.isEnd);
		}

		@Override
		public Object[] next() {
			Object[] data = this.buffer[this.currentIndex];
			this.currentIndex++;
			if(this.currentIndex > this.lastObject){
				try {
					this.updateBuffer();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return data;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
}
