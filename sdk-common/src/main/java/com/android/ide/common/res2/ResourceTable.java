/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.common.res2;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.android.resources.ResourceUrl;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Mutable, three-dimensional table for storing {@link ResourceItem}, indexed by components of a
 * {@link ResourceUrl}.
 *
 * <p>The first dimension is namespace. Can be taken straight from {@link ResourceUrl}.
 *
 * <p>The second dimension is the type of resources in question.
 *
 * <p>The value is a multimap that maps resource name (third dimension) to all matching {@link
 * ResourceItem}s. There can be multiple items defined under the same name with different resource
 * qualifiers.
 *
 * @see NamespaceAwareTable
 * @see com.android.ide.common.resources.configuration.FolderConfiguration
 * @see com.android.ide.common.resources.configuration.ResourceQualifier
 */
public final class ResourceTable
        extends NamespaceAwareTable<ResourceType, ListMultimap<String, ResourceItem>> {

    public ResourceTable() {
        super(Tables.newCustomTable(new HashMap<>(), () -> Maps.newEnumMap(ResourceType.class)));
    }

    /**
     * Removes the given {@link ResourceItem} from the table, making sure no empty multimaps are
     * left as {@link Table} values. This way the set of rows and columns we get from the {@link
     * Table} reflects reality.
     */
    public void remove(ResourceItem resourceItem) {
        String namespace = resourceItem.getNamespace();
        ResourceType type = resourceItem.getType();
        String name = resourceItem.getName();

        ListMultimap<String, ResourceItem> multimap = get(namespace, type);
        if (multimap != null) {
            multimap.remove(name, resourceItem);
            if (multimap.isEmpty()) {
                remove(namespace, type);
            }
        }
    }

    /**
     * Gets the corresponding multimap from the table, if necessary creating an empty one and
     * putting it in the table.
     */
    @NonNull
    public ListMultimap<String, ResourceItem> getOrPutEmpty(
            @Nullable String namespace, @NonNull ResourceType resourceType) {
        ListMultimap<String, ResourceItem> multimap = get(namespace, resourceType);
        if (multimap == null) {
            multimap = ArrayListMultimap.create();
            put(namespace, resourceType, multimap);
        }
        return multimap;
    }

    /**
     * Updates this {@link ResourceTable} by listening to events emitted by the {@link
     * ResourceMerger}.
     *
     * <p>Only makes sense for a newly created {@link ResourceTable} or if the table was initialized
     * by the same {@link ResourceMerger} before.
     */
    public void update(ResourceMerger merger) {
        MergeConsumer<ResourceItem> mergeConsumer =
                new MergeConsumer<ResourceItem>() {
                    @Override
                    public void start(@NonNull DocumentBuilderFactory factory)
                            throws ConsumerException {}

                    @Override
                    public void end() throws ConsumerException {}

                    @Override
                    public void addItem(@NonNull ResourceItem item) throws ConsumerException {
                        if (item.isTouched()) {
                            ListMultimap<String, ResourceItem> multimap =
                                    ResourceTable.this.getOrPutEmpty(
                                            item.getNamespace(), item.getType());

                            if (!multimap.containsEntry(item.getName(), item)) {
                                multimap.put(item.getName(), item);
                            }
                        }
                    }

                    @Override
                    public void removeItem(
                            @NonNull ResourceItem removedItem, @Nullable ResourceItem replacedBy)
                            throws ConsumerException {
                        ResourceTable.this.remove(removedItem);
                    }

                    @Override
                    public boolean ignoreItemInMerge(ResourceItem item) {
                        // we never ignore any item.
                        return false;
                    }
                };
        try {
            merger.mergeData(mergeConsumer, true);
        } catch (MergingException e) {
            throw new RuntimeException(e);
        }
    }
}
