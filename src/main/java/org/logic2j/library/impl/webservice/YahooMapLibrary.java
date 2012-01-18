package org.logic2j.library.impl.webservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.core.IsInstanceOf;
import org.logic2j.PrologImplementor;
import org.logic2j.io.format.FormatUtils;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.impl.io.IOLibrary;
import org.logic2j.library.mgmt.Primitive;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.Bindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;
import org.logic2j.util.HttpUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class YahooMapLibrary extends LibraryBase {

    private static final String serviceTrunkUrl = "http://where.yahooapis.com/geocode";

    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IOLibrary.class);

    public final static int COORDINATE_RESULT_SIZE = 2;
    public final static int COORDINATE_LATITUDE = 0;
    public final static int COORDINATE_LONGITUDE = 1;
    public final static int YAHOOMAP_ADDRESS = 0;
    public final static int YAHOOMAP_COORDINATES = 1;

    // Definition of the arrayList with the suffix parts of the urls
    // corresponding to the services to implement
    private static final Map<Integer, String> featureSuffixUrlMap = new HashMap<Integer, String>();
    static {
        featureSuffixUrlMap.put(YAHOOMAP_ADDRESS, "");
        featureSuffixUrlMap.put(YAHOOMAP_COORDINATES, "");
    }

    // Definition of the parameters needed for all the features instantiated.
    private static final Map<Integer, List<String>> featuresParameters = new HashMap<Integer, List<String>>();
    private static final List<String> addressParameters = new ArrayList<String>();
    private static final List<String> coordinatesParameters = new ArrayList<String>();
    static {
        // YAHOOMAP_ADDRESS
        // to specify that we are giving an address information
        addressParameters.add("q");
        addressParameters.add("gflags");
        addressParameters.add("appid");
        featuresParameters.put(YAHOOMAP_ADDRESS, addressParameters);
        // YAHOOMAP_LATITUDE_AND_LONGITUDE
        // to specify that we are giving an address information
        coordinatesParameters.add("q");
        coordinatesParameters.add("flags");
        coordinatesParameters.add("appid");
        featuresParameters.put(YAHOOMAP_COORDINATES, coordinatesParameters);
    }

    public YahooMapLibrary(PrologImplementor theProlog) {
        super(theProlog);
    }

    /**
     * This method defines a primitive that calls the Web service YahooMap
     * Geocode.
     * 
     * @param theListener
     *            is the SolutionListener used into the Prolog goal solving.
     * @param theGoalFrame
     *            is the GoalFrame, also used into the Prolog goal solving.
     * @param theBindings
     *            is the Bindings that contains all the Bindings concerned by
     *            the elements involved into the goal solving.
     * @param address
     *            is a Term corresponding to the address that will be used for
     *            that primitive (can be either a Var or a proper Prolog
     *            constant).
     * @param latitude
     *            is a Term corresponding to the latitude that will be used for
     *            that primitive (can be either a Var or a proper Prolog
     *            constant).
     * @param longitude
     *            is a Term corresponding to the longitude that will be used for
     *            that primitive (can be either a Var or a proper Prolog
     *            constant).
     * @param appId
     *            is also a Term corresponding to the appId that will be used
     *            for that primitive (can be either a Var or a proper Prolog
     *            constant).
     */
    @Primitive
    public void yahoomap(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term address,
            Term latitude, Term longitude, Term appId) {

        final Term addressValue = theBindings.focus(address, Term.class).getReferrer();
        final Term longitudeValue = theBindings.focus(longitude, Term.class).getReferrer();
        final Term latitudeValue = theBindings.focus(latitude, Term.class).getReferrer();
        final Term appIdValue = theBindings.focus(appId, Term.class).getReferrer();

        if (!(appIdValue instanceof Var)) {
            if (addressValue instanceof Var && (!(longitudeValue instanceof Var) && !(latitudeValue instanceof Var))) {
                yahoomapAddress(theListener, theGoalFrame, theBindings, addressValue, latitudeValue, longitudeValue,
                        appIdValue);
            } else if (!(addressValue instanceof Var)) {
                yahoomapCoordinates(theListener, theGoalFrame, theBindings, addressValue, latitudeValue,
                        longitudeValue, appIdValue);
            }
        }
    }

    /**
     * That method unifies the variables given into the parameters (among the
     * Terms) and then try to unify them by requesting the web service YahooMap
     * geocode. <br>
     * The service of that API targeted by this method is the service that takes
     * coordinates and then returns the corresponding address<br>
     * <br>
     * At that point the Terms given into the parameters have to respect some
     * constraints. The Terms <b>latitudeValue</b> and <b>longitudeValue</b>
     * have to be proper values (not Vars, but Atoms or TNumbers). <br>
     * On the other side, the parameter addressValue, can be either a Var or a
     * proper Atom, it doesn't matter. If it is not a Var, then the goal solver
     * will then try to unify with its corresponding value.
     * 
     * @param theListener
     * @param theGoalFrame
     * @param theBindings
     * @param addressValue
     *            is a Term that can be either a Var or an Atom
     * @param latitudeValue
     *            is a Term that has to be a proper value, so either an Atom or
     *            a TNumber.
     * @param longitudeValue
     *            is a Term that has to be a proper value, so either an Atom or
     *            a TNumber.
     * @param appIdValue
     *            has to be a proper value, which means a correct Atom.
     */
    public void yahoomapAddress(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings,
            Term addressValue, Term latitudeValue, Term longitudeValue, Term appIdValue) {
        String formatLatitude = FormatUtils.removeApices(getProlog().getFormatter().format(latitudeValue));
        String formatLongitude = FormatUtils.removeApices(getProlog().getFormatter().format(longitudeValue));
        String formatAppId = FormatUtils.removeApices(getProlog().getFormatter().format(appIdValue));

        Map<String, String> parameters = constructAddressRequestParameters(formatLatitude, formatLongitude, formatAppId);

        List<String> values = coordinatesToAddress(HttpUtils.buildHttpRequestFromService(serviceTrunkUrl
                + featureSuffixUrlMap.get(YAHOOMAP_ADDRESS), parameters));
        if (values.size() != 0) {
            if (addressValue instanceof Var) {
                for (int i = 0; i < values.size(); i++) {
                    Var[] addressVars = { (Var) addressValue };
                    Object[] valueTable = { values.get(i) };
                    unifyAndNotify(addressVars, valueTable, theBindings, theGoalFrame, theListener);
                }
            }
        }
    }

    /**
     * That method unifies the variables given into the parameters (among the
     * Terms) and then try to unify then by requesting the web service YahooMap
     * geocode. <br>
     * The service of that API targeted by this method is the service that takes
     * an address and then returns the corresponding coordinates (longitude and
     * latitude). <br>
     * <br>
     * At that point the Terms given into the parameters have to respect some
     * constraints. The Term <b>addressValue</b> has to be a proper value (not a
     * Var, but an Atom). <br>
     * On the other side, the two parameters latitudeValue and longitudeValue,
     * both Term can be either a Var, a proper Atom or a TNumber, it doesn't
     * matter. If it is not a Var, then the goal solver will then try to unify
     * with those correspondings values.
     * 
     * @param theListener
     * @param theGoalFrame
     * @param theBindings
     * @param addressValue
     *            is a Term that has to be in fact an Atom
     * @param latitudeValue
     *            is a Term that can be either a Var, an Atom or a TNumber,
     *            doesn't matter.
     * @param longitudeValue
     *            such as latitudeValue, it can be either a Var, an Atom, or a
     *            TNumber, doesn't matter.
     * @param appIdValue
     *            has to be a proper value, which means a correct Atom.
     */
    public void yahoomapCoordinates(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings,
            Term addressValue, Term latitudeValue, Term longitudeValue, Term appIdValue) {

        String formatAddress = FormatUtils.removeApices(getProlog().getFormatter().format(addressValue));
        String formatAppId = FormatUtils.removeApices(getProlog().getFormatter().format(appIdValue));
        Map<String, String> parameters = constructCoordinatesRequestParameters(formatAddress, formatAppId);
        List<String[]> values = addressToCoordinate(HttpUtils.buildHttpRequestFromService(serviceTrunkUrl
                + featureSuffixUrlMap.get(YAHOOMAP_ADDRESS), parameters));
        if (values.size() != 0) {
            for (int i = 0; i < values.size(); i++) {
                List<Var> vars = new ArrayList<Var>();
                List<Object> currentValue = new ArrayList<Object>();
                if (latitudeValue instanceof Var) {
                    vars.add((Var) latitudeValue);
                    currentValue.add(values.get(i)[COORDINATE_LATITUDE]);
                }
                if (longitudeValue instanceof Var) {
                    vars.add((Var) longitudeValue);
                    currentValue.add(values.get(i)[COORDINATE_LONGITUDE]);
                }

                if (vars.size() > 0) {
                    Var[] varTable = new Var[vars.size()];
                    for (int j = 0; j < vars.size(); j++)
                        varTable[j] = vars.get(j);
                    unifyAndNotify(varTable, currentValue.toArray(), theBindings, theGoalFrame, theListener);
                }
            }
        }
    }

    /**
     * This method builds a Map of indexed by Strings and for which the
     * contained object are string that are the value corresponding to the
     * fields given as indexes. <br>
     * <br>
     * The targeted service is geocode (Yahoo!Map), and the parameters are :
     * <ul>
     * <li>q : latitude,+longitude</li>
     * <li>flags : R</li>
     * <li>appid : appId</li>
     * </ul>
     * <br>
     * Here, "q", "flags" and "appid" are the indexes of the three elements that
     * will be contained in the Map. <i><b>e.g. : </b></i>
     * 
     * @param latitude
     *            is a String corresponding to the value of the latitude that
     *            will be used to search the address.
     * @param longitude
     *            is a String corresponding to the value of the longitude that
     *            will be used to search the address.
     * @param appId
     *            is a String corresponding to the appId used as a parameter to
     *            identify the application from which the WebService is called.
     * @return the corresponding Map<String, String> with the parameters needed
     *         to call the service and their corresponding values.
     */
    public Map<String, String> constructAddressRequestParameters(String latitude, String longitude, String appId) {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put(featuresParameters.get(YAHOOMAP_ADDRESS).get(0), latitude + ",+" + longitude);
        requestParameters.put(featuresParameters.get(YAHOOMAP_ADDRESS).get(1), "R");
        requestParameters.put(featuresParameters.get(YAHOOMAP_ADDRESS).get(2), appId);
        return requestParameters;
    }

    /**
     * This method builds a Map of indexed by Strings and for which the
     * contained object are string that are the value corresponding to the
     * fields given as indexes. <br>
     * <br>
     * The targeted service is geocode (Yahoo!Map), and the parameters are :
     * <ul>
     * <li>q : address</li>
     * <li>gflags : C</li>
     * <li>appid : appId</li>
     * </ul>
     * <br>
     * Here, "q", "gflags" and "appid" are the indexes of the three elements
     * that will be contained in the Map. <i><b>e.g. : </b></i>
     * 
     * @param latitude
     *            is a String corresponding to the value of the latitude that
     *            will be used to search the address.
     * @param address
     *            is a String corresponding to the value of the address of which
     *            the service will return the coordinates.
     * @param appId
     *            is a String corresponding to the appId used as a parameter to
     *            identify the application from which the WebService is called.
     * @return the corresponding Map<String, String> with the parameters needed
     *         to call the service and their corresponding values.
     */
    public Map<String, String> constructCoordinatesRequestParameters(String address, String appId) {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put(featuresParameters.get(YAHOOMAP_COORDINATES).get(0), address);
        requestParameters.put(featuresParameters.get(YAHOOMAP_COORDINATES).get(1), "C");
        requestParameters.put(featuresParameters.get(YAHOOMAP_COORDINATES).get(2), appId);
        return requestParameters;
    }

    /**
     * This method takes the url that will be used to call the service to obtain
     * the coordinates corresponding to the address given into the parameters of
     * the url.
     * 
     * @param fullUrl
     *            is a String containing the full url that will be used to call
     *            the service through an HTTP protocol.
     * @return a List of table of Strings containing the results of the service.
     *         Each table of String of the List contains 2 Strings. The first
     *         one (index=0) corresponds to a Latitude, and the second one to a
     *         longitude.
     */
    public static List<String[]> addressToCoordinate(String fullUrl) {
        List<String[]> result = new ArrayList<String[]>();
        Document doc = HttpUtils.responseToDocument(fullUrl);
        NodeList resultsFromService = doc.getFirstChild().getChildNodes();
        // for each funded coordinate.
        for (int i = 0; i < resultsFromService.getLength(); i++) {
            if (resultsFromService.item(i).getNodeName().equals("Result")) {
                NodeList currentResult = resultsFromService.item(i).getChildNodes();
                String latitude = "";
                String longitude = "";
                for (int j = 0; j < currentResult.getLength(); j++) {
                    if (currentResult.item(j).getNodeName().equals("latitude")) {
                        latitude = currentResult.item(j).getTextContent();
                    }
                    if (currentResult.item(j).getNodeName().equals("longitude")) {
                        longitude = currentResult.item(j).getTextContent();
                    }
                }
                String[] currentResultTable = new String[COORDINATE_RESULT_SIZE];
                currentResultTable[COORDINATE_LATITUDE] = latitude;
                currentResultTable[COORDINATE_LONGITUDE] = longitude;
                result.add(currentResultTable);
            }
        }
        return result;
    }

    /**
     * This method takes the url that will be used to call the service to obtain
     * the address corresponding to the coordinates (latitude and longitude)
     * given into the parameters within the url.
     * 
     * @param fullUrl
     *            is a String containing the full url that will be used to call
     *            the service through an HTTP protocol.
     * @return a List of Strings containing the results of the service. Each
     *         String of the List corresponds to a resulted address.
     */
    public static List<String> coordinatesToAddress(String fullUrl) {
        List<String> result = new ArrayList<String>();
        Document doc = HttpUtils.responseToDocument(fullUrl);
        NodeList resultsFromService = doc.getFirstChild().getChildNodes();
        // for each funded address.
        for (int i = 0; i < resultsFromService.getLength(); i++) {
            if (resultsFromService.item(i).getNodeName().equals("Result")) {
                NodeList currentResult = resultsFromService.item(i).getChildNodes();
                // prepare of the string variable of the current address
                String house = "";
                String street = "";
                String postal = "";
                String city = "";
                String country = "";
                for (int j = 0; j < currentResult.getLength(); j++) {
                    if (currentResult.item(j).getNodeName().equals("house")) {
                        house = currentResult.item(j).getTextContent();
                    }
                    if (currentResult.item(j).getNodeName().equals("street")) {
                        street = currentResult.item(j).getTextContent();
                    }
                    if (currentResult.item(j).getNodeName().equals("postal")) {
                        postal = currentResult.item(j).getTextContent();
                    }
                    if (currentResult.item(j).getNodeName().equals("city")) {
                        city = currentResult.item(j).getTextContent();
                    }
                    if (currentResult.item(j).getNodeName().equals("country")) {
                        country = currentResult.item(j).getTextContent();
                    }
                }
                String currentAddress = house;
                // That means that it is a non empty String which doesn't finish
                // with a blank character logically.
                if (currentAddress.length() > 0)
                    currentAddress += " ";
                currentAddress += street;
                if (currentAddress.length() > 0)
                    currentAddress += " ";
                currentAddress += postal;
                if (currentAddress.length() > 0)
                    currentAddress += " ";
                currentAddress += city;
                if (currentAddress.length() > 0)
                    currentAddress += " ";
                currentAddress += country;

                if (currentAddress.length() > 0) {
                    result.add(currentAddress);
                }
            }
        }
        return result;
    }
}
