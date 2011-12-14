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

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;


/**
 * @author CARRAL Florent
 *
 */
public class HttpUtilsTest {

    /**
     * Test method for {@link org.logic2j.util.HttpUtils#buildHttpRequestFromService(java.lang.String, java.lang.String, java.util.Map)}.
     */
    @Test
    public void testBuildHttpRequestFromService() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        // url without parameter.
        assertTrue(HttpUtils.buildHttpRequestFromService("www.test.com/my/Service", parameters).compareTo("www.test.com/my/Service") == 0);
        
        // url with one parameter.
        parameters.put("A", "6538654275");
        assertTrue(HttpUtils.buildHttpRequestFromService("www.test.com/my/Service", parameters).compareTo("www.test.com/my/Service?A=6538654275") == 0);
             
        // url with several parameters.
        parameters.put("B", "2");
        parameters.put("C", "3");
        assertTrue(HttpUtils.buildHttpRequestFromService("www.test.com/my/Service", parameters).compareTo("www.test.com/my/Service?A=6538654275&B=2&C=3") == 0);

    }

}
