package eu.ebsi.TrustedIssuersRegistry

data class Issuer(
    var did : String,
    var attributes : Collection<Attribute>
)
