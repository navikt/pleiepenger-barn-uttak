package no.nav.pleiepengerbarn.uttak.server.db

enum class EnvironmentClass {

    LOCALHOST, PREPROD, PROD;

    open fun mountPath(): String {
        return "postgresql/" + name.toLowerCase() + "-fss"
    }
}