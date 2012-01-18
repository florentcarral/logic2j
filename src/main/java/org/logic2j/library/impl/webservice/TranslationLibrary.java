package org.logic2j.library.impl.webservice;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
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
    static {
        // MYMEMORY_GET without IP
        translationParameter.add("q"); // the sentence to translate
        translationParameter.add("langpair"); // the pair of language codes used
        featuresParameters.put(MYMEMORY_GET, translationParameter);
        // MYMEMORY_GET with a specified IP address (allow user to use bigger
        // amount of request)
        translationWithIPParameter.add("q"); // the sentence to translate
        translationWithIPParameter.add("langpair"); // the pair of language
                                                    // codes used
        translationWithIPParameter.add("ip"); // the ip address to use for the
                                              // translation
        featuresParameters.put(MYMEMORY_GET_IP, translationWithIPParameter);
    }

    public TranslationLibrary(PrologImplementor theProlog) {
        super(theProlog);
    }

    /**
     * This primitive enables user to translate a given text (parameter Term
     * <b>text</b>) from a source language (parameter Term
     * <b>sourceLanguage</b>) into another language (Term
     * <b>targetLanguage</b>). <br>
     * The translated text will be "stored" into the Term <b>translatedText</b>.
     * In that primitive the user gives its IP address to enable large amount of
     * translation requests. (It is a recommendation of the WebService MyMemory
     * translated).
     * 
     * @param theListener
     * @param theGoalFrame
     * @param theBindings
     * @param text
     *            is a Term that corresponds to the text to translate.
     * @param sourceLanguage
     *            is a Term that corresponds to the actual language of the text,
     *            also given as an in parameter.
     * @param targetLanguage
     *            is the targeted language for the translation of the text given
     *            as an in parameter.
     * @param translatedText
     *            is a Term that will be used to contain the result of te
     *            translation (the translated text).
     * @param ipAddress
     *            is a Term that corresponds to the IP address of the user
     *            calling the requested service. By providing that IP address,
     *            the user will be enable to use the webservice without any
     *            usage restriction.
     */
    @Primitive
    public void translate(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term text,
            Term sourceLanguage, Term targetLanguage, Term translatedText, Term ipAddress) {

        final Term textValue = theBindings.focus(text, Term.class).getReferrer();
        String formatText = FormatUtils.removeApices(getProlog().getFormatter().format(textValue));

        final Term sourceLanguageValue = theBindings.focus(sourceLanguage, Term.class).getReferrer();
        String formatSourceLanguage = FormatUtils.removeApices(getProlog().getFormatter().format(sourceLanguageValue));

        final Term targetLanguageValue = theBindings.focus(targetLanguage, Term.class).getReferrer();
        String formatTargetLanguage = FormatUtils.removeApices(getProlog().getFormatter().format(targetLanguageValue));

        final Term ipAddressValue = theBindings.focus(ipAddress, Term.class).getReferrer();
        String formatIpAddress = FormatUtils.removeApices(getProlog().getFormatter().format(ipAddressValue));

        Map<String, String> parameters = constructTranslationWithIpParameters(formatText, formatSourceLanguage,
                formatTargetLanguage, formatIpAddress);
        String value = translation(HttpUtils.buildHttpRequestFromService(
                serviceTrunkUrl + featureSuffixUrlMap.get(MYMEMORY_GET), parameters));

        if (value != null) {
            Var[] translatedTextVar = { (Var) translatedText };
            Object[] valueTable = { value };
            unifyAndNotify(translatedTextVar, valueTable, theBindings, theGoalFrame, theListener);
        }

    }

    /**
     * This primitive enables user to translate a given text (parameter Term
     * <b>text</b>) from a source language (parameter Term
     * <b>sourceLanguage</b>) into another language (Term
     * <b>targetLanguage</b>). <br>
     * The translated text will be "stored" into the Term <b>translatedText</b>.
     * 
     * <b>NB : The languages (<b>sourceLanguage</b> and <b>targetLanguage</b>)
     * have to match with the <b>ISO standard</b> names or <b>RFC3066.</b>
     * 
     * @param theListener
     * @param theGoalFrame
     * @param theBindings
     * @param text
     *            is a Term that corresponds to the text to translate.
     * @param sourceLanguage
     *            is a Term that corresponds to the actual language of the text,
     *            also given as an in parameter.
     * @param targetLanguage
     *            is the targeted language for the translation of the text given
     *            as an in parameter.
     * @param translatedText
     *            is a Term that will be used to contain the result of the
     *            translation (the translated text).
     */
    @Primitive
    public void translate(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term text,
            Term sourceLanguage, Term targetLanguage, Term translatedText) {

        final Term textValue = theBindings.focus(text, Term.class).getReferrer();
        String formatText = FormatUtils.removeApices(getProlog().getFormatter().format(textValue));

        final Term sourceLanguageValue = theBindings.focus(sourceLanguage, Term.class).getReferrer();
        String formatSourceLanguage = FormatUtils.removeApices(getProlog().getFormatter().format(sourceLanguageValue));

        final Term targetLanguageValue = theBindings.focus(targetLanguage, Term.class).getReferrer();
        String formatTargetLanguage = FormatUtils.removeApices(getProlog().getFormatter().format(targetLanguageValue));

        Map<String, String> parameters = constructTranslationParameters(formatText, formatSourceLanguage,
                formatTargetLanguage);
        String value = translation(HttpUtils.buildHttpRequestFromService(
                serviceTrunkUrl + featureSuffixUrlMap.get(MYMEMORY_GET), parameters));

        if (value != null) {
            Var[] translatedTextVar = { (Var) translatedText };
            Object[] valueTable = { value };
            unifyAndNotify(translatedTextVar, valueTable, theBindings, theGoalFrame, theListener);
        }
    }

    /**
     * This method builds a Map of indexed by Strings and for which the
     * contained object are string that are the value corresponding to the
     * fields given as indexes. <br>
     * <br>
     * e.g: For an get http request, if the expected parameters are
     * <i>langpair</i> and <i>theText</i> and their corresponding values are
     * <i>"en|it"</i> and <i>"A text to translate"</i>. <br>
     * Then the Map will be as followed :
     * <ul>
     * <li>aMap.put("langpair", "en|it")</li>
     * <li>aMap.put("theText", "A text to translate")
     * <li>
     * </ul>
     * 
     * @param formatText
     *            is the text that will have to be passed as a parameter into
     *            the url to be then translated.
     * @param formatSourceLanguage
     *            is the language code (ISO standard names or RFC 3066)
     *            corresponding to the language of the original text.
     * @param formatTargetLanguage
     *            is the language code (ISO standard names or RFC 3066)
     *            corresponding to the language into which the text will be
     *            translated.
     * @return the corresponding Map which is composed of the parameters and
     *         their values where the parameters names are used as indexes of
     *         the map.
     */
    public Map<String, String> constructTranslationParameters(String formatText, String formatSourceLanguage,
            String formatTargetLanguage) {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put(featuresParameters.get(MYMEMORY_GET).get(0), formatText);
        requestParameters.put(featuresParameters.get(MYMEMORY_GET).get(1), formatSourceLanguage + "|"
                + formatTargetLanguage);
        return requestParameters;
    }

    /**
     * This method builds a Map of indexed by Strings and for which the
     * contained object are string that are the value corresponding to the
     * fields given as indexes. <br>
     * <br>
     * e.g: For an get http request, if the expected parameters are
     * <i>langpair</i> and <i>theText</i> and their corresponding values are
     * <i>"en|it"</i> and <i>"A text to translate"</i>. <br>
     * Then the Map will be as followed :
     * <ul>
     * <li>aMap.put("langpair", "en|it")</li>
     * <li>aMap.put("theText", "A text to translate")
     * <li>
     * </ul>
     * 
     * @param formatText
     *            is the text that will have to be passed as a parameter into
     *            the url to be then translated.
     * @param formatSourceLanguage
     *            is the language code (ISO standard names or RFC 3066)
     *            corresponding to the language of the original text.
     * @param formatTargetLanguage
     *            is the language code (ISO standard names or RFC 3066)
     *            corresponding to the language into which the text will be
     *            translated.
     * @param formatIp
     *            is the ip that is used into the request.
     * @return the corresponding Map which is composed of the parameters and
     *         their values where the parameters names are used as indexes of
     *         the map.
     */
    public Map<String, String> constructTranslationWithIpParameters(String formatText, String formatSourceLanguage,
            String formatTargetLanguage, String formatIp) {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put(featuresParameters.get(MYMEMORY_GET_IP).get(0), formatText);
        requestParameters.put(featuresParameters.get(MYMEMORY_GET_IP).get(1), formatSourceLanguage + "|"
                + formatTargetLanguage);
        requestParameters.put(featuresParameters.get(MYMEMORY_GET_IP).get(2), formatIp);
        return requestParameters;
    }

    /**
     * This method takes the URL that will be used to call the web service of
     * MyMemory and then does the request and takes the JSON result to extract
     * the expected translated text and return it.
     * 
     * @param fullUrl
     *            is the full url that has to be used to formulate the request.
     * @return a String corresponding to the translated Text extracted from the
     *         result of the request.
     */
    public static String translation(String fullUrl) {
        String result = null;
        try {
            JsonParser parser = new JsonFactory().createJsonParser(new URL(fullUrl).openConnection().getInputStream());
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                // The first textual value found in the JSON result corresponds
                // to our waited result
                if (token == JsonToken.VALUE_STRING) {
                    result = parser.getText();
                    return result;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return result;
    }

}
