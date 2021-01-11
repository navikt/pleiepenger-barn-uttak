package no.nav.pleiepengerbarn.uttak.server.openapi

import com.fasterxml.jackson.databind.JavaType
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.media.Schema
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import java.time.Duration

internal class CustomModelConverter : ModelConverter {
    override fun resolve(
            type: AnnotatedType,
            context: ModelConverterContext,
            chain: Iterator<ModelConverter>): Schema<*>? {
        val schema = (if (chain.hasNext()) {
            chain.next().resolve(type, context, chain)
        } else {
            null
        }) ?: return null

        val javaType = Json.mapper().constructType(type.type)

        if (javaType != null) {
            if (javaType.erDuration()) {
                schema.example("PT7H30M")
            } else if(javaType.isTypeOrSubTypeOf(LukketPeriode::class.java)){
                schema.example("2021-01-01/2021-01-10")
            }
        }

        return schema
    }

    private fun JavaType.erDuration() = Duration::class.java.isAssignableFrom(rawClass)
}