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
package org.logic2j.model.var;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.logic2j.model.BaseTermVisitor;
import org.logic2j.model.TermVisitor;
import org.logic2j.model.exception.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.symbol.Var;
import org.logic2j.util.ReflectUtils;

/**
 * Store the actual values of all variables of a {@link Term}, as a list of {@link Binding}s, 
 * one per {@link Var}iable found within it.<br/>
 * Usually the {@link Term} is a {@link Struct} that represents a goal to be demonstrated or 
 * unified. The Term referring to this object is called the "referrer".<br/>
 * 
 * TODO Improve performance: instantiation of {@link #Bindings(Term)}. 
 * Find a better way than runtime instantiation.
 */
public class Bindings {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Bindings.class);
  private static boolean isDebug = logger.isDebugEnabled();

  private static final TermApi TERM_API = new TermApi();

  /**
   * The Term, usually a {@link Struct}, whose {@link Var}iables refer to this Bindings
   * through their indexes.
   */
  private final Term referrer;

  /**
   * All {@link Binding}s, one per instance of {@link Var}iable.
   * There are as many bindings as the distinct number of variables in 
   * the referrer Term, i.e. the length of bindings equals the maximum
   * of all indexes in all {@link Var}s of the referrer, plus one.
   * See also {@link Var#getIndex()}.
   * This array is never null, but may be empty (length=0) when the
   * referrer Term does not contain any {@link Var}iable.
   */
  private Binding[] bindings;

  /**
   * Determine how free (unbound) variables will be represented in resulting bindings
   * returned by {@link Bindings#explicitBindings(FreeVarRepresentation)}.
   */
  public enum FreeVarRepresentation {
    /**
     * Free variables will not be included in result bindings. Asking the value of 
     * a variable that is not bound to a literal Term is likely to throw a {@link RuntimeException}.
     */
    SKIPPED,

    /**
     * Free variables will be represented by the existence of a Map {@link Entry} with 
     * a valid key, and a value equal to null. You are required to use {@link Map#containsKey(Object)}
     * to identify this case, since {@link Map#get(Object)} returning null won't allow you to distinguish between
     * a free variable (asking for "X" when X is still unbound) or asking the value of an undefined variable
     * (asking "TOTO" when not in original goal).
     */
    NULL,

    /**
     * Free variables will be reported with their terminal value. If we have the chain of bindings being
     * X -> Y -> Z, this mode will represent X with the mapping "X"->"Z". 
     * A particular case is when X is unified to itself (via a loop), then the mapping becomes "X" -> "X" 
     * which may not be desireable... See FREE_NOT_SELF.
     */
    FREE,

    /**
     * TBD. 
     */
    FREE_NOT_SELF

  }

  /**
   * A {@link TermVisitor} used to assign a reference to the original {@link Var}iable into a {@link Binding}.
   */
  private static class SetVarInBindingVisitor extends BaseTermVisitor<Var> {

    private Binding binding;
    private int index;

    SetVarInBindingVisitor(Binding theBinding, int theIndex) {
      this.binding = theBinding;
      this.index = theIndex;
    }

    @SuppressWarnings("synthetic-access")
    @Override
    public Var visit(Var theVar) {
      if (theVar.getIndex() == index) {
        binding.setVar(theVar);
        // Returning non-null will stop useless recursion
        return theVar;
      }
      return null;
    }

  }

  /**
   * Instantiate a Bindings to hold all variables of a given {@link Term}, named
   * further the "referrer", which is ususally a {@link Struct}.
   * @param theReferrer The Term whose {@link Var}iables refer to this object.
   * @see Bindings#getReferrer() to further access theTerm
   */
  public Bindings(Term theReferrer) {
    // Check arguments
    //
    // Note: this constructor should be called only with Var or Struct as arguments, but we don't check this. Should we?
    final short index = theReferrer.getIndex();
    if (index == Term.NO_INDEX) {
      throw new InvalidTermException("Cannot create Bindings for uninitialized Term " + theReferrer);
    }
    this.referrer = theReferrer;
    // Determine number of distinct variables
    final int nbVars;
    if (theReferrer instanceof Var) {
      if (((Var) theReferrer).isAnonymous()) {
        nbVars = 0;
      } else {
        nbVars = 1;
      }
    } else {
      // Will be a Struct; in that case the index is the number of bindings
      nbVars = index;
    }
    //
    // Allocate and initialize all bindings
    //
    this.bindings = new Binding[nbVars];
    for (int i = 0; i < nbVars; i++) {
      final int varIndex = i; // Need a final var for visitor subclass
      final Binding binding = new Binding();
      this.bindings[varIndex] = binding;
      // Assign Binding.var field 
      // TODO This is costly see https://github.com/ltettoni/logic2j/issues/26
      theReferrer.accept(new SetVarInBindingVisitor(binding, varIndex));
    }
  }

  /**
   * Copy (cloning) constructor, used for efficiency since the original one
   * needs to a complete traversal of the term.<br/>
   * The referrer of the new Bindings is the same as theOriginal.
   * @param theOriginal The one to clone from, remains intact.
   */
  public Bindings(Bindings theOriginal) {
    this.referrer = theOriginal.referrer;
    final int nbVars = theOriginal.bindings.length;
    this.bindings = new Binding[nbVars];
    // All bindings need cloning
    for (int i = 0; i < nbVars; i++) {
      this.bindings[i] = theOriginal.bindings[i].cloneIt();
    }
  }


  /**
   * PROTOTYPE
   * @param theOriginal
   * @param theTerm
   */
  private Bindings(Bindings theOriginal, Term theTerm) {
    this.referrer = theTerm;
    // 
//    this.bindings = theOriginal.bindings;
    // Cloning - not necessary??
    final int nbVars = theOriginal.bindings.length;
    this.bindings = new Binding[nbVars];
    // All bindings need cloning
    for (int i = 0; i < nbVars; i++) {
      this.bindings[i] = theOriginal.bindings[i].cloneIt();
    }
  }

  //---------------------------------------------------------------------------
  // Methods for extracting values from variable Bindings
  //---------------------------------------------------------------------------
  
  /**
   * Refocus to a particular Term within this {@link Bindings}, returning a new {@link Bindings} with
   * this Term as its referrer. When Term is a {@link Var}iable, will following through bound variables
   * until a free or literal is found.
   * @param theTerm Must be one of the root or sub terms that was used to instantiate this {@link Bindings}
   * @param theClass
   * @return null if theTerm was a free {@link Var}iable
   */
  public Bindings focus(Term theTerm, Class<? extends Term> theClass) {
    if (theTerm instanceof Var) {
      final Var origin = (Var)theTerm;
      if (origin.isAnonymous()) {
        return null;
      }
      // Go to fetch the effective variable value if any
      final Binding finalBinding = origin.bindingWithin(this).followLinks();
      if (finalBinding.getType()==BindingType.LIT) {
        return new Bindings(finalBinding.getLiteralBindings(), finalBinding.getTerm());
      } else if (finalBinding.getType()==BindingType.FREE) {
        // Refocus on original var (we now know it is free), keep the same original bindings
        return new Bindings(this, origin);
      } else {
        throw new IllegalStateException("Should not have been here");
      }
    }
    // Anything else than a Var
    
    // Make sure it's of the desired class
    ReflectUtils.safeCastNotNull("obtaining resolved term", theTerm, theClass);
    return new Bindings(this, theTerm);
  }

  
  
  /**
   * Considering this object's current bindings as a snapshot to a solution, extract
   * the content of the variables and their bound values in a safe place (a Map) so that
   * inference can resume towards other solutions.
   * @param theRepresentation How to represent free (non-ground) variables
   * @return All variable bindings resolved, with representation as specified for free bindings.
   */
  public Map<String, Term> explicitBindings(FreeVarRepresentation theRepresentation) {
    // For every Binding in this object, identify to which Var it finally refers (following linked bindings)
    // ending up with either null (on a literal), or a real Var (on a free var).
    final IdentityHashMap<Binding, Var> bindingToVar = new IdentityHashMap<Binding, Var>();
    for (Binding binding : this.bindings) {
      // Follow linked bindings
      final Binding finalBinding = binding.followLinks();
      // At this stage finalBinding may be either literal, or free
      bindingToVar.put(finalBinding, binding.getVar());
    }
    
    final Map<String, Term> result = new TreeMap<String, Term>();
    for (Binding binding : this.bindings) {
      final Var originalVar = binding.getVar();
      if (originalVar == null) {
        throw new IllegalStateException("Bindings not properly initialized: Binding " + binding
            + " does not refer to Var (null)");
      }
      // The original name of our variable
      final String originalVarName = originalVar.getName();
      // Now reach the effective lastest binding
      binding = binding.followLinks();
      final Var finalVar = binding.getVar();
      switch (binding.getType()) {
        case LIT:
          if (originalVarName == null) {
            throw new IllegalStateException("Cannot assign null (undefined) var, not all variables of " + this
                + " are referenced from Term " + this.referrer + ", binding " + binding + " can't be assigned a variable name");
          }
          final Term boundTerm = binding.getTerm();
          final Term substitute = TERM_API.substitute(boundTerm, binding.getLiteralBindings(), bindingToVar);
          // Literals are not unbound terms, they are returned the same way for all types of representations asked
          result.put(originalVarName, substitute);
          break;
        case FREE:
          switch (theRepresentation) {
            case SKIPPED:
              // Nothing added to the resulting bindings: no Map entry (no key, no value)
              break;
            case NULL:
              // Add one entry with null value
              result.put(originalVarName, null);
              break;
            case FREE:
              result.put(originalVarName, finalVar);
              break;
            case FREE_NOT_SELF:
              if (originalVar.getName()!=finalVar.getName()) {
                // Avoid reporting "X=null" for free variables or "X=X" as a binding...
                result.put(originalVarName, finalVar);
              }
              break;
          }
          break;
        case LINK:
          throw new IllegalStateException("Should not happen we have followed links already");
      }
    }
    return result;
  }

  /**
   * Find the local bindings corresponding to one of the variables of the
   * Struct referred to by this Bindings.
   * TODO This method is only used once from a library - ensure it makes sense and belongs here
   * @param theVar
   * @return null when not found
   */
  public Bindings findBindings(Var theVar) {
    // Search root level
    int index = 0;
    for (Binding binding : this.bindings) {
      // FIXME: dubious use of == instead of static equality
      if (binding.getVar() == theVar && index == theVar.getIndex()) {
        return this;
      }
      index++;
    }
    // Not found: search deeper through bindings
    for (Binding binding : this.bindings) {
      if (binding.getType() == BindingType.LIT) {
        Bindings foundDeeper = binding.getLiteralBindings().findBindings(theVar);
        if (foundDeeper != null) {
          return foundDeeper;
        }
      }
    }
    // Not found
    return null;
  }

  /**
   * @return true if this {@link Bindings}'s {@link #getReferrer()} is a free variable.
   */
  public boolean isFreeReferrer() {
    if (! (this.referrer instanceof Var)) {
      return false;
    }
    return ((Var)this.referrer).bindingWithin(this).followLinks().isFree();
  }
  
  //---------------------------------------------------------------------------
  // Methods of java.lang.Object
  //---------------------------------------------------------------------------
  
  @Override
  public String toString() {
    final String address = isDebug ? ('@' + Integer.toHexString(super.hashCode())) : "";
    if (getSize()==0) {
      return this.getClass().getSimpleName() + address + "(empty)";
    }
    return this.getClass().getSimpleName() + address + Arrays.asList(this.bindings);
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  /**
   * @return The number of {@link Binding}s held in this object, corresponds to the
   * number of distinct variables in {@link #getReferrer()}.
   */
  public int getSize() {
    return this.bindings.length;
  }

  /**
   * @param theIndex
   * @return The {@link Binding} at theIndex.
   */
  public Binding getBinding(short theIndex) {
    return this.bindings[theIndex];
  }

  /**
   * @return The Term whose variable values are held in this structure, 
   * actually the one that was provided to the consturctor {@link #Bindings(Term)}.
   */
  public Term getReferrer() {
    return this.referrer;
  }

}
