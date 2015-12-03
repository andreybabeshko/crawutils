package com.digitallyinspired;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by Iurii Sokyrskyi on 12/3/2015.
 */
public class IndexAdapter {
    public static final String CONFIG_FILE_NAME = "connection.properties";
    private HashMap<String, String> config;
    private MongoClient mongoClient;

    public static void main(String[] args) {
        IndexAdapter adapter = new IndexAdapter();
        adapter.copyAndUpdateHistory();
    }

    private void connect() {
        mongoClient = new MongoClient(config.get("db.hostname"), Integer.parseInt(config.get("db.port")));
    }

    private void disconnect() {
        mongoClient.close();
    }

    private int copyAndUpdateHistory() {
        loadConfiguration();
        connect();
        MongoDatabase db = mongoClient.getDatabase(config.get("db.name"));
        MongoCollection nutchCollection = db.getCollection(config.get("db.collection.nutch"));
        final MongoCollection historyCollection = db.getCollection(config.get("db.collection.history"));
        MongoCursor<Document> cursor = nutchCollection.find(new Document("text", new Document("$exists", "true"))).iterator();
        int recordsCopied = 0;
        try {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                System.out.println("Copy URL " + document.get("baseUrl"));
                String newId = document.get("_id").toString() + document.get("prevFetchTime").toString();
                MongoCursor<Document> tempCursor = historyCollection.find(new Document("_id", newId)).iterator();
                if (!tempCursor.hasNext()) {
                    historyCollection.insertOne(
                            new Document().
                                    append("_id", newId).
                                    append("baseUrl", document.get("baseUrl")).
                                    append("text", document.get("text")).
                                    append("title", document.get("title"))
                    );
                    recordsCopied++;
                }
                tempCursor.close();
            }
        } finally {
            cursor.close();
        }
        disconnect();
        System.out.println(recordsCopied + " records copied");
        return recordsCopied;
    }

    private void loadConfiguration() {
        Properties props = new Properties();

        try {
            FileInputStream in = new FileInputStream(CONFIG_FILE_NAME);
            props.load(in);
        } catch (FileNotFoundException e) {
            System.out.println("Connection properties file connection.properties not found. Terminate.");
            System.exit(1);
        } catch (IOException e) {
            System.out.print("IO error occured:");
            e.printStackTrace();
            System.exit(1);
        }

        config = new HashMap<String, String>();
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            config.put(key, value);
        }
    }

}
