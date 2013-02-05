/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.core.tests;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.dltk.core.tests.builder.BuildParticipantManagerTests;
import org.eclipse.dltk.core.tests.buildpath.BuildpathTests;
import org.eclipse.dltk.core.tests.buildpath.SetContainerEventsTest;
import org.eclipse.dltk.core.tests.cache.CacheTests;
import org.eclipse.dltk.core.tests.cache.SourceModuleInfoCacheTest;
import org.eclipse.dltk.core.tests.compiler.CompilerCharOperationTests;
import org.eclipse.dltk.core.tests.compiler.CompilerUtilTests;
import org.eclipse.dltk.core.tests.ddp.CoreDDPTests;
import org.eclipse.dltk.core.tests.launching.EnvironmentResolverTests;
import org.eclipse.dltk.core.tests.launching.InterpreterConfigTests;
import org.eclipse.dltk.core.tests.mixin.MixinIndexTests;
import org.eclipse.dltk.core.tests.mixin.MixinModelTests;
import org.eclipse.dltk.core.tests.model.BufferTests;
import org.eclipse.dltk.core.tests.model.ModelMembersTests;
import org.eclipse.dltk.core.tests.model.NamespaceTests;
import org.eclipse.dltk.core.tests.model.WorkingCopyTests;
import org.eclipse.dltk.core.tests.parser.SourceParserTests;
import org.eclipse.dltk.core.tests.search.Bug387751Test;
import org.eclipse.dltk.core.tests.util.CharacterStackTests;
import org.eclipse.dltk.core.tests.utils.CharOperationTests;
import org.eclipse.dltk.core.tests.utils.IntListTests;
import org.eclipse.dltk.core.tests.utils.InternalCoreUtilTest;
import org.eclipse.dltk.core.tests.utils.TextUtilsTest;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("org.eclipse.dltk.core.tests.model");
		// $JUnit-BEGIN$
		suite.addTest(new TestSuite(BuildParticipantManagerTests.class));
		suite.addTest(BuildpathTests.suite());

		suite.addTest(new TestSuite(CacheTests.class));

		suite.addTest(new TestSuite(CompilerUtilTests.class));
		suite.addTest(new TestSuite(CompilerCharOperationTests.class));
		suite.addTestSuite(IntListTests.class);

		suite.addTest(CoreDDPTests.suite());

		suite.addTest(EnvironmentResolverTests.suite());
		suite.addTest(InterpreterConfigTests.suite());

		suite.addTest(new TestSuite(MixinIndexTests.class));
		suite.addTest(new TestSuite(MixinModelTests.class));

		suite.addTest(BufferTests.suite());
		suite.addTest(ModelMembersTests.suite());
		suite.addTestSuite(NamespaceTests.class);
		suite.addTest(WorkingCopyTests.suite());

		suite.addTest(new TestSuite(SourceParserTests.class));

		suite.addTestSuite(CharacterStackTests.class);

		suite.addTest(CharOperationTests.suite());
		suite.addTest(new TestSuite(InternalCoreUtilTest.class));
		suite.addTest(TextUtilsTest.suite());
		suite.addTest(new JUnit4TestAdapter(Bug387751Test.class));
		suite.addTest(new JUnit4TestAdapter(SourceModuleInfoCacheTest.class));
		suite.addTest(new JUnit4TestAdapter(SetContainerEventsTest.class));
		// $JUnit-END$
		return suite;
	}
}
