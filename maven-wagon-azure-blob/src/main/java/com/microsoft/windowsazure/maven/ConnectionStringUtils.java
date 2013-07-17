/**
 * 
 */
package com.microsoft.windowsazure.maven;

import org.apache.commons.lang3.StringUtils;

/**
 * @author jurgenma
 *
 */
public class ConnectionStringUtils {

	public static final String AZUREBLOB_WAGONPROTOCOL = "azureblob://";

	public static final String storageConnectionString(final String connectionString) {
		
		String result = StringUtils.EMPTY;
		
		if (StringUtils.isNotEmpty(connectionString)) {
			
			if (connectionString.startsWith(AZUREBLOB_WAGONPROTOCOL))
				result = connectionString.substring(AZUREBLOB_WAGONPROTOCOL.length());
			else
				result = connectionString;
			
			String containerName = ConnectionStringUtils.blobContainer(result);
			if (StringUtils.isNotEmpty(containerName)) {
				result = StringUtils.remove(result, "/" + containerName);
			}
		}
		
		return result;
	}
	
	public static final String wagonProtocol(final String connectionString) {
		
		if (StringUtils.isNotEmpty(connectionString)) {
			String[] parts = StringUtils.split(connectionString, "://");
			if (parts != null && parts.length > 0)
				return parts[0];
			else
				return StringUtils.EMPTY;
		} else {
			return StringUtils.EMPTY;
		}
	}
	
	public static final String blobContainer(final String connectionString) {
		
		String result = StringUtils.EMPTY;
		
		if (StringUtils.isNotEmpty(connectionString)) {
			String[] parts = StringUtils.split(connectionString, ";");
			if (parts != null && parts.length > 0) {
				for(String part : parts) {
					if (part.startsWith("AccountName=")) {
						String[] accountNameParts = StringUtils.split(part, "=");
						if (accountNameParts != null && accountNameParts.length > 1) {
							int containerStartIndex = accountNameParts[1].indexOf('/');
							if (containerStartIndex != -1)
								result = accountNameParts[1].substring(containerStartIndex+1);
						}
					}
				}
			}
		}

		return result;
	}
}
