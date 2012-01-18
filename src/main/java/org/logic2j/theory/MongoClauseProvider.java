/**
 * 
 */
package org.logic2j.theory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.sql.DataSource;

import org.bson.types.ObjectId;
import org.logic2j.ClauseProvider;
import org.logic2j.PrologImplementor;
import org.logic2j.TermFactory;
import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.io.format.FormatUtils;
import org.logic2j.io.parse.DefaultTermFactory;
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
import org.logic2j.util.SqlRunnerAdHoc;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author Vincent Berthet
 * 
 */
public class MongoClauseProvider implements ClauseProvider {
    private PrologImplementor theProlog;
    private DB db;
    private final String prefix;
    private AllStringsAsAtoms termFactory;

    /**
     * @return the theProlog
     */
    public PrologImplementor getTheProlog() {
        return theProlog;
    }

    public MongoClauseProvider(PrologImplementor theProlog, DB dbSource) {
        this(theProlog, dbSource, "");
    }

    public MongoClauseProvider(PrologImplementor theProlog, DB dbSource, String prefix) {
        super();
        this.theProlog = theProlog;
        this.db = dbSource;
        this.prefix = prefix;
        this.termFactory = new MongoClauseProvider.AllStringsAsAtoms(theProlog);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.logic2j.ClauseProvider#listMatchingClauses(org.logic2j.model.symbol
     * .Struct, org.logic2j.model.var.Bindings)
     */
    @Override
    public Iterable<Clause> listMatchingClauses(Struct theGoal, Bindings theGoalBindings) {
        BasicDBObject query = new BasicDBObject();
        BasicDBObject resField = new BasicDBObject();

        for (int i = 0; i < theGoal.getArity(); i += 2) {
            Term field = theGoal.getArg(i);
            Term value = theGoal.getArg(i + 1);

            if (!field.isAtom()) {
                throw new RuntimeException("Mongo predicate's syntax is not respected");
            }
            resField.put(((Struct) field).getName(), 1);
            if (value instanceof Var && theGoalBindings != null) {
                value = (new TermApi()).substitute(value, theGoalBindings, null);
            }
            if (value.isAtom() || value instanceof TNumber) {
                query.put(
                        FormatUtils.removeApices(field.toString()),
                        (value instanceof TNumber) ? ((TNumber) value).longValue() : FormatUtils.removeApices(value
                                .toString()));
            } else if (value.isList()) {
                // query.put(FormatUtils.removeApices(field.toString()),
                // new BasicDBObject("$in",
                // this.listStringFromTerm((Struct)value)));
                throw new UnsupportedOperationException("");
            }
        }

        return this.queryForClauses(query, resField, theGoal.getName());
    }

    private List<String> listStringFromTerm(Struct pList) {
        ArrayList<String> lst = new ArrayList<String>();
        Struct t = pList;
        while (!t.isEmptyList()) {
            lst.add(FormatUtils.removeApices(t.getLHS().toString()));
            t = (Struct) t.getRHS();
        }
        return lst;
    }

    private Clause clauseBuilder(DBObject row, String predicateName) {
        BasicDBObject rowObj = new BasicDBObject(row.toMap());
        Term[] args = new Term[(rowObj.size() - 1) * 2];
        Iterator<Entry<String, Object>> itRow = rowObj.entrySet().iterator();
        int i = 0;
        while (itRow.hasNext()) {
            Entry<String, Object> entry = itRow.next();
            if (!(entry.getValue() instanceof ObjectId)) {
                args[2 * i] = this.termFactory.create(entry.getKey(), FactoryMode.ANY_TERM);
                args[2 * i + 1] = this.termFactory.create(entry.getValue(), FactoryMode.ANY_TERM);
                i++;
            }
        }
        final Clause cl = new Clause(getTheProlog(), new Struct(predicateName, args));
        return cl;
    }

    protected Iterable<Clause> queryForClauses(BasicDBObject query, BasicDBObject selectField,
            final String predicateName) {
        System.out.println(query);
        DBCollection coll = db.getCollection(predicateName.substring(this.prefix.length()));
        Iterable<DBObject> rows = coll.find(query, selectField);
        return new DynIterable<Clause, DBObject>(new DynIterable.DynBuilder<Clause, DBObject>() {

            @Override
            public Clause build(DBObject input) {
                return clauseBuilder(input, predicateName);
            }
        }, rows);
    }

    /**
     * A {@link TermFactory} that will parse all strings as atoms (especially
     * those starting with uppercase that must not become bindings).
     */
    public static class AllStringsAsAtoms extends DefaultTermFactory {
        private static final TermApi TERM_API = new TermApi();

        public AllStringsAsAtoms(PrologImplementor theProlog) {
            super(theProlog);
        }

        @Override
        public Term parse(CharSequence theExpression) {
            return new Struct(theExpression.toString());
        }

        @Override
        public Term create(Object theObject, FactoryMode theMode) {
            // Ignore theMode argument, and use forcing of atom instead
            return TERM_API.valueOf(theObject, FactoryMode.ATOM);
        }
    }
}
