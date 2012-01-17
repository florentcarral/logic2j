package org.logic2j.theory.webService;

import java.util.List;
import java.util.Map;

import org.logic2j.ClauseProvider;
import org.logic2j.model.Clause;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.var.Bindings;

public abstract class WebServiceClauseProvider implements ClauseProvider {
    
    //Those 3 variables have to be initialized at the creation time of the class.
    protected String trunkUrl;
    protected String suffixFeatureUrl;
    protected String featureConcerned = null;
        
    
    public abstract String constructWebServiceRequest(Struct theGoal);
    
    public abstract List<String> getParameterList(String featureAddressed);
    
    
    
    @Override
    public Iterable<Clause> listMatchingClauses(Struct theGoal,
            Bindings theGoalBindings) {
        
        String request = constructWebServiceRequest(theGoal);
        
        
        //TODO this method is not done yet
        return null;
    }
}
