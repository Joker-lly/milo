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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.NodeCache;
import org.eclipse.milo.opcua.sdk.client.model.nodes.variables.PropertyTypeNode;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.serialization.UaEnumeration;
import org.eclipse.milo.opcua.stack.core.serialization.UaStructure;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.milo.opcua.sdk.core.util.StreamUtil.opt2stream;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;
import static org.eclipse.milo.opcua.stack.core.util.FutureUtils.failedUaFuture;

public abstract class UaNode implements Node {

    protected final NodeCache nodeCache;

    private NodeId nodeId;
    private NodeClass nodeClass;
    private QualifiedName browseName;
    private LocalizedText displayName;
    private LocalizedText description;
    private UInteger writeMask;
    private UInteger userWriteMask;

    protected final OpcUaClient client;

    public UaNode(
        OpcUaClient client,
        NodeId nodeId,
        NodeClass nodeClass,
        QualifiedName browseName,
        LocalizedText displayName,
        LocalizedText description,
        UInteger writeMask,
        UInteger userWriteMask
    ) {

        this.client = client;
        this.nodeId = nodeId;
        this.nodeClass = nodeClass;
        this.browseName = browseName;
        this.displayName = displayName;
        this.description = description;
        this.writeMask = writeMask;
        this.userWriteMask = userWriteMask;

        nodeCache = client.getNodeCache();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned attribute is the most recently seen value; it is not read live from the server.
     */
    @Override
    public synchronized NodeId getNodeId() {
        return nodeId;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned attribute is the most recently seen value; it is not read live from the server.
     */
    @Override
    public synchronized NodeClass getNodeClass() {
        return nodeClass;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned attribute is the most recently seen value; it is not read live from the server.
     */
    @Override
    public synchronized QualifiedName getBrowseName() {
        return browseName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned attribute is the most recently seen value; it is not read live from the server.
     */
    @Override
    public synchronized LocalizedText getDisplayName() {
        return displayName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned attribute is the most recently seen value; it is not read live from the server.
     */
    @Override
    public synchronized LocalizedText getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned attribute is the most recently seen value; it is not read live from the server.
     */
    @Override
    public synchronized UInteger getWriteMask() {
        return writeMask;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned attribute is the most recently seen value; it is not read live from the server.
     */
    @Override
    public synchronized UInteger getUserWriteMask() {
        return userWriteMask;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The attribute is only update locally; it is not written to the server.
     */
    @Override
    public synchronized void setNodeId(NodeId nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The attribute is only update locally; it is not written to the server.
     */
    @Override
    public synchronized void setNodeClass(NodeClass nodeClass) {
        this.nodeClass = nodeClass;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The attribute is only update locally; it is not written to the server.
     */
    @Override
    public synchronized void setBrowseName(QualifiedName browseName) {
        this.browseName = browseName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The attribute is only update locally; it is not written to the server.
     */
    @Override
    public synchronized void setDisplayName(LocalizedText displayName) {
        this.displayName = displayName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The attribute is only update locally; it is not written to the server.
     */
    @Override
    public synchronized void setDescription(LocalizedText description) {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The attribute is only update locally; it is not written to the server.
     */
    @Override
    public synchronized void setWriteMask(UInteger writeMask) {
        this.writeMask = writeMask;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The attribute is only update locally; it is not written to the server.
     */
    @Override
    public synchronized void setUserWriteMask(UInteger userWriteMask) {
        this.userWriteMask = userWriteMask;
    }

    /**
     * Read the NodeId attribute for this Node from the server and update the local attribute if
     * the operation succeeds.
     *
     * @return the {@link NodeId} read from the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public NodeId readNodeId() throws UaException {
        NodeId nodeId = (NodeId) getGoodValueOrThrow(
            readAttribute(AttributeId.NodeId)
        );
        setNodeId(nodeId);
        return nodeId;
    }

    /**
     * Read the NodeClass attribute for this Node from the server and update the local attribute if
     * the operation succeeds.
     *
     * @return the {@link NodeClass} read from the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public NodeClass readNodeClass() throws UaException {
        Integer value = (Integer) getGoodValueOrThrow(
            readAttribute(AttributeId.NodeClass)
        );
        NodeClass nodeClass = NodeClass.from(value);
        setNodeClass(nodeClass);
        return nodeClass;
    }

    /**
     * Read the BrowseName attribute for this Node from the server and update the local attribute
     * if the operation succeeds.
     *
     * @return the {@link QualifiedName} read from the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public QualifiedName readBrowseName() throws UaException {
        QualifiedName browseName = (QualifiedName) getGoodValueOrThrow(
            readAttribute(AttributeId.BrowseName)
        );
        setBrowseName(browseName);
        return browseName;
    }

    /**
     * Read the DisplayName attribute for this Node from the server and update the local attribute
     * if the operation succeeds.
     *
     * @return the {@link LocalizedText} read from the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public LocalizedText readDisplayName() throws UaException {
        LocalizedText displayName = (LocalizedText) getGoodValueOrThrow(
            readAttribute(AttributeId.DisplayName)
        );
        setDisplayName(displayName);
        return displayName;
    }

    /**
     * Read the Description attribute for this Node from the server and update the local attribute
     * if the operation succeeds.
     *
     * @return the {@link LocalizedText} read from the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public LocalizedText readDescription() throws UaException {
        LocalizedText description = (LocalizedText) getGoodValueOrThrow(
            readAttribute(AttributeId.Description)
        );
        setDescription(description);
        return description;
    }

    /**
     * Read the WriteMask attribute for this Node from the server and update the local attribute if
     * the operation succeeds.
     *
     * @return the {@link UInteger} read from the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public UInteger readWriteMask() throws UaException {
        UInteger writeMask = (UInteger) getGoodValueOrThrow(
            readAttribute(AttributeId.UserWriteMask)
        );
        setWriteMask(writeMask);
        return writeMask;
    }

    /**
     * Read the UserWriteMask attribute for this Node from the server and update the local
     * attribute if the operation succeeds.
     *
     * @return the {@link UInteger} read from the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public UInteger readUserWriteMask() throws UaException {
        UInteger userWriteMask = (UInteger) getGoodValueOrThrow(
            readAttribute(AttributeId.UserWriteMask)
        );
        setUserWriteMask(userWriteMask);
        return userWriteMask;
    }

    /**
     * Write a new NodeId attribute for this Node to the server and update the local attribute if
     * the operation succeeds.
     *
     * @param nodeId the {@link NodeId} to write to the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public void writeNodeId(NodeId nodeId) throws UaException {
        DataValue value = DataValue.valueOnly(new Variant(nodeId));
        StatusCode statusCode = writeAttribute(AttributeId.NodeId, value);

        if (statusCode == null || statusCode.isGood()) {
            setNodeId(nodeId);
        } else {
            throw new UaException(statusCode, "write NodeId failed");
        }
    }

    /**
     * Write a new NodeClass attribute for this Node to the server and update the local attribute
     * if the operation succeeds.
     *
     * @param nodeClass the {@link NodeClass} to write to the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public void writeNodeClass(NodeClass nodeClass) throws UaException {
        DataValue value = DataValue.valueOnly(new Variant(nodeClass));
        StatusCode statusCode = writeAttribute(AttributeId.NodeClass, value);

        if (statusCode == null || statusCode.isGood()) {
            setNodeClass(nodeClass);
        } else {
            throw new UaException(statusCode, "write NodeClass failed");
        }
    }

    /**
     * Write a new BrowseName attribute for this Node to the server and update the local attribute
     * if the operation succeeds.
     *
     * @param browseName the {@link QualifiedName} to write to the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public void writeBrowseName(QualifiedName browseName) throws UaException {
        DataValue value = DataValue.valueOnly(new Variant(browseName));
        StatusCode statusCode = writeAttribute(AttributeId.BrowseName, value);

        if (statusCode == null || statusCode.isGood()) {
            setBrowseName(browseName);
        } else {
            throw new UaException(statusCode, "write BrowseName failed");
        }
    }

    /**
     * Write a new DisplayName attribute for this Node to the server and update the local attribute
     * if the operation succeeds.
     *
     * @param displayName the {@link LocalizedText} to write to the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public void writeDisplayName(LocalizedText displayName) throws UaException {
        DataValue value = DataValue.valueOnly(new Variant(displayName));
        StatusCode statusCode = writeAttribute(AttributeId.DisplayName, value);

        if (statusCode == null || statusCode.isGood()) {
            setDisplayName(displayName);
        } else {
            throw new UaException(statusCode, "write DisplayName failed");
        }
    }

    /**
     * Write a new Description attribute for this Node to the server and update the local attribute
     * if the operation succeeds.
     *
     * @param description the {@link LocalizedText} to write to the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public void writeDescription(LocalizedText description) throws UaException {
        DataValue value = DataValue.valueOnly(new Variant(description));
        StatusCode statusCode = writeAttribute(AttributeId.Description, value);

        if (statusCode == null || statusCode.isGood()) {
            setDescription(description);
        } else {
            throw new UaException(statusCode, "write Description failed");
        }
    }

    /**
     * Write a new WriteMask attribute for this Node to the server and update the local attribute
     * if the operation succeeds.
     *
     * @param writeMask the {@link UInteger} to write to the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public void writeWriteMask(UInteger writeMask) throws UaException {
        DataValue value = DataValue.valueOnly(new Variant(writeMask));
        StatusCode statusCode = writeAttribute(AttributeId.WriteMask, value);

        if (statusCode == null || statusCode.isGood()) {
            setWriteMask(writeMask);
        } else {
            throw new UaException(statusCode, "write WriteMask failed");
        }
    }

    /**
     * Write a new UserWriteMask attribute for this Node to the server and update the local
     * attribute if the operation succeeds.
     *
     * @param userWriteMask the {@link UInteger} to write to the server.
     * @throws UaException if a service- or operation-level error occurs.
     */
    public void writeUserWriteMask(UInteger userWriteMask) throws UaException {
        DataValue value = DataValue.valueOnly(new Variant(userWriteMask));
        StatusCode statusCode = writeAttribute(AttributeId.UserWriteMask, value);

        if (statusCode == null || statusCode.isGood()) {
            setUserWriteMask(userWriteMask);
        } else {
            throw new UaException(statusCode, "write UserWriteMask failed");
        }
    }

    /**
     * Read the attribute identified by an {@code attributeId} from the server.
     * <p>
     * This operation does not update the local attribute.
     *
     * @param attributeId the {@link AttributeId} of the attribute to read.
     * @return a {@link DataValue} containing the attribute value.
     * @throws UaException if a service-level error occurs.
     */
    public DataValue readAttribute(AttributeId attributeId) throws UaException {
        try {
            return readAttributeAsync(attributeId).get();
        } catch (ExecutionException | InterruptedException e) {
            throw UaException.extract(e)
                .orElse(new UaException(StatusCodes.Bad_UnexpectedError, e));
        }
    }

    /**
     * Write {@code value} to the attribute identified by {@code attributeId}.
     * <p>
     * This operation does not update the local attribute.
     *
     * @param attributeId the {@link AttributeId} of the attribute to write.
     * @param value       a {@link DataValue} containing the attribute value.
     * @return the {@link StatusCode} from the write operation.
     * @throws UaException if a service-level error occurs.
     */
    public StatusCode writeAttribute(AttributeId attributeId, DataValue value) throws UaException {
        try {
            return writeAttributeAsync(attributeId, value).get();
        } catch (ExecutionException | InterruptedException e) {
            throw UaException.extract(e)
                .orElse(new UaException(StatusCodes.Bad_UnexpectedError, e));
        }
    }

    public CompletableFuture<DataValue> readAttributeAsync(AttributeId attributeId) {
        ReadValueId readValueId = new ReadValueId(
            nodeId,
            attributeId.uid(),
            null,
            QualifiedName.NULL_VALUE
        );

        CompletableFuture<ReadResponse> future = client.read(
            0.0,
            TimestampsToReturn.Both,
            newArrayList(readValueId)
        );

        return future.thenApply(response -> response.getResults()[0]);
    }

    public CompletableFuture<StatusCode> writeAttributeAsync(AttributeId attributeId, DataValue value) {
        WriteValue writeValue = new WriteValue(
            nodeId,
            attributeId.uid(),
            null,
            value
        );

        CompletableFuture<WriteResponse> future = client.write(newArrayList(writeValue));

        return future.thenApply(response -> response.getResults()[0]);
    }

    protected CompletableFuture<PropertyTypeNode> getPropertyNode(QualifiedProperty<?> property) {
        return property.getQualifiedName(client.getNamespaceTable())
            .map(this::getPropertyNode)
            .orElse(failedUaFuture(StatusCodes.Bad_NotFound));
    }

    protected CompletableFuture<PropertyTypeNode> getPropertyNode(QualifiedName browseName) {
        UInteger nodeClassMask = uint(NodeClass.Variable.getValue());
        UInteger resultMask = uint(BrowseResultMask.BrowseName.getValue());

        CompletableFuture<BrowseResult> future = client.browse(
            new BrowseDescription(
                nodeId,
                BrowseDirection.Forward,
                Identifiers.HasProperty,
                false,
                nodeClassMask,
                resultMask
            )
        );

        return future.thenCompose(result -> {
            List<ReferenceDescription> references = l(result.getReferences());

            Optional<PropertyTypeNode> node = references.stream()
                .filter(r -> browseName.equals(r.getBrowseName()))
                .flatMap(r -> {
                    Optional<PropertyTypeNode> opt = r.getNodeId()
                        .local(client.getNamespaceTable())
                        .map(id -> new PropertyTypeNode(client, id));

                    return opt2stream(opt);
                })
                .findFirst();

            return node
                .map(CompletableFuture::completedFuture)
                .orElse(failedUaFuture(StatusCodes.Bad_NotFound));
        });
    }

    public <T> CompletableFuture<T> getProperty(QualifiedProperty<T> property) {
        return getPropertyNode(property)
            .thenCompose(UaVariableNode::getValue)
            .thenApply(value -> cast(value, property.getJavaType()));
    }

    protected <T> CompletableFuture<StatusCode> setProperty(QualifiedProperty<T> property, T value) {
        return getPropertyNode(property)
            .thenCompose(node -> node.setValue(value));
    }

    protected CompletableFuture<DataValue> readProperty(QualifiedProperty<?> property) {
        return getPropertyNode(property)
            .thenCompose(UaVariableNode::readValue);
    }

    protected CompletableFuture<StatusCode> writeProperty(QualifiedProperty<?> property, DataValue value) {
        return getPropertyNode(property)
            .thenCompose(node -> node.writeValue(value));
    }

    /**
     * Gets the attribute value out of the {@link DataValue} or fails if the {@link StatusCode} is bad.
     *
     * @param readFuture the {@link CompletableFuture} providing the {@link DataValue}.
     * @return the attribute value.
     */
    protected CompletableFuture<Object> getAttributeOrFail(CompletableFuture<DataValue> readFuture) {
        return readFuture.thenCompose(value -> {
            StatusCode statusCode = value.getStatusCode();

            if (statusCode == null || statusCode.isGood()) {
                try {
                    return completedFuture(value.getValue().getValue());
                } catch (Throwable t) {
                    return failedUaFuture(t, StatusCodes.Bad_UnexpectedError);
                }
            } else {
                return failedUaFuture(statusCode);
            }
        });
    }

    /**
     * Get the value out of a {@link DataValue}, throwing if the {@link StatusCode} is bad quality.
     *
     * @param value the {@link DataValue}.
     * @return the value Object from a {@link DataValue}.
     * @throws UaException with any non-good {@link StatusCode} is bad quality.
     */
    protected Object getGoodValueOrThrow(DataValue value) throws UaException {
        StatusCode statusCode = value.getStatusCode();

        if (statusCode == null || statusCode.isGood()) {
            return value.getValue().getValue();
        } else {
            throw new UaException(statusCode);
        }
    }

    /**
     * Call the Browse service to get this {@link UaNode}s references.
     *
     * @param referenceTypeId the {@link NodeId} of the ReferenceType, including subtypes, to get.
     * @param forward         {@code true} to get forward references, {@code false} for inverse references..
     * @return a List of {@link ReferenceDescription}s.
     */
    public List<ReferenceDescription> getReferences(NodeId referenceTypeId, boolean forward) {
        return null; // TODO
    }

    public CompletableFuture<List<ReferenceDescription>> getReferencesAsync(NodeId referenceTypeId, boolean forward) {
        return null; // TODO
    }

    public List<UaNode> getReferencedNodes(NodeId referenceTypeId, boolean forward) {
        return null; // TODO
    }

    public CompletableFuture<List<UaNode>> getReferencedNodesAsync(NodeId referenceTypeId, boolean forward) {
        return null; // TODO
    }

    /**
     * An implementation of cast with special handling for {@link UaEnumeration} and
     * {@link UaStructure} destination types.
     * <p>
     * If the destination type is a {@link UaEnumeration} and the from object is an Integer, an attempt is made to
     * convert the Integer into the corresponding UaEnumeration type.
     * <p>
     * If the destination type is a {@link UaStructure} and the from object is an {@link ExtensionObject}, an attempt
     * is made to decode the {@link ExtensionObject} into an object cast to the type of {@code clazz}.
     *
     * @param o     the Object to cast from.
     * @param clazz the type to cast {@code o} to.
     * @return the object after casting, or null if {@code o} is null.
     */
    protected <T> T cast(Object o, Class<T> clazz) {
        if (UaEnumeration.class.isAssignableFrom(clazz) && o instanceof Integer) {
            try {
                Object enumeration = clazz
                    .getMethod("from", new Class[]{Integer.class})
                    .invoke(null, o);

                return clazz.cast(enumeration);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                return null;
            }
        } else if (o instanceof ExtensionObject) {
            ExtensionObject xo = (ExtensionObject) o;
            Object decoded = xo.decode(client.getSerializationContext());
            return clazz.cast(decoded);
        } else if (o instanceof ExtensionObject[]) {
            ExtensionObject[] xos = (ExtensionObject[]) o;
            Class<?> componentType = clazz.getComponentType();

            Object array = Array.newInstance(componentType, xos.length);

            for (int i = 0; i < xos.length; i++) {
                ExtensionObject xo = xos[i];

                Object decoded = xo.decode(client.getSerializationContext());

                Array.set(array, i, componentType.cast(decoded));
            }

            return clazz.cast(array);
        } else {
            return clazz.cast(o);
        }
    }


}
