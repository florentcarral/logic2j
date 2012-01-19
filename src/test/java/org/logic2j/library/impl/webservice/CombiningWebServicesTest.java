package org.logic2j.library.impl.webservice;

import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.io.IOLibrary;

public class CombiningWebServicesTest extends PrologTestBase {

    @Test
    public void yahoomapAndTranslation() throws Exception {
        loadLibrary(new IOLibrary(getProlog()));
        loadLibrary(new YahooMapLibrary(getProlog()));
        loadLibrary(new TranslationLibrary(getProlog()));
        loadLibrary(new DetectLanguageLibrary(getProlog()));
        
        System.out.println("\n1st test : ");
        assertNSolutions(1, "Text='Ceci est un joli texte en français dont on aimerait une traduction en anglais',detect_language(Text, Language),write(Language),nl,translate(Text,Language,'en', TranslatedText),write(TranslatedText),nl");
        System.out.println("\n2nd test : ");
        assertNSolutions(1, "TargetLanguage='fr',Text='This is a nice French text which we would like a translation in English',translate(Text,'en',TargetLanguage,TranslatedText),detect_language(TranslatedText,TargetLanguage),write(TranslatedText),nl");
    }
    
}
