/**
 * <copyright>
 * </copyright>
 *
 * $Id: CachePackageImpl.java,v 1.2 2009/05/19 09:04:45 asobolev Exp $
 */
package org.eclipse.dltk.core.caching.cache.impl;

import org.eclipse.dltk.core.caching.cache.CacheEntry;
import org.eclipse.dltk.core.caching.cache.CacheEntryAttribute;
import org.eclipse.dltk.core.caching.cache.CacheFactory;
import org.eclipse.dltk.core.caching.cache.CacheIndex;
import org.eclipse.dltk.core.caching.cache.CachePackage;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.emf.ecore.impl.EPackageImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class CachePackageImpl extends EPackageImpl implements CachePackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass cacheEntryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass cacheEntryAttributeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass cacheIndexEClass = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see org.eclipse.dltk.core.caching.cache.CachePackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private CachePackageImpl() {
		super(eNS_URI, CacheFactory.eINSTANCE);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 * 
	 * <p>This method is used to initialize {@link CachePackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static CachePackage init() {
		if (isInited) return (CachePackage)EPackage.Registry.INSTANCE.getEPackage(CachePackage.eNS_URI);

		// Obtain or create and register package
		CachePackageImpl theCachePackage = (CachePackageImpl)(EPackage.Registry.INSTANCE.get(eNS_URI) instanceof CachePackageImpl ? EPackage.Registry.INSTANCE.get(eNS_URI) : new CachePackageImpl());

		isInited = true;

		// Create package meta-data objects
		theCachePackage.createPackageContents();

		// Initialize created meta-data
		theCachePackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theCachePackage.freeze();

  
		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(CachePackage.eNS_URI, theCachePackage);
		return theCachePackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getCacheEntry() {
		return cacheEntryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getCacheEntry_Path() {
		return (EAttribute)cacheEntryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getCacheEntry_Timestamp() {
		return (EAttribute)cacheEntryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getCacheEntry_Attributes() {
		return (EReference)cacheEntryEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getCacheEntry_LastAccessTime() {
		return (EAttribute)cacheEntryEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getCacheEntryAttribute() {
		return cacheEntryAttributeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getCacheEntryAttribute_Name() {
		return (EAttribute)cacheEntryAttributeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getCacheEntryAttribute_Location() {
		return (EAttribute)cacheEntryAttributeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getCacheIndex() {
		return cacheIndexEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getCacheIndex_LastIndex() {
		return (EAttribute)cacheIndexEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getCacheIndex_Entries() {
		return (EReference)cacheIndexEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getCacheIndex_Environment() {
		return (EAttribute)cacheIndexEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public CacheFactory getCacheFactory() {
		return (CacheFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		cacheEntryEClass = createEClass(CACHE_ENTRY);
		createEAttribute(cacheEntryEClass, CACHE_ENTRY__PATH);
		createEAttribute(cacheEntryEClass, CACHE_ENTRY__TIMESTAMP);
		createEReference(cacheEntryEClass, CACHE_ENTRY__ATTRIBUTES);
		createEAttribute(cacheEntryEClass, CACHE_ENTRY__LAST_ACCESS_TIME);

		cacheEntryAttributeEClass = createEClass(CACHE_ENTRY_ATTRIBUTE);
		createEAttribute(cacheEntryAttributeEClass, CACHE_ENTRY_ATTRIBUTE__NAME);
		createEAttribute(cacheEntryAttributeEClass, CACHE_ENTRY_ATTRIBUTE__LOCATION);

		cacheIndexEClass = createEClass(CACHE_INDEX);
		createEAttribute(cacheIndexEClass, CACHE_INDEX__LAST_INDEX);
		createEReference(cacheIndexEClass, CACHE_INDEX__ENTRIES);
		createEAttribute(cacheIndexEClass, CACHE_INDEX__ENVIRONMENT);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes

		// Initialize classes and features; add operations and parameters
		initEClass(cacheEntryEClass, CacheEntry.class, "CacheEntry", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getCacheEntry_Path(), ecorePackage.getEString(), "path", null, 0, 1, CacheEntry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCacheEntry_Timestamp(), ecorePackage.getELong(), "timestamp", null, 0, 1, CacheEntry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getCacheEntry_Attributes(), this.getCacheEntryAttribute(), null, "attributes", null, 0, -1, CacheEntry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCacheEntry_LastAccessTime(), ecorePackage.getELong(), "lastAccessTime", "0", 0, 1, CacheEntry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(cacheEntryAttributeEClass, CacheEntryAttribute.class, "CacheEntryAttribute", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getCacheEntryAttribute_Name(), ecorePackage.getEString(), "name", null, 0, 1, CacheEntryAttribute.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCacheEntryAttribute_Location(), ecorePackage.getEString(), "location", null, 0, 1, CacheEntryAttribute.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(cacheIndexEClass, CacheIndex.class, "CacheIndex", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getCacheIndex_LastIndex(), ecorePackage.getELong(), "lastIndex", null, 0, 1, CacheIndex.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getCacheIndex_Entries(), this.getCacheEntry(), null, "entries", null, 0, -1, CacheIndex.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCacheIndex_Environment(), ecorePackage.getEString(), "environment", null, 0, 1, CacheIndex.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Create resource
		createResource(eNS_URI);
	}

} //CachePackageImpl
