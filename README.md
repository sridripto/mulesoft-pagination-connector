# MuleSoft Pagination Connector

A custom MuleSoft 4 scope connector that handles paginated API calls. Drop it around your HTTP request and it automatically loops through all pages, collecting records until there are no more results. Supports JSON and XML responses. Compatible with Java 17 and Mule 4.6+.

## Requirements

- Mule Runtime 4.6.0+
- Java 17
- Anypoint Studio 7.x

## Build from source
```cmd

mvn clean install

```

## Installation

Add to your Mule project's `pom.xml`:

```xml

<dependency>
   <groupId>com.github.pagination</groupId>
   <artifactId>pagination-connector</artifactId>
   <version>1.2.0</version>
   <classifier>mule-plugin</classifier>
</dependency>

```

## Release Notes
### Release 1.2.0

- New field "Aggregate Results" added in the Request section. This lets you decide if you want the aggregated payload after exiting the scope.
- Example added on how to use "Stop Condition Field". Note: Do not use dataweave expression mode in this field.
- Custom Icon
