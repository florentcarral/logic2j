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

import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.PrologImpl.InitLevel;
import org.logic2j.library.impl.core.CoreLibrary;
import org.logic2j.theory.DefaultTheoryManager;
import org.logic2j.theory.TheoryContent;
import org.logic2j.theory.TheoryManager;

/**
 */
public class TheoryTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TheoryTest.class);

  @Override
  protected InitLevel initLevel() {
    return InitLevel.L0_BARE;
  }

  @Test
  public void testLoadTheory() {
    final TheoryManager theoryManager = new DefaultTheoryManager(getProlog());
    final TheoryContent content = theoryManager.load(new CoreLibrary(getProlog()));
    logger.info("Loaded theory: {}", content);
  }

}
