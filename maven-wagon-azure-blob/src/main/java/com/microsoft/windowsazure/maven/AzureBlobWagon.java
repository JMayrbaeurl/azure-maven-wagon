/**
 * 
 */
package com.microsoft.windowsazure.maven;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;

import com.microsoft.windowsazure.services.blob.client.CloudBlob;
import com.microsoft.windowsazure.services.blob.client.CloudBlobClient;
import com.microsoft.windowsazure.services.blob.client.CloudBlobContainer;
import com.microsoft.windowsazure.services.blob.client.CloudBlobDirectory;
import com.microsoft.windowsazure.services.blob.client.CloudBlockBlob;
import com.microsoft.windowsazure.services.blob.client.ListBlobItem;
import com.microsoft.windowsazure.services.core.storage.CloudStorageAccount;
import com.microsoft.windowsazure.services.core.storage.StorageErrorCode;
import com.microsoft.windowsazure.services.core.storage.StorageException;

/**
 * @author jurgenma
 *
 * @plexus.component role="org.apache.maven.wagon.Wagon"
 * role-hint="azureblob"
 * instantiation-strategy="per-lookup"
 */
public class AzureBlobWagon extends StreamWagon {

	private CloudStorageAccount storageAccount = null;
	
	private String baseBlobContainer = null;
	
	private CloudBlobClient blobClient = null;
	
	public static final String WAGON_PROTOCOL = "azureblob";
	
	static final String USEDEVELOPMENTSTORAGE_STRING = "UseDevelopmentStorage";
	
	static final String ACCOUNTKEY_STRING = "AccountKey";
	
