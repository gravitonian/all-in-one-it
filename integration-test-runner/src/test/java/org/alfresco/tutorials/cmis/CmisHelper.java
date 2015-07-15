/*
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package org.alfresco.tutorials.cmis;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author martin.bergljung@alfresco.com
 */
public class CmisHelper {
    private static Log logger = LogFactory.getLog(CmisHelper.class);

    // Map with all open connections, will only be one for now
    private static Map<String, Session> connections = new ConcurrentHashMap<String, Session>();

    /**
     * Get an Open CMIS session to use when talking to the Alfresco repo.
     * Will check if there is already a connection to the Alfresco repo
     * and re-use that session.
     *
     * @param connectionName the name of the new connection to be created
     * @param username       the Alfresco username to connect with
     * @param pwd            the Alfresco password to connect with
     * @return an Open CMIS Session object
     */
    public Session getSession(String connectionName, String username, String pwd) {
        Session session = connections.get(connectionName);
        if (session == null) {
            logger.info("Not connected, creating new connection to Alfresco with the connection id ("
                    + connectionName + ")");

            // No connection to Alfresco available, create a new one
            SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put(SessionParameter.USER, username);
            parameters.put(SessionParameter.PASSWORD, pwd);

//            parameters.put(SessionParameter.ATOMPUB_URL,
  //                  "http://localhost:8080/alfresco/api/-default-/cmis/versions/1.1/atom");
    //        parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

            parameters.put(SessionParameter.BROWSER_URL,
                    "http://localhost:8080/alfresco/api/-default-/cmis/versions/1.1/browser");
            parameters.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());

            parameters.put(SessionParameter.COMPRESSION, "true");
            parameters.put(SessionParameter.CACHE_TTL_OBJECTS, "0");

            // If there is only one repository exposed (e.g. Alfresco), these
            // lines will help detect it and its ID
            List<Repository> repositories = sessionFactory.getRepositories(parameters);
            Repository alfrescoRepository = null;
            if (repositories != null && repositories.size() > 0) {
                logger.info("Found (" + repositories.size() + ") Alfresco repositories");
                alfrescoRepository = repositories.get(0);
                logger.info("Info about the first Alfresco repo [ID=" + alfrescoRepository.getId() +
                        "][name=" + alfrescoRepository.getName() +
                        "][CMIS ver supported=" + alfrescoRepository.getCmisVersionSupported() + "]");
            } else {
                throw new CmisConnectionException(
                        "Could not connect to the Alfresco Server, no repository found!");
            }

            // Create a new session with the Alfresco repository
            session = alfrescoRepository.createSession();

            // Save connection for reuse
            connections.put(connectionName, session);
        } else {
            logger.info("Already connected to Alfresco with the connection id (" + connectionName + ")");
        }

