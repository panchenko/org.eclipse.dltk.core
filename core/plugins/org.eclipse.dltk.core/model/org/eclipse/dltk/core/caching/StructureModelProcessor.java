package org.eclipse.dltk.core.caching;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.dltk.compiler.IElementRequestor.ElementInfo;
import org.eclipse.dltk.compiler.IElementRequestor.FieldInfo;
import org.eclipse.dltk.compiler.IElementRequestor.ImportInfo;
import org.eclipse.dltk.compiler.IElementRequestor.MethodInfo;
import org.eclipse.dltk.compiler.IElementRequestor.TypeInfo;
import org.eclipse.dltk.compiler.ISourceElementRequestor;

public class StructureModelProcessor extends AbstractDataLoader implements
		IStructureConstants {

	private ISourceElementRequestor requestor;

	public StructureModelProcessor(InputStream stream,
			ISourceElementRequestor requestor) {
		super(stream);
		this.requestor = requestor;
	}

	public void perform() throws IOException {
		readStrings();
		readData();
	}

	private void readData() {
		while (true) {
			try {
				int tag = in.readInt();
				switch (tag) {
				case TAG_FIELD_REFERENCE:
					acceptFieldReference();
					break;
				case TAG_METHOD_REF1:
					acceptMethodReference();
					break;
				case TAG_PACKAGE:
					acceptPackage();
					break;
				case TAG_TYPE_REFERENCE1:
					acceptTypeReference1();
					break;
				case TAG_TYPE_REFERENCE2:
					acceptTypeReference2();
					break;
				case TAG_ENTER_FIELD:
					enterField();
					break;
				case TAG_ENTER_FIELD_DUPL:
					enterFieldCheckDuplicates();
					break;
				case TAG_ENTER_FIELD_WITH_PARENT:
					enterFieldWithParentType();
					break;
				case TAG_ENTER_METHOD:
					enterMethod();
					break;
				case TAG_ENTER_METHOD_REMOVE_SAME:
					enterMethodRemoveSame();
					break;
				case TAG_ENTER_METHOD_WITH_PARENT:
					enterMethodWithParentType();
					break;
				case TAG_ENTER_MODULE:
					enterModule();
					break;
				case TAG_ENTER_MODULE_ROOT:
					enterModuleRoot();
					break;
				case TAG_ENTER_TYPE:
					enterType();
					break;
				case TAG_ENTER_TYPE_APPEND:
					enterTypeAppend();
					break;
				case TAG_EXIT_FIELD:
					exitField();
					break;
				case TAG_EXIT_METHOD:
					exitMethod();
					break;
				case TAG_EXIT_MODULE:
					exitModule();
					break;
				case TAG_EXIT_MODULE_ROOT:
					exitModuleRoot();
					break;
				case TAG_EXIT_TYPE:
					exitType();
					break;
				case TAG_ACCEPT_IMPORT:
					acceptImport();
					break;
				}
			} catch (EOFException e) {
				break;

			} catch (IOException e) {
				break;
			}
		}

	}

	private char[] readDataString() throws IOException {
		String text = readString();
		if (text == null) {
			return null;
		}
		return text.toCharArray();
	}

	private char[][] readDataStrings() throws IOException {
		int size = in.readInt();
		if (size == 0) {
			return null;
		}

		char[][] result = new char[size][];
		for (int i = 0; i < size; ++i) {
			result[i] = readDataString();
		}
		return result;
	}

	private String[] readDataStringsStr() throws IOException {
		int size = in.readInt();
		if (size == 0) {
			return null;
		}

		String[] result = new String[size];
		for (int i = 0; i < size; ++i) {
			result[i] = readString();
		}
		return result;
	}

	public void acceptFieldReference() {
		try {
			String fieldName = readString();
			int sourcePosition = in.readInt();
			this.requestor.acceptFieldReference(fieldName, sourcePosition);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void acceptMethodReference() {
		try {
			String methodName = readString();
			int argCount = in.readInt();
			int sourcePosition = in.readInt();
			int sourceEndPosition = in.readInt();
			this.requestor.acceptMethodReference(methodName, argCount,
					sourcePosition, sourceEndPosition);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void acceptPackage() {
		try {
			String name = readString();
			int declarationStart = in.readInt();
			int declarationEnd = in.readInt();
			this.requestor
					.acceptPackage(declarationStart, declarationEnd, name);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void acceptTypeReference1() {
		try {
			/* char[][] typeName = */readDataStrings();
			/* int sourceStart = */in.readInt();
			/* int sourceEnd = */in.readInt();
			// this.requestor.acceptTypeReference(typeName, sourceStart,
			// sourceEnd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void acceptTypeReference2() {
		try {
			String typeName = readString();
			int sourcePosition = in.readInt();
			this.requestor.acceptTypeReference(typeName, sourcePosition);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void enterField() {
		try {
			FieldInfo info = new FieldInfo();
			readFieldInfo(info);
			this.requestor.enterField(info);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readFieldInfo(FieldInfo info) throws IOException {
		readElementInfo(info);
	}

	private void readMethodInfo(MethodInfo info) throws IOException {
		readElementInfo(info);
		info.parameterNames = readDataStringsStr();
		info.parameterInitializers = readDataStringsStr();
		info.exceptionTypes = readDataStringsStr();
		info.isConstructor = in.readBoolean();
	}

	private void readTypeInfo(TypeInfo info) throws IOException {
		readElementInfo(info);
		info.superclasses = readDataStringsStr();
	}

	private void readElementInfo(ElementInfo info) throws IOException {
		info.name = readString();
		info.modifiers = in.readInt();
		info.nameSourceStart = in.readInt();
		info.nameSourceEnd = in.readInt();
		info.declarationStart = in.readInt();
	}

	public boolean enterFieldCheckDuplicates() {
		try {
			FieldInfo info = new FieldInfo();
			readFieldInfo(info);
			boolean result = in.readBoolean();
			this.requestor.enterFieldCheckDuplicates(info);
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean enterFieldWithParentType() {
		boolean result = false;
		try {
			FieldInfo info = new FieldInfo();
			readFieldInfo(info);
			/* String parentName = */readString();
			/* String delimiter = */readString();
			in.readBoolean();
			// requestor.enterFieldWithParentType(info, parentName, delimiter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public void enterMethod() {
		try {
			MethodInfo info = new MethodInfo();
			readMethodInfo(info);
			this.requestor.enterMethod(info);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void enterMethodRemoveSame() {
		try {
			MethodInfo info = new MethodInfo();
			readMethodInfo(info);
			this.requestor.enterMethodRemoveSame(info);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean enterMethodWithParentType() {
		boolean result = false;
		try {
			MethodInfo info = new MethodInfo();
			readMethodInfo(info);
			/* String parentName = */readString();
			/* String delimiter = */readString();
			// result = this.requestor.enterMethodWithParentType(info,
			// parentName,
			// delimiter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public void enterModule() {
		this.requestor.enterModule();
	}

	public void enterModuleRoot() {
		this.requestor.enterModuleRoot();
	}

	public void enterType() {
		try {
			TypeInfo info = new TypeInfo();
			readTypeInfo(info);
			this.requestor.enterType(info);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean enterTypeAppend() {
		try {
			String fullName = readString();
			String delimiter = readString();
			return this.requestor.enterTypeAppend(fullName, delimiter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void exitField() {
		try {
			this.requestor.exitField(in.readInt());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exitMethod() {
		try {
			this.requestor.exitMethod(in.readInt());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exitModule() {
		try {
			this.requestor.exitModule(in.readInt());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exitModuleRoot() {
		this.requestor.exitModuleRoot();
	}

	public void exitType() {
		try {
			this.requestor.exitType(in.readInt());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void acceptImport() {
		try {
			ImportInfo importInfo = new ImportInfo();
			importInfo.sourceStart = in.readInt();
			importInfo.sourceEnd = in.readInt();
			importInfo.containerName = readString();
			importInfo.name = readString();
			importInfo.version = readString();
			this.requestor.acceptImport(importInfo);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
