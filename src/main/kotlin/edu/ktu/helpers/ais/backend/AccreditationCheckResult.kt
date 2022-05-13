package edu.ktu.helpers.ais.backend

data class AccreditationCheckResult(
    var organizationName : String?,
    var isAccredited : Boolean,
    var accreditedBy : String?,
    var accreditationInformationLocation : String?
)
