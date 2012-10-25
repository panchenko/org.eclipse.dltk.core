package org.eclipse.dltk.core.model.binary;

import org.eclipse.dltk.core.INamespace;
import org.eclipse.dltk.internal.core.ModelElementInfo;

/**
 * @since 2.0
 */
class BinaryMemberInfo extends ModelElementInfo {
	private int flags;

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public int getFlags() {
		return flags;
	}

	private INamespace namespace;

	public INamespace getNamespace() {
		return namespace;
	}

	public void setNamespace(INamespace namespace) {
		this.namespace = namespace;
	}
}
