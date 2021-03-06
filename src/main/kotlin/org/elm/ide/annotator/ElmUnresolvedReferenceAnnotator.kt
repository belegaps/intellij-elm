package org.elm.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.elm.ide.intentions.ElmImportIntentionAction
import org.elm.ide.intentions.ElmMakeDeclarationIntentionAction
import org.elm.lang.core.psi.ancestors
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.reference.TypeVariableReference
import org.elm.lang.core.resolve.scope.GlobalScope

class ElmUnresolvedReferenceAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        for (ref in element.references) {
            if (ref.resolve() == null) {

                if (element is ElmTypeAnnotation) {
                    holder.createWeakWarningAnnotation(element, "'${ref.canonicalText}' does not exist")
                            .also { it.registerFix(ElmMakeDeclarationIntentionAction()) }
                } else if (!safeToIgnore(ref, element)) {
                    // TODO [kl] make this smarter in the case of qualified references
                    // so that we don't report a double error when really the problem
                    // is with the qualified module name reference.
                    holder.createErrorAnnotation(element, "Unresolved reference '${ref.canonicalText}'")
                            .also { it.registerFix(ElmImportIntentionAction()) }
                }
            }
        }
    }

    private fun safeToIgnore(ref: PsiReference, element: PsiElement): Boolean {
        // Ignore refs to built-in types and values
        if (GlobalScope.allBuiltInSymbols.contains(ref.canonicalText))
            return true

        // Ignore refs to Kernel (JavaScript) modules
        if (element is ElmValueExpr && element.upperCaseQID?.isQualifiedNativeRef()
                ?: element.valueQID?.isQualifiedNativeRef() ?: false) {
            return true
        } else if (element is ElmImportClause && element.moduleQID.isQualifiedNativeRef()) {
            return true
        }

        // Ignore refs to type variables in a type annotation
        if (ref is TypeVariableReference && element.ancestors.any { it is ElmTypeAnnotation }) {
            return true
        }

        return false
    }
}

private fun ElmUpperCaseQID.isQualifiedNativeRef() =
        isQualified && isKernelModule(upperCaseIdentifierList)

private fun ElmValueQID.isQualifiedNativeRef() =
        isQualified && isKernelModule(upperCaseIdentifierList)

private fun isKernelModule(identifiers: List<PsiElement>): Boolean {
    val moduleName = identifiers.joinToString(".") { it.text }
    return moduleName.startsWith("Elm.Kernel.")
            || moduleName.startsWith("Native.") // TODO [drop 0.18] remove the "Native" clause
}
