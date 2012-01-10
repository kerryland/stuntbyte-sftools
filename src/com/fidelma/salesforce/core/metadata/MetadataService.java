package com.fidelma.salesforce.core.metadata;

import java.util.List;

/**
 */
public interface MetadataService {
    
    List<String> getMetadataTypes();

    List<Metadata> getMetadataByType(String metaDataType);

    void emptyCache();
}
