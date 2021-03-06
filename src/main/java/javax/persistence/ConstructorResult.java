/*
 * Copyright (c) 2008, 2009, 2011 Oracle, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.  The Eclipse Public License is available
 * at http://www.eclipse.org/legal/epl-v10.html and the Eclipse Distribution License
 * is available at http://www.eclipse.org/org/documents/edl-v10.php.
 */
package javax.persistence;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used in conjunction with the {@link SqlResultSetMapping} annotation to map the SELECT clause of a SQL query
 * to a constructor.
 * <p>
 * Applies a constructor for the target class, passing in as arguments values from the specified columns. All
 * columns corresponding to arguments of the intended constructor must be specified using the {@code columns}
 * element of the {@code ConstructorResult} annotation in the same order as that of the argument list of the
 * constructor. Any entities returned as constructor results will be in either the new or detached state,
 * depending on whether a primary key is retrieved for the constructed object.
 */
@Target(value = {})
@Retention(RUNTIME)
public @interface ConstructorResult
{
    /**
     * (Required) The class whose constructor is to be invoked.
     * @return target class
     */
    Class targetClass();

    /**
     * (Required) The mapping of columns in the SELECT list to the arguments of the intended constructor, in
     * order.
     * @return columns
     */
	ColumnResult[] columns();
}

