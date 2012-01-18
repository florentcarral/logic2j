package org.logic2j.library.impl.webservice;

import java.net.URL;
import java.util.HashMap;
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

    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DetectLanguageLibrary.class);

    public DetectLanguageLibrary(PrologImplementor theProlog) {
        super(theProlog);
    }

    @Primitive
    public void detect_language(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term text,
            Term language) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(
                "q",
                FormatUtils.removeApices(getProlog().getFormatter().format(
                        theBindings.focus(text, Term.class).getReferrer())));
        String value = getResponse(HttpUtils.buildHttpRequestFromService("http://ws.detectlanguage.com/0.1/detect",
                parameters));
        if (value != null) {
            Var[] languageVar = { (Var) language };
            Object[] valueTable = { value };
            unifyAndNotify(languageVar, valueTable, theBindings, theGoalFrame, theListener);
        }
    }

    private static String getResponse(String fullUrl) {
        try {
            JsonParser parser = new JsonFactory().createJsonParser(new URL(fullUrl).openConnection().getInputStream());
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.VALUE_STRING)
                    return parser.getText();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

}
