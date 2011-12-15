/*
 * logic2j - "Bring Logic to your Java" - Copyright (C) 2011 Laurent.Tettoni@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.logic2j.util;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;
import org.logic2j.library.impl.webservice.YahooMapLibrary;
import org.w3c.dom.Document;

/**
 * @author CARRAL Florent
 * 
 */
public class HttpUtilsTest {

    /**
     * Test method for
     * {@link org.logic2j.util.HttpUtils#buildHttpRequestFromService(java.lang.String, java.lang.String, java.util.Map)}
     * .
     */
    @Test
    public void testBuildHttpRequestFromService() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        // url without parameter.
        assertTrue(HttpUtils.buildHttpRequestFromService(
                "www.test.com/my/Service", parameters).compareTo(
                "www.test.com/my/Service") == 0);

        // url with one parameter.
        parameters.put("A", "6538654275");
        assertTrue(HttpUtils.buildHttpRequestFromService(
                "www.test.com/my/Service", parameters).compareTo(
                "www.test.com/my/Service?A=6538654275") == 0);

        // url with several parameters.
        parameters.put("B", "2");
        parameters.put("C", "3");
        assertTrue(HttpUtils.buildHttpRequestFromService(
                "www.test.com/my/Service", parameters).compareTo(
                "www.test.com/my/Service?A=6538654275&B=2&C=3") == 0);

    }

    @Test
    public void testresponseToDocument() {
        // sample test 
        String fullUrl = "http://where.yahooapis.com/geocode?q=La+Voix+Creuse+1,+1202+Geneve,+Suisse&appid=dj0yJmk9QkhEQlJQOXlLODhtJmQ9WVdrOVNsTXpXREo1TlRRbWNHbzlNekkyTlRJek16WXkmcz1jb25zdW1lcnNlY3JldCZ4PTJj";
        Document dct = HttpUtils.responseToDocument(fullUrl);
        assertTrue(dct.getFirstChild().getNodeName().equals("ResultSet"));
        // test for find a information inside the xml document.
        fullUrl = "http://where.yahooapis.com/geocode?q=46.218079,6.142825&gflags=R&appid=dj0yJmk9QkhEQlJQOXlLODhtJmQ9WVdrOVNsTXpXREo1TlRRbWNHbzlNekkyTlRJek16WXkmcz1jb25zdW1lcnNlY3JldCZ4PTJj";
        System.out.println(YahooMapLibrary.coordonatesToAddress(fullUrl));
    }

}
