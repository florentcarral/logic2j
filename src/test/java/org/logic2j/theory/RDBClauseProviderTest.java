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
package org.logic2j.theory;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.PrologWithDataSourcesTestBase;
import org.logic2j.library.impl.rdb.RDBBase;
import org.logic2j.model.symbol.Struct;

public class RDBClauseProviderTest extends PrologWithDataSourcesTestBase {
    private RDBClauseProvider provider;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.provider = new RDBClauseProvider(getProlog(), zipcodesDataSource());
    }

    @Test
    public void test_getConnection() throws SQLException {
        assertNotNull(zipcodesConnection());
    }

    @Test
    public void listMatchingClauses() throws IOException {
        getProlog().getTheoryManager().addTheory(
                getProlog().getTheoryManager().load(new File("src/test/resources/test-config.pl")));
        //assertNotNull(this.provider);
        final Struct theGoal = new Struct("zipcodesdb_zip_code", "Zip", "Lat");
        getProlog().getClauseProviders().get(1).listMatchingClauses(theGoal, null);
    }

    @Test
    public void listMatchingClausesWithSpecialTransformer() throws IOException {
        //assertNotNull(this.provider);
        getProlog().getTheoryManager().addTheory(getProlog().getTheoryManager().load(new File("src/test/resources/test-config.pl")));
        final Struct theGoal = new Struct("zipcodesdb_zip_code", "Zip", "Lat");
        ((RDBClauseProvider) getProlog().getClauseProviders().get(1)).setTermFactory(new RDBBase.AllStringsAsAtoms(getProlog()));
        getProlog().getClauseProviders().get(1).listMatchingClauses(theGoal, /* No vars in theGoal */null);
    }

    @Test
    public void matchClausesFromProlog() throws IOException {
        // When loading a theory with an initialize predicate containing a
        // rdb_config predicate. This one will create new RDBClauseProviders
        // linked with the appropriate datasource.
        getProlog().getTheoryManager().addTheory(
                getProlog().getTheoryManager().load(new File("src/test/resources/test-config.pl")));

        // getProlog().getClauseProviders().add(this.provider);
        // Matching all
        assertNSolutions(79991, "zipcodesdb_zip_code(_, _)");
        assertNSolutions(79991, "zipcodesdb_zip_code(X, _)");
        assertNSolutions(79991, "zipcodesdb_zip_code(_, Y)");
        assertNSolutions(79991, "zipcodesdb_zip_code(X, Y)");
        // Match on first argument
        assertNSolutions(0, "zipcodesdb_zip_code('90008', 1300.123123)");
        assertNSolutions(4, "zipcodesdb_zip_code('90008', _)");
        assertNSolutions(4, "zipcodesdb_zip_code('90008', Y)");
        assertNSolutions(4, "Z='90008', Y=dummy, zipcodesdb_zip_code(Z, _)");
        assertNoSolution("Y=1300.123123, zipcodesdb_zip_code('90008', Y)");
        assertNoSolution("Y=1300.123123, Z=other, zipcodesdb_zip_code('90008', Y)");
        assertNSolutions(4, "Z=dummy, zipcodesdb_zip_code('90008', Y)");
        assertNoSolution("zipcodesdb_zip_code('90008', Y), Y=1300.123123");
        // Match on second argument
        assertNSolutions(79, "zipcodesdb_zip_code(_, 34.0)");
        assertNSolutions(79, "zipcodesdb_zip_code(X, 34.0)");
        assertNoSolution("X=dummy, zipcodesdb_zip_code(X, 34.0)");
        assertNoSolution("zipcodesdb_zip_code(X, 34.0), X=dummy");
        // Match on both arguments
        assertNSolutions(4, "zipcodesdb_zip_code('90008', 34.0)");
        // Match on list testing
        assertNSolutions(0, "zipcodesdb_zip_code(['90008',dummy], Y)");
        assertNoSolution("Y=[1300.123123,34.0], zipcodesdb_zip_code('90008', Y)");
        // NO matches
        assertNoSolution("zipcodesdb_zip_code('00000', 0)");
        assertNoSolution("zipcodesdb_zip_code('90008', 0)");
        assertNoSolution("zipcodesdb_zip_code('00000', 34.0)");
        assertNoSolution("zipcodesdb_zip_code(X, X)");
    }

}