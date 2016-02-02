package com.erudika.para.persistence;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.erudika.para.Para;
import com.erudika.para.utils.Config;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

public final class MongoDBUtils {

	private static MongoClient ddbClient;
	private static MongoDatabase ddb;
	private static final String DBHOST = Config.MONGODB_DBHOST;
	private static final int DBPORT = Config.MONGODB_DBPORT;
	private static final String DBNAME = Config.MONGODB_DBNAME;
	private static final String DBUSER = Config.MONGODB_DBUSER;
	private static final String DBPASS = Config.MONGODB_DBPASS;
	private static final Logger logger = LoggerFactory.getLogger(MongoDBUtils.class);

	private MongoDBUtils() { }

	/**
	 * Returns a client instance for MongoDB
	 * @return a client that talks to MongoDB
	 */
	public static MongoDatabase getClient() {
		if (ddb != null) {
			return ddb;
		}
		
		logger.info("MongoDatabase host:" + DBHOST + " port:" + DBPORT + " name:" + DBNAME);
		MongoCredential credential = MongoCredential.createCredential(DBUSER, DBNAME, DBPASS.toCharArray());
		ServerAddress s = new ServerAddress(DBHOST, DBPORT);
		
		ddbClient = new MongoClient(s, Arrays.asList(credential));
		ddb = ddbClient.getDatabase(DBNAME);
		
		if (!existsTable(Config.APP_NAME_NS)) {
			createTable(Config.APP_NAME_NS);
		}

		Para.addDestroyListener(new Para.DestroyListener() {
			public void onDestroy() {
				shutdownClient();
			}
		});

		return ddb;
	}

	/**
	 * Stops the client and releases resources.
	 * <b>There's no need to call this explicitly!</b>
	 */
	protected static void shutdownClient() {
		if (ddbClient != null) {
			ddbClient.close();
			ddbClient = null;
		}
	}

	/**
	 * Checks if the main table exists in the database.
	 * @param appid name of the {@link com.erudika.para.core.App}
	 * @return true if the table exists
	 */
	public static boolean existsTable(String appid) {
		if (StringUtils.isBlank(appid)) {
			return false;
		}
		try {
			MongoIterable<String> collectionNames = ddb.listCollectionNames();
		    for (final String name : collectionNames) {
		        if (name.equalsIgnoreCase(appid)) {
		            return true;
		        }
		    }
		    return false;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Creates a table in MongoDB.
	 * @param appid name of the {@link com.erudika.para.core.App}
	 * @return true if created
	 */
	public static boolean createTable(String appid) {
		if (StringUtils.isBlank(appid) || StringUtils.containsWhitespace(appid) || existsTable(appid)) {
			return false;
		}
		try {
			ddb.createCollection(getTableNameForAppid(appid));
		} catch (Exception e) {
			logger.error(null, e);
			return false;
		}
		return true;
	}

	/**
	 * Deletes the main table from MongoDB.
	 * @param appid name of the {@link com.erudika.para.core.App}
	 * @return true if deleted
	 */
	public static boolean deleteTable(String appid) {
		if (StringUtils.isBlank(appid) || !existsTable(appid)) {
			return false;
		}
		try {
			MongoCollection<Document> collection = ddb.getCollection(getTableNameForAppid(appid));
			collection.drop();			
		} catch (Exception e) {
			logger.error(null, e);
			return false;
		}
		return false;
	}

	/**
	 * Gives count information about a MongoDB table.
	 * @param appid name of the collection
	 * @return a long
	 */
	public static long getTableCount(final String appid) {
		if (StringUtils.isBlank(appid)) {
			return -1;
		}
		try {
			MongoCollection<Document> td = getTable(appid);			
			return td.count();			
		} catch (Exception e) {
			logger.error(null, e);
		}
		return -1;
	}

	/**
	 * Get the mongodb table requested
	 * @param appid name of the collection
	 * @return
	 */
	public static MongoCollection<Document> getTable(String appid) {
		try {
			return ddb.getCollection(getTableNameForAppid(appid));			
		} catch (Exception e) {
			logger.error(null, e);
		}
		return null;
	}

	/**
	 * Lists all table names for this account.
	 * @return a list of MongoDB tables
	 */
	public static MongoIterable<String> listAllTables() {
		MongoIterable<String> collectionNames = ddb.listCollectionNames();
		return collectionNames;
	}

	/**
	 * Returns the table name for a given app id. Table names are usually in the form 'prefix-appid'.
	 * @param appIdentifier app id
	 * @return the table name
	 */
	public static String getTableNameForAppid(String appIdentifier) {
		if (StringUtils.isBlank(appIdentifier)) {
			return null;
		} else {
			return (appIdentifier.equals(Config.APP_NAME_NS) || appIdentifier.startsWith(Config.PARA.concat("-"))) ?
					appIdentifier : Config.PARA + "-" + appIdentifier;
		}
	}

}
