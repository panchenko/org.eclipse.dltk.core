/**
 * <copyright>
 * </copyright>
 *
 * $Id: CacheEntryImpl.java,v 1.2 2009/05/19 09:04:45 asobolev Exp $
 */
package org.eclipse.dltk.core.caching.cache.impl;

import java.util.Collection;

import org.eclipse.dltk.core.caching.cache.CacheEntry;
import org.eclipse.dltk.core.caching.cache.CacheEntryAttribute;
import org.eclipse.dltk.core.caching.cache.CachePackage;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Entry</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.dltk.core.caching.cache.impl.CacheEntryImpl#getPath <em>Path</em>}</li>
 *   <li>{@link org.eclipse.dltk.core.caching.cache.impl.CacheEntryImpl#getTimestamp <em>Timestamp</em>}</li>
 *   <li>{@link org.eclipse.dltk.core.caching.cache.impl.CacheEntryImpl#getAttributes <em>Attributes</em>}</li>
 *   <li>{@link org.eclipse.dltk.core.caching.cache.impl.CacheEntryImpl#getLastAccessTime <em>Last Access Time</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class CacheEntryImpl extends MinimalEObjectImpl implements CacheEntry {
	/**
	 * The default value of the '{@link #getPath() <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPath()
	 * @generated
	 * @ordered
	 */
	protected static final String PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPath() <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPath()
	 * @generated
	 * @ordered
	 */
	protected String path = PATH_EDEFAULT;

	/**
	 * The default value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected static final long TIMESTAMP_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected long timestamp = TIMESTAMP_EDEFAULT;

	/**
	 * The cached value of the '{@link #getAttributes() <em>Attributes</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAttributes()
	 * @generated
	 * @ordered
	 */
	protected EList<CacheEntryAttribute> attributes;

	/**
	 * The default value of the '{@link #getLastAccessTime() <em>Last Access Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLastAccessTime()
	 * @generated
	 * @ordered
	 */
	protected static final long LAST_ACCESS_TIME_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getLastAccessTime() <em>Last Access Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLastAccessTime()
	 * @generated
	 * @ordered
	 */
	protected long lastAccessTime = LAST_ACCESS_TIME_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected CacheEntryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return CachePackage.Literals.CACHE_ENTRY;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getPath() {
		return path;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPath(String newPath) {
		String oldPath = path;
		path = newPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, CachePackage.CACHE_ENTRY__PATH, oldPath, path));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTimestamp(long newTimestamp) {
		long oldTimestamp = timestamp;
		timestamp = newTimestamp;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, CachePackage.CACHE_ENTRY__TIMESTAMP, oldTimestamp, timestamp));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<CacheEntryAttribute> getAttributes() {
		if (attributes == null) {
			attributes = new EObjectContainmentEList<CacheEntryAttribute>(CacheEntryAttribute.class, this, CachePackage.CACHE_ENTRY__ATTRIBUTES);
		}
		return attributes;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public long getLastAccessTime() {
		return lastAccessTime;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setLastAccessTime(long newLastAccessTime) {
		long oldLastAccessTime = lastAccessTime;
		lastAccessTime = newLastAccessTime;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, CachePackage.CACHE_ENTRY__LAST_ACCESS_TIME, oldLastAccessTime, lastAccessTime));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case CachePackage.CACHE_ENTRY__ATTRIBUTES:
				return ((InternalEList<?>)getAttributes()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case CachePackage.CACHE_ENTRY__PATH:
				return getPath();
			case CachePackage.CACHE_ENTRY__TIMESTAMP:
				return getTimestamp();
			case CachePackage.CACHE_ENTRY__ATTRIBUTES:
				return getAttributes();
			case CachePackage.CACHE_ENTRY__LAST_ACCESS_TIME:
				return getLastAccessTime();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case CachePackage.CACHE_ENTRY__PATH:
				setPath((String)newValue);
				return;
			case CachePackage.CACHE_ENTRY__TIMESTAMP:
				setTimestamp((Long)newValue);
				return;
			case CachePackage.CACHE_ENTRY__ATTRIBUTES:
				getAttributes().clear();
				getAttributes().addAll((Collection<? extends CacheEntryAttribute>)newValue);
				return;
			case CachePackage.CACHE_ENTRY__LAST_ACCESS_TIME:
				setLastAccessTime((Long)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case CachePackage.CACHE_ENTRY__PATH:
				setPath(PATH_EDEFAULT);
				return;
			case CachePackage.CACHE_ENTRY__TIMESTAMP:
				setTimestamp(TIMESTAMP_EDEFAULT);
				return;
			case CachePackage.CACHE_ENTRY__ATTRIBUTES:
				getAttributes().clear();
				return;
			case CachePackage.CACHE_ENTRY__LAST_ACCESS_TIME:
				setLastAccessTime(LAST_ACCESS_TIME_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case CachePackage.CACHE_ENTRY__PATH:
				return PATH_EDEFAULT == null ? path != null : !PATH_EDEFAULT.equals(path);
			case CachePackage.CACHE_ENTRY__TIMESTAMP:
				return timestamp != TIMESTAMP_EDEFAULT;
			case CachePackage.CACHE_ENTRY__ATTRIBUTES:
				return attributes != null && !attributes.isEmpty();
			case CachePackage.CACHE_ENTRY__LAST_ACCESS_TIME:
				return lastAccessTime != LAST_ACCESS_TIME_EDEFAULT;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (path: ");
		result.append(path);
		result.append(", timestamp: ");
		result.append(timestamp);
		result.append(", lastAccessTime: ");
		result.append(lastAccessTime);
		result.append(')');
		return result.toString();
	}

} //CacheEntryImpl
