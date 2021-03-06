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
package org.logic2j.model.symbol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Test;
import org.logic2j.Prolog;
import org.logic2j.PrologImpl;
import org.logic2j.PrologImpl.InitLevel;
import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.model.exception.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.TLong;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.Bindings;

/**
 * Low-level tests of the {@link TermApi} facade.
 * 
 */
public class TermApiTest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TermApiTest.class);

  private final Prolog prolog = new PrologImpl(InitLevel.L0_BARE);
  private static final TermApi TERM_API = new TermApi();

  @Test
  public void test_staticallyEquals() {
    // Vars are never statically equal ...
    assertFalse(new Var("X").staticallyEquals(new Var("Y")));
    Var x1 = new Var("X");
    Var x2 = new Var("X");
    // ... even when they have the same name
    assertFalse(x1.staticallyEquals(x2));
    Struct s = new Struct("s", x1, x2);
    assertFalse(s.getArg(0).staticallyEquals(s.getArg(1)));
    // After compacting, the 2 X will be same
    Struct s2 = (Struct) TERM_API.compact(s);
    assertNotSame(s, s2);
    assertFalse(s.staticallyEquals(s2));
    assertTrue(s2.getArg(0).staticallyEquals(s2.getArg(1)));
  }

  // TODO Check this more carefully, see https://github.com/ltettoni/logic2j/issues/13
  @Test
  public void test_staticallyEquals2() {
    //    assertTrue(new Var().staticallyEquals(new Var()));
    //    assertTrue(new Var().staticallyEquals(new Var("_")));
    //    assertFalse(new Var("X").staticallyEquals(new Var()));
    //    assertFalse(new Var().staticallyEquals(new Var("X")));
    //    assertFalse(new Var("X").staticallyEquals(new Var("Y")));
  }

  @Test
  public void test_flatTerms() {
    Term term;
    //
    term = new Struct("p", "X", 2);
    logger.info("Flat terms: {}", TERM_API.flatTerms(term));
    //
    term = new Struct("a", new Struct("b"), "c");
    logger.info("Flat terms: {}", TERM_API.flatTerms(term));
    //
    term = new Struct(Struct.FUNCTOR_CLAUSE, new Struct("a", new Struct("p", "X", "Y")), new Struct("p", "X", "Y"));
    logger.info("Flat terms: {}", TERM_API.flatTerms(term));
    //
    Term clause = new Struct(Struct.FUNCTOR_CLAUSE, new Struct("a", new Struct("p", "X", "Y")), new Struct("p", "X", "Y"));
    logger.info("Flat terms of original {}", TERM_API.flatTerms(clause));
    Term t2 = TERM_API.normalize(clause, null);
    logger.info("Found {} bindings", t2.getIndex());
    assertEquals(2, t2.getIndex());
    logger.info("Flat terms of copy     {}", TERM_API.flatTerms(t2));
    assertEquals(clause.toString(), t2.toString());
  }

  @Test
  public void test_assignVarOffsets() {
    int nbVars;
    nbVars = TERM_API.assignVarOffsets(new TLong(2));
    assertEquals(0, nbVars);
    nbVars = TERM_API.assignVarOffsets(new Struct("f"));
    assertEquals(0, nbVars);
    nbVars = TERM_API.assignVarOffsets(new Var("X"));
    assertEquals(1, nbVars);
    nbVars = TERM_API.assignVarOffsets(new Var("_"));
    assertEquals(0, nbVars);
  }

  // FIXME: works fine when run alone, or with class, but fails when running all test cases at the same time!
  @Test
  public void test_substitute() {
    try {
      Term v = Var.ANONYMOUS_VAR;
      TERM_API.substitute(v, new Bindings(v), null);
      fail();
    } catch (InvalidTermException e) {
      // Expected to happen
    }

    Term a = this.prolog.term("a");
    // Empty binding yields same term since no bindings to resolve
    assertSame(a, TERM_API.substitute(a, new Bindings(a), null));

    // Bindings without
    TERM_API.substitute(a, new Bindings(a), null);

    Term x = this.prolog.term("X");
    TERM_API.substitute(x, new Bindings(x), null);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void test_avoidCycle() {
    Struct cyclic = new Struct("s", Struct.createEmptyPList());
    cyclic.setArg(0, cyclic);
    try {
      cyclic.avoidCycle(new ArrayList<Term>());
      fail("Should have thrown an IllegalStateException");
    } catch (IllegalStateException e) {
      // OK
    }
  }

  @Test
  public void test_selections() {
    Term term = this.prolog.getTermFactory().create("a(b(c,c2),b2)", FactoryMode.ANY_TERM);
    //
    assertSame(term, TERM_API.selectTerm(term, "", Struct.class));
    assertSame(term, TERM_API.selectTerm(term, "a", Struct.class));
    try {
      TERM_API.selectTerm(term, "a[-1]", Struct.class);
      fail("Should fail");
    } catch (InvalidTermException e) {
      // OK
    }
    //
    try {
      TERM_API.selectTerm(term, "a[0]", Struct.class);
      fail("Should fail");
    } catch (InvalidTermException e) {
      // OK
    }
    //
    try {
      TERM_API.selectTerm(term, "a[4]", Struct.class);
      fail("Should fail");
    } catch (InvalidTermException e) {
      // OK
    }
    //
    try {
      TERM_API.selectTerm(term, "z", Struct.class);
      fail("Should fail");
    } catch (InvalidTermException e) {
      // OK
    }
    //
    assertSame(((Struct) term).getArg(0), TERM_API.selectTerm(term, "a/", Struct.class));
    assertSame(((Struct) term).getArg(0), TERM_API.selectTerm(term, "a[1]", Struct.class));
    assertSame(((Struct) term).getArg(0), TERM_API.selectTerm(term, "[1]", Struct.class));
    assertSame(((Struct) term).getArg(0), TERM_API.selectTerm(term, "a/b", Struct.class));
    assertSame(((Struct) term).getArg(0), TERM_API.selectTerm(term, "a[1]/b", Struct.class));
    assertEquals(new Struct("b2"), TERM_API.selectTerm(term, "a[2]", Struct.class));
    assertEquals(new Struct("b2"), TERM_API.selectTerm(term, "a[2]/b2", Struct.class));
    assertSame(((Struct) ((Struct) term).getArg(0)).getArg(0), TERM_API.selectTerm(term, "a/b/c", Struct.class));
    assertSame(((Struct) ((Struct) term).getArg(0)).getArg(0), TERM_API.selectTerm(term, "a/b[1]", Struct.class));
    assertSame(((Struct) ((Struct) term).getArg(0)).getArg(0), TERM_API.selectTerm(term, "a/[1]", Struct.class));
    assertEquals(new Struct("c2"), TERM_API.selectTerm(term, "a/b[2]", Struct.class));
  }
}
