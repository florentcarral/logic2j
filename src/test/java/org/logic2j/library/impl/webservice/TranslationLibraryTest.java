package org.logic2j.library.impl.webservice;

import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.io.IOLibrary;

public class TranslationLibraryTest extends PrologTestBase {

    @Test
    public void translation() throws Exception {
        loadLibrary(new IOLibrary(getProlog()));
        loadLibrary(new TranslationLibrary(getProlog()));
        System.out.println("\n1st test : ");
        assertNSolutions(1, "X='Ciao',translate('Hello', 'en', 'it', X)");
        System.out.println("\n2nd test : ");
        assertNoSolution("X='Ciaos',translate('Hello', 'en', 'it', X)");
        System.out.println("\n3nd test : ");
        assertNSolutions(1, "X='Salut Bob',translate('Hi%20Bob!', 'en', 'fr', X), write(X)");
        System.out.println("\n4th test : ");
        assertNSolutions(
                1,
                "X='Je peux vous appeler quand vous voulez.',translate('I%20can%20call%20you%20whenever%20you%20want.', 'es', 'fr', X), write(X)");
    }
}
