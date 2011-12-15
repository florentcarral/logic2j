package org.logic2j.library.impl.webservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.logic2j.PrologImplementor;
import org.logic2j.io.format.FormatUtils;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.impl.io.IOLibrary;
import org.logic2j.library.mgmt.Primitive;
import org.logic2j.model.TermVisitor;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.Binding;
import org.logic2j.model.var.Bindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;
import org.logic2j.util.HttpUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class YahooMapLibrary extends LibraryBase {

    private static final String serviceTrunkUrl = "http://where.yahooapis.com/geocode";

    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
            .getLogger(IOLibrary.class);

    public final static int YAHOOMAP_ADDRESS = 0;
    public final static int YAHOOMAP_LATITUDE_AND_LONGITUDE = 1;

    // Definition of the arrayList with the suffix parts of the urls
    // corresponding to the services to implement
    private static final Map<Integer, String> featureSuffixUrlMap = new HashMap<Integer, String>();
    static {
        featureSuffixUrlMap.put(YAHOOMAP_ADDRESS, "");
        featureSuffixUrlMap.put(YAHOOMAP_LATITUDE_AND_LONGITUDE, "");
    }

    // Definition of the parameters needed for all the features instantiated.
    private static final Map<Integer, List<String>> featuresParameters = new HashMap<Integer, List<String>>();
    private static final List<String> addressParameters = new ArrayList<String>();
    private static final List<String> latitudeAndLongitudeParameters = new ArrayList<String>();
    static{
        //YAHOOMAP_ADDRESS
        addressParameters.add("q");
        addressParameters.add("gflags");
        addressParameters.add("appid");
        featuresParameters.put(YAHOOMAP_ADDRESS, addressParameters);
        //YAHOOMAP_LATITUDE_AND_LONGITUDE
        latitudeAndLongitudeParameters.add("address");
        latitudeAndLongitudeParameters.add("flags");
        latitudeAndLongitudeParameters.add("appid");
        featuresParameters.put(YAHOOMAP_LATITUDE_AND_LONGITUDE, addressParameters);
    }

    
    public YahooMapLibrary(PrologImplementor theProlog) {
        super(theProlog);
    }
    
    
    @Primitive
    public void yahoomap_address(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term address, Term latitude, Term longitude, Term apiKey) {

        final Bindings longitudeBindings = theBindings.focus(longitude,Term.class);
        final Term longitudeValue = longitudeBindings.getReferrer();
        String formatLongitude = getProlog().getFormatter().format(longitudeValue);
        formatLongitude = FormatUtils.removeApices(formatLongitude);

        final Bindings latitudeBindings = theBindings.focus(latitude,Term.class);
        final Term latitudeValue = latitudeBindings.getReferrer();
        String formatLatitude = getProlog().getFormatter().format(latitudeValue);
        formatLatitude = FormatUtils.removeApices(formatLatitude);

        final Bindings apiKeyBindings = theBindings.focus(apiKey, Term.class);
        final Term apiKeyValue = apiKeyBindings.getReferrer();
        String formatApiKey = getProlog().getFormatter().format(apiKeyValue);
        formatApiKey = FormatUtils.removeApices(formatApiKey);
        
        Map<String,String> parameters = constructAddressRequestParameters(formatLatitude, formatLongitude, formatApiKey);        
        List<String> values = coordonatesToAddress(HttpUtils.buildHttpRequestFromService(serviceTrunkUrl+featureSuffixUrlMap.get(YAHOOMAP_ADDRESS), parameters));
        
        if (values.size()!=0){
            if (address instanceof Var){
                Var[] addressVars = {(Var)address};
                unifyAndNotify(addressVars, values.toArray(), theBindings, theGoalFrame, theListener);
            }
        }
    }

    
    
    public Map<String,String> constructAddressRequestParameters(String latitude, String longitude, String apiKey){
        Map<String,String> requestParameters = new HashMap<String, String>();
        requestParameters.put(featuresParameters.get(YAHOOMAP_ADDRESS).get(0), latitude+",+"+longitude);
        requestParameters.put(featuresParameters.get(YAHOOMAP_ADDRESS).get(1), "R");
        requestParameters.put(featuresParameters.get(YAHOOMAP_ADDRESS).get(2), apiKey);
        return requestParameters;
    }
    

    /*public static List<String[]> addressToCoordonate(String fullUrl){
        List<String[]> result = new ArrayList<String[]>();
        Document doc = HttpUtils.responseToDocument(fullUrl);
        NodeList resultsFromService = doc.getFirstChild().getChildNodes();
        
    }*/
    
    
    public static List<String> coordonatesToAddress(String fullUrl) {
        List<String> result = new ArrayList<String>();
        Document doc = HttpUtils.responseToDocument(fullUrl);
        NodeList resultsFromService = doc.getFirstChild().getChildNodes();
        // for each funded address.
        for (int i = 0; i < resultsFromService.getLength(); i++) {
            if (resultsFromService.item(i).getNodeName().equals("Result")) {
                NodeList currentResult = resultsFromService.item(i).getChildNodes();
                // prepare of the string variable of the current address
                String house = "";
                String street = "";
                String postal = "";
                String city = "";
                String country = "";
                for (int j = 0; j < currentResult.getLength(); j++) {
                        if (currentResult.item(j).getNodeName().equals("house")) {
                            house = currentResult.item(j).getTextContent();
                        }
                        if (currentResult.item(j).getNodeName().equals("street")) {
                            street = currentResult.item(j).getTextContent();
                        }
                        if (currentResult.item(j).getNodeName().equals("postal")) {
                            postal = currentResult.item(j).getTextContent();
                        }
                        if (currentResult.item(j).getNodeName().equals("city")) {
                            city = currentResult.item(j).getTextContent();
                        }
                        if (currentResult.item(j).getNodeName().equals("country")) {
                            country = currentResult.item(j).getTextContent();
                        }
                }
                String currentAddress = house;
                //That means that it is a non empty String which doesn't finish with a blank character logically.
                if (currentAddress.length()>0) currentAddress+=" ";
                currentAddress += street;
                if (currentAddress.length()>0) currentAddress+=" ";
                currentAddress += postal;
                if (currentAddress.length()>0) currentAddress+=" ";
                currentAddress += city;
                if (currentAddress.length()>0) currentAddress+=" ";
                currentAddress += country;
                
                if (currentAddress.length()>0){
                    result.add(currentAddress);
                }
            }
        }
        return result;
    }
}
