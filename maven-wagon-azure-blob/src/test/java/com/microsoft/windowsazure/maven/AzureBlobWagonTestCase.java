/**
 * 
 */
package com.microsoft.windowsazure.maven;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.junit.Assert;

import com.microsoft.windowsazure.services.blob.client.CloudBlobClient;
import com.microsoft.windowsazure.services.core.storage.CloudStorageAccount;
import com.microsoft.windowsazure.services.core.storage.StorageException;

/**
 * @author Jürgen Mayrbäurl
 *
 */
public class AzureBlobWagonTestCase extends AbstractAzureBlobWagonTestCase {
	
	private static String TESTBLOBCONTAINERNAME = "images";
	
	private static String REPOSITORY_URL = null;
	
	private static String BASE_DIRECTORY = null;
	
	private static String AZURESTORAGE_CONNECTIONSTRING = null;
	
	private static String AZURESTORAGE_ACCOUNTKEY = null;
	
	/* (non-Javadoc)
	 * @see com.microsoft.windowsazure.maven.AbstractAzureBlobWagonTestCase#getAzureStorageConnectionString()
	 */
	@Override
	protected String getAzureStorageConnectionString() {

		if (AZURESTORAGE_CONNECTIONSTRING == null) {
			loadTestAzureStorageConfiguration();
		}
		
		return AZURESTORAGE_CONNECTIONSTRING;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.windowsazure.maven.AbstractAzureBlobWagonTestCase#getAzureRepositoryUrl()
	 */
	@Override
	protected String getAzureRepositoryUrl() {

		if (REPOSITORY_URL == null) {
			loadTestAzureStorageConfiguration();
		}
		
		return REPOSITORY_URL;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.windowsazure.maven.AbstractAzureBlobWagonTestCase#getAzureStorageAccountKey()
	 */
	@Override
	protected String getAzureStorageAccountKey() {
		
		if (AZURESTORAGE_ACCOUNTKEY == null) {
			loadTestAzureStorageConfiguration();
		}
		
		return AZURESTORAGE_ACCOUNTKEY;
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.wagon.WagonTestCase#getAuthInfo()
	 */
	@Override
	protected AuthenticationInfo getAuthInfo() {
		
		AuthenticationInfo result = new AuthenticationInfo();
		result.setPrivateKey(this.getAzureStorageAccountKey());
		
		return result;
	}
	
	private String createTestBlobContainername() {
		
		if (StringUtils.isNotEmpty(BASE_DIRECTORY)) {
			return BASE_DIRECTORY.endsWith("/") ? BASE_DIRECTORY + TESTBLOBCONTAINERNAME : BASE_DIRECTORY + "/" + TESTBLOBCONTAINERNAME;
		} else
			return TESTBLOBCONTAINERNAME;
	}
	
	public void testPlexusSetup() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
		Assert.assertNotNull(wagon);
	}

	public void testGetFileListFromImagesContainer() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
		this.connectWagon(wagon);
        
		String testContainername = this.createTestBlobContainername();
		
        boolean containerExists = this.containerExists(testContainername);
        boolean created = this.createAzureBlob(testContainername+"/AzureDevCamp.jpg", new File("src/blobs/AzureDevCamp.jpg"));

        try {
	        List<String> entries = wagon.getFileList(TESTBLOBCONTAINERNAME);
	        Assert.assertNotNull(entries);
	        Assert.assertFalse(entries.isEmpty());
        } finally {
        	if (created) {
        		this.deleteAzureBlob(testContainername+"/AzureDevCamp.jpg");
        		if (!containerExists)
        			this.deleteAzureBlobContainer(testContainername);
        	}
        	
        	this.disconnectWagon(wagon);
        }
	}	
	
	public void testGetFileListFromNonExisting() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
        wagon.connect( this.testRepository, this.getAuthInfo() );
        
        try {
        	wagon.getFileList("doesnotexist");
        } catch(ResourceDoesNotExistException ex) {}
        finally {
        	wagon.disconnect();
        }
	}
	
	public void testGetFileListWithWrongKey() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
		AuthenticationInfo authInfo = this.getAuthInfo();
		authInfo.setPassword("3kYyZ3oWIH7ty1Edn1MVOqm/R/riLrAAA3MX6Hgs7s+zSU2Zijm45vIcoyw/r+S0xIoI4ezT4n9Yrye98YBwiQ==");
        
		wagon.connect( this.testRepository, authInfo );
        
