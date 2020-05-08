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
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.core.nodes.VariableNodeProperties;
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
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EUInformation;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.TimeZoneDataType;
import org.eclipse.milo.opcua.stack.core.util.FutureUtils;

import static org.eclipse.milo.opcua.sdk.core.util.StreamUtil.opt2stream;
import static org.eclipse.milo.opcua.stack.core.types.builtin.DataValue.valueOnly;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;
import static org.eclipse.milo.opcua.stack.core.util.FutureUtils.failedUaFuture;

public class UaVariableNode extends UaNode implements VariableNode {

    public UaVariableNode(OpcUaClient client, NodeId nodeId) {
        super(client, nodeId);
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
                nodeId,
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

    public CompletableFuture<? extends UaVariableTypeNode> getTypeDefinition() {
        UInteger nodeClassMask = uint(NodeClass.VariableType.getValue());
        UInteger resultMask = uint(BrowseResultMask.All.getValue());

        CompletableFuture<BrowseResult> future = client.browse(
            new BrowseDescription(
                nodeId,
                BrowseDirection.Forward,
                Identifiers.HasTypeDefinition,
                false,
                nodeClassMask,
                resultMask
            )
        );

        return future.thenCompose(result -> {
            List<ReferenceDescription> references = l(result.getReferences());

            Optional<UaVariableTypeNode> node = references.stream()
                .flatMap(r -> {
                    Optional<UaVariableTypeNode> opt = r.getNodeId()
                        .local(client.getNamespaceTable())
                        .map(id -> client.getAddressSpace().createVariableTypeNode(id));

                    return opt2stream(opt);
                })
                .findFirst();

            return node.map(CompletableFuture::completedFuture)
                .orElse(FutureUtils.failedUaFuture(StatusCodes.Bad_NotFound));
        });
    }

    @Override
    public CompletableFuture<Object> getValue() {
        return getAttributeOrFail(readValue());
    }

    @Override
    public CompletableFuture<NodeId> getDataType() {
        return getAttributeOrFail(readDataType());
    }

    @Override
    public CompletableFuture<Integer> getValueRank() {
        return getAttributeOrFail(readValueRank());
    }

    @Override
    public CompletableFuture<UInteger[]> getArrayDimensions() {
        return getAttributeOrFail(readArrayDimensions());
    }

    @Override
    public CompletableFuture<UByte> getAccessLevel() {
        return getAttributeOrFail(readAccessLevel());
    }

    @Override
    public CompletableFuture<UByte> getUserAccessLevel() {
        return getAttributeOrFail(readUserAccessLevel());
    }

    @Override
    public CompletableFuture<Double> getMinimumSamplingInterval() {
        return getAttributeOrFail(readMinimumSamplingInterval());
    }

    @Override
    public CompletableFuture<Boolean> getHistorizing() {
        return getAttributeOrFail(readHistorizing());
    }

    @Override
    public CompletableFuture<StatusCode> setValue(Object value) {
        return writeValue(valueOnly(new Variant(value)));
    }

    @Override
    public CompletableFuture<StatusCode> setDataType(NodeId dataType) {
        return writeDataType(valueOnly(new Variant(dataType)));
    }

    @Override
    public CompletableFuture<StatusCode> setValueRank(int valueRank) {
        return writeValueRank(valueOnly(new Variant(valueRank)));
    }

    @Override
    public CompletableFuture<StatusCode> setArrayDimensions(UInteger[] arrayDimensions) {
        return writeArrayDimensions(valueOnly(new Variant(arrayDimensions)));
    }

    @Override
    public CompletableFuture<StatusCode> setAccessLevel(UByte accessLevel) {
        return writeAccessLevel(valueOnly(new Variant(accessLevel)));
    }

    @Override
    public CompletableFuture<StatusCode> setUserAccessLevel(UByte userAccessLevel) {
        return writeUserAccessLevel(valueOnly(new Variant(userAccessLevel)));
    }

    @Override
    public CompletableFuture<StatusCode> setMinimumSamplingInterval(double minimumSamplingInterval) {
        return writeMinimumSamplingInterval(valueOnly(new Variant(minimumSamplingInterval)));
    }

    @Override
    public CompletableFuture<StatusCode> setHistorizing(boolean historizing) {
        return writeHistorizing(valueOnly(new Variant(historizing)));
    }

    @Override
    public CompletableFuture<DataValue> readValue() {
        return readAttributeAsync(AttributeId.Value);
    }

    @Override
    public CompletableFuture<DataValue> readDataType() {
        return readAttributeAsync(AttributeId.DataType);
    }

    @Override
    public CompletableFuture<DataValue> readValueRank() {
        return readAttributeAsync(AttributeId.ValueRank);
    }

    @Override
    public CompletableFuture<DataValue> readArrayDimensions() {
        return readAttributeAsync(AttributeId.ArrayDimensions);
    }

    @Override
    public CompletableFuture<DataValue> readAccessLevel() {
        return readAttributeAsync(AttributeId.AccessLevel);
    }

    @Override
    public CompletableFuture<DataValue> readUserAccessLevel() {
        return readAttributeAsync(AttributeId.UserAccessLevel);
    }

    @Override
    public CompletableFuture<DataValue> readMinimumSamplingInterval() {
        return readAttributeAsync(AttributeId.MinimumSamplingInterval);
    }

    @Override
    public CompletableFuture<DataValue> readHistorizing() {
        return readAttributeAsync(AttributeId.Historizing);
    }

    @Override
    public CompletableFuture<StatusCode> writeValue(DataValue value) {
        return writeAttributeAsync(AttributeId.Value, value);
    }

    @Override
    public CompletableFuture<StatusCode> writeDataType(DataValue value) {
        return writeAttributeAsync(AttributeId.DataType, value);
    }

    @Override
    public CompletableFuture<StatusCode> writeValueRank(DataValue value) {
        return writeAttributeAsync(AttributeId.ValueRank, value);
    }

    @Override
    public CompletableFuture<StatusCode> writeArrayDimensions(DataValue value) {
        return writeAttributeAsync(AttributeId.ArrayDimensions, value);
    }

    @Override
    public CompletableFuture<StatusCode> writeAccessLevel(DataValue value) {
        return writeAttributeAsync(AttributeId.AccessLevel, value);
    }

    @Override
    public CompletableFuture<StatusCode> writeUserAccessLevel(DataValue value) {
        return writeAttributeAsync(AttributeId.UserAccessLevel, value);
    }

    @Override
    public CompletableFuture<StatusCode> writeMinimumSamplingInterval(DataValue value) {
        return writeAttributeAsync(AttributeId.MinimumSamplingInterval, value);
    }

    @Override
    public CompletableFuture<StatusCode> writeHistorizing(DataValue value) {
        return writeAttributeAsync(AttributeId.Historizing, value);
    }

    /**
     * Get the value of the {@link VariableNodeProperties#NodeVersion} Property, if it exists.
     *
     * @return the value of the NodeVersion Property, if it exists.
     * @see VariableNodeProperties
     */
    public CompletableFuture<String> getNodeVersion() {
        return getProperty(VariableNodeProperties.NodeVersion);
    }

    /**
     * Get the value of the {@link VariableNodeProperties#LocalTime} Property, if it exists.
     *
     * @return the value of the LocalTime Property, if it exists.
     * @see VariableNodeProperties
     */
    public CompletableFuture<TimeZoneDataType> getLocalTime() {
        return getProperty(VariableNodeProperties.LocalTime);
    }

    /**
     * Get the value of the {@link VariableNodeProperties#DataTypeVersion} Property, if it exists.
     *
     * @return the value of the DataTypeVersion Property, if it exists.
     * @see VariableNodeProperties
     */
    public CompletableFuture<String> getDataTypeVersion() {
        return getProperty(VariableNodeProperties.DataTypeVersion);
    }

    /**
     * Get the value of the {@link VariableNodeProperties#DictionaryFragment} Property, if it exists.
     *
     * @return the value of the DictionaryFragment Property, if it exists.
     * @see VariableNodeProperties
     */
    public CompletableFuture<ByteString> getDictionaryFragment() {
        return getProperty(VariableNodeProperties.DictionaryFragment);
    }

    /**
     * Get the value of the AllowNulls Property, if it exists.
     *
     * @return the value of the AllowNulls Property, if it exists.
     * @see VariableNodeProperties#AllowNulls
     */
    public CompletableFuture<Boolean> getAllowNulls() {
        return getProperty(VariableNodeProperties.AllowNulls);
    }

    /**
     * Get the value of the {@link VariableNodeProperties#ValueAsText} Property, if it exists.
     *
     * @return the value of the ValueAsText Property, if it exists.
     * @see VariableNodeProperties
     */
    public CompletableFuture<LocalizedText> getValueAsText() {
        return getProperty(VariableNodeProperties.ValueAsText);
    }

    /**
     * Get the value of the {@link VariableNodeProperties#MaxStringLength} Property, if it exists.
     *
     * @return the value of the MaxStringLength Property, if it exists.
     * @see VariableNodeProperties
     */
    public CompletableFuture<UInteger> getMaxStringLength() {
        return getProperty(VariableNodeProperties.MaxStringLength);
    }

    /**
     * Get the value of the {@link VariableNodeProperties#MaxArrayLength} Property, if it exists.
     *
     * @return the value of the MaxArrayLength Property, if it exists.
     * @see VariableNodeProperties
     */
    public CompletableFuture<UInteger> getMaxArrayLength() {
        return getProperty(VariableNodeProperties.MaxArrayLength);
    }

    /**
     * Get the value of the {@link VariableNodeProperties#EngineeringUnits} Property, if it exists.
     *
     * @return the value of the EngineeringUnits Property, if it exists.
     * @see VariableNodeProperties
     */
    public CompletableFuture<EUInformation> getEngineeringUnits() {
        return getProperty(VariableNodeProperties.EngineeringUnits);
    }

    /**
     * Set the value of the {@link VariableNodeProperties#NodeVersion} Property, if it exists.
     *
     * @param nodeVersion the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see VariableNodeProperties
     */
    public CompletableFuture<StatusCode> setNodeVersion(String nodeVersion) {
        return setProperty(VariableNodeProperties.NodeVersion, nodeVersion);
    }

    /**
     * Set the value of the {@link VariableNodeProperties#LocalTime} Property, if it exists.
     *
     * @param localTime the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see VariableNodeProperties
     */
    public CompletableFuture<StatusCode> setLocalTime(TimeZoneDataType localTime) {
        return setProperty(VariableNodeProperties.LocalTime, localTime);
    }

    /**
     * Set the value of the {@link VariableNodeProperties#DataTypeVersion} Property, if it exists.
     *
     * @param dataTypeVersion the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see VariableNodeProperties
     */
    public CompletableFuture<StatusCode> setDataTypeVersion(String dataTypeVersion) {
        return setProperty(VariableNodeProperties.DataTypeVersion, dataTypeVersion);
    }

    /**
     * Set the value of the {@link VariableNodeProperties#DictionaryFragment} Property, if it exists.
     *
     * @param dictionaryFragment the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see VariableNodeProperties
     */
    public CompletableFuture<StatusCode> setDictionaryFragment(ByteString dictionaryFragment) {
        return setProperty(VariableNodeProperties.DictionaryFragment, dictionaryFragment);
    }

    /**
     * Set the value of the {@link VariableNodeProperties#AllowNulls} Property, if it exists.
     *
     * @param allowNulls the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see VariableNodeProperties
     */
    public CompletableFuture<StatusCode> setAllowNulls(Boolean allowNulls) {
        return setProperty(VariableNodeProperties.AllowNulls, allowNulls);
    }

    /**
     * Set the value of the {@link VariableNodeProperties#ValueAsText} Property, if it exists.
     *
     * @param valueAsText the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see VariableNodeProperties
     */
    public CompletableFuture<StatusCode> setValueAsText(LocalizedText valueAsText) {
        return setProperty(VariableNodeProperties.ValueAsText, valueAsText);
    }

    /**
     * Set the value of the {@link VariableNodeProperties#MaxStringLength} Property, if it exists.
     *
     * @param maxStringLength the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see VariableNodeProperties
     */
    public CompletableFuture<StatusCode> setMaxStringLength(UInteger maxStringLength) {
        return setProperty(VariableNodeProperties.MaxStringLength, maxStringLength);
    }

    /**
     * Set the value of the {@link VariableNodeProperties#MaxArrayLength} Property, if it exists.
     *
     * @param maxArrayLength the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see VariableNodeProperties
     */
    public CompletableFuture<StatusCode> setMaxArrayLength(UInteger maxArrayLength) {
        return setProperty(VariableNodeProperties.MaxArrayLength, maxArrayLength);
    }

    /**
     * Set the value of the {@link VariableNodeProperties#EngineeringUnits} Property, if it exists.
     *
     * @param engineeringUnits the value to set.
     * @return a {@link CompletableFuture} that completes with the {@link StatusCode} of the write operation.
     * @see VariableNodeProperties
     */
    public CompletableFuture<StatusCode> setEngineeringUnits(EUInformation engineeringUnits) {
        return setProperty(VariableNodeProperties.EngineeringUnits, engineeringUnits);
    }

}

