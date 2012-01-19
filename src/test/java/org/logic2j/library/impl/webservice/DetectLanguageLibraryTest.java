package org.logic2j.library.impl.webservice;

import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.io.IOLibrary;

public class DetectLanguageLibraryTest extends PrologTestBase {

    @Test
    public void detectionTest() throws Exception {
        loadLibrary(new IOLibrary(getProlog()));
        loadLibrary(new DetectLanguageLibrary(getProlog()));
        assertNSolutions(1, "detect_language('Hello world !', Language), write(Language), nl");
        assertNSolutions(1, "Text = 'Salut, ma maison est bleue !', detect_language(Text, Language), write(Language), nl");
    }
}
