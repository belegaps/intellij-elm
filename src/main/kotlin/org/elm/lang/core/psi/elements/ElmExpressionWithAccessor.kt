package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


/**
 * A parenthesized expression with an accessor.
 *
 * e.g. `(fn arg).foo.bar`
 */
class ElmExpressionWithAccessor(node: ASTNode) : ElmPsiElementImpl(node), ElmFunctionCallTargetTag, ElmOperandTag {
    val expression: ElmExpression get() = childOfType()!!
    val accessor: ElmExpressionAccessor get() = childOfType()!!
}
