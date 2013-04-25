/**
 * 
 */
package com.microsoft.windowsazure.maven;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jürgen Mayrbäurl
 *
 */
public class AzureBlobWagonTestCase extends AbstractAzureBlobWagonTestCase {
	
	private static String TESTBLOBCONTAINERNAME = "images";
	
	// private static boolean testBlobcontainerWasCreated = false;
	
	private static String AZURESTORAGE_CONNECTIONSTRING = "DefaultEndpointsProtocol=https;AccountName=mavenwagontests";
	
	private static String AZURESTORAGE_ACCOUNTKEY = "3kYyZ3oWIH7ty1Edn1MVOqm/R/riLrHpr3MX6Hgs7s+zSU2Zijm45vIcoyw/r+S0xIoI4ezT4n9Yrye98YBwiQ==";
	
/*	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		// Create Azure Blob container for testing. Since deleting a blob container via API is done 
		// asynchronously, it's no good idea to do it in each test
		testBlobcontainerWasCreated = createAzureBlobContainer(TESTBLOBCONTAINERNAME, 
				CloudStorageAccount.parse(createAzureStorageConnectionStringWithKey(
						AZURESTORAGE_CONNECTIONSTRING, AZURESTORAGE_ACCOUNTKEY)).createCloudBlobClient());
	}

	@AfterClass
	public static void tearDownClass() throws Exception {

		if (testBlobcontainerWasCreated) {
			deleteAzureBlobContainer(TESTBLOBCONTAINERNAME, CloudStorageAccount.parse(createAzureStorageConnectionStringWithKey(
					AZURESTORAGE_CONNECTIONSTRING, AZURESTORAGE_ACCOUNTKEY)).createCloudBlobClient());	
		}
	}
*/
	/* (non-Javadoc)
	 * @see com.microsoft.windowsazure.maven.AbstractAzureBlobWagonTestCase#getAzureStorageConnectionString()
	 */
	@Override
	protected String getAzureStorageConnectionString() {

		return AZURESTORAGE_CONNECTIONSTRING;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.windowsazure.maven.AbstractAzureBlobWagonTestCase#getAzureStorageAccountKey()
	 */
	@Override
	protected String getAzureStorageAccountKey() {
		
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
	
	@Test
	public void testPlexusSetup() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
		Assert.assertNotNull(wagon);
	}

	@Test
	public void testGetFileListFromImagesContainer() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
        wagon.connect( this.testRepository, this.getAuthInfo() );
        
        boolean containerExists = this.containerExists(TESTBLOBCONTAINERNAME);
        boolean created = this.createAzureBlob(TESTBLOBCONTAINERNAME+"/AzureDevCamp.jpg", new File("src/blobs/AzureDevCamp.jpg"));

        try {
	        List<String> entries = wagon.getFileList(TESTBLOBCONTAINERNAME);
	        Assert.assertNotNull(entries);
	        Assert.assertFalse(entries.isEmpty());
        } finally {
        	if (created) {
        		this.deleteAzureBlob(TESTBLOBCONTAINERNAME+"/AzureDevCamp.jpg");
        		if (!containerExists)
        			this.deleteAzureBlobContainer(TESTBLOBCONTAINERNAME);
        	}
        	
        	wagon.disconnect();
        }
	}
	
	@Test
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
	
	@Test
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
	
	@Test
	public void testBlobDirectoryNameExtraction() {
		
		final String dirName = "https://mavenwagontests.blob.core.windows.net/images/others/";
		
		Assert.assertEquals("others", StringUtils.substringAfterLast(dirName.substring(0, dirName.length()-1), "/"));
	}
	
	@Test
	public void testGetImageFromAzure() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
        wagon.connect( this.testRepository, this.getAuthInfo() );
        
        boolean containerExists = this.containerExists(TESTBLOBCONTAINERNAME);
        boolean created = this.createAzureBlob(TESTBLOBCONTAINERNAME+"/AzureDevCamp.jpg", new File("src/blobs/AzureDevCamp.jpg"));
        
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
        		this.deleteAzureBlob(TESTBLOBCONTAINERNAME+"/AzureDevCamp.jpg");
        		if (!containerExists)
        			this.deleteAzureBlobContainer(TESTBLOBCONTAINERNAME);
        	}
        	
        	wagon.disconnect();
        }
	}
	
	@Test
	public void testPutImageToAzure() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
        wagon.connect( this.testRepository, this.getAuthInfo() );
        
        this.message("Uploading file 'src/blobs/AzureDevCamp.jpg' to Azure storage '"
        		+ this.blobClient.getEndpoint() + "' as 'images/AzureDevCamp1.jpg'");
        
        final String blobDestination = TESTBLOBCONTAINERNAME+"/AzureDevCamp1.jpg";
        
        try {
	        wagon.put(new File("src/blobs/AzureDevCamp.jpg"), TESTBLOBCONTAINERNAME+"/AzureDevCamp1.jpg");
	        Assert.assertTrue(this.blobExists(blobDestination));
        } finally {
	        this.deleteAzureBlob(TESTBLOBCONTAINERNAME+"/AzureDevCamp1.jpg");
	        
	        wagon.disconnect();
        }
	}
	
	@Test
	public void testPutImageToAzureNewContainer() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
        wagon.connect( this.testRepository, this.getAuthInfo() );
        
        final String blobDestination = "newcontainer/images/AzureDevCamp1.jpg";
        wagon.put(new File("src/blobs/AzureDevCamp.jpg"), blobDestination);
        Assert.assertTrue(this.blobExists(blobDestination));
        
        this.deleteAzureBlobContainer("newcontainer");
        wagon.disconnect();
	}
	
	@Test
	public void testNonExistingBlob() throws Exception {
		
		this.setupRepositories();
		
		Wagon wagon = this.getWagon();
        wagon.connect( this.testRepository, this.getAuthInfo() );
        
		Assert.assertFalse(wagon.resourceExists("a/bad/resource/name/that/should/not/exist.txt"));
		
		wagon.disconnect();
	}
}