        return session;
    }

    /**
     * Create a folder under /Company Home.
     *
     * @param session the connection session
     * @param folderName the name of the folder to create
     * @return the folder object for the newly created folder
     */
    public Folder createFolder(Session session, String folderName) {
        Folder parentFolder = session.getRootFolder();

        // Make sure the user is allowed to create a folder under the root folder
        if (parentFolder.getAllowableActions().getAllowableActions().contains(Action.CAN_CREATE_FOLDER) == false) {
            throw new CmisUnauthorizedException("Current user does not have permission to create a sub-folder in " +
                    parentFolder.getPath());
        }

        // Check if folder already exist, if not create it
        Folder newFolder = (Folder) getObject(session, parentFolder, folderName);
        if (newFolder == null) {
            Map<String, Object> newFolderProps = new HashMap<String, Object>();
            newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
            newFolderProps.put(PropertyIds.NAME, folderName);
            newFolder = parentFolder.createFolder(newFolderProps);

            logger.info("Created new folder: " + newFolder.getPath() +
                    " [creator=" + newFolder.getCreatedBy() + "][created=" +
                    date2String(newFolder.getCreationDate().getTime()) + "]");
        } else {
            logger.info("Folder already exist: " + newFolder.getPath());
        }

        return newFolder;
    }

    /**
     * Create a text document with sample text in passed in folder.
     *
     * @param session the connection session
     * @param parentFolder the folder where the document should be created
     * @param documentName the name of the document file that should be created
     * @return the document object if the text document was created successfully
     * @throws IOException if text document could not be created
     */
    public Document createDocument(Session session, Folder parentFolder, String documentName) throws IOException {
        // Make sure the user is allowed to create a document in the passed in folder
        if (parentFolder.getAllowableActions().getAllowableActions().contains(Action.CAN_CREATE_DOCUMENT) == false) {
            throw new CmisUnauthorizedException("Current user does not have permission to create a document in " +
                    parentFolder.getPath());
        }

        // Check if document already exist, if not create it
        Document newDocument = (Document) getObject(session, parentFolder, documentName);
        if (newDocument == null) {
            // Setup document metadata
            Map<String, Object> newDocumentProps = new HashMap<String, Object>();
            String typeId = "cmis:document";
            newDocumentProps.put(PropertyIds.OBJECT_TYPE_ID, typeId);
            newDocumentProps.put(PropertyIds.NAME, documentName);

            // Setup document content
            String mimetype = "text/plain; charset=UTF-8";
            String documentText = "This is a test document!";
            byte[] bytes = documentText.getBytes("UTF-8");
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            ContentStream contentStream = session.getObjectFactory().createContentStream(
                    documentName, bytes.length, mimetype, input);

            // Check if we need versioning
            VersioningState versioningState = VersioningState.NONE;
            DocumentType docType = (DocumentType) session.getTypeDefinition(typeId);
            if (Boolean.TRUE.equals(docType.isVersionable())) {
                logger.info("Document type " + typeId + " is versionable, setting MAJOR version state.");
                versioningState = VersioningState.MAJOR;
            }

            // Create versioned document object
            newDocument = parentFolder.createDocument(newDocumentProps, contentStream, versioningState);

            logger.info("Created new document: " + getDocumentPath(newDocument) +
                    " [version=" + newDocument.getVersionLabel() + "][creator=" + newDocument.getCreatedBy() +
                    "][created=" + date2String(newDocument.getCreationDate().getTime()) + "]");
        } else {
            logger.info("Document already exist: " + getDocumentPath(newDocument));
        }

        return newDocument;
    }

    /**
     * Get a CMIS Object by name from a specified folder.
     *
     * @param parentFolder the parent folder where the object might exist
     * @param objectName   the name of the object that we are looking for
     * @return the Cmis Object if it existed, otherwise null
     */
    private CmisObject getObject(Session session, Folder parentFolder, String objectName) {
        CmisObject object = null;

        try {
            String path2Object = parentFolder.getPath();
            if (!path2Object.endsWith("/")) {
                path2Object += "/";
            }
            path2Object += objectName;
            object = session.getObjectByPath(path2Object);
        } catch (CmisObjectNotFoundException nfe0) {
            // Nothing to do, object does not exist
        }

        return object;
    }

    /**
     * Returns date as a string
     *
     * @param date date object
     * @return date as a string formatted with "yyyy-MM-dd HH:mm:ss z"
     */
    private String date2String(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(date);
    }

    /**
     * Get the absolute path to the passed in Document object.
     * Called the primary folder path in the Alfresco world as most documents only have one parent folder.
     *
     * @param document the Document object to get the path for
     * @return the path to the passed in Document object, or "Un-filed/{object name}" if it does not have a parent folder
     */
    private String getDocumentPath(Document document) {
        String path2Doc = getParentFolderPath(document);
        if (!path2Doc.endsWith("/")) {
            path2Doc += "/";
        }
        path2Doc += document.getName();
        return path2Doc;
    }

    /**
     * Get the parent folder path for passed in Document object
     *
     * @param document the document object to get the path for
     * @return the parent folder path, or "Un-filed" if the document is un-filed and does not have a parent folder
     */
    private String getParentFolderPath(Document document) {
        Folder parentFolder = getDocumentParentFolder(document);
        return parentFolder == null ? "Un-filed" : parentFolder.getPath();
    }

    /**
     * Get the parent folder for the passed in Document object.
     * Called the primary parent folder in the Alfresco world as most documents only have one parent folder.
     *
     * @param document the Document object to get the parent folder for
     * @return the parent Folder object, or null if it does not have a parent folder and is un-filed
     */
    private Folder getDocumentParentFolder(Document document) {
        // Get all the parent folders (could be more than one if multi-filed)
        List<Folder> parentFolders = document.getParents();

        // Grab the first parent folder
        if (parentFolders.size() > 0) {
            if (parentFolders.size() > 1) {
                logger.info("The " + document.getName() + " has more than one parent folder, it is multi-filed");
            }

            return parentFolders.get(0);
        } else {
            logger.info("Document " + document.getName() + " is un-filed and does not have a parent folder");
            return null;
        }
    }
}
