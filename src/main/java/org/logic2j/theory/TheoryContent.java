/*
 * logic2j - "Bring Logic to your Java" - Copyright (C) 2011 Laurent.Tettoni@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.logic2j.theory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.logic2j.model.Clause;
import org.logic2j.model.symbol.Struct;

/**
 * Description of the content of a theory: an structured number of {@link Clause}s.
 *
 */
public class TheoryContent {

  private Map<String, List<Clause>> content = new HashMap<String, List<Clause>>();

  /**
   * Create with empty content.
   */
  public TheoryContent() {
    super();
  }

  /**
   * Add one clause.
   * @param theClause
   */
  public void add(Clause theClause) {
    final String key = theClause.getPredicateKey();
    List<Clause> list = this.content.get(key);
    if (list == null) {
      list = new ArrayList<Clause>();
      this.content.put(key, list);
    }
    list.add(theClause);
  }

  /**
   * Add all clauses contained in theExtraContent.
   * @param theExtraContent
   */
  public void add(TheoryContent theExtraContent) {
    for (Map.Entry<String, List<Clause>> extraEntry : theExtraContent.content.entrySet()) {
      final String key = extraEntry.getKey();
      List<Clause> extraClauses = extraEntry.getValue();
      if (this.content.containsKey(key)) {
        this.content.get(key).addAll(extraClauses);
      } else {
        this.content.put(key, extraClauses);
      }
    }
  }

  /**
   * Retrieve clauses matching theGoalTerm.
   * @param theGoalTerm
   * @return An iterable for a foreach() loop.
   */
  public Iterable<Clause> find(Struct theGoalTerm) {
    final String key = theGoalTerm.getPredicateIndicator();
    final List<Clause> list = this.content.get(key);
    if (list == null) {
      // Predicate not registered in this theory content, return empty, it's not a failure condition
      return Collections.emptyList();
    }
    return list;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + '(' + this.content + ')';
  }
}
