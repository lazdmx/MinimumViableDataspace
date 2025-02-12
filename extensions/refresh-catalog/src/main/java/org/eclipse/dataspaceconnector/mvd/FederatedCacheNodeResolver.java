/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.mvd;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.ids.spi.Protocols;
import org.eclipse.dataspaceconnector.registration.client.models.Participant;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Resolves the {@link FederatedCacheNode}s from the Participant's Did Document.
 */
class FederatedCacheNodeResolver {

    public static final String IDS_MESSAGING = "IDSMessaging";
    public static final List<String> SUPPORTED_PROTOCOLS = List.of(Protocols.IDS_MULTIPART);

    private final DidResolverRegistry resolver;
    private final Monitor monitor;

    FederatedCacheNodeResolver(DidResolverRegistry resolver, Monitor monitor) {
        this.resolver = resolver;
        this.monitor = monitor;
    }

    public Result<FederatedCacheNode> toFederatedCacheNode(Participant participant) {
        var did = participant.getDid();
        monitor.debug(format("Resolving Did Document for did %s.", did));
        var didDocument = resolver.resolve(did);
        if (didDocument.failed()) {
            monitor.severe(() -> format("Failed to resolve DID Document for %s. %s", did, didDocument.getFailureDetail()));
            return Result.failure("Can't resolve Did Document for participant: " + did);
        }
        return getUrl(didDocument.getContent())
                .map(url -> Result.success(new FederatedCacheNode(didDocument.getContent().getId(), url, SUPPORTED_PROTOCOLS)))
                .orElseGet(() -> Result.failure(format("Can't resolve Did Document for participant: %s", did)));
    }

    private Optional<String> getUrl(DidDocument didDocument) {
        return didDocument
                .getService().stream()
                .filter(service -> service.getType().equals(IDS_MESSAGING))
                .map(Service::getServiceEndpoint)
                .findFirst();
    }
}
