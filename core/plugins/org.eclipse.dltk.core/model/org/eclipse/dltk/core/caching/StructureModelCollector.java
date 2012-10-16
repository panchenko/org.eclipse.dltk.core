package org.eclipse.dltk.core.caching;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.dltk.compiler.ISourceElementRequestor;
import org.eclipse.dltk.core.DLTKCore;

public class StructureModelCollector extends AbstractDataSaver implements
		ISourceElementRequestor, IStructureConstants {

	private ISourceElementRequestor baseRequestor;

	public StructureModelCollector(ISourceElementRequestor requestor) {
		this.baseRequestor = requestor;
	}

	/**
	 * @since 2.0
	 */
	protected void writeTag(int tag) throws IOException {
		out.writeInt(tag);
	}

	private void writeString(String[] strs) throws IOException {
		if (strs == null) {
			out.writeInt(0);
		} else {
			out.writeInt(strs.length);
			for (int i = 0; i < strs.length; i++) {
				writeString(strs[i]);
			}
		}
	}

	/**
	 * @since 2.0
	 */
	public void acceptFieldReference(String fieldName, int sourcePosition) {
		this.baseRequestor.acceptFieldReference(fieldName, sourcePosition);
		try {
			writeTag(TAG_FIELD_REFERENCE);
			writeString(fieldName);
			out.writeInt(sourcePosition);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @since 2.0
	 */
	public void acceptMethodReference(String methodName, int argCount,
			int sourcePosition, int sourceEndPosition) {
		this.baseRequestor.acceptMethodReference(methodName, argCount,
				sourcePosition, sourceEndPosition);
		try {
			writeTag(TAG_METHOD_REF1);
			writeString(methodName);
			out.writeInt(argCount);
			out.writeInt(sourcePosition);
			out.writeInt(sourceEndPosition);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @since 2.0
	 */
	public void acceptPackage(int declarationStart, int declarationEnd,
			String name) {
		this.baseRequestor
				.acceptPackage(declarationStart, declarationEnd, name);
		try {
			writeTag(TAG_PACKAGE);
			writeString(name);
			out.writeInt(declarationStart);
			out.writeInt(declarationEnd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void acceptTypeReference(String typeName, int sourcePosition) {
		this.baseRequestor.acceptTypeReference(typeName, sourcePosition);
		try {
			writeTag(TAG_TYPE_REFERENCE2);
			writeString(typeName);
			out.writeInt(sourcePosition);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @since 2.0
	 */
	public void enterField(FieldInfo info) {
		this.baseRequestor.enterField(info);
		try {
			writeTag(TAG_ENTER_FIELD);
			writeFieldInfo(info);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeFieldInfo(FieldInfo info) throws IOException {
		writeElementInfo(info);
	}

	private void writeMethodInfo(MethodInfo info) throws IOException {
		writeElementInfo(info);
		writeString(info.parameterNames);
		writeString(info.parameterInitializers);
		writeString(info.exceptionTypes);
		out.writeBoolean(info.isConstructor);
	}

	private void writeTypeInfo(TypeInfo info) throws IOException {
		writeElementInfo(info);
		writeString(info.superclasses);
	}

	private void writeElementInfo(ElementInfo info) throws IOException {
		writeString(info.name);
		out.writeInt(info.modifiers);
		out.writeInt(info.nameSourceStart);
		out.writeInt(info.nameSourceEnd);
		out.writeInt(info.declarationStart);
	}

	/**
	 * @since 2.0
	 */
	public boolean enterFieldCheckDuplicates(FieldInfo info) {
		boolean result = this.baseRequestor.enterFieldCheckDuplicates(info);
		try {
			writeTag(TAG_ENTER_FIELD_DUPL);
			writeFieldInfo(info);
			out.writeBoolean(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * @since 2.0
	 */
	public void enterMethod(MethodInfo info) {
		this.baseRequestor.enterMethod(info);
		try {
			writeTag(TAG_ENTER_METHOD);
			writeMethodInfo(info);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @since 2.0
	 */
	public void enterMethodRemoveSame(MethodInfo info) {
		this.baseRequestor.enterMethodRemoveSame(info);
		try {
			writeTag(TAG_ENTER_METHOD_REMOVE_SAME);
			writeMethodInfo(info);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void enterModule() {
		this.baseRequestor.enterModule();
		try {
			writeTag(TAG_ENTER_MODULE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void enterModuleRoot() {
		this.baseRequestor.enterModuleRoot();
		try {
			writeTag(TAG_ENTER_MODULE_ROOT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @since 2.0
	 */
	public void enterType(TypeInfo info) {
		this.baseRequestor.enterType(info);
		try {
			writeTag(TAG_ENTER_TYPE);
			writeTypeInfo(info);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean enterTypeAppend(String fullName, String delimiter) {
		boolean result = this.baseRequestor
				.enterTypeAppend(fullName, delimiter);
		try {
			writeTag(TAG_ENTER_TYPE_APPEND);
			writeString(fullName);
			writeString(delimiter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public void exitField(int declarationEnd) {
		this.baseRequestor.exitField(declarationEnd);
		try {
			writeTag(TAG_EXIT_FIELD);
			out.writeInt(declarationEnd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exitMethod(int declarationEnd) {
		this.baseRequestor.exitMethod(declarationEnd);
		try {
			writeTag(TAG_EXIT_METHOD);
			out.writeInt(declarationEnd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exitModule(int declarationEnd) {
		this.baseRequestor.exitModule(declarationEnd);
		try {
			writeTag(TAG_EXIT_MODULE);
			out.writeInt(declarationEnd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exitModuleRoot() {
		this.baseRequestor.exitModuleRoot();
		try {
			writeTag(TAG_EXIT_MODULE_ROOT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exitType(int declarationEnd) {
		this.baseRequestor.exitType(declarationEnd);
		try {
			writeTag(TAG_EXIT_TYPE);
			out.writeInt(declarationEnd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @since 2.0
	 */
	public void acceptImport(ImportInfo importInfo) {
		this.baseRequestor.acceptImport(importInfo);
		try {
			writeTag(TAG_ACCEPT_IMPORT);
			out.writeInt(importInfo.sourceStart);
			out.writeInt(importInfo.sourceEnd);
			writeString(importInfo.containerName);
			writeString(importInfo.name);
			writeString(importInfo.version);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void enterNamespace(String[] namespace) {
		this.baseRequestor.enterNamespace(namespace);
		try {
			writeTag(TAG_ENTER_NAMESPACE);
			writeString(namespace);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exitNamespace() {
		this.baseRequestor.exitNamespace();
		try {
			writeTag(TAG_EXIT_NAMESPACE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public byte[] getBytes() {
		final ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			saveTo(stream);
		} catch (IOException e) {
			if (DLTKCore.DEBUG) {
				e.printStackTrace();
			}
		}
		return stream.toByteArray();
	}
}
