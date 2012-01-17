package org.logic2j.library.impl.webservice;

import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.io.IOLibrary;

public class YahooMapLibraryTest extends PrologTestBase {

    @Test
    public void yahoomap_address() throws Exception {
        loadLibrary(new IOLibrary(getProlog()));
        loadLibrary(new YahooMapLibrary(getProlog()));
        System.out.println("\n 1st test : ");
        assertNSolutions(1,"yahoomap_address(X, '12.05435', '13.2345', 'MY_API_KEY'), write(X)");
        System.out.println("\n 2nd test : ");
        assertNoSolution("yahoomap_address('a fake address', '12.05435', '13.2345', 'MY_API_KEY'), write(X)");
        System.out.println("\n 3rd test : ");
        assertNSolutions(1,"yahoomap_address(X, Y, '13.2345', 'MY_API_KEY'), write(X), write('    and Y =>'), write(Y)");
        System.out.println("\n 4th test : ");
        assertNSolutions(1,"Y='12.05435', yahoomap_address(X, Y, '13.2345', 'MY_API_KEY'), write(X), write('    and Y =>'), write(Y)");
        System.out.println("\n 5th test : ");
        assertNoSolution("yahoomap_address(X,'0', '0', 'MY_API_KEY')");
    }

    @Test
    public void yahoomap_coordinates() throws Exception {
        loadLibrary(new IOLibrary(getProlog()));
        loadLibrary(new YahooMapLibrary(getProlog()));
        System.out.println("\n 1st test : ");
        assertNSolutions(1,"yahoomap_coordinates(LATITUDE, LONGITUDE, '244+chemin+de+la+Cassine,73000+Chambery', 'MY_API_KEY'), write(LATITUDE), write(LONGITUDE)");
    }
    
    
    
    
}