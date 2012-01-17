/**
 * 
 */
package org.logic2j.theory;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.sql.DataSource;

import org.logic2j.ClauseProvider;
import org.logic2j.PrologImplementor;
import org.logic2j.TermFactory;
import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.io.parse.DefaultTermFactory;
import org.logic2j.model.Clause;
import org.logic2j.model.exception.InvalidTermException;
import org.logic2j.model.symbol.Struct;
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

    public MongoClauseProvider(PrologImplementor theProlog,DB dbSource){
        this(theProlog,dbSource,"");
    }
    
    public MongoClauseProvider(PrologImplementor theProlog,DB dbSource,String prefix) {
        super();
        this.theProlog = theProlog;
        this.db = dbSource;
        this.prefix = prefix;
        this.termFactory = new MongoClauseProvider.AllStringsAsAtoms(theProlog);
      }
    
    /* (non-Javadoc)
     * @see org.logic2j.ClauseProvider#listMatchingClauses(org.logic2j.model.symbol.Struct, org.logic2j.model.var.Bindings)
     */
    @Override
    public Iterable<Clause> listMatchingClauses(Struct theGoal, Bindings theGoalBindings) {        
        BasicDBObject query = new BasicDBObject();
        BasicDBObject resField = new BasicDBObject();
        
        for (int i = 0; i<theGoal.getArity(); i+=2){
            Term fName = theGoal.getArg(i);
            Term t = theGoal.getArg(i+1);
            
            if (t instanceof Var && theGoalBindings!=null) {
                t = (new TermApi()).substitute(theGoal.getArg(i), theGoalBindings, null);
            }
            if(!fName.isAtom()){
                throw new RuntimeException("Mongo predicate's syntax is not respected");
            }
            if(t.isAtom()){
                query.put(((Struct)fName).getName(), ((Struct)t).getName());
            }
            else{
                if(t.isList()){
                    throw new UnsupportedOperationException("");
                }
                else{
                    resField.put(((Struct)fName).getName(), 1);
                }
            }
        }
        
        return this.queryForClauses(query, resField, theGoal.getName());
    }

    private Clause clauseBuilder(DBObject row, String predicateName){
        BasicDBObject rowObj = new BasicDBObject(row.toMap());
        Term[] args = new Term[rowObj.size()*2];
        Iterator<Entry<String, Object>> itRow = rowObj.entrySet().iterator();
        int i = 0;
        while(itRow.hasNext()){
            Entry<String, Object> entry = itRow.next();
            args[i] = this.termFactory.create(entry.getKey(), FactoryMode.ANY_TERM);
            args[i+1] = this.termFactory.create(entry.getValue(), FactoryMode.ANY_TERM);
            i*=2;
        }
        final Clause cl = new Clause(getTheProlog(), new Struct(predicateName, args));
        return cl;
    }
    
    protected Iterable<Clause> queryForClauses(BasicDBObject query,BasicDBObject selectField, final String predicateName) {
        DBCollection coll = db.getCollection(predicateName.substring(this.prefix.length()));
        Iterable<DBObject> rows = coll.find(query, selectField);
        System.out.println(coll.count(query));
        return new DynIterable<Clause, DBObject>(new DynIterable.DynBuilder<Clause, DBObject>() {

            @Override
            public Clause build(DBObject input) {
                return clauseBuilder(input, predicateName);
            }
        }, rows);
    }
    
    /**
     * A {@link TermFactory} that will parse all strings as atoms 
     * (especially those starting with uppercase that must not become bindings).
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
