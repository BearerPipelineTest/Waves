package com.wavesplatform.lang.v1

import cats.syntax.semigroup._
import com.wavesplatform.lang.{Common, Global}
import com.wavesplatform.lang.directives.DirectiveSet
import com.wavesplatform.lang.directives.values.V3
import com.wavesplatform.lang.v1.compiler.Terms
import com.wavesplatform.lang.v1.evaluator.EvaluatorV2
import com.wavesplatform.lang.v1.evaluator.ctx.impl.PureContext
import com.wavesplatform.lang.v1.evaluator.ctx.impl.waves.WavesContext
import com.wavesplatform.lang.v1.traits.Environment
import monix.eval.Coeval

package object estimator {
  private val ctx =
    PureContext.build(V3, fixUnicodeFunctions = true, useNewPowPrecision = true).withEnvironment[Environment] |+|
    WavesContext.build(Global, DirectiveSet.contractDirectiveSet)

  val evaluatorV2AsEstimator = new ScriptEstimator {
    override val version: Int = 0

    override def apply(declaredVals: Set[String], functionCosts: Map[FunctionHeader, Coeval[Long]], expr: Terms.EXPR): Either[String, Long] = {
      val evalCtx = ctx.evaluationContext(Common.emptyBlockchainEnvironment())
      Right(EvaluatorV2.applyCompleted(evalCtx, expr, V3, correctFunctionCallScope = true)._2)
    }
  }
}
