package no.nav.pleiepengerbarn.uttak.regler

class FeatureToggle {
    companion object {
        fun isActive(key: String, default: Boolean): Boolean {
            if (System.getProperties().containsKey(key)) {
                return System.getProperty(key).toBoolean()
            }
            if (System.getenv().containsKey(key)) {
                return System.getenv(key).toBoolean()
            }
            return default
        }
    }
}
