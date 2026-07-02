package com.example.orderservice.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.s3.AmazonS3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Idempotent bootstrap so the demo is self-contained: creates the tables and bucket if missing.
 *
 * The orders table is created with minimal provisioned capacity, so it can throttle under load
 * (ProvisionedThroughputExceededException). The inventory table uses on-demand billing so it
 * does not throttle.
 *
 * Failures here are logged but do not stop the app — in environments where the tables/bucket are
 * pre-provisioned or the role lacks create permissions, the service still starts and serves.
 */
@Configuration
public class StartupBootstrap {

    private static final Logger log = LoggerFactory.getLogger(StartupBootstrap.class);

    @Bean
    ApplicationRunner bootstrap(AmazonDynamoDB dynamoDb,
                                AmazonS3 s3,
                                @Value("${orders.table}") String ordersTable,
                                @Value("${orders.table.read-capacity}") long ordersRcu,
                                @Value("${orders.table.write-capacity}") long ordersWcu,
                                @Value("${inventory.table}") String inventoryTable,
                                @Value("${reports.bucket}") String reportsBucket) {
        return args -> {
            createProvisionedTable(dynamoDb, ordersTable, "orderId", ordersRcu, ordersWcu);
            createOnDemandTable(dynamoDb, inventoryTable, "sku");
            seedInventory(dynamoDb, inventoryTable);
            ensureBucket(s3, reportsBucket);
        };
    }

    private void createProvisionedTable(AmazonDynamoDB dynamoDb, String table, String key,
                                        long rcu, long wcu) {
        try {
            dynamoDb.createTable(new CreateTableRequest()
                    .withTableName(table)
                    .withKeySchema(new KeySchemaElement(key, KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition(key, ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput(rcu, wcu)));
            log.info("Created provisioned table {} ({} RCU / {} WCU)", table, rcu, wcu);
        } catch (ResourceInUseException e) {
            log.info("Table {} already exists", table);
        } catch (RuntimeException e) {
            log.warn("Could not create table {}: {}", table, e.getMessage());
        }
    }

    private void createOnDemandTable(AmazonDynamoDB dynamoDb, String table, String key) {
        try {
            dynamoDb.createTable(new CreateTableRequest()
                    .withTableName(table)
                    .withKeySchema(new KeySchemaElement(key, KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition(key, ScalarAttributeType.S))
                    .withBillingMode(BillingMode.PAY_PER_REQUEST));
            log.info("Created on-demand table {}", table);
        } catch (ResourceInUseException e) {
            log.info("Table {} already exists", table);
        } catch (RuntimeException e) {
            log.warn("Could not create table {}: {}", table, e.getMessage());
        }
    }

    private void seedInventory(AmazonDynamoDB dynamoDb, String table) {
        String[][] rows = {
                {"SKU-1001", "Widget", "500"},
                {"SKU-1002", "Gadget", "120"},
                {"SKU-1003", "Gizmo", "75"},
        };
        for (String[] row : rows) {
            try {
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("sku", new AttributeValue(row[0]));
                item.put("name", new AttributeValue(row[1]));
                item.put("quantity", new AttributeValue().withN(row[2]));
                dynamoDb.putItem(new PutItemRequest(table, item));
            } catch (RuntimeException e) {
                log.warn("Could not seed inventory row {}: {}", row[0], e.getMessage());
            }
        }
    }

    private void ensureBucket(AmazonS3 s3, String bucket) {
        try {
            if (!s3.doesBucketExistV2(bucket)) {
                s3.createBucket(bucket);
                log.info("Created bucket {}", bucket);
            } else {
                log.info("Bucket {} already exists", bucket);
            }
        } catch (RuntimeException e) {
            log.warn("Could not ensure bucket {}: {}", bucket, e.getMessage());
        }
    }
}
