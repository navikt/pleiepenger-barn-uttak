package no.nav.pleiepengerbarn.uttak.regler

class FeatureToggle {
    companion object {
        fun isActive(key: String): Boolean {
            if (System.getProperties().containsKey(key)) {
                return System.getProperty(key).toBoolean()
            }
            return System.getenv(key).toBoolean()
        }
    }
}
