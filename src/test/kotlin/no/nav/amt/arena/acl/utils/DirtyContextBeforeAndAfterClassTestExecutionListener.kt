package no.nav.amt.arena.acl.utils

import org.springframework.core.Ordered
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.support.AbstractTestExecutionListener


class DirtyContextBeforeAndAfterClassTestExecutionListener : AbstractTestExecutionListener() {
	override fun getOrder(): Int {
		return Ordered.HIGHEST_PRECEDENCE
	}

	override fun beforeTestClass(testContext: org.springframework.test.context.TestContext) {
		testContext.markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE)
	}

	override fun afterTestClass(testContext: org.springframework.test.context.TestContext) {
		testContext.markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE)
	}
}
