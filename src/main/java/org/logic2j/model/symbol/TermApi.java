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

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

import org.logic2j.TermFactory;
import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.library.mgmt.LibraryContent;
import org.logic2j.model.exception.InvalidTermException;
import org.logic2j.model.var.Binding;
import org.logic2j.model.var.Bindings;
import org.logic2j.util.ReflectUtils;

/**
 * External facade API to the {@link Term} hierarchy.
 *
 */
public class TermApi {

  public TermApi() {
    super();
  }

  Collection<Term> flatTerms(Term theTerm) {
    Collection<Term> theFlatTerms = new ArrayList<Term>(20);
    theTerm.flattenTerms(theFlatTerms);
    return theFlatTerms;
  }

  /**
   * Compact a term, this means recursively traversing the term structure
   * and assigning any duplicates substructures to the same references.
   * @param theTerm
   * @return The compacted term, may be equal to the argument.
   */
  // TODO Rename to "factorize", since we merge common factors
  Term compact(Term theTerm) {
    return theTerm.compact(flatTerms(theTerm));
  }

  void avoidCycle(Struct theClause) {
    List<Term> visited = new ArrayList<Term>(20);
    theClause.avoidCycle(visited);
  }

  short assignVarOffsets(Term theTerm) {
    return theTerm.assignVarOffsets((short) 0);
  }

  /**
   * Entry point for normalizing terms before they can be used for inference.
   * Must exclusively be called from {@link TermFactory#normalize(Term)}, with the exception of test cases.
   * @param theTerm
   * @param theLibraryContent
   * @return A normalized copy of theTerm
   */
  public Term normalize(Term theTerm, LibraryContent theLibraryContent) {
    final Term compacted = compact(theTerm);
    assignVarOffsets(compacted);
    if (theTerm instanceof Struct && theLibraryContent != null) {
      assignPrimitiveInfo((Struct) compacted, theLibraryContent);
    }
    return compacted;
  }

  /**
   * Substitute, recursively, any bound {@link Var}s to their actual values.
   * This delegates the call to {@link Term#substitute(Bindings, IdentityHashMap)}. 
   * @param theTerm
   * @param theBindings
   * @param theBindingsToVars
   * @return An equivalent Term with all bound variables pointing to literals, this implies a deep
   * cloning of substructures that contain variables. When no variables are bound, then the
   * same refernce is returned.
   * Important note: the caller cannot know if the returned reference was cloned or not, so it must never mutate it!
   */
  public Term substitute(Term theTerm, final Bindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars)
      throws InvalidTermException {
    if ((theTerm instanceof Struct && theTerm.index == 0) || theBindings.getSize()==0) {
      // No variables identified in the term, or no variables passed as argument: do not need to substitute
      return theTerm;
    }
    // Delegate to actual subclass
    return theTerm.substitute(theBindings, theBindingsToVars);
  }

  /**
   * @param theStruct
   * @param theLib2Content
   */
  // TODO Move assignPrimitiveInfo() to LibraryManager
  private void assignPrimitiveInfo(Struct theStruct, LibraryContent theLib2Content) {
    theStruct.assignPrimitiveInfo(theLib2Content);
  }

  /**
   * Lowest level to factorize an Object into a simple {@link Term}, this can create an atomic
   * Struct, but not parse a prolog term into a compound Struct. For parsing, you must use the
   * {@link TermFactory}.
   * @param theObject
   * @param theMode
   * @return An instance of a subclass of {@link Term}.
   * @throws InvalidTermException
   */
  public Term valueOf(Object theObject, FactoryMode theMode) throws InvalidTermException {
    if (theObject == null) {
      throw new InvalidTermException("Cannot create Term from null argument");
    }
    final Term term;
    if (theObject instanceof Term) {
      term = (Term) theObject;
    } else if (theObject instanceof Integer) {
      term = new TLong((Integer) theObject);
    } else if (theObject instanceof Long) {
      term = new TLong((Long) theObject);
    } else if (theObject instanceof Double) {
      term = new TDouble((Double) theObject);
    } else if (theObject instanceof Float) {
      term = new TDouble((Float) theObject);
    } else if (theObject instanceof Boolean) {
      term = (Boolean) theObject ? Struct.ATOM_TRUE : Struct.ATOM_FALSE;
    } else if (theObject instanceof CharSequence || theObject instanceof Character) {
      // Rudimentary parsing
      final String chars = theObject.toString();
      if (theMode == FactoryMode.ATOM) {
        // Anything becomes an atom, actually only a Struct since we don't have powerful parsing here
        term = new Struct(chars);
      } else {
        if (Var.ANONYMOUS_VAR_NAME.equals(chars)) {
          term = Var.ANONYMOUS_VAR;
        } else if (chars.isEmpty()) {
          // Dubious for real programming, but data sources may contain empty fields, and this is the only way to represent them as a Term
          term = new Struct("");
        } else if (Character.isUpperCase(chars.charAt(0)) || chars.startsWith(Var.ANONYMOUS_VAR_NAME)) {
          term = new Var(chars);
        } else {
          // Otherwise it's an atom
          term = new Struct(chars);
        }
      }
    } else {
      throw new InvalidTermException("Cannot create Term from '" + theObject + "' of " + theObject.getClass());
    }
    return term;
  }

  /**
   * @param theTPathExpression
   * @param theClass 
   */
  // TODO Should this go to TermFactory - since we return a new Term
  @SuppressWarnings("unchecked")
  public <T extends Term> T selectTerm(Term theTerm, String theTPathExpression, Class<T> theClass) {
    if (theTPathExpression.isEmpty()) {
      return ReflectUtils.safeCastNotNull("selecting term", theTerm, theClass);
    }
    if (theTerm instanceof Struct) {
      final Struct s = (Struct) theTerm;
      int position = 0;
      String level0 = theTPathExpression;
      int end = theTPathExpression.length();
      final int slash = theTPathExpression.indexOf('/');
      if (slash >= 1) {
        end = slash;
        level0 = theTPathExpression.substring(0, slash);
        position = 1;
      }
      String functor = level0;
      final int par = level0.indexOf('[');
      if (par >= 0) {
        end = max(par, end);
        functor = level0.substring(0, par);
        if (!level0.endsWith("]")) {
          throw new InvalidTermException("Malformed TPath expresson: \"" + theTPathExpression + "\": missing ending ']'");
        }
        position = Integer.parseInt(level0.substring(par + 1, level0.length() - 1));
        if (position <= 0) {
          throw new InvalidTermException("Index " + position + " in \"" + theTPathExpression + "\" is <=0");
        }
        if (position > s.getArity()) {
          throw new InvalidTermException("Index " + position + " in \"" + theTPathExpression + "\" is > arity of " + s.getArity());
        }
      }
      // In case functor was defined ("f[n]", since the expression "[n]" without f is also allowed)
      if (!functor.isEmpty()) {
        // Make sure the root name matches the struct at level 0
        if (!s.getName().equals(functor)) {
          throw new InvalidTermException("Term \"" + theTerm + "\" does not start with functor  \"" + functor + '"');
        }
      }
      if (position >= 1) {
        String levelsTail = theTPathExpression.substring(min(theTPathExpression.length(), end + 1));
        return selectTerm(s.getArg(position - 1), levelsTail, theClass);
      }
      if (!(theClass.isAssignableFrom(theTerm.getClass()))) {
        throw new ClassCastException("Cannot extract Term of " + theClass + " at expression=" + theTPathExpression + " from "
            + theTerm);
      }
      return (T) theTerm;
    }
    throw new IllegalArgumentException("Cannot extract \"" + theTPathExpression + "\" from " + theTerm);
  }

}
