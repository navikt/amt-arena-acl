package no.nav.amt.arena.acl.mocks

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

open class MockHttpServer(
	startImmediately: Boolean = false,
) {

	private val server = MockWebServer()

	private val log = LoggerFactory.getLogger(javaClass)

	private var lastRequestCount = 0

	private val responseHandlers = mutableMapOf<(request: RecordedRequest) -> Boolean, MockResponse>()

	init {
	    if (startImmediately) {
			start()
		}
	}

	fun start() {
		try {
		    server.start()
			server.dispatcher = createResponseDispatcher()
		} catch (e: IllegalArgumentException) {
			log.info("${javaClass.simpleName} is already started")
		}
	}

	open fun reset() {
		lastRequestCount = server.requestCount
		responseHandlers.clear()
		flushRequests()
	}

	fun serverUrl(): String {
		return server.url("").toString().removeSuffix("/")
	}

	fun addResponseHandler(requestMatcher: (req: RecordedRequest) -> Boolean, response: MockResponse) {
		responseHandlers[requestMatcher] = response
	}

	fun handleRequest(
		matchPath: String? = null,
		matchRegexPath: Regex? = null,
		matchMethod: String? = null,
		matchHeaders: Map<String, String>? = null,
		matchBodyContains: String? = null,
		response: MockResponse
	) {
		val requestMatcher = matcher@{ req: RecordedRequest ->
			if (matchPath != null && req.path != matchPath)
				return@matcher false

			if (matchRegexPath != null && !req.path!!.matches(matchRegexPath))
				return@matcher false

			if (matchMethod != null && req.method != matchMethod)
				return@matcher false

			if (matchHeaders != null && !hasExpectedHeaders(req.headers, matchHeaders))
				return@matcher false

			if (matchBodyContains != null && !req.body.readUtf8().contains(matchBodyContains))
				return@matcher false

			true
		}

		addResponseHandler(requestMatcher, response)
	}

	fun latestRequest(): RecordedRequest {
		return server.takeRequest()
	}

	fun requestCount(): Int {
		return server.requestCount - lastRequestCount
	}

	fun shutdown() {
		server.shutdown()
	}

	private fun createResponseDispatcher(): Dispatcher {
		return object : Dispatcher() {
			override fun dispatch(request: RecordedRequest): MockResponse {
				val response = responseHandlers.entries.find { it.key.invoke(request) }?.value
					?: throw IllegalStateException("No handler for $request")

				log.info("Responding [${request.path}]: $response")

				return response
			}
		}
	}

	private fun hasExpectedHeaders(requestHeaders: okhttp3.Headers, expectedHeaders: Map<String, String>): Boolean {
		var hasHeaders = true

		expectedHeaders.forEach { (name, value) ->
			if (requestHeaders[name] != value)
				hasHeaders = false
		}

		return hasHeaders
	}

	private fun flushRequests() {
		while (server.takeRequest(1, TimeUnit.NANOSECONDS) != null) {}
	}
}
