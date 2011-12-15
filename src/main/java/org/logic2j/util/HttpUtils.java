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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author CARRAL Florent
 * 
 */
public class HttpUtils {

    /**
     * Build the string of an URL with parameters ( like
     * www.example.com&parameter1=1).
     * 
     * @param url
     *            the base URL (www.example.com).
     * @param parameters
     *            a map with the name of parameters for key.
     * @return the full URL
     */
    public static String buildHttpRequestFromService(String url,
            Map<String, String> parameters) {
        // iterator from the map.
        Iterator<String> iteratorOfParameters = parameters.keySet().iterator();
        boolean firstParameter = true;
        while (iteratorOfParameters.hasNext()) {

            String currentParameter = iteratorOfParameters.next();
            // if the request has at least one parameter, add of the char '?'.
            if (firstParameter) {
                url += "?" + currentParameter + "="
                        + parameters.get(currentParameter);
                firstParameter = false;
            } else {
                // if it's not the first parameter, add of the char '&'.
                url += "&" + currentParameter + "="
                        + parameters.get(currentParameter);
            }

        }

        return url;
    }

    /**
     * Return a document {@link org.w3c.dom.Document} object to navigate in the
     * xml response of an URL of a web service.
     * 
     * @param fullUrl
     *            the URL of the web service with the parameters.
     * @return the XML response in a document object
     */
    public static Document responseToDocument(String fullUrl) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        Document document = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(fullUrl);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return document;
    }

    /**
     * Build from base URL with the parameters the URL of a web service and
     * return a document {@link org.w3c.dom.Document} object to navigate in the
     * xml response
     * 
     * @param url
     *            the base URL of the web service
     * @param parameters
     *            a map with the name of parameters for key.
     * @return the XML response in a document object
     */
    public static Document responseToDocument(String url,
            Map<String, String> parameters) {
        return responseToDocument(buildHttpRequestFromService(url, parameters));
    }
}
