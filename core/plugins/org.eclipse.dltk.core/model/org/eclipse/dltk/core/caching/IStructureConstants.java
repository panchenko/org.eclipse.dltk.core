package org.eclipse.dltk.core.caching;

public interface IStructureConstants {
	static final int TAG_FIELD_REFERENCE = 0;
	static final int TAG_METHOD_REF1 = 1;
	static final int TAG_PACKAGE = 2;
	@Deprecated
	static final int TAG_TYPE_REFERENCE1 = 3;
	static final int TAG_TYPE_REFERENCE2 = 4;
	static final int TAG_ENTER_FIELD = 5;
	static final int TAG_ENTER_FIELD_DUPL = 6;
	@Deprecated
	static final int TAG_ENTER_FIELD_WITH_PARENT = 7;
	static final int TAG_ENTER_METHOD = 8;
	static final int TAG_ENTER_METHOD_REMOVE_SAME = 9;
	@Deprecated
	static final int TAG_ENTER_METHOD_WITH_PARENT = 10;
	static final int TAG_ENTER_MODULE = 11;
	static final int TAG_ENTER_MODULE_ROOT = 12;
	static final int TAG_ENTER_TYPE = 13;
	static final int TAG_ENTER_TYPE_APPEND = 14;
	static final int TAG_EXIT_FIELD = 15;
	static final int TAG_EXIT_METHOD = 16;
	static final int TAG_EXIT_MODULE = 17;
	static final int TAG_EXIT_MODULE_ROOT = 18;
	static final int TAG_EXIT_TYPE = 19;
	/**
	 * @since 2.0
	 */
	static final int TAG_ACCEPT_IMPORT = 20;

	int TAG_ENTER_NAMESPACE = 21;
	int TAG_EXIT_NAMESPACE = 22;

	/**
	 * @since 3.0
	 */
	@Deprecated
	static final int TAG_UPDATE_FIELD = 23;

}
