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
package org.logic2j.library.mgmt;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.logic2j.LibraryManager;
import org.logic2j.Prolog;
import org.logic2j.library.PLibrary;
import org.logic2j.library.mgmt.PrimitiveInfo.PrimitiveType;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.Bindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;
import org.logic2j.theory.TheoryManager;
import org.logic2j.util.ReflectUtils;

/**
 */
public class DefaultLibraryManager implements LibraryManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultLibraryManager.class);

  /**
   * SolutionListener, GoalFrame, Bindings, Term, ...
   */
  private static final int NB_EXTRA_PARAMS = 3;

  // TODO Should this be a PrologImplementor instead of Prolog? If so it allows moving Prolog.getLibararyManager to PrologImplementor.
  private final Prolog prolog;

  private LibraryContent wholeContent = new LibraryContent();

  private Map<Class<? extends PLibrary>, PLibrary> libraries = new HashMap<Class<? extends PLibrary>, PLibrary>();

  /**
   * @param theProlog
   */
  public DefaultLibraryManager(Prolog theProlog) {
    this.prolog = theProlog;
  }

  @Override
  public LibraryContent loadLibrary(PLibrary theLibrary) {
    if (alreadyLoaded(theLibrary)) {
      return this.wholeContent;
    }
    final LibraryContent loadedContent = loadLibraryInternal(theLibrary);
    updateWholeContent(loadedContent);
    // Load the theory text associated to the library
    final TheoryManager theoryManager = this.prolog.getTheoryManager();
    theoryManager.addTheory(theoryManager.load(theLibrary));
    return loadedContent;
  }

  /**
   * @param theLibrary
   * @return True when one library of the same class was already loaded in this engine.
   */
  private boolean alreadyLoaded(PLibrary theLibrary) {
    return this.libraries.containsKey(theLibrary.getClass());
  }

  private void updateWholeContent(LibraryContent loadedContent) {
    this.wholeContent.addAll(loadedContent);
    // Houston we have a problem - we need to reassign our primitives upon loading libs!
    Struct.ATOM_TRUE.assignPrimitiveInfo(this.wholeContent);
    Struct.ATOM_FALSE.assignPrimitiveInfo(this.wholeContent);
    Struct.ATOM_CUT.assignPrimitiveInfo(this.wholeContent);
  }

  private LibraryContent loadLibraryInternal(PLibrary theLibrary) {
    final LibraryContent content = new LibraryContent();
    logger.debug("Loading new library {}", theLibrary);
    final Class<? extends PLibrary> libraryClass = theLibrary.getClass();

    // Load all annotated methods
    for (Method method : libraryClass.getMethods()) {
      final Primitive annotation = method.getAnnotation(Primitive.class);
      if (annotation != null) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        final Class<?> returnType = method.getReturnType();
        final PrimitiveType type;
        if (Void.class.equals(returnType) || Void.TYPE.equals(returnType)) {
          type = PrimitiveType.PREDICATE;
        } else if (Term.class.equals(returnType)) {
          type = PrimitiveType.FUNCTOR;
        } else if (Void.TYPE.equals(returnType)) {
          type = PrimitiveType.DIRECTIVE;
        } else {
          throw new IllegalStateException("Unexpected return type " + returnType.getName() + " for primitive " + annotation);
        }

        final int nbMethodParams = paramTypes.length;
        int i = 0;
        if (!(SolutionListener.class.isAssignableFrom(paramTypes[i]))) {
          throw new IllegalStateException("Argument type at index " + i + " of metohd " + method + " not of proper "
              + SolutionListener.class);
        }
        i++;
        if (!(GoalFrame.class.isAssignableFrom(paramTypes[i]))) {
          throw new IllegalStateException("Argument type at index " + i + " of metohd " + method + " not of proper "
              + GoalFrame.class);
        }
        i++;
        if (!(Bindings.class.isAssignableFrom(paramTypes[i]))) {
          throw new IllegalStateException("Argument type at index " + i + " of metohd " + method + " not of proper "
              + Bindings.class);
        }
        i++;
        boolean varargs = false;
        if (i < nbMethodParams) {
          if (Term[].class.isAssignableFrom(paramTypes[i])) {
            varargs = true;
          } else {
            while (i < nbMethodParams) {
              if (!(Term.class.isAssignableFrom(paramTypes[i]))) {
                throw new IllegalStateException("Argument type at index " + i + " of metohd " + method + " not of proper "
                    + Term.class);
              }
              i++;
            }
          }
        }
        // Main name (default = method's name) for the primitive
        String primitiveName = annotation.name();
        if (primitiveName == null || primitiveName.isEmpty()) {
          primitiveName = method.getName();
        }
        final String arityIndicator = varargs ? VARARG_ARITY_INDICATOR : Integer.toString(nbMethodParams - NB_EXTRA_PARAMS);
        final String key1 = primitiveName + '/' + arityIndicator;
        final PrimitiveInfo desc = new PrimitiveInfo(type, theLibrary, primitiveName, method, varargs);
        content.putPrimitive(key1, desc);

        // All other accepted synonyms for this primitive
        for (String synonym : annotation.synonyms()) {
          final String key2 = synonym + '/' + arityIndicator;
          final PrimitiveInfo desc2 = new PrimitiveInfo(type, theLibrary, primitiveName, method, varargs);
          content.putPrimitive(key2, desc2);
        }
      }
    }
    this.libraries.put(libraryClass, theLibrary);
    return content;
  }

  /**
   * @return The whole libraries content.
   */
  @Override
  public LibraryContent wholeContent() {
    return this.wholeContent;
  }

  @Override
  public <T extends PLibrary> T getLibrary(Class<T> theClass) {
    final PLibrary lib = this.libraries.get(theClass);
    final T pLibrary = ReflectUtils.safeCastNotNull("obtaining library of " + theClass, lib, theClass);
    if (pLibrary == null) {
      throw new IllegalArgumentException("No library bound of " + theClass);
    }
    return pLibrary;
  }

}
