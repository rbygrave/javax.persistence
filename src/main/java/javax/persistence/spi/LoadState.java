/*
 * Copyright (c) 2008, 2009, 2011 Oracle, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.  The Eclipse Public License is available
 * at http://www.eclipse.org/legal/epl-v10.html and the Eclipse Distribution License
 * is available at http://www.eclipse.org/org/documents/edl-v10.php.
 */
package javax.persistence.spi;

/**
 * Load states returned by the {@link ProviderUtil} SPI methods.
 */
public enum LoadState 
{
	/**
	 * The state of the element is known to have been loaded.
	 */
	LOADED,
	/**
	 * The state of the element is known not to have been loaded.
	 */
	NOT_LOADED,
	/**
	 * The load state of the element cannot be determined.
	 */
	UNKNOWN
}
