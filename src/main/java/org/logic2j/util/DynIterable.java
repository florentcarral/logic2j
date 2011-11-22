/**
 * 
 */
package org.logic2j.util;

import java.util.Iterator;

/**
 * @author vincent
 * @param <T>
 *
 */
public class DynIterable<OutType,InType> implements Iterable<OutType> {
	private Iterable<InType> dataProvider;
	private DynBuilder<OutType, InType> builder;
	
	public DynIterable(DynBuilder<OutType, InType> builder, Iterable<InType> dataProvider) {
		super();
		this.dataProvider = dataProvider;
		this.builder = builder;
	}

	@Override
	public Iterator<OutType> iterator() {
		return new Iterator<OutType>() {
			public Iterator<InType> iterator = dataProvider.iterator();
			
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public OutType next() {
				InType obj = iterator.next();
				return builder.build(obj);
			}

			@Override
			public void remove() {
				iterator.remove();
			}
		};
	}

	public interface DynBuilder<OutType,InType>{
		public OutType build(InType input);
	}
}
