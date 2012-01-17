/**
 * 
 */
package org.logic2j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.logic2j.model.symbol.Struct;

/**
 * @author Vincent Berthet
 */
public class ClauseProviderResolver {

    private static final List<ClauseProvider> EMPTY = Collections.emptyList();
    private final HashMap<String, List<ClauseProvider>> register = new HashMap<String, List<ClauseProvider>>();

    public void register(String predicateKey, ClauseProvider provider) {
        List<ClauseProvider> providers = register.get(predicateKey);
        if (providers == null) {
            providers = new ArrayList<ClauseProvider>();
            register.put(predicateKey, providers);
            providers.add(provider);
        } else if (!providers.contains(provider)) {
            providers.add(provider);
        }
    }

    public Iterable<ClauseProvider> find(Struct struct) {
        List<ClauseProvider> list = register.get(struct.getPredicateIndicator());
        if (list == null) list = register.get(struct.getName());
        return list == null ? EMPTY : list;
    }
}
