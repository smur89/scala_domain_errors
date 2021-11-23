package smur89.domain_errors.interpreters.logging

import io.odin.loggers.HasContext

final case class TracedContext(correlationId: String)

object TracedContext {
  implicit val hasContext: HasContext[TracedContext] = (ctx: TracedContext) => Map("correlation-id" -> ctx.correlationId)

}
