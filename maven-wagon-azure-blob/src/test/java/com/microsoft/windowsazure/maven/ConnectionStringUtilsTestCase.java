package com.microsoft.windowsazure.maven;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConnectionStringUtilsTestCase {

	@Test
	public void testWagonProtocol() {
		String connectionString = "azureblob://DefaultEndpointsProtocol=https;AccountName=mavenwagontests";
		assertEquals("azureblob", ConnectionStringUtils.wagonProtocol(connectionString));
	}

	@Test
	public void testStorageConnectionString() {
		String connectionString = "azureblob://DefaultEndpointsProtocol=https;AccountName=mavenwagontests";
		assertEquals("DefaultEndpointsProtocol=https;AccountName=mavenwagontests", ConnectionStringUtils.storageConnectionString(connectionString));
	}
	
	@Test
	public void testStorageConnectionStringWithContainerName() {
		String connectionString = "azureblob://DefaultEndpointsProtocol=https;AccountName=mavenwagontests/snapshots";
		assertEquals("DefaultEndpointsProtocol=https;AccountName=mavenwagontests", ConnectionStringUtils.storageConnectionString(connectionString));
	}
	
	@Test
	public void testBlobContainer() {
		String connectionString = "azureblob://DefaultEndpointsProtocol=https;AccountName=mavenwagontests/snapshots";
		assertEquals("snapshots", ConnectionStringUtils.blobContainer(connectionString));
	}
}
