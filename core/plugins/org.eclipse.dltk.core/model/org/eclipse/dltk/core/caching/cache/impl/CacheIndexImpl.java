/**
 * <copyright>
 * </copyright>
 *
 * $Id: CacheIndexImpl.java,v 1.1 2009/05/12 09:42:47 asobolev Exp $
 */
package org.eclipse.dltk.core.caching.cache.impl;

import java.util.Collection;

import org.eclipse.dltk.core.caching.cache.CacheEntry;
import org.eclipse.dltk.core.caching.cache.CacheIndex;
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
 * An implementation of the model object '<em><b>Index</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.dltk.core.caching.cache.impl.CacheIndexImpl#getLastIndex <em>Last Index</em>}</li>
 *   <li>{@link org.eclipse.dltk.core.caching.cache.impl.CacheIndexImpl#getEntries <em>Entries</em>}</li>
 *   <li>{@link org.eclipse.dltk.core.caching.cache.impl.CacheIndexImpl#getEnvironment <em>Environment</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class CacheIndexImpl extends MinimalEObjectImpl implements CacheIndex {
	/**
	 * The default value of the '{@link #getLastIndex() <em>Last Index</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLastIndex()
	 * @generated
	 * @ordered
	 */
	protected static final long LAST_INDEX_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getLastIndex() <em>Last Index</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLastIndex()
	 * @generated
	 * @ordered
	 */
	protected long lastIndex = LAST_INDEX_EDEFAULT;

	/**
	 * The cached value of the '{@link #getEntries() <em>Entries</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEntries()
	 * @generated
	 * @ordered
	 */
	protected EList<CacheEntry> entries;

	/**
	 * The default value of the '{@link #getEnvironment() <em>Environment</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEnvironment()
	 * @generated
	 * @ordered
	 */
	protected static final String ENVIRONMENT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getEnvironment() <em>Environment</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEnvironment()
	 * @generated
	 * @ordered
	 */
	protected String environment = ENVIRONMENT_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected CacheIndexImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return CachePackage.Literals.CACHE_INDEX;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public long getLastIndex() {
		return lastIndex;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setLastIndex(long newLastIndex) {
		long oldLastIndex = lastIndex;
		lastIndex = newLastIndex;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, CachePackage.CACHE_INDEX__LAST_INDEX, oldLastIndex, lastIndex));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<CacheEntry> getEntries() {
		if (entries == null) {
			entries = new EObjectContainmentEList<CacheEntry>(CacheEntry.class, this, CachePackage.CACHE_INDEX__ENTRIES);
		}
		return entries;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getEnvironment() {
		return environment;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setEnvironment(String newEnvironment) {
		String oldEnvironment = environment;
		environment = newEnvironment;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, CachePackage.CACHE_INDEX__ENVIRONMENT, oldEnvironment, environment));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case CachePackage.CACHE_INDEX__ENTRIES:
				return ((InternalEList<?>)getEntries()).basicRemove(otherEnd, msgs);
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
			case CachePackage.CACHE_INDEX__LAST_INDEX:
				return getLastIndex();
			case CachePackage.CACHE_INDEX__ENTRIES:
				return getEntries();
			case CachePackage.CACHE_INDEX__ENVIRONMENT:
				return getEnvironment();
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
			case CachePackage.CACHE_INDEX__LAST_INDEX:
				setLastIndex((Long)newValue);
				return;
			case CachePackage.CACHE_INDEX__ENTRIES:
				getEntries().clear();
				getEntries().addAll((Collection<? extends CacheEntry>)newValue);
				return;
			case CachePackage.CACHE_INDEX__ENVIRONMENT:
				setEnvironment((String)newValue);
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
			case CachePackage.CACHE_INDEX__LAST_INDEX:
				setLastIndex(LAST_INDEX_EDEFAULT);
				return;
			case CachePackage.CACHE_INDEX__ENTRIES:
				getEntries().clear();
				return;
			case CachePackage.CACHE_INDEX__ENVIRONMENT:
				setEnvironment(ENVIRONMENT_EDEFAULT);
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
			case CachePackage.CACHE_INDEX__LAST_INDEX:
				return lastIndex != LAST_INDEX_EDEFAULT;
			case CachePackage.CACHE_INDEX__ENTRIES:
				return entries != null && !entries.isEmpty();
			case CachePackage.CACHE_INDEX__ENVIRONMENT:
				return ENVIRONMENT_EDEFAULT == null ? environment != null : !ENVIRONMENT_EDEFAULT.equals(environment);
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
		result.append(" (lastIndex: ");
		result.append(lastIndex);
		result.append(", environment: ");
		result.append(environment);
		result.append(')');
		return result.toString();
	}

} //CacheIndexImpl
