package no.nav.amt.arena.acl.exceptions

class ValidationException(
	val validationMessage: String
) : Exception(validationMessage)
