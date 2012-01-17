package org.logic2j.theory.webservice;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.symbol.Var;
import org.logic2j.theory.webService.YahooFinanceClauseProvider;

public class YahooFinanceClauseProviderTest {

    @Test
    public void getParameterListTest() {
        YahooFinanceClauseProvider yahooService = new YahooFinanceClauseProvider("finance");
        
        List<String> listParameter = new ArrayList<String>();
        listParameter.add("s");
        assertEquals(listParameter, yahooService.getParameterList("finance"));
    }
    
    
    @Test
    public void constructWebServiceRequest(){
        Struct theGoal = new Struct("finance", new Var("X") , (new TermApi()).valueOf("Intel", FactoryMode.ANY_TERM));
        
        YahooFinanceClauseProvider yahooService = new YahooFinanceClauseProvider("finance");
       
        assertEquals("http://finance.yahoo.com/rss/headline?s=Intel",yahooService.constructWebServiceRequest(theGoal));
    }

}
