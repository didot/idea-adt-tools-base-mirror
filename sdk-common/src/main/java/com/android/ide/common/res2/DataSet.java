/*
 * Copyright (C) 2012 The Android Open Source Project
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
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.blame.Message;
import com.android.utils.ILogger;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Represents a set of {@link DataItem}s.
 *
 * <p>The items can be coming from multiple source folders, and duplicates are detected.
 *
 * <p>Each source folders is considered to be at the same level. To use overlays, a {@link
 * DataMerger} must be used.
 *
 * <p>Creating the set and adding folders does not load the data. The data can be loaded from the
 * files, or from a blob which is generated by the set itself.
 *
 * <p>Upon loading the data from the blob, the data can be updated with fresher files. Each item
 * that is updated is flagged as such, in order to manage incremental update.
 *
 * <p>Writing/Loading the blob is not done through this class directly, but instead through the
 * {@link DataMerger} which contains DataSet objects.
 */
abstract class DataSet<I extends DataItem<F>, F extends DataFile<I>>
        implements SourceSet, DataMap<I> {
    static final String NODE_SOURCE = "source";
    static final String NODE_FILE = "file";
    static final String ATTR_CONFIG = "config";
    static final String ATTR_PATH = "path";
    static final String ATTR_NAME = "name";
    static final String ATTR_TIMESTAMP = "timestamp";

    private final String mConfigName;

    private final boolean mValidateEnabled;

    /** List of source files. The may not have been loaded yet. */
    private final List<File> mSourceFiles = new ArrayList<>();

    /**
     * The key is the {@link DataItem#getKey()}. This is a multimap to support moving a data item
     * from one file to another (values file) during incremental update.
     *
     * <p>Use LinkedListMultimap to preserve original order of items for any display of resources
     * that want to show them in order.
     */
    private final ListMultimap<String, I> mItems = LinkedListMultimap.create();

    /**
     * Map of source files to DataFiles. This is a multimap because the key is the source
     * file/folder, not the File for the DataFile itself.
     */
    private final ListMultimap<File, F> mSourceFileToDataFilesMap = ArrayListMultimap.create();

    /** Map from a File to its DataFile. */
    private final Map<File, F> mDataFileMap = new HashMap<>();

    /**
     * Creates a DataSet with a given configName. The name is used to identify the set across
     * sessions.
     *
     * @param configName the name of the config this set is associated with.
     */
    public DataSet(String configName, boolean validateEnabled) {
        mConfigName = configName;
        mValidateEnabled = validateEnabled;
    }

    protected abstract DataSet<I, F> createSet(String name);

    /**
     * Creates a DataFile and associated DataItems from an XML node from a file created with
     * {@link DataSet#appendToXml(Node, Document, MergeConsumer, boolean)}
     *
     * @param file the file represented by the DataFile
     * @param fileNode the XML node.
     * @return a DataFile
     */
    protected abstract F createFileAndItemsFromXml(@NonNull File file, @NonNull Node fileNode)
            throws MergingException;

    /**
     * Reads the content of a data folders and loads the DataItem.
     *
     * This should generate DataFiles, and process them with
     * {@link #processNewDataFile(java.io.File, DataFile, boolean)}.
     *
     * @param sourceFolder the source folder to load the resources from.
     *
     * @throws MergingException if something goes wrong
     */
    protected abstract void readSourceFolder(File sourceFolder, ILogger logger)
            throws MergingException;

    @Nullable
    protected abstract F createFileAndItems(File sourceFolder, File file, ILogger logger)
            throws MergingException;

    /**
     * Adds a collection of source files.
     * @param files the source files to add.
     */
    public void addSources(Collection<File> files) {
        mSourceFiles.addAll(files);
    }

    /**
     * Adds a new source file
     *
     * @param file the source file.
     */
    public void addSource(File file) {
        mSourceFiles.add(file);
    }

    /** Returns the list of source files. */
    @NonNull
    @Override
    public List<File> getSourceFiles() {
        return mSourceFiles;
    }

    /**
     * Returns the config name.
     * @return the config name.
     */
    public String getConfigName() {
        return mConfigName;
    }

    /**
     * Returns the longest path matching Source file that contains a given file.
     *
     * "contains" means that the source file/folder is the root folder
     * of this file. The folder and/or file doesn't have to exist.
     * "longest" means that if two source folder satisfy the contains predicate above, the longest
     * absolute path will be returned.
     *
     * @param file the file to search for
     * @return the Source file or null if no match is found.
     */
    @Override
    public File findMatchingSourceFile(File file) {
        File matchingSourceFile = null;
        for (File sourceFile : mSourceFiles) {
            if (sourceFile.equals(file)) {
                return sourceFile;
            } else if (sourceFile.isDirectory()) {
                String sourcePath = sourceFile.getAbsolutePath() + File.separator;
                if (file.getAbsolutePath().startsWith(sourcePath) &&
                        (matchingSourceFile == null ||
                        matchingSourceFile.getAbsolutePath().length()
                                < sourceFile.getAbsolutePath().length())) {
                    matchingSourceFile = sourceFile;
                }
            }
        }

        return matchingSourceFile;
    }

    /**
     * Returns the number of items.
     * @return the number of items.
     *
     * @see DataMap
     */
    @Override
    public int size() {
        // returns the number of keys, not the size of the multimap which would include duplicate
        // ResourceItem objects.
        return mItems.keySet().size();
    }

    /**
     * Returns whether the set is empty of items.
     * @return true if the set contains no items.
     */
    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    /**
     * Returns a map of the items.
     * @return a map of items.
     *
     * @see DataMap
     */
    @NonNull
    @Override
    public ListMultimap<String, I> getDataMap() {
        return mItems;
    }

    /**
     * Loads the DataSet from the files its source folders contain.
     *
     * All loaded items are set to TOUCHED. This is so that after loading the resources from
     * the files, they can be written directly (since touched force them to be written).
     *
     * This also checks for duplicates items.
     *
     * @throws MergingException if something goes wrong
     */
    public void loadFromFiles(ILogger logger) throws MergingException {
        List<Message> errors = new ArrayList<>();
        for (File file : mSourceFiles) {
            if (file.isDirectory()) {
                try {
                    readSourceFolder(file, logger);
                } catch (MergingException e) {
                    errors.addAll(e.getMessages());
                }

            } else if (file.isFile()) {
                // TODO support resource bundle
            }
        }
        MergingException.throwIfNonEmpty(errors);
        checkItems();
    }

    /**
     * Loads a single dataFile from the given source folder (rather than load / parse from all
     * source folders).
     *
     * <p>Like loadFromFiles, all loaded items are set to TOUCHED.
     *
     * @param sourceFolder the source folder
     * @param dataFile the data file within source folder
     * @param logger logs errors
     * @return a DataFile if successfully loaded, and null otherwise
     */
    @Nullable
    public F loadFile(@NonNull File sourceFolder, @NonNull File dataFile, @NonNull ILogger logger)
            throws MergingException {
        return handleNewFile(sourceFolder, dataFile, logger);
    }

    /**
     * Appends the DataSet to a given DOM object.
     *
     * @param setNode the root node for this set.
     * @param document The root XML document
     * @param includeTimestamps whether or not FILE nodes should be tagged with a timestamp.
     */
    void appendToXml(@NonNull Node setNode, @NonNull Document document,
                     @NonNull MergeConsumer<I> consumer, boolean includeTimestamps) {
        // add the config name attribute
        NodeUtils.addAttribute(document, setNode, null, ATTR_CONFIG, mConfigName);

        // add the source files.
        // we need to loop on the source files themselves and not the map to ensure we
        // write empty resourceSets
        for (File sourceFile : mSourceFiles) {

            // the node for the source and its path attribute
            Node sourceNode = document.createElement(NODE_SOURCE);
            setNode.appendChild(sourceNode);
            NodeUtils.addAttribute(document, sourceNode, null, ATTR_PATH,
                    sourceFile.getAbsolutePath());

            Collection<F> dataFiles = mSourceFileToDataFilesMap.get(sourceFile);

            for (F dataFile : dataFiles) {
                if (!dataFile.hasNotRemovedItems()) {
                    // We have either removed the file (and all its inputs), or the file is empty.
                    if (dataFile.mFile.exists()) {
                        createFileElement(document, sourceNode, dataFile, includeTimestamps);
                    }
                    continue;
                }

                Node fileNode = null;
                switch (dataFile.getType()) {
                    case GENERATED_FILES:
                        // Fall through. getDetailsXml() will return the XML which describes the
                        // generated files.
                    case XML_VALUES:
                        for (I item : dataFile.getItems()) {
                            if (item.isRemoved() || consumer.ignoreItemInMerge(item)) {
                                continue;
                            }
                            if (fileNode == null) {
                                fileNode =
                                        createFileElement(
                                                document, sourceNode, dataFile, includeTimestamps);
                            }
                            Node adoptedNode = item.getDetailsXml(document);
                            if (adoptedNode != null) {
                                fileNode.appendChild(adoptedNode);
                            }
                        }
                        break;
                    case SINGLE_FILE:
                        // no need to check for isRemoved here since it's checked
                        // at the file level and there's only one item.
                        I dataItem = dataFile.getItem();
                        if (consumer.ignoreItemInMerge(dataItem)) {
                            continue;
                        }
                        fileNode =
                                createFileElement(
                                        document, sourceNode, dataFile, includeTimestamps);
                        NodeUtils.addAttribute(
                                document, fileNode, null, ATTR_NAME, dataItem.getName());
                        dataItem.addExtraAttributes(document, fileNode, null);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }
    }

    private Node createFileElement(
            @NonNull Document document, Node sourceNode, F dataFile, boolean includeTimestamps) {
        // the node for the file and its path and qualifiers attribute
        Node fileNode = document.createElement(NODE_FILE);
        sourceNode.appendChild(fileNode);
        NodeUtils.addAttribute(document, fileNode, null, ATTR_PATH,
                               dataFile.getFile().getAbsolutePath());
        if (includeTimestamps) {
            long timestamp = dataFile.getFile().lastModified();
            if (timestamp != 0) {
                NodeUtils.addAttribute(
                        document, fileNode, null, ATTR_TIMESTAMP, Long.toString(timestamp));
            }
        }
        dataFile.addExtraAttributes(document, fileNode, null);
        return fileNode;
    }

    /**
     * Creates and returns a new DataSet from an XML node that was created with
     * {@link #appendToXml(Node, Document, MergeConsumer, boolean)}
     *
     * The object this method is called on is not modified. This should be static but can't be
     * due to children classes.
     *
     * @param dataSetNode the node to read from.
     * @return a new DataSet object or null.
     */
    DataSet<I,F> createFromXml(Node dataSetNode) throws MergingException {
        // get the config name
        Attr configNameAttr = (Attr) dataSetNode.getAttributes().getNamedItem(ATTR_CONFIG);
        if (configNameAttr == null) {
            return null;
        }

        // create the DataSet that will be filled with the content of the XML.
        DataSet<I, F> dataSet = createSet(configNameAttr.getValue());

        // loop on the source nodes
        NodeList sourceNodes = dataSetNode.getChildNodes();
        for (int i = 0, n = sourceNodes.getLength(); i < n; i++) {
            Node sourceNode = sourceNodes.item(i);

            if (sourceNode.getNodeType() != Node.ELEMENT_NODE ||
                    !NODE_SOURCE.equals(sourceNode.getLocalName())) {
                continue;
            }

            Attr pathAttr = (Attr) sourceNode.getAttributes().getNamedItem(ATTR_PATH);
            if (pathAttr == null) {
                continue;
            }

            File sourceFolder = new File(pathAttr.getValue());
            dataSet.mSourceFiles.add(sourceFolder);

            // now loop on the files inside the source folder.
            NodeList fileNodes = sourceNode.getChildNodes();
            for (int j = 0, m = fileNodes.getLength(); j < m; j++) {
                Node fileNode = fileNodes.item(j);

                if (fileNode.getNodeType() != Node.ELEMENT_NODE ||
                        !NODE_FILE.equals(fileNode.getLocalName())) {
                    continue;
                }

                pathAttr = (Attr) fileNode.getAttributes().getNamedItem(ATTR_PATH);
                if (pathAttr == null) {
                    continue;
                }
                File actualFile = new File(pathAttr.getValue());
                // Check the optional timestamp.
                Attr timestampAttr = (Attr) fileNode.getAttributes().getNamedItem(ATTR_TIMESTAMP);
                if (timestampAttr != null) {
                    try {
                        long blobDataFileTimestamp = Long.parseLong(timestampAttr.getValue());
                        long actualFileTimestamp = actualFile.lastModified();
                        if (actualFileTimestamp == 0 || blobDataFileTimestamp < actualFileTimestamp) {
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
                
                F dataFile = createFileAndItemsFromXml(actualFile, fileNode);

                if (dataFile != null) {
                    dataSet.processNewDataFile(sourceFolder, dataFile, false /*setTouched*/);
                }
            }
        }

        return dataSet;
    }

    /**
     * Checks for duplicate items across all source files.
     *
     * @throws DuplicateDataException if a duplicated item is found.
     */
    protected void checkItems() throws DuplicateDataException {
        if (!mValidateEnabled) {
            return;
        }
        Collection<Collection<I>> duplicateCollections = new ArrayList<>();
        // check a list for duplicate, ignoring removed items.
        for (Map.Entry<String, Collection<I>> entry : mItems.asMap().entrySet()) {
            Collection<I> items = entry.getValue();

            // there can be several version of the same key if some are "removed"
            I lastItem = null;
            for (I item : items) {
                if (!item.isRemoved()) {
                    if (lastItem == null) {
                        lastItem = item;
                    } else {
                        // We have duplicates, store them and throw the exception later, so
                        // the user gets all the error messages at once.
                        duplicateCollections.add(items);
                    }
                }
            }
        }
        if (!duplicateCollections.isEmpty()) {
            throw new DuplicateDataException(
                    DuplicateDataException.createMessages(duplicateCollections));
        }
    }

    /**
     * Update the DataSet with a given file.
     *
     * @param sourceFolder the sourceFile containing the changedFile
     * @param changedFile The changed file
     * @param fileStatus the change state
     * @return true if the set was properly updated, false otherwise
     * @throws MergingException if something goes wrong
     */
    public boolean updateWith(File sourceFolder, File changedFile, FileStatus fileStatus,
                              ILogger logger)
            throws MergingException {
        switch (fileStatus) {
            case NEW:
                handleNewFile(sourceFolder, changedFile, logger);
                return true;
            case CHANGED:
                return handleChangedFile(sourceFolder, changedFile, logger);
            case REMOVED:
                return handleRemovedFile(changedFile);
        }

        return false;
    }

    protected boolean handleRemovedFile(File removedFile) {
        F dataFile = getDataFile(removedFile);

        if (dataFile == null) {
            return false;
        }

        // flag all resource items are removed
        for (I dataItem : dataFile.getItems()) {
            dataItem.setRemoved();
        }
        return true;
    }

    protected boolean isValidSourceFile(@NonNull File sourceFolder, @NonNull File file) {
        return checkFileForAndroidRes(file);
    }

    @Nullable
    protected F handleNewFile(File sourceFolder, File file, ILogger logger)
            throws MergingException {
        F dataFile = createFileAndItems(sourceFolder, file, logger);
        if (dataFile != null) {
            processNewDataFile(sourceFolder, dataFile, true /*setTouched*/);
        }
        return dataFile;
    }

    protected void processNewDataFile(@NonNull File sourceFolder,
                                      @NonNull F dataFile,
                                      boolean setTouched) throws MergingException {
        Collection<I> dataItems = dataFile.getItems();

        addDataFile(sourceFolder, dataFile);

        for (I dataItem : dataItems) {
            mItems.put(dataItem.getKey(), dataItem);
            if (setTouched) {
                dataItem.setTouched();
            }
        }
    }

    protected boolean handleChangedFile(
            @NonNull File sourceFolder,
            @NonNull File changedFile,
            @NonNull ILogger logger) throws MergingException {
        F dataFile = mDataFileMap.get(changedFile);
        for (I item : dataFile.getItems()) {
            item.setTouched();
        }
        return true;
    }

    protected void addItem(@NonNull I item, @Nullable String key) throws MergingException {
        if (key == null) {
            key = item.getKey();
        }

        mItems.put(key, item);
    }

    protected F getDataFile(@NonNull File file) {
        return mDataFileMap.get(file);
    }

    /**
     * Adds a new DataFile to this.
     *
     * @param sourceFile the parent source file.
     * @param dataFile the DataFile
     */
    @VisibleForTesting
    void addDataFile(@NonNull File sourceFile, @NonNull F dataFile) {
        mSourceFileToDataFilesMap.put(sourceFile, dataFile);
        mDataFileMap.put(dataFile.getFile(), dataFile);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .addValue(mConfigName)
                .add("sources", Arrays.toString(mSourceFiles.toArray()))
                .toString();
    }

    /**
     * Checks a file to make sure it is a valid file in the android res/asset folders.
     * @param file the file to check
     * @return true if it is a valid file, false if it should be ignored.
     */
    protected boolean checkFileForAndroidRes(@NonNull File file) {
        return !isIgnored(file);
    }

    /**
     * The pattern to use for ignoring asset files. Defaults to the same value as aapt but
     * can be customized via {@code $ANDROID_AAPT_IGNORE}.
     * <p>
     * Patterns syntax:
     * <ul>
     *   <li> Delimiter is :
     *   <li> Entry can start with the flag ! to avoid printing a warning
     *        about the file being ignored.
     *   <li> Entry can have the flag {@code <dir>} to match only directories
     *        or {@code <file>} to match only files. Default is to match both.
     *   <li> Entry can be a simplified glob {@code <prefix>*} or {@code *<suffix>}
     *        where prefix/suffix must have at least 1 character (so that
     *        we don't match a '*' catch-all pattern.)
     *   <li> The special filenames "." and ".." are always ignored.
     *   <li> Otherwise the full string is matched.
     *   <li> match is not case-sensitive.
     * </ul>
     */
    private Iterable<String> sIgnoredPatterns;

    {
        String patterns = System.getenv("ANDROID_AAPT_IGNORE"); //$NON-NLS-1$
        if (patterns == null || patterns.isEmpty()) {
            // Matches aapt: frameworks/base/tools/aapt/AaptAssets.cpp:gDefaultIgnoreAssets
            patterns = "!.svn:!.git:!.ds_store:!*.scc:.*:<dir>_*:!CVS:!thumbs.db:!picasa.ini:!*~";
        }

        setIgnoredPatterns(patterns);
    }

    public void setIgnoredPatterns(String aaptStylePattern) {
        // don't keep the result of split and put it in a new list instead.
        // This is because the custom iterable returned by Splitter does not implement equals.
        sIgnoredPatterns = Splitter.on(':').splitToList(aaptStylePattern);
    }

    /**
     * Returns whether the given file should be ignored.
     *
     * @param file the file to check
     * @return true if the file is hidden
     */
    public boolean isIgnored(@NonNull File file) {
        String path = file.getPath();
        int nameIndex = path.lastIndexOf(File.separatorChar) + 1;

        if (path.equals(".") || path.equals("..")) {
            return true;
        }

        boolean ignore = false;
        boolean isDirectory = file.isDirectory();

        int nameLength = path.length() - nameIndex;
        for (String token : sIgnoredPatterns) {
            if (token.isEmpty()) {
                continue;
            }
            int tokenIndex = 0;
            if (token.charAt(tokenIndex) == '!') {
                tokenIndex++; // skip !
            }

            if (token.regionMatches(tokenIndex, "<dir>", 0, 5)) {
                if (!isDirectory) {
                    continue;
                }
                tokenIndex += 5;
            }
            if (token.regionMatches(tokenIndex, "<file>", 0, 6)) {
                if (isDirectory) {
                    continue;
                }
                tokenIndex += 6;
            }

            int n = token.length() - tokenIndex;

            if (token.charAt(tokenIndex) == '*') {
                // Match *suffix such as *.scc or *~
                tokenIndex++;
                n--;
                if (n <= nameLength) {
                    ignore = token.regionMatches(true, tokenIndex, path,
                            nameIndex + nameLength - n, n);
                }
            } else if (n > 1 && token.charAt(token.length() - 1) == '*') {
                // Match prefix* such as .* or _*
                ignore = token.regionMatches(true, tokenIndex, path, nameIndex, n - 1);
            } else {
                // Match exactly, such as thumbs.db, .git, etc.
                ignore = (token.length() - tokenIndex) == (path.length() - nameIndex)
                        && token.regionMatches(true, tokenIndex, path, nameIndex,
                        path.length() - nameIndex);
            }

            if (ignore) {
                break;
            }
        }

        return ignore;
    }

    protected boolean getValidateEnabled() {
        return mValidateEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataSet<?, ?> dataSet = (DataSet<?, ?>) o;
        return mValidateEnabled == dataSet.mValidateEnabled &&
                Objects.equals(mConfigName, dataSet.mConfigName) &&
                Objects.equals(mSourceFiles, dataSet.mSourceFiles) &&
                Objects.equals(mItems, dataSet.mItems) &&
                Objects.equals(mSourceFileToDataFilesMap, dataSet.mSourceFileToDataFilesMap)
                &&
                Objects.equals(mDataFileMap, dataSet.mDataFileMap) &&
                Objects.equals(sIgnoredPatterns, dataSet.sIgnoredPatterns);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(mConfigName, mValidateEnabled, mSourceFiles, mItems,
                        mSourceFileToDataFilesMap,
                        mDataFileMap, sIgnoredPatterns);
    }
}
