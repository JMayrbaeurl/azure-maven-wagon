/**
 * 
 */
package com.microsoft.windowsazure.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
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
public class AzureBlobWagon extends AbstractWagon {

	private CloudStorageAccount storageAccount = null;
	
	private CloudBlobClient blobClient = null;
	
	public static final String WAGON_PROTOCOL = "azureblob";
	
	static final String USEDEVELOPMENTSTORAGE_STRING = "UseDevelopmentStorage";
	
	static final String ACCOUNTKEY_STRING = "AccountKey";
	
	static final String DEFAULTENDPOINTSPROTOCOL_STRING = "DefaultEndpointsProtocol";
	
	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.Wagon#get(java.lang.String, java.io.File)
	 */
	@Override
	public void get(final String resourceName, final File destination)
			throws TransferFailedException, ResourceDoesNotExistException,
			AuthorizationException {

		if (StringUtils.isEmpty(resourceName)) {
			throw new IllegalArgumentException("Parameter 'resourceName' must not be empty");
		}
		
		if (destination == null)
			throw new IllegalArgumentException("Parameter 'destination' must not be null");
		
		CloudBlockBlob blob = null;
		
		try {
			blob = this.blobClient.getBlockBlobReference(resourceName);
			if (!blob.exists())
				throw new ResourceDoesNotExistException("Blob with URI '" 
						+ resourceName + "' doesn't exist in Azure at " + this.storageAccount.getBlobEndpoint());
		} catch (URISyntaxException e) {
			throw new ResourceDoesNotExistException("Blob with URI '" 
					+ resourceName + "' doesn't exist in Azure at " + this.storageAccount.getBlobEndpoint(), e);
		} catch (StorageException e) {
			throw new TransferFailedException("Failed to get resource '" + resourceName + 
					"' from Azure at " + this.storageAccount.getBlobEndpoint(), e);
		}
		
		this.downloadBlobToDestination(blob, destination);
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.Wagon#getIfNewer(java.lang.String, java.io.File, long)
	 */
	@Override
	public boolean getIfNewer(final String resourceName, final File destination,
			long timestamp) throws TransferFailedException,
			ResourceDoesNotExistException, AuthorizationException {
		
		if (StringUtils.isEmpty(resourceName)) {
			throw new IllegalArgumentException("Parameter 'resourceName' must not be empty");
		}
		
		if (destination == null)
			throw new IllegalArgumentException("Parameter 'destination' must not be null");

		CloudBlockBlob blob = null;
		
		try {
			blob = this.blobClient.getBlockBlobReference(resourceName);
			if (!blob.exists())
				throw new ResourceDoesNotExistException("Blob with URI '" 
						+ resourceName + "' doesn't exist in Azure at " + this.storageAccount.getBlobEndpoint());
		} catch (URISyntaxException e) {
			throw new ResourceDoesNotExistException("Blob with URI '" 
					+ resourceName + "' doesn't exist in Azure at " + this.storageAccount.getBlobEndpoint(), e);
		} catch (StorageException e) {
			throw new TransferFailedException("Failed to get resource '" + resourceName + 
					"' from Azure at " + this.storageAccount.getBlobEndpoint(), e);
		}
		
		boolean result = blob.getProperties().getLastModified().getTime() > timestamp;
		
		if (result) {
			this.downloadBlobToDestination(blob, destination);
		}
		
		return result;
	}
	
	/**
	 * @param blob
	 * @param destination
	 * @throws TransferFailedException
	 */
	private void downloadBlobToDestination(final CloudBlob blob, final File destination) throws TransferFailedException {
		
		OutputStream outputStream = null;
		String resourceName = null;
		
		try {
			resourceName = blob.getName();
		} catch (URISyntaxException e1) {
			throw new TransferFailedException("Failed to get Azure blob name", e1);
		}
		
		try {
			outputStream = new FileOutputStream(destination);
			blob.download(outputStream);
		} catch (FileNotFoundException e) {
			throw new TransferFailedException("Destination file '" + destination.getAbsolutePath() + 
					"' not found. Can't download Azure blob '" + resourceName + "'", e);
		} catch (StorageException e) {
			throw new TransferFailedException("Failed to download Azure blob '" 
					+ resourceName + "' from " + this.storageAccount.getBlobEndpoint(), e);
		} catch (IOException e) {
			throw new TransferFailedException("Failure writing Azure blob '" + 
					resourceName + "' to local file system at " + destination.getAbsolutePath(), e);
		}
		finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException ignoreEx) {}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.Wagon#put(java.io.File, java.lang.String)
	 */
	@Override
	public void put(final File source, final String destination)
			throws TransferFailedException, ResourceDoesNotExistException,
			AuthorizationException {
		
		if (StringUtils.isEmpty(destination)) {
			throw new IllegalArgumentException("Parameter 'destination' must not be empty");
		}
		
		if (source == null)
			throw new IllegalArgumentException("Parameter 'source' must not be null");
		
		CloudBlockBlob blob = null;
		
		try {
			blob = this.blobClient.getBlockBlobReference(destination);
			blob.getContainer().createIfNotExist();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		InputStream fileStream = null;
		
		try {
			fileStream = new FileInputStream(source);
			blob.upload(fileStream, source.length());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fileStream != null) {
				try {
					fileStream.close();
				} catch (IOException e) {}
			}
		}
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
		
		this.blobClient = storageAccount.createCloudBlobClient();
		this.blobClient.setTimeoutInMs(this.getTimeout());	
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.AbstractWagon#closeConnection()
	 */
	@Override
	protected void closeConnection() throws ConnectionException {
		
		this.blobClient = null;
		this.storageAccount = null;
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.AbstractWagon#getFileList(java.lang.String)
	 */
	@Override
	public List<String> getFileList(final String destinationDirectory)
			throws TransferFailedException, ResourceDoesNotExistException,
			AuthorizationException {

		ArrayList<String> result = new ArrayList<String>();
		
		try {
			CloudBlobContainer blobContainer = this.blobClient.getContainerReference(destinationDirectory);
			
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
							result.add(StringUtils.substringAfterLast(blobDirURL.substring(0, blobDirURL.length()-1), "/"));
					}
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

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.AbstractWagon#resourceExists(java.lang.String)
	 */
	@Override
	public boolean resourceExists(final String resourceName)
			throws TransferFailedException, AuthorizationException {
		
		boolean result = false;
		
		if (StringUtils.isEmpty(resourceName))
			throw new IllegalArgumentException("Parameter 'resourceName' must not be empty");
		
		try {
			CloudBlockBlob blob = this.blobClient.getBlockBlobReference(resourceName);
			if (blob.getContainer().getName().length() >= 3) {
				result = blob.exists();
			} else {
				this.fireTransferDebug("Invalid resource name '"
						+ resourceName + "'. Resource names for Azure blobs must have containers with 3 chars length at least");
			}
		} catch (StorageException e) {
			throw new TransferFailedException("Can not access blob '" + resourceName + "'", e);
		} catch (URISyntaxException e) {
			throw new TransferFailedException("Blob '" + resourceName + "' has an invalid URI", e);
		}
		
		return result;
	}

}
