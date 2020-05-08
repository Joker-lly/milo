/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.client.nodes;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.nodes.ObjectNode;
import org.eclipse.milo.opcua.sdk.core.nodes.ObjectNodeProperties;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NamingRuleType;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

import static org.eclipse.milo.opcua.sdk.core.util.StreamUtil.opt2stream;
import static org.eclipse.milo.opcua.stack.core.types.builtin.DataValue.valueOnly;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;
import static org.eclipse.milo.opcua.stack.core.util.FutureUtils.failedUaFuture;

public class UaObjectNode extends UaNode implements ObjectNode {

    private UByte eventNotifier;

    public UaObjectNode(
        OpcUaClient client,
        NodeId nodeId,
        NodeClass nodeClass,
        QualifiedName browseName,
        LocalizedText displayName,
        LocalizedText description,
        UInteger writeMask,
        UInteger userWriteMask,
        UByte eventNotifier
    ) {

        super(client, nodeId, nodeClass, browseName, displayName, description, writeMask, userWriteMask);

        this.eventNotifier = eventNotifier;
    }

    @Override
    public UByte getEventNotifier() {
        return eventNotifier;
    }

    @Override
    public void setEventNotifier(UByte eventNotifier) {
        this.eventNotifier = eventNotifier;
    }

    public CompletableFuture<UByte> getEventNotifierAsync() {
        return getAttributeOrFail(readEventNotifierAsync())
            .thenApply(UByte.class::cast);
    }

    public CompletableFuture<StatusCode> setEventNotifierAsync(UByte eventNotifier) {
        return writeEventNotifierAsync(valueOnly(new Variant(eventNotifier)));
    }

    @Override
    public CompletableFuture<DataValue> readEventNotifierAsync() {
        return readAttributeAsync(AttributeId.EventNotifier);
    }

    @Override
    public CompletableFuture<StatusCode> writeEventNotifierAsync(DataValue value) {
        return writeAttributeAsync(AttributeId.EventNotifier, value);
    }

    public CompletableFuture<? extends UaNode> getComponent(QualifiedName browseName) {
        UInteger nodeClassMask = uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue());
        UInteger resultMask = uint(BrowseResultMask.All.getValue());

        CompletableFuture<BrowseResult> future = client.browse(
            new BrowseDescription(
                getNodeId(),
                BrowseDirection.Forward,
                Identifiers.HasComponent,
                false,
                nodeClassMask,
                resultMask
            )
        );

