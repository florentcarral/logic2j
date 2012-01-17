package org.logic2j.theory.webService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.logic2j.model.symbol.Struct;
import org.logic2j.util.HttpUtils;

public class YahooFinanceClauseProvider extends WebServiceClauseProvider{
    
    public final static String FINANCE_FEATURE = "finance";
    
    
    
    //Definition of the arrayList with the suffix parts of the urls corresponding to the services to implement
    private static final Map<String, String> featureSuffixUrlMap = new HashMap<String, String>();
    static{
        featureSuffixUrlMap.put(FINANCE_FEATURE, "/rss/headline");          // finance service
    }
    
    //Definition of the parameters needed for all the features instantiated.
    private static final Map<String,List<String>> featuresParameters = new HashMap<String, List<String>>();
    private static final List<String> financeParameters = new ArrayList<String>();
    static{
        financeParameters.add("s");
        featuresParameters.put(FINANCE_FEATURE, financeParameters);
    }
    
    
    
    public YahooFinanceClauseProvider(String featureString){
        if (featureSuffixUrlMap.containsKey(featureString)) {
            this.suffixFeatureUrl = featureSuffixUrlMap.get(featureString);
            this.featureConcerned = featureString;
        }
        else{
            this.featureConcerned = null;
            this.suffixFeatureUrl = null;
        }
        this.trunkUrl = "http://finance.yahoo.com";
    }
    
    
    
    /* ************************************************************************
     ************************* OVERRIDED METHODS ******************************
     *********************************************************************** */
    
    /**
     * That method builds the url that has to be used to call the web service to solve the goal given as a parameter.
     * @param theGoal : this is the goal which contains the definition of the service to call for the specific web service (Yahoo Finance in our case) and also te parameters used to precise the request.
     * @return a String that corresponds to the http url which will be used to call the web service.
     */
    @Override
    public String constructWebServiceRequest(Struct theGoal) {
        String httpRequest = null;
        if (this.featureConcerned!=null && featureSuffixUrlMap.containsKey(featureConcerned)){
            List<String> parameterList = getParameterList(featureConcerned);
            if (parameterList!=null){
                //Parameter values are stored into the goal
                //The parameterList contains the names of the parameters that are used to create the http request => GET parameters
                
                /* This is an ugly way to do it, because in our process we take into consideration that the variable within the goal is its first parameter.
                 * 
                 * TODO => imperative to change that way to do it.
                 */
                
                if (theGoal.getArity() == (parameterList.size()+1)){
                    Map<String, String> parameterCouples = new HashMap<String, String>();
                    for (int i=0; i<parameterList.size(); i++){
                        parameterCouples.put(parameterList.get(i), theGoal.getArg(i+1).toString());
                    }
                    httpRequest = HttpUtils.buildHttpRequestFromService(trunkUrl+suffixFeatureUrl, parameterCouples);
                }
            }
        }
        return httpRequest;
    }
    
    
    
    /**
     * This methods returns the list of the parameters name for the feature <i>featureAdressed</i> that is addressed for our specific web service.
     * @param featureAddressed : is a String that corresponds to the key word used to indicate which feature of the web service has to be called.
     * @return a list of String corresponding to those names of parameters such as they appear in the url of the http request.
     */
    @Override
    public List<String> getParameterList(String featureAddressed){
        List<String> parameterList = null;
        if (featuresParameters.containsKey(featureAddressed)){
            parameterList = featuresParameters.get(featureAddressed);
        }
        return parameterList;
    }
}
