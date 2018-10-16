/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.connector;

import org.eclipse.kapua.KapuaException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface MessageHandler<M> {

    void handle(MessageContext<M> message, Handler<AsyncResult<Void>> resultFuture) throws KapuaException;
}