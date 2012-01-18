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

public class DetectLanguageLibrary extends LibraryBase {

    private static final String serviceTrunkUrl = "http://ws.detectlanguage.com/0.1/";

    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DetectLanguageLibrary.class);

    public final static int DETECTLANGUAGE_DETECT = 0;

    // Definition of the arrayList with the suffix parts of the urls
    // corresponding to the services to implement
    private static final Map<Integer, String> featureSuffixUrlMap = new HashMap<Integer, String>();
    static {
        featureSuffixUrlMap.put(DETECTLANGUAGE_DETECT, "detect");
    }

    // Definition of the parameters needed for all the features instantiated.
    private static final Map<Integer, List<String>> featuresParameters = new HashMap<Integer, List<String>>();
    private static final List<String> translationParameter = new ArrayList<String>();
    static {
        // The sentence which we want to detect the language.
        translationParameter.add("q");
        featuresParameters.put(DETECTLANGUAGE_DETECT, translationParameter);
    }

    public DetectLanguageLibrary(PrologImplementor theProlog) {
        super(theProlog);
    }

    @Primitive
    public void detect_language(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term text,
            Term language) {
        Map<String, String> parameters = constructDetectParameters(FormatUtils.removeApices(getProlog().getFormatter()
                .format(theBindings.focus(text, Term.class).getReferrer())));
        String value = getResponse(HttpUtils.buildHttpRequestFromService(
                serviceTrunkUrl + featureSuffixUrlMap.get(DETECTLANGUAGE_DETECT), parameters));
        if (value != null) {
            Var[] languageVar = { (Var) language };
            Object[] valueTable = { value };
            unifyAndNotify(languageVar, valueTable, theBindings, theGoalFrame, theListener);
        }
    }

    private Map<String, String> constructDetectParameters(String formatText) {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put(featuresParameters.get(DETECTLANGUAGE_DETECT).get(0), formatText);
        return requestParameters;
    }

    private static String getResponse(String fullUrl) {
        String result = null;
        try {
            JsonParser parser = new JsonFactory().createJsonParser(new URL(fullUrl).openConnection().getInputStream());
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.VALUE_STRING) {
                    result = parser.getText();
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return result;
    }

}