        try {
        	wagon.getFileList(TESTBLOBCONTAINERNAME);
        } catch (AuthorizationException ex) {}
        finally {
        	wagon.disconnect();
        }
	}
	
	public void testBlobDirectoryNameExtraction() {
		
		final String dirName = "https://mavenwagontests.blob.core.windows.net/images/others/";
		
		Assert.assertEquals("others", StringUtils.substringAfterLast(dirName.substring(0, dirName.length()-1), "/"));
	}
	
	public void testGetImageFromAzure() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
        wagon.connect( this.testRepository, this.getAuthInfo() );
        
        String testContainername = this.createTestBlobContainername();
        
        boolean containerExists = this.containerExists(TESTBLOBCONTAINERNAME);
        boolean created = this.createAzureBlob(testContainername+"/AzureDevCamp.jpg", new File("src/blobs/AzureDevCamp.jpg"));
        
        try {
	        File destDir = new File("target/downloads");
	        if (!destDir.exists())
	        	destDir.mkdir();
	        
	        File destFile = new File("target/downloads/AzureDevCamp.jpg");
	        wagon.get(TESTBLOBCONTAINERNAME+"/AzureDevCamp.jpg", destFile);
	        Assert.assertTrue(destFile.exists());
	        
	        destFile.delete();
        } finally {
        	if (created) {
        		this.deleteAzureBlob(testContainername+"/AzureDevCamp.jpg");
        		if (!containerExists)
        			this.deleteAzureBlobContainer(TESTBLOBCONTAINERNAME);
        	}
        	
        	wagon.disconnect();
        }
	}
	
	public void testPutImageToAzure() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
        wagon.connect( this.testRepository, this.getAuthInfo() );
        
        this.message("Uploading file 'src/blobs/AzureDevCamp.jpg' to Azure storage '"
        		+ this.blobClient.getEndpoint() + "' as 'images/AzureDevCamp1.jpg'");
        
        final String testContainername = this.createTestBlobContainername();
        final String blobDestination = testContainername + "/AzureDevCamp1.jpg";
        
        try {
	        wagon.put(new File("src/blobs/AzureDevCamp.jpg"), TESTBLOBCONTAINERNAME+"/AzureDevCamp1.jpg");
	        Assert.assertTrue(this.blobExists(blobDestination));
        } finally {
	        this.deleteAzureBlob(TESTBLOBCONTAINERNAME+"/AzureDevCamp1.jpg");
	        
	        wagon.disconnect();
        }
	}
	
	public void testPutImageToAzureNewContainer() throws Exception {
		
		if (StringUtils.isEmpty(BASE_DIRECTORY)) {
			this.setupRepositories();
			
			Wagon wagon = this.getWagon();
	        wagon.connect( this.testRepository, this.getAuthInfo() );
	        
	        final String blobDestination = "newcontainer/images/AzureDevCamp1.jpg";
	        wagon.put(new File("src/blobs/AzureDevCamp.jpg"), blobDestination);
	        Assert.assertTrue(this.blobExists(blobDestination));
	        
	        this.deleteAzureBlobContainer("newcontainer");
	        wagon.disconnect();
		}
	}
	
	public void testNonExistingBlob() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
        wagon.connect( this.testRepository, this.getAuthInfo() );
        
		Assert.assertFalse(wagon.resourceExists("a/bad/resource/name/that/should/not/exist.txt"));
		
		wagon.disconnect();
	}
	
	public void testGetImageFromAzureRootContainer() throws Exception {
		
		if (StringUtils.isEmpty(BASE_DIRECTORY)) {
			this.setupRepositories();
			
			Wagon wagon = this.getWagon();
	        wagon.connect( this.testRepository, this.getAuthInfo() );
	        
	        boolean created = this.createAzureBlob("$root/AzureDevCamp", 
	        		new File("src/blobs/AzureDevCamp.jpg"));
	        
	        try {
		        File destDir = new File("target/downloads");
		        if (!destDir.exists())
		        	destDir.mkdir();
		        
		        File destFile = new File("target/downloads/AzureDevCamp.jpg");
		        wagon.get("AzureDevCamp", destFile);
		        Assert.assertTrue(destFile.exists());
		        
		        destFile.delete();
	        } finally {
	        	if (created) {
	        		this.deleteAzureBlob("$root/AzureDevCamp");
	        	}
	        	
	        	wagon.disconnect();
	        }
		}
	}
	
	private static void loadTestAzureStorageConfiguration() {
		
		Properties props = new Properties();
		try {
			props.load(AzureBlobWagonTestCase.class.getResourceAsStream("/azureteststorage.properties"));
		} catch (IOException e) {
			throw new IllegalStateException("No configuration file for testing with Azure Storage found.", e);
		}
		
		if ((props.containsKey("maven.wagon.azure.blob.test.account.connectionstring") 
				|| (props.containsKey("maven.wagon.azure.blob.test.repository.url")))
				&& props.containsKey("maven.wagon.azure.blob.test.account.key")) {
			
			if (props.containsKey("maven.wagon.azure.blob.test.repository.url")) {
				REPOSITORY_URL = props.getProperty("maven.wagon.azure.blob.test.repository.url");
				AZURESTORAGE_CONNECTIONSTRING = ConnectionStringUtils.storageConnectionString(REPOSITORY_URL);
				BASE_DIRECTORY = ConnectionStringUtils.blobContainer(REPOSITORY_URL);
			} else {
				AZURESTORAGE_CONNECTIONSTRING = props.getProperty("maven.wagon.azure.blob.test.account.connectionstring");
			}
			AZURESTORAGE_ACCOUNTKEY = props.getProperty("maven.wagon.azure.blob.test.account.key");
		}
		else {
			throw new IllegalStateException("Invalid Windows Azure storage configuration file");
		}
		
		try {
			CloudBlobClient client = CloudStorageAccount.parse(AzureBlobWagonTestCase.createAzureStorageConnectionStringWithKey(
					AZURESTORAGE_CONNECTIONSTRING, AZURESTORAGE_ACCOUNTKEY)).createCloudBlobClient();
			client.getContainerReference(TESTBLOBCONTAINERNAME).createIfNotExist();
		} catch (InvalidKeyException e) {
			throw new IllegalStateException("Invalid Windows Azure storage account key specified", e);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid Windows Azure storage connection string specified", e);
		} catch (StorageException e) {
			throw new IllegalStateException("Failure accessing Windows Azure testing storage account", e);
		}
	}
}
