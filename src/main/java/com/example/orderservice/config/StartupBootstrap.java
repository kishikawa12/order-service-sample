package com.example.orderservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.HashMap;
import java.util.Map;

/**
 * Idempotent bootstrap so the demo is self-contained: creates the tables if missing.
 *
 * Failures here are logged but do not stop the app — in environments where the tables are
 * pre-provisioned or the role lacks create permissions, the service still starts and serves.
 */
@Configuration
public class StartupBootstrap {

    private static final Logger log = LoggerFactory.getLogger(StartupBootstrap.class);

    @Bean
    ApplicationRunner bootstrap(DynamoDbClient dynamoDb,
                                @Value("${orders.table}") String ordersTable,
                                @Value("${orders.table.read-capacity}") long ordersRcu,
                                @Value("${orders.table.write-capacity}") long ordersWcu,
                                @Value("${inventory.table}") String inventoryTable) {
        return args -> {
            createProvisionedTable(dynamoDb, ordersTable, "orderId", ordersRcu, ordersWcu);
            createOnDemandTable(dynamoDb, inventoryTable, "sku");
            seedInventory(dynamoDb, inventoryTable);
        };
    }

    private void createProvisionedTable(DynamoDbClient dynamoDb, String table, String key,
                                        long rcu, long wcu) {
        try {
            dynamoDb.createTable(CreateTableRequest.builder()
                    .tableName(table)
                    .keySchema(KeySchemaElement.builder().attributeName(key).keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder().attributeName(key).attributeType(ScalarAttributeType.S).build())
                    .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(rcu).writeCapacityUnits(wcu).build())
                    .build());
            log.info("Created provisioned table {} ({} RCU / {} WCU)", table, rcu, wcu);
        } catch (ResourceInUseException e) {
            log.info("Table {} already exists", table);
        } catch (RuntimeException e) {
            log.warn("Could not create table {}: {}", table, e.getMessage());
        }
    }

    private void createOnDemandTable(DynamoDbClient dynamoDb, String table, String key) {
        try {
            dynamoDb.createTable(CreateTableRequest.builder()
                    .tableName(table)
                    .keySchema(KeySchemaElement.builder().attributeName(key).keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder().attributeName(key).attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            log.info("Created on-demand table {}", table);
        } catch (ResourceInUseException e) {
            log.info("Table {} already exists", table);
        } catch (RuntimeException e) {
            log.warn("Could not create table {}: {}", table, e.getMessage());
        }
    }

    private void seedInventory(DynamoDbClient dynamoDb, String table) {
        String[][] rows = {
                {"SKU-1001", "Widget", "500"},
                {"SKU-1002", "Gadget", "120"},
                {"SKU-1003", "Gizmo", "75"},
        };
        for (String[] row : rows) {
            try {
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("sku", AttributeValue.builder().s(row[0]).build());
                item.put("name", AttributeValue.builder().s(row[1]).build());
                item.put("quantity", AttributeValue.builder().n(row[2]).build());
                dynamoDb.putItem(PutItemRequest.builder().tableName(table).item(item).build());
            } catch (RuntimeException e) {
                log.warn("Could not seed inventory row {}: {}", row[0], e.getMessage());
            }
        }
    }
}