        return future.thenCompose(result -> {
            List<ReferenceDescription> references = l(result.getReferences());

            Optional<CompletableFuture<? extends UaNode>> node = references.stream()
                .filter(r -> browseName.equals(r.getBrowseName()))
                .flatMap(r -> {
                    Optional<CompletableFuture<? extends UaNode>> opt = r.getNodeId()
                        .local(client.getNamespaceTable())
                        .map(id -> client.getAddressSpace().createNode(id));

                    return opt2stream(opt);
                })
                .findFirst();

            return node.orElse(failedUaFuture(StatusCodes.Bad_NotFound));
        });
    }

    public CompletableFuture<? extends ObjectNode> getObjectComponent(String namespaceUri, String name) {
        UShort namespaceIndex = client.getNamespaceTable().getIndex(namespaceUri);

        if (namespaceIndex != null) {
            return getObjectComponent(new QualifiedName(namespaceIndex, name));
        } else {
            return failedUaFuture(StatusCodes.Bad_NotFound);
        }
    }

    public CompletableFuture<? extends UaObjectNode> getObjectComponent(QualifiedName browseName) {
        UInteger nodeClassMask = uint(NodeClass.Object.getValue());
        UInteger resultMask = uint(BrowseResultMask.All.getValue());

        CompletableFuture<BrowseResult> future = client.browse(
            new BrowseDescription(
                getNodeId(),
                BrowseDirection.Forward,
                Identifiers.HasComponent,
                false,
                nodeClassMask,
                resultMask
            )
        );

        return future.thenCompose(result -> {
            List<ReferenceDescription> references = l(result.getReferences());

            Optional<CompletableFuture<UaObjectNode>> node = references.stream()
                .filter(r -> browseName.equals(r.getBrowseName()))
                .flatMap(r -> {
                    Optional<CompletableFuture<UaObjectNode>> opt = r.getNodeId()
                        .local(client.getNamespaceTable())
                        .map(id -> client.getAddressSpace().getObjectNode(id));

                    return opt2stream(opt);
                })
                .findFirst();

            return node.orElse(failedUaFuture(StatusCodes.Bad_NotFound));
        });
    }

    public CompletableFuture<? extends UaVariableNode> getVariableComponent(String namespaceUri, String name) {
        UShort namespaceIndex = client.getNamespaceTable().getIndex(namespaceUri);

        if (namespaceIndex != null) {
            return getVariableComponent(new QualifiedName(namespaceIndex, name));
        } else {
            return failedUaFuture(StatusCodes.Bad_NotFound);
        }
    }

    public CompletableFuture<? extends UaVariableNode> getVariableComponent(QualifiedName browseName) {
        UInteger nodeClassMask = uint(NodeClass.Variable.getValue());
        UInteger resultMask = uint(BrowseResultMask.All.getValue());

        CompletableFuture<BrowseResult> future = client.browse(
            new BrowseDescription(
                getNodeId(),
                BrowseDirection.Forward,
                Identifiers.HasComponent,
                false,
                nodeClassMask,
                resultMask
            )
        );

        return future.thenCompose(result -> {
            List<ReferenceDescription> references = l(result.getReferences());

            Optional<CompletableFuture<UaVariableNode>> node = references.stream()
                .filter(r -> browseName.equals(r.getBrowseName()))
                .flatMap(r -> {
                    Optional<CompletableFuture<UaVariableNode>> opt = r.getNodeId()
                        .local(client.getNamespaceTable())
                        .map(id -> client.getAddressSpace().getVariableNode(id));

                    return opt2stream(opt);
                })
                .findFirst();

            return node.orElse(failedUaFuture(StatusCodes.Bad_NotFound));
        });
    }

    public CompletableFuture<? extends UaObjectTypeNode> getTypeDefinition() {
        UInteger nodeClassMask = uint(NodeClass.ObjectType.getValue());
        UInteger resultMask = uint(BrowseResultMask.All.getValue());

        CompletableFuture<BrowseResult> future = client.browse(
            new BrowseDescription(
                getNodeId(),
                BrowseDirection.Forward,
                Identifiers.HasTypeDefinition,
                false,
                nodeClassMask,
                resultMask
            )
        );

        return future.thenCompose(result -> {
            List<ReferenceDescription> references = l(result.getReferences());

            Optional<UaObjectTypeNode> node = references.stream()
                .flatMap(r -> {
                    Optional<UaObjectTypeNode> opt = r.getNodeId()
                        .local(client.getNamespaceTable())
                        .map(id -> client.getAddressSpace().createObjectTypeNode(id));

                    return opt2stream(opt);
                })
                .findFirst();

            return node.map(CompletableFuture::completedFuture)
                .orElse(failedUaFuture(StatusCodes.Bad_NotFound));
        });
    }

    /**
     * Get the value of the {@link ObjectNodeProperties#NodeVersion} Property, if it exists.
     *
     * @return the value of the NodeVersion Property, if it exists.
     * @see ObjectNodeProperties
     */
    public CompletableFuture<String> getNodeVersion() {
        return getProperty(ObjectNodeProperties.NodeVersion);
    }

    /**
     * Get the value of the {@link ObjectNodeProperties#Icon} Property, if it exists.
     *
     * @return the value of the Icon Property, if it exists.
     * @see ObjectNodeProperties
     */
    public CompletableFuture<ByteString> getIcon() {
        return getProperty(ObjectNodeProperties.Icon);
    }

    /**
     * Get the value of the {@link ObjectNodeProperties#NamingRule} Property, if it exists.
     *
     * @return the value of the NamingRule Property, if it exists.
     * @see ObjectNodeProperties
     */
    public CompletableFuture<NamingRuleType> getNamingRule() {
        return getProperty(ObjectNodeProperties.NamingRule);
    }

    /**
     * Set the value of the {@link ObjectNodeProperties#NodeVersion} Property, if it exists.
     *
     * @param nodeVersion the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see ObjectNodeProperties
     */
    public CompletableFuture<StatusCode> setNodeVersion(String nodeVersion) {
        return setProperty(ObjectNodeProperties.NodeVersion, nodeVersion);
    }

    /**
     * Set the value of the {@link ObjectNodeProperties#Icon} Property, if it exists.
     *
     * @param icon the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see ObjectNodeProperties
     */
    public CompletableFuture<StatusCode> setIcon(ByteString icon) {
        return setProperty(ObjectNodeProperties.Icon, icon);
    }

    /**
     * Set the value of the {@link ObjectNodeProperties#NamingRule} Property, if it exists.
     *
     * @param namingRule the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see ObjectNodeProperties
     */
    public CompletableFuture<StatusCode> setNamingRule(NamingRuleType namingRule) {
        return setProperty(ObjectNodeProperties.NamingRule, namingRule);
    }

}
