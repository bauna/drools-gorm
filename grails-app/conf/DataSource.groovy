hibernate {
    cache.use_second_level_cache = false
    cache.use_query_cache = false
}
// environment specific settings
environments {
    test {
        dataSource {
            driverClassName = "org.h2.Driver"
            dialect = "org.hibernate.dialect.H2Dialect"
            username = "sa"
            password = ""
            dbCreate = "create"
            url = "jdbc:h2:mem"

        }
    }
}
