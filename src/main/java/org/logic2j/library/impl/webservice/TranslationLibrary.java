package org.logic2j.library.impl.webservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.logic2j.PrologImplementor;
import org.logic2j.io.format.FormatUtils;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.mgmt.Primitive;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.Bindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;
import org.logic2j.util.HttpUtils;

public class TranslationLibrary extends LibraryBase {

    private static final String serviceTrunkUrl = "http://mymemory.translated.net/api/";

    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TranslationLibrary.class);

    public final static int COORDINATE_RESULT_SIZE = 2;
    public final static int COORDINATE_LATITUDE = 0;
    public final static int COORDINATE_LONGITUDE = 1;
    public final static int MYMEMORY_GET = 0;
    public final static int MYMEMORY_GET_IP = 1;

    // Definition of the arrayList with the suffix parts of the urls
    // corresponding to the services to implement
    private static final Map<Integer, String> featureSuffixUrlMap = new HashMap<Integer, String>();
    static {
        featureSuffixUrlMap.put(MYMEMORY_GET, "get");
        featureSuffixUrlMap.put(MYMEMORY_GET_IP, "get");
    }

    // Definition of the parameters needed for all the features instantiated.
    private static final Map<Integer, List<String>> featuresParameters = new HashMap<Integer, List<String>>();
    private static final List<String> translationParameter = new ArrayList<String>();
    private static final List<String> translationWithIPParameter = new ArrayList<String>();
    static{
        //MYMEMORY_GET without IP
        translationParameter.add("q");  // the sentence to translate
        translationParameter.add("langpair");   //the pair of language codes used
        featuresParameters.put(MYMEMORY_GET, translationParameter);
        //MYMEMORY_GET with a specified IP address (allow user to use bigger amount of request)
        translationWithIPParameter.add("q");        // the sentence to translate
        translationWithIPParameter.add("langpair"); //the pair of language codes used
        translationWithIPParameter.add("ip");       //the ip address to use for the translation
        featuresParameters.put(MYMEMORY_GET_IP, translationWithIPParameter);
    }
    
    
    public TranslationLibrary(PrologImplementor theProlog) {
        super(theProlog);
    }


    /**
     * This primitive enables user to translate a given text (parameter Term <b>text</b>) from a source language (parameter Term <b>sourceLanguage</b>) into another language (Term <b>targetLanguage</b>).
     * <br>The translated text will be "stored" into the Term <b>translatedText</b>. In that primitive the user gives its IP address to enable large amount of translation requests. (It is a recommendation of the WebService MyMemory translated). 
     * 
     * @param theListener
     * @param theGoalFrame
     * @param theBindings
     * @param text is a Term that corresponds to the text to translate.
     * @param sourceLanguage is a Term that corresponds to the actual language of the text, also given as an in parameter.
     * @param targetLanguage is the targeted language for the translation of the text given as an in parameter.
     * @param translatedText is a Term that will be used to contain the result of te translation (the translated text).
     * @param ipAddress is a Term that corresponds to the IP address of the user calling the requested service. By providing that IP address, the user will be enable to use the webservice without any usage restriction.
     */
    @Primitive
    public void translate(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term text, Term sourceLanguage, Term targetLanguage, Term translatedText, Term ipAddress) {

        final Term  textValue = theBindings.focus(text,Term.class).getReferrer();
        String formatText = FormatUtils.removeApices(getProlog().getFormatter().format(textValue));
        
        final Term sourceLanguageValue = theBindings.focus(sourceLanguage, Term.class).getReferrer();
        String formatSourceLanguage = FormatUtils.removeApices(getProlog().getFormatter().format(sourceLanguageValue));
        
        final Term targetLanguageValue = theBindings.focus(targetLanguage,Term.class).getReferrer();
        String formatTargetLanguage = FormatUtils.removeApices(getProlog().getFormatter().format(targetLanguageValue));
        
        final Term ipAddressValue = theBindings.focus(ipAddress,Term.class).getReferrer();
        String formatIpAddress = FormatUtils.removeApices(getProlog().getFormatter().format(ipAddressValue));
        
        
        Map<String,String> parameters = constructTranslationWithIpParameters(formatText, formatSourceLanguage, formatTargetLanguage, formatIpAddress);
        String value = translation(HttpUtils.buildHttpRequestFromService(serviceTrunkUrl+featureSuffixUrlMap.get(MYMEMORY_GET), parameters));
        
        if (value!=null){
            Var[] translatedTextVar = {(Var)translatedText};
            Object[] valueTable = {value};
            unifyAndNotify(translatedTextVar, valueTable, theBindings, theGoalFrame, theListener);
        }
        
    }
    
    
    
    /**
     * This primitive enables user to translate a given text (parameter Term <b>text</b>) from a source language (parameter Term <b>sourceLanguage</b>) into another language (Term <b>targetLanguage</b>).
     * <br>The translated text will be "stored" into the Term <b>translatedText</b>. 
     * 
     * <b>NB : The languages (<b>sourceLanguage</b> and <b>targetLanguage</b>) have to match with the <b>ISO standard</b> names or <b>RFC3066.</b>
     * @param theListener
     * @param theGoalFrame
     * @param theBindings
     * @param text is a Term that corresponds to the text to translate.
     * @param sourceLanguage is a Term that corresponds to the actual language of the text, also given as an in parameter.
     * @param targetLanguage is the targeted language for the translation of the text given as an in parameter.
     * @param translatedText is a Term that will be used to contain the result of the translation (the translated text).
     */
    @Primitive
    public void translate(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term text, Term sourceLanguage, Term targetLanguage, Term translatedText) {

        final Term  textValue = theBindings.focus(text,Term.class).getReferrer();
        String formatText = FormatUtils.removeApices(getProlog().getFormatter().format(textValue));
        
        final Term sourceLanguageValue = theBindings.focus(sourceLanguage, Term.class).getReferrer();
        String formatSourceLanguage = FormatUtils.removeApices(getProlog().getFormatter().format(sourceLanguageValue));
        
        final Term targetLanguageValue = theBindings.focus(targetLanguage,Term.class).getReferrer();
        String formatTargetLanguage = FormatUtils.removeApices(getProlog().getFormatter().format(targetLanguageValue));
        
        Map<String,String> parameters = constructTranslationParameters(formatText, formatSourceLanguage, formatTargetLanguage);
        String value = translation(HttpUtils.buildHttpRequestFromService(serviceTrunkUrl+featureSuffixUrlMap.get(MYMEMORY_GET), parameters));
        
        if (value!=null){
            Var[] translatedTextVar = {(Var)translatedText};
            Object[] valueTable = {value};
            unifyAndNotify(translatedTextVar, valueTable, theBindings, theGoalFrame, theListener);
        }
    }
    
    
    public Map<String, String> constructTranslationParameters(String formatText, String formatSourceLanguage, String formatTargetLanguage){
        Map<String,String> requestParameters = new HashMap<String, String>();
        requestParameters.put(featuresParameters.get(MYMEMORY_GET).get(0), formatText);
        requestParameters.put(featuresParameters.get(MYMEMORY_GET).get(1), formatSourceLanguage+"|"+formatTargetLanguage);
        return requestParameters;
    }
    

    public Map<String, String> constructTranslationWithIpParameters(String formatText, String formatSourceLanguage, String formatTargetLanguage, String formatIp){
        Map<String,String> requestParameters = new HashMap<String, String>();
        requestParameters.put(featuresParameters.get(MYMEMORY_GET_IP).get(0), formatText);
        requestParameters.put(featuresParameters.get(MYMEMORY_GET_IP).get(1), formatSourceLanguage+"|"+formatTargetLanguage);
        requestParameters.put(featuresParameters.get(MYMEMORY_GET_IP).get(2), formatIp);
        return requestParameters;
    }
    
    
    public static String translation(String fullUrl){
        //TODO the whole function    
        return null;
    }
    
    
    /*
    public static List<String[]> addressToCoordinate(String fullUrl){
        List<String[]> result = new ArrayList<String[]>();
        Document doc = HttpUtils.responseToDocument(fullUrl);
        NodeList resultsFromService = doc.getFirstChild().getChildNodes();
        // for each funded coordinate.
        for (int i = 0; i < resultsFromService.getLength(); i++) {
            if (resultsFromService.item(i).getNodeName().equals("Result")) {
                NodeList currentResult = resultsFromService.item(i)
                        .getChildNodes();
                String latitude = "";
                String longitude = "";
                for (int j = 0; j < currentResult.getLength(); j++) {
                    if (currentResult.item(j).getNodeName().equals("latitude")) {
                        latitude = currentResult.item(j).getTextContent();
                    }
                    if (currentResult.item(j).getNodeName().equals("longitude")) {
                        longitude = currentResult.item(j).getTextContent();
                    }
                }
                String[] currentResultTable = new String[COORDINATE_RESULT_SIZE];
                currentResultTable[COORDINATE_LATITUDE] = latitude;
                currentResultTable[COORDINATE_LONGITUDE] = longitude;
                result.add(currentResultTable);
            }
        }
        return result;
    }
    
    
    public static List<String> coordinatesToAddress(String fullUrl) {
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
    */
}
