package com.github.pagination.connector.extension;

import com.github.pagination.connector.internal.operations.PaginationScopeOperations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.meta.JavaVersion;

@Extension(name = "Pagination Connector")
@Operations(PaginationScopeOperations.class)
@Xml(prefix = "pagination", namespace = "http://www.mulesoft.org/schema/mule/pagination")
@JavaVersionSupport({JavaVersion.JAVA_17})
public class PaginationConnector {
}