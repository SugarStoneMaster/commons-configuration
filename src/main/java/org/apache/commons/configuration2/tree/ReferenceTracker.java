/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.configuration2.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A class which allows an {@link InMemoryNodeModel} to associate arbitrary objects with nodes.
 * </p>
 * <p>
 * Some special configuration implementations need additional data to be stored with their nodes structure. We call such
 * data &quot;references&quot; because objects required by a configuration are referenced. In such constellations, it is
 * necessary to keep track about the nodes associated with references even if they are replaced by others during an
 * update operation of the model. This is the task of this class.
 * </p>
 * <p>
 * Basically, an instance manages a map associating nodes with reference objects. When a node is replaced the map gets
 * updated. References becoming orphans because the nodes pointing to them were removed are tracked, too. They may be of
 * importance for special configuration implementations as they might require additional updates. A concrete use case
 * for this class is {@code XMLConfiguration} which stores the DOM nodes represented by configuration nodes as
 * references.
 * </p>
 * <p>
 * Implementation note: This class is intended to work in a concurrent environment. Instances are immutable. The
 * represented state can be updated by creating new instances which are then stored by the owning node model.
 * </p>
 */
final class ReferenceTracker {
    /** A map with reference data. */
    private final Map<ImmutableNode, Object> references;

    /** A list with the removed references. */
    private final List<Object> removedReferences;

    /**
     * Creates a new instance of {@code ReferenceTracker}. This instance does not yet contain any data about references.
     */
    public ReferenceTracker() {
        this(Collections.<ImmutableNode, Object>emptyMap(), Collections.emptyList());
    }

    /**
     * Creates a new instance of {@code ReferenceTracker} and sets the data to be managed. This constructor is used
     * internally when references are updated.
     *
     * @param refs the references
     * @param removedRefs the removed references
     */
    private ReferenceTracker(final Map<ImmutableNode, Object> refs, final List<Object> removedRefs) {
        references = refs;
        removedReferences = removedRefs;
    }

    /**
     * Adds all references stored in the passed in map to the managed references. A new instance is created managing this
     * new set of references.
     *
     * @param refs the references to be added
     * @return the new instance
     */
    public ReferenceTracker addReferences(final Map<ImmutableNode, ?> refs) {
        final Map<ImmutableNode, Object> newRefs = new HashMap<>(references);
        newRefs.putAll(refs);
        return new ReferenceTracker(newRefs, removedReferences);
    }

    /**
     * Gets the reference object associated with the given node.
     *
     * @param node the node
     * @return the reference object for this node or <b>null</b>
     */
    public Object getReference(final ImmutableNode node) {
        return references.get(node);
    }

    /**
     * Gets the list with removed references. This list is immutable.
     *
     * @return the list with removed references
     */
    public List<Object> getRemovedReferences() {
        return Collections.unmodifiableList(removedReferences);
    }

    /**
     * Updates the references managed by this object at the end of a model transaction. This method is called by the
     * transaction with the nodes that have been replaced by others and the nodes that have been removed. The internal data
     * structures are updated correspondingly.
     *
     * @param replacedNodes the map with nodes that have been replaced
     * @param removedNodes the list with nodes that have been removed
     * @return the new instance
     */
    public ReferenceTracker updateReferences(final Map<ImmutableNode, ImmutableNode> replacedNodes,
                                             final Collection<ImmutableNode> removedNodes) {
        // If we have no references, nothing to update
        if (references.isEmpty()) {
            return this;
        }

        Map<ImmutableNode, Object> newRefs = null;

        // 1) Handle replaced nodes
        newRefs = handleReplacedNodes(replacedNodes, newRefs);

        // 2) If we replaced at least one node, we prepare a new list
        //    for removed references; otherwise keep it null
        List<Object> newRemovedRefs = (newRefs != null) ? new LinkedList<>(removedReferences) : null;

        // 3) Handle removed nodes
        newRefs = handleRemovedNodes(removedNodes, newRefs, newRemovedRefs);

        // 4) If we created new references, return a new ReferenceTracker
        if (newRefs != null) {
            return new ReferenceTracker(newRefs, newRemovedRefs);
        }
        // Otherwise, nothing changed
        return this;
    }

    /**
     * Processes the map of replaced nodes. If a node has a reference, we copy
     * the entire map of references (lazy creation of newRefs) and update it
     * so that the old node is replaced by the new node's reference.
     *
     * @param replacedNodes the map of old-node to new-node
     * @param newRefs       the mutable reference map (may be null if not yet created)
     * @return the possibly updated reference map (unchanged or newly created)
     */
    private Map<ImmutableNode, Object> handleReplacedNodes(
            final Map<ImmutableNode, ImmutableNode> replacedNodes,
            Map<ImmutableNode, Object> newRefs) {

        for (final Map.Entry<ImmutableNode, ImmutableNode> e : replacedNodes.entrySet()) {
            final Object ref = references.get(e.getKey());
            if (ref != null) {
                // We need a fresh modifiable map if we haven't created one yet
                if (newRefs == null) {
                    newRefs = new HashMap<>(references);
                }
                newRefs.put(e.getValue(), ref);
                newRefs.remove(e.getKey());
            }
        }
        return newRefs;
    }

    /**
     * Processes the collection of removed nodes. If a node has a reference, we
     * ensure we have a new modifiable references map, remove that node's reference,
     * and add it to newRemovedRefs (also created lazily).
     *
     * @param removedNodes   the collection of nodes that got removed
     * @param newRefs        the mutable reference map (may be null if not yet created)
     * @param newRemovedRefs the list of removed references (may be null if not yet created)
     * @return the possibly updated reference map (unchanged or newly created)
     */
    private Map<ImmutableNode, Object> handleRemovedNodes(
            final Collection<ImmutableNode> removedNodes,
            Map<ImmutableNode, Object> newRefs,
            List<Object> newRemovedRefs) {

        for (final ImmutableNode node : removedNodes) {
            final Object ref = references.get(node);
            if (ref != null) {
                if (newRefs == null) {
                    newRefs = new HashMap<>(references);
                }
                newRefs.remove(node);
                if (newRemovedRefs == null) {
                    newRemovedRefs = new LinkedList<>(removedReferences);
                }
                newRemovedRefs.add(ref);
            }
        }
        return newRefs;
    }
}
