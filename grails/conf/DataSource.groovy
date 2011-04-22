// grails generate-all my-project
// grails run-all
dataSource {
    loggingSql = true
    pooled = true
    driverClassName = "com.fidelma.salesforce.jdbc.SfDriver"
    username = "salesforce@fidelma.com"
    password = "u9SABqa2dQxG0Y3kqWiJQVEwnYtryr1Ja1"
    dialect  = com.fidelma.salesforce.jdbc.hibernate.SalesforceDialect
}
hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class = 'net.sf.ehcache.hibernate.EhCacheProvider'
}
// environment specific settings
environments {
    development {
        dataSource {
            dbCreate = "create" // one of 'create', 'create-drop','update'
            url = "jdbc:sfdc:https://login.salesforce.com"
        }
    }
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:sfdc:https://login.salesforce.com"
        }
    }
    production {
        dataSource {
            dbCreate = "update"
            url = "jdbc:sfdc:https://login.salesforce.com"
        }
    }
}
