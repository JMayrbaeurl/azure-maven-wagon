package com.microsoft.windowsazure.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

import com.microsoft.windowsazure.services.blob.client.CloudBlobClient;
import com.microsoft.windowsazure.services.blob.client.CloudBlockBlob;
import com.microsoft.windowsazure.services.core.storage.CloudStorageAccount;
import com.microsoft.windowsazure.services.core.storage.StorageException;

public abstract class AbstractAzureBlobWagonTestCase extends StreamingWagonTestCase {

	protected CloudBlobClient blobClient;
	
	/**
	 * 
	 */
	public AbstractAzureBlobWagonTestCase() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.WagonTestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		
		super.setUp();
		
		this.blobClient = CloudStorageAccount.parse(this.getAzureStorageConnectionStringWithKey()).createCloudBlobClient();
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.WagonTestCase#getProtocol()
	 */
	@Override
	protected String getProtocol() {
		
		return AzureBlobWagon.WAGON_PROTOCOL;
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.WagonTestCase#getTestRepositoryPort()
	 */
	@Override
	protected int getTestRepositoryPort() {
		
		return 0;
	}
	
	protected abstract String getAzureStorageConnectionString();
	
	protected String getAzureStorageAccountKey() {
		
		return StringUtils.EMPTY;
	}
	
	protected abstract String getAzureRepositoryUrl();
	
	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.WagonTestCase#getTestRepositoryUrl()
	 */
	@Override
	protected String getTestRepositoryUrl() throws IOException {
		
		return this.getAzureRepositoryUrl();
	}
	
	protected String getAzureStorageConnectionStringWithKey() {
		
		return createAzureStorageConnectionStringWithKey(
				this.getAzureStorageConnectionString(), this.getAzureStorageAccountKey());
	}
	
	protected static String createAzureStorageConnectionStringWithKey(final String storageConnectionString,
			final String storageAccountKey) {
		
		String connectionString = storageConnectionString;
		
		if (StringUtils.isEmpty(connectionString))
			throw new IllegalStateException("No Azure storage connection string provided. Must not be empty");
		
		if (!connectionString.startsWith(AzureBlobWagon.USEDEVELOPMENTSTORAGE_STRING) 
				&& !connectionString.contains(AzureBlobWagon.ACCOUNTKEY_STRING)) {
			if (StringUtils.isEmpty(storageAccountKey))
				throw new IllegalStateException("No Azure storage account key provided. Must not be empty");
			else
				connectionString = connectionString.concat(";" + 
						AzureBlobWagon.ACCOUNTKEY_STRING + "=" + storageAccountKey);
		}
		
		return connectionString;
	}
	
	protected boolean createAzureBlobContainer(final String containername) {
		
		if (StringUtils.isEmpty(containername))
			throw new IllegalArgumentException("Parameter 'containername' must not be empty");
		
		if (this.blobClient == null)
			throw new IllegalStateException("No Azure blob client available");
		
		return createAzureBlobContainer(containername, this.blobClient);
	}
	
	protected static boolean createAzureBlobContainer(final String containername, final CloudBlobClient blobClient) {

		if (StringUtils.isEmpty(containername))
			throw new IllegalArgumentException("Parameter 'containername' must not be empty");
		
		if (blobClient == null)
			throw new IllegalArgumentException("Parameter 'blobClient' must not be empty");
		
		try {
			return blobClient.getContainerReference(containername).createIfNotExist();
		} catch (StorageException e) {
			logger.error("Could not create Azure blob container '" + containername + "'. Reason: " + e.getMessage());
			return false;
		} catch (URISyntaxException e) {
			logger.error("Could not create Azure blob container '" + containername + "'. Reason: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * @param containername
	 * @return
	 */
	protected boolean deleteAzureBlobContainer(final String containername) {
		
		if (StringUtils.isEmpty(containername))
			throw new IllegalArgumentException("Parameter 'containername' must not be empty");
		
		if (this.blobClient == null)
			throw new IllegalStateException("No Azure blob client available");
		
		return deleteAzureBlobContainer(containername, this.blobClient);
	}
	
	protected static boolean deleteAzureBlobContainer(final String containername, final CloudBlobClient blobClient) {
		
		if (StringUtils.isEmpty(containername))
			throw new IllegalArgumentException("Parameter 'containername' must not be empty");
		
		if (blobClient == null)
			throw new IllegalArgumentException("Parameter 'blobClient' must not be empty");
		
		try {
			return blobClient.getContainerReference(containername).deleteIfExists();
		} catch (StorageException e) {
			logger.error("Could not delete Azure blob container '" + containername + "'. Reason: " + e.getMessage());
			return false;
		} catch (URISyntaxException e) {
			logger.error("Could not delete Azure blob container '" + containername + "'. Reason: " + e.getMessage());
			return false;
		}
	}
	
	protected boolean createAzureBlob(final String blobPath, final File sourceFile) {
		
		boolean created = false;
		
		if (StringUtils.isEmpty(blobPath))
			throw new IllegalArgumentException("Parameter 'blobPath' must not be empty");
		
		if (sourceFile == null)
			throw new IllegalArgumentException("Parameter 'sourceFile' must not be null");
		
		if (this.blobClient == null)
			throw new IllegalStateException("No Azure blob client available");	
		
		try {
			CloudBlockBlob blob = this.blobClient.getBlockBlobReference(blobPath);
			if (!blob.exists()) {
				blob.getContainer().createIfNotExist();
				blob.upload(new FileInputStream(sourceFile), sourceFile.length());
				
				created = true;
			}
		} catch (URISyntaxException e) {
			logger.error("Could not create Azure blob '" + blobPath + "'. Reason: " + e.getMessage());
			throw new IllegalStateException("Could not create Azure blob '" + blobPath + "'. Reason: " + e.getMessage(), e);
		} catch (StorageException e) {
			logger.error("Could not create Azure blob '" + blobPath + "'. Reason: " + e.getMessage());
			throw new IllegalStateException("Could not create Azure blob '" + blobPath + "'. Reason: " + e.getMessage(), e);
		} catch (FileNotFoundException e) {
			logger.error("Could not create Azure blob '" + blobPath + "'. Reason: " + e.getMessage());
			throw new IllegalStateException("Could not create Azure blob '" + blobPath + "'. Reason: " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Could not create Azure blob '" + blobPath + "'. Reason: " + e.getMessage());
			throw new IllegalStateException("Could not create Azure blob '" + blobPath + "'. Reason: " + e.getMessage(), e);
		}
		
		return created;
	}
	
	protected boolean deleteAzureBlob(final String blobPath) {
		
		if (StringUtils.isEmpty(blobPath))
			throw new IllegalArgumentException("Parameter 'blobPath' must not be empty");
		
		if (this.blobClient == null)
			throw new IllegalStateException("No Azure blob client available");
		
		this.message("Deleting blob '" + blobPath + "' from Azure storage account '" + 
				this.blobClient.getEndpoint());
		
		try {
			return this.blobClient.getBlockBlobReference(blobPath).deleteIfExists();
		} catch (StorageException e) {
			logger.error("Could not delete Azure blob '" + blobPath + "'. Reason: " + e.getMessage());
			return false;
		} catch (URISyntaxException e) {
			logger.error("Could not delete Azure blob '" + blobPath + "'. Reason: " + e.getMessage());
			return false;
		}
	}
	
	protected boolean blobExists(final String blobPath) {
		
		if (StringUtils.isEmpty(blobPath))
			throw new IllegalArgumentException("Parameter 'blobPath' must not be empty");

		if (this.blobClient == null)
			throw new IllegalStateException("No Azure blob client available");
		
		try {
			return this.blobClient.getBlockBlobReference(blobPath).exists();
		} catch (StorageException e) {
			logger.error("Could not access Azure blob '" + blobPath + "'. Reason: " + e.getMessage());
			return false;
		} catch (URISyntaxException e) {
			logger.error("Could not access Azure blob '" + blobPath + "'. Reason: " + e.getMessage());
			return false;
		}
	}
	
	protected boolean containerExists(final String containername) {
		
		if (StringUtils.isEmpty(containername))
			throw new IllegalArgumentException("Parameter 'containername' must not be empty");

		if (this.blobClient == null)
			throw new IllegalStateException("No Azure blob client available");
		
		boolean exists = false;
		
		try {
			exists = this.blobClient.getContainerReference(containername).exists();
		} catch (StorageException e) {
			logger.error("Could not access Azure blob container '" + containername + "'. Reason: " + e.getMessage());
		} catch (URISyntaxException e) {
			logger.error("Could not access Azure blob container '" + containername + "'. Reason: " + e.getMessage());
		}
		
		return exists;
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.WagonTestCase#getExpectedLastModifiedOnGet(org.apache.maven.wagon.repository.Repository, org.apache.maven.wagon.resource.Resource)
	 */
	@Override
	protected long getExpectedLastModifiedOnGet(final Repository repository,
			final Resource resource) {
		
		try {
			String blobPath = resource.getName();		
			
			String basecontainer = ConnectionStringUtils.blobContainer(this.getAzureRepositoryUrl());
			if (StringUtils.isNotEmpty(basecontainer)) {
				blobPath = basecontainer + 
						(basecontainer.endsWith("/") ? resource.getName() : ("/" + resource.getName()));
			}
			
			CloudBlockBlob blob = this.blobClient.getBlockBlobReference(blobPath);
			blob.downloadAttributes();
			
			return blob.getProperties().getLastModified().getTime();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return super.getExpectedLastModifiedOnGet(repository, resource);
		} catch (StorageException e) {
			e.printStackTrace();
			return super.getExpectedLastModifiedOnGet(repository, resource);
		}
	}
}
