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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sql.DataSource;

import org.logic2j.ClauseProvider;
import org.logic2j.PrologImplementor;
import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.library.impl.rdb.RDBBase;
import org.logic2j.model.Clause;
import org.logic2j.model.exception.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.TNumber;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.Bindings;
import org.logic2j.util.DynIterable;
import org.logic2j.util.SqlBuilder3;
import org.logic2j.util.SqlBuilder3.Table;
import org.logic2j.util.SqlRunnerAdHoc;

/**
 * List {@link Clause}s (facts, never rules) from relational database tables or
 * views accessed from the JDBC {@link DataSource} API. When trying to solve the
 * goal "zipcode_city(94101, City)" which yields City='SAN FRANCISCO', this
 * class expects a database table or view such as
 * "PRED_ZIPCODE_CITY(INTEGER ARG_0, VARCHAR ARG_1)".
 */
public class RDBClauseProvider extends RDBBase implements ClauseProvider {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RDBClauseProvider.class);

    /**
     * Meta-data of the tables linked to this provider. Note that the first cell
     * in a table meta-data array is the original name of the table (this is
     * important because table names are always converted to lower-case to match
     * prolog syntax, so we have to retreive the real name of the table to query
     * the database).
     */
    private final HashMap<String, String[]> tablesMetaData = new HashMap<String, String[]>();

    /**
     * Prefix of the predicate to query the RDB.
     */
    private final String prefix;

    public RDBClauseProvider(PrologImplementor theProlog, DataSource theDataSource) {
        this(theProlog, theDataSource, "");
    }

    public RDBClauseProvider(PrologImplementor theProlog, DataSource theDataSource, String prefix) {
        super(theProlog, theDataSource);
        this.prefix = prefix;
    }

    /**
     * Register a table meta-data.
     * 
     * @param tableIdentifier
     *            Table identifier (must be in lower-case).
     * @param originalNames
     *            Original names of the table and its columns. Note that the
     *            original table name must be in the fist cell.
     */
    public void registerTableMetaData(String tableIdentifier, String[] originalNames) {
        tablesMetaData.put(tableIdentifier, originalNames);
    }

    @Override
    public Iterable<Clause> listMatchingClauses(Struct theGoal, Bindings theGoalBindings) {
        String predicateName = theGoal.getName();
        SqlBuilder3 builder = new SqlBuilder3();
        builder.setInstruction(SqlBuilder3.SELECT);
        String tableIdentifier = theGoal.getName().substring(prefix.length());
        String[] originalNames = tablesMetaData.get(tableIdentifier);
        // The original table name is sored in the first cell of the array.
        Table table = builder.table(originalNames[0]);
        for (int i = 0; i < theGoal.getArity(); i++) {
            builder.addProjection(builder.column(table, originalNames[i + 1]));
        }
        for (int i = 0; i < theGoal.getArity(); i++) {
            Term t = theGoal.getArg(i);
            if (t instanceof Var && theGoalBindings != null) {
                t = (new TermApi()).substitute(theGoal.getArg(i), theGoalBindings, null);
            }
            if (t instanceof TNumber) {
                builder.addConjunction(builder.criterion(builder.column(table, originalNames[i + 1]),
                        SqlBuilder3.OPERATOR_EQ_OR_IN, t.toString()));
            } else if (t instanceof Struct && (t.isAtom() || t.isList())) {
                if (t.isAtom()) {
                    builder.addConjunction(builder.criterion(builder.column(table, originalNames[i + 1]),
                            SqlBuilder3.OPERATOR_EQ_OR_IN, ((Struct) t).getName()));
                } else if (t.isList()) {
                    addConjunctionList(builder, table, tableIdentifier, i + 1,
                            ((Struct) t).javaListFromPList(new ArrayList<Term>(), Term.class));
                }
            }
            // Here we check if there is any bindings (theGoalBindings) that we
            // can unify with the Term theGoal.getArg(i) which is a variable.
        }
        Iterable<Clause> clauses = queryForClauses(builder, predicateName);
        return clauses;
    }

    protected Iterable<Clause> queryForClauses(SqlBuilder3 builder, final String predicateName) {
        Iterable<Object[]> rows;
        try {
            builder.generateSelect();
            rows = new SqlRunnerAdHoc(getDataSource()).query(builder.getSql(), builder.getParameters());
            return new DynIterable<Clause, Object[]>(new DynIterable.DynBuilder<Clause, Object[]>() {
                @Override
                public Clause build(Object[] input) {
                    return clauseBuilder(input, predicateName);
                }
            }, rows);
        } catch (SQLException e) {
            throw new InvalidTermException("Exception not handled: " + e, e);
        }
    }

    protected void addConjunctionList(SqlBuilder3 builder, Table table, String tableIdentifier, int columnNumber,
            ArrayList<Term> structList) {
        Object[] listValues = new Object[structList.size()];
        for (int i = 0; i < structList.size(); i++) {
            Term term = structList.get(i);
            listValues[i] = term instanceof Struct ? ((Struct) term).getName() : term.toString();
        }
        builder.addConjunction(builder.criterion(
                builder.column(table, tablesMetaData.get(tableIdentifier)[columnNumber]), listValues));
    }

    private Clause clauseBuilder(Object[] row, String predicateName) {
        Term[] args = new Term[row.length];
        for (int i = 0; i < row.length; i++) {
            Object object = row[i];
            args[i] = getTermFactory().create(object, FactoryMode.ANY_TERM);
        }
        final Clause cl = new Clause(getProlog(), new Struct(predicateName, args));
        return cl;
    }

}
