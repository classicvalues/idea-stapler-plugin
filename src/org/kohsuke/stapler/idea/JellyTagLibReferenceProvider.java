package org.kohsuke.stapler.idea;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.intellij.openapi.util.TextRange;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Let IDEA know what some of the attribute values are referring to.
 *
 * <p>
 * This data drives Ctrl+Click. 
 *
 * @author Kohsuke Kawaguchi
 */
public class JellyTagLibReferenceProvider extends PsiReferenceProvider {
    /*
        The basic idea of ReferenceProvider is to create a reference speculatively,
        then the reference object will later try to find the target.
     */
    @NotNull
    public PsiReference[] getReferencesByElement(@NotNull PsiElement e, @NotNull ProcessingContext processingContext) {
        /*
        This was the old way of marking references to tag files, but
        with the custom XmlNSDescriptor this is no longer necessary

        if (e instanceof XmlTag) {
            XmlTag t = (XmlTag)e;
            if(TagReference.isApplicable(t))
                return array(new TagReference(t));
        }
         */

        // is this <st:include page="..."> ?
        if (e instanceof XmlAttributeValue)
            return onAttributeValue((XmlAttributeValue) e);

        // this doesn't work, because XmlAttributeImpl doesn't call reference providers.
        // instead, XmlAttribute can only reference XmlAttributeDescriptor.getDeclaration()
//        if (e instanceof XmlAttribute) {
//            XmlAttribute a = (XmlAttribute) e;
//            PsiReference tagRef = a.getParent().getReference();
//            if(tagRef instanceof TagReference) {
//                return array(new TagAttributeReference((TagReference)tagRef,a));
//            }
//        }

        return PsiReference.EMPTY_ARRAY;
    }

    /**
     * Creates {@link PsiReference} for &lt;st:include page="..." /> attribute.
     */
    private PsiReference[] onAttributeValue(final XmlAttributeValue xav) {
        PsiElement _xa = xav.getParent();
        if (!(_xa instanceof XmlAttribute))
            return PsiReference.EMPTY_ARRAY;

        XmlAttribute a = (XmlAttribute) _xa;
        if(!a.getName().equals("page"))
            return PsiReference.EMPTY_ARRAY;

        XmlTag p = a.getParent();
        if(p==null)
            return PsiReference.EMPTY_ARRAY;

        if(!p.getLocalName().equals("include")
        || !p.getNamespace().equals("jelly:stapler"))
            return PsiReference.EMPTY_ARRAY;

        if(p.getAttribute("it")==null
        && p.getAttribute("from")==null) {
            // the page must be coming from the same object
            return array(new PsiReferenceBase<XmlAttributeValue>(xav,
                    TextRange.from(1,xav.getTextLength()-2)) {
                public PsiFile resolve() {
                    PsiFile f = xav.getContainingFile();
                    PsiDirectory p = f.getParent();
                    if(p==null) return null;
                    return p.findFile(xav.getValue());
                }

                public Object[] getVariants() {
                    return ArrayUtils.EMPTY_OBJECT_ARRAY;
                }
            });
        }

        return PsiReference.EMPTY_ARRAY;
    }

    private PsiReference[] array(PsiReference ref) {
        return new PsiReference[] {ref};
    }
}
