package org.logic2j.library.impl.webservice;

import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.io.IOLibrary;

public class DetectLanguageLibraryTest extends PrologTestBase {

    @Test
    public void detectTest() throws Exception {
        loadLibrary(new IOLibrary(getProlog()));
        loadLibrary(new DetectLanguageLibrary(getProlog()));
        assertNSolutions(1, "detect_language('Hello world !', Language), write(Language)");
        assertNSolutions(1, "Text = 'Salut, ma maison est bleue !', detect_language(Text, Language), write(Language)");
    }
    
    @Test
    public void detectAndTranslationTest() throws Exception {
        loadLibrary(new IOLibrary(getProlog()));
        loadLibrary(new DetectLanguageLibrary(getProlog()));
        loadLibrary(new TranslationLibrary(getProlog()));
        assertNSolutions(1, "Text = 'Salut,%20ma%20maison%20est%20bleue%20!', detect_language(Text, Language), translate(Text, Language, 'en', TranslatedText), write(TranslatedText)");
    }
    
}
