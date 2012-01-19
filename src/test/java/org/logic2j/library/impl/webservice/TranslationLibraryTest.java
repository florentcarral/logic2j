package org.logic2j.library.impl.webservice;

import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.io.IOLibrary;

public class TranslationLibraryTest extends PrologTestBase {

    /*
     * WARNING : the translation tests that are corrects at one point may become
     * incorrect in the way that the translator may change a bit the ways words
     * are ordered, and also change some words, even if the translation is
     * correct.
     */
    @Test
    public void translation() throws Exception {
        loadLibrary(new IOLibrary(getProlog()));
        loadLibrary(new TranslationLibrary(getProlog()));
        System.out.println("\n1st test : ");
        assertNSolutions(1, "X='Ciao',translate('Hello', 'en', 'it', X)");
        System.out.println("\n2nd test : ");
        assertNoSolution("X='Ciaos',translate('Hello', 'en', 'it', X)");
        System.out.println("\n3nd test : ");
        assertNSolutions(1, "X='Salut Bob',translate('Hi Bob!', 'en', 'fr', X)");
        System.out.println("\n4th test : ");
        assertNSolutions(1, "X='Je suis allé à la ville',translate('I went to the city!', 'en', 'fr', X)");
        System.out.println("\n5th test : ");
        assertNSolutions(1, "X='Où vous situez-vous',translate('Where are you located', 'en', 'fr', X)");
        System.out.println("\6th test : ");
        assertNSolutions(1,
                "X='Je peux vous appeler.',translate('I can call you.', 'en', 'fr', X)");
        System.out.println("\n7th test : ");
        assertNSolutions(1,
                "X='Je suis un homme très grand.',translate('Yo soy un hombre muy grande.', 'es', 'fr', X)");
    }
}