	static final String DEFAULTENDPOINTSPROTOCOL_STRING = "DefaultEndpointsProtocol";
	
	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.StreamWagon#fillInputData(org.apache.maven.wagon.InputData)
	 */
	@Override
	public void fillInputData(InputData inputData)
			throws TransferFailedException, ResourceDoesNotExistException,
			AuthorizationException {
		
		CloudBlockBlob blob = null;
		String resourceName = inputData.getResource().getName();
		
		String blobName = null;
		if (StringUtils.isNotEmpty(this.baseBlobContainer)) {
			blobName = this.baseBlobContainer + 
					(this.baseBlobContainer.endsWith("/") ? resourceName : ("/" + resourceName));
		} else
			blobName = resourceName;
		
		try {
			blob = this.blobClient.getBlockBlobReference(blobName);
			if (!blob.exists())
				throw new ResourceDoesNotExistException("Blob with URI '" 
						+ blobName + "' doesn't exist in Azure at " + this.storageAccount.getBlobEndpoint());
			else {
				inputData.setInputStream(blob.openInputStream());
				inputData.getResource().setContentLength(blob.getProperties().getLength());
				inputData.getResource().setLastModified(blob.getProperties().getLastModified().getTime());
			}
		} catch (URISyntaxException e) {
			throw new ResourceDoesNotExistException("Blob with URI '" 
					+ blobName + "' doesn't exist in Azure at " + this.storageAccount.getBlobEndpoint(), e);
		} catch (StorageException e) {
			throw new TransferFailedException("Failed to get resource '" + resourceName + 
					"' from Azure at " + this.storageAccount.getBlobEndpoint(), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.StreamWagon#fillOutputData(org.apache.maven.wagon.OutputData)
	 */
	@Override
	public void fillOutputData(OutputData outputData)
			throws TransferFailedException {
		
		CloudBlockBlob blob = null;
		String resourceName = outputData.getResource().getName();
		
		String blobName = null;
		if (StringUtils.isNotEmpty(this.baseBlobContainer)) {
			blobName = this.baseBlobContainer + 
					(this.baseBlobContainer.endsWith("/") ? resourceName : ("/" + resourceName));
		} else
			blobName = resourceName;

		try {
			blob = this.blobClient.getBlockBlobReference(blobName);
			
			if (resourceName.contains("/"))
				blob.getContainer().createIfNotExist();
			else
				this.blobClient.getContainerReference("$root").createIfNotExist();
			
			outputData.setOutputStream(blob.openOutputStream());
		} catch (URISyntaxException e) {
			throw new TransferFailedException("Blob with URI '" 
					+ blobName + "' doesn't exist in Azure at " + this.storageAccount.getBlobEndpoint(), e);
		} catch (StorageException e) {
			throw new TransferFailedException("Failed to put resource '" + resourceName + 
					"' to Azure at " + this.storageAccount.getBlobEndpoint(), e);
		}
		
		this.fireTransferDebug( "resource = " + outputData.getResource());
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.AbstractWagon#openConnectionInternal()
	 */
	@Override
	protected void openConnectionInternal() throws ConnectionException,
			AuthenticationException {

		String password = null;
		
		AuthenticationInfo authInfo = this.getAuthenticationInfo();

		if (authInfo != null) {
			password = authInfo.getPassword();
			if (password == null)
				password = authInfo.getPrivateKey();
			if (password == null)
				password = authInfo.getPassphrase();
		}
		
		String connectionString = this.getRepository().getUrl();
		
		if (connectionString == null) {
			throw new AuthenticationException(
					"Azure storage connection string must be specified in URL parameter for repository " + getRepository().getId());
		} else {
			connectionString = ConnectionStringUtils.storageConnectionString(connectionString);
			
			if (!(connectionString.startsWith(AzureBlobWagon.DEFAULTENDPOINTSPROTOCOL_STRING) 
					||(connectionString.startsWith(AzureBlobWagon.USEDEVELOPMENTSTORAGE_STRING)))) {
				throw new AuthenticationException(
						"Azure storage connection string must start with 'DefaultEndpointsProtocol' or 'UseDevelopmentStorage' for repository " + getRepository().getId());
			}
		}
		if (password == null && (!connectionString.contains(AzureBlobWagon.ACCOUNTKEY_STRING))) {
			throw new AuthenticationException(
					"Azure storage account key must be specified in Password, Private Key or Passphrase parameter for repository "
							+ getRepository().getId());
		}
		
		if (!connectionString.contains(AzureBlobWagon.ACCOUNTKEY_STRING) && !StringUtils.isEmpty(password)) {
			connectionString = connectionString.concat(";" + AzureBlobWagon.ACCOUNTKEY_STRING + "=" + password);
		}

		try {
			this.storageAccount = CloudStorageAccount.parse(connectionString);
		} catch (URISyntaxException e) {
			throw new ConnectionException("Can not open connection to Azure blob storage account. Invalid URI", e);
		} catch (InvalidKeyException e) {
			throw new ConnectionException("Can not open connection to Azure blob storage account. Invalid key", e);
		}
		
		this.baseBlobContainer = ConnectionStringUtils.blobContainer(this.getRepository().getUrl());
		
		this.blobClient = storageAccount.createCloudBlobClient();
		this.blobClient.setTimeoutInMs(this.getTimeout());	
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.AbstractWagon#closeConnection()
	 */
	@Override
	public void closeConnection() throws ConnectionException {
		
		this.blobClient = null;
		this.storageAccount = null;
	}

/*
	@Override
	public List<String> getFileList(final String destinationDirectory)
			throws TransferFailedException, ResourceDoesNotExistException,
			AuthorizationException {

		ArrayList<String> result = new ArrayList<String>();
		
		String containername = StringUtils.isNotEmpty(this.baseBlobContainer) ?
				this.baseBlobContainer : destinationDirectory;
		
		if (StringUtils.isEmpty(containername))
			containername = "$root";
		
		try {
			CloudBlobContainer blobContainer = this.blobClient.getContainerReference(containername);
			
			if (!blobContainer.exists()) {
				throw new ResourceDoesNotExistException("Blob container/directory with URL '"
						+ blobContainer.getUri().toString() + "' doesn't exist");
			}
			
			Iterable<ListBlobItem> blobs = blobContainer.listBlobs();
			if (blobs != null) {
				Iterator<ListBlobItem> iter = blobs.iterator();
				while(iter.hasNext()) {
					ListBlobItem blob = iter.next();
					if (CloudBlob.class.isAssignableFrom(blob.getClass()))
						result.add(((CloudBlob)blob).getName());
					else if (CloudBlobDirectory.class.isAssignableFrom(blob.getClass())) {
						String blobDirURL = ((CloudBlobDirectory)blob).getUri().toString();
						if (blobDirURL != null && (blobDirURL.length() > 1) && (blobDirURL.endsWith("/")) )
							result.add(StringUtils.substringAfterLast(
									blobDirURL.substring(0, blobDirURL.length()-1), "/") + "/");
					}
				}
			}
			
			if (StringUtils.isEmpty(destinationDirectory)) {
				Iterator<CloudBlobContainer> iter = this.blobClient.listContainers().iterator();
				while(iter.hasNext()) {
					result.add(iter.next().getName() + "/");
				}
			}
			
		} catch (URISyntaxException e) {
			throw new ResourceDoesNotExistException("Blob container/directory doesn't exist");
		} catch (StorageException e) {
			if (StorageErrorCode.ACCESS_DENIED.toString().equals(e.getErrorCode()))
				throw new AuthorizationException("Failed to authorize access to Azure Blob storage account");
			else 
				throw new TransferFailedException("Failed to get file list", e);
		}
		
		return result;
	}
*/	
	
	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.AbstractWagon#getFileList(java.lang.String)
	 */
	@Override
	public List<String> getFileList(final String destinationDirectory)
			throws TransferFailedException, ResourceDoesNotExistException,
			AuthorizationException {

		ArrayList<String> result = new ArrayList<String>();
		
		String dirname = destinationDirectory;
		if (StringUtils.isNotEmpty(this.baseBlobContainer)) {
			dirname = this.baseBlobContainer + 
					(this.baseBlobContainer.endsWith("/") ? destinationDirectory : ("/" + destinationDirectory));
		}
				
		try {
			CloudBlobDirectory blobDir = this.blobClient.getDirectoryReference(dirname);
						
			Iterable<ListBlobItem> blobs = blobDir.listBlobs();
			if (blobs != null) {
				Iterator<ListBlobItem> iter = blobs.iterator();
				while(iter.hasNext()) {
					ListBlobItem blob = iter.next();
					if (CloudBlob.class.isAssignableFrom(blob.getClass())) {
						result.add(StringUtils.substringAfterLast(((CloudBlob)blob).getName(), "/"));
					} else if (CloudBlobDirectory.class.isAssignableFrom(blob.getClass())) {
						String blobDirURL = ((CloudBlobDirectory)blob).getUri().toString();
						if (blobDirURL != null && (blobDirURL.length() > 1) && (blobDirURL.endsWith("/")) )
							result.add(StringUtils.substringAfterLast(
									blobDirURL.substring(0, blobDirURL.length()-1), "/") + "/");
					}
				}
			}
			
			if (StringUtils.isEmpty(destinationDirectory)) {
				Iterator<CloudBlobContainer> iter = this.blobClient.listContainers().iterator();
				while(iter.hasNext()) {
					result.add(iter.next().getName() + "/");
				}
			}
			
			if (result.isEmpty() && StringUtils.isNotEmpty(this.baseBlobContainer)) {
				throw new ResourceDoesNotExistException("Blob container/directory with URL '"
						+ blobDir.getUri().toString() + "' doesn't exist");
			}
			
		} catch (URISyntaxException e) {
			throw new ResourceDoesNotExistException("Blob container/directory doesn't exist");
		} catch (StorageException e) {
			if (StorageErrorCode.ACCESS_DENIED.toString().equals(e.getErrorCode()))
				throw new AuthorizationException("Failed to authorize access to Azure Blob storage account");
			else 
				throw new TransferFailedException("Failed to get file list", e);
		} catch (NoSuchElementException e) {
			if (StorageException.class.isAssignableFrom(e.getCause().getClass())) {
				StorageException storeEx = (StorageException)e.getCause();
				if (storeEx.getHttpStatusCode() == 403)
					throw new AuthorizationException("Failed to authorize access to Azure Blob storage account");
				else 
					throw new TransferFailedException("Failed to get file list", storeEx);
			} else
				throw e;
		}
		
		return result;
	}	

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.AbstractWagon#resourceExists(java.lang.String)
	 */
	@Override
	public boolean resourceExists(final String resourceName)
			throws TransferFailedException, AuthorizationException {
		
		boolean result = false;
		
		if (StringUtils.isEmpty(resourceName))
			throw new IllegalArgumentException("Parameter 'resourceName' must not be empty");
		
		String blobName = null;
		if (StringUtils.isNotEmpty(this.baseBlobContainer)) {
			blobName = this.baseBlobContainer + 
					(this.baseBlobContainer.endsWith("/") ? resourceName : ("/" + resourceName));
		} else
			blobName = resourceName;
		
		try {
			CloudBlockBlob blob = this.blobClient.getBlockBlobReference(blobName);
			if (blob.getContainer().getName().length() >= 3) {
				result = blob.exists();
			} else {
				this.fireTransferDebug("Invalid resource name '"
						+ blobName + "'. Resource names for Azure blobs must have containers with 3 chars length at least");
			}
		} catch (StorageException e) {
			throw new TransferFailedException("Can not access blob '" + resourceName + "'", e);
		} catch (URISyntaxException e) {
			throw new TransferFailedException("Blob '" + resourceName + "' has an invalid URI", e);
		}
		
		return result;
	}

}
