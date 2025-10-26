package org.cachewrapper.processor;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.Name;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.Set;

/**
 * Annotation processor that injects an async companion method for methods annotated with @Async.
 * <p>
 * Limitations / notes:
 * - Uses internal javac APIs (com.sun.tools.javac.*) — works with javac, not Eclipse compiler.
 * - Compiler must be run with --add-exports for jdk.compiler packages on recent JDKs.
 * - Does not copy method modifiers like static; supports instance methods only in this simple example.
 * - Does not propagate checked exceptions (the generated lambda will rethrow unchecked only).
 */
@SupportedAnnotationTypes("org.cachewrapper.annotation.method.Async")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class AsyncProcessor extends AbstractProcessor {

    private JavacTrees trees;
    private TreeMaker treeMaker;
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;

        TypeElement asyncAnn = processingEnv.getElementUtils().getTypeElement("org.cachewrapper.annotation.method.Async");
        if (asyncAnn == null) return false;

        for (Element annotated : roundEnv.getElementsAnnotatedWith(asyncAnn)) {
            System.out.println("AsyncProcessor processing " + annotated);
            if (annotated.getKind() != ElementKind.METHOD) continue;

            JCTree tree = trees.getTree(annotated);
            if (!(tree instanceof JCTree.JCMethodDecl methodDecl)) continue;

            JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getTree(annotated.getEnclosingElement());
            injectAsyncCompanion(classDecl, methodDecl);
        }
        return true;
    }

    private void injectAsyncCompanion(JCTree.JCClassDecl classDecl, JCTree.JCMethodDecl original) {
        Name asyncName = names.fromString(original.getName() + "Async");
        List<JCTree.JCVariableDecl> params = original.getParameters();

        List<JCTree.JCExpression> args = List.nil();
        for (JCTree.JCVariableDecl p : params) args = args.append(treeMaker.Ident(p.getName()));
        JCTree.JCExpression call = treeMaker.Apply(List.nil(),
                treeMaker.Select(treeMaker.Ident(names.fromString("this")), original.getName()), args);

        boolean isVoid = original.getReturnType().type.getTag() == TypeTag.VOID;

        JCTree.JCExpression lambda;
        if (isVoid) {
            JCTree.JCStatement stmt = treeMaker.Exec(call);
            lambda = treeMaker.Lambda(List.nil(), treeMaker.Block(0, List.of(stmt)));
        } else {
            lambda = treeMaker.Lambda(List.nil(), call);
        }

        JCTree.JCExpression cfCall = treeMaker.Apply(
                com.sun.tools.javac.util.List.nil(),
                treeMaker.Select(chainDots("java", "util", "concurrent", "CompletableFuture"),
                        names.fromString(isVoid ? "runAsync" : "supplyAsync")),
                com.sun.tools.javac.util.List.of(lambda)
        );

        JCTree.JCExpression returnType;
        if (isVoid) {
            returnType = treeMaker.TypeApply(
                    chainDots("java", "util", "concurrent", "CompletableFuture"),
                    com.sun.tools.javac.util.List.of(chainDots("java", "lang", "Void"))
            );
        } else {
            returnType = treeMaker.TypeApply(
                    chainDots("java", "util", "concurrent", "CompletableFuture"),
                    com.sun.tools.javac.util.List.of(treeMaker.Type(original.getReturnType().type))
            );
        }

        JCTree.JCMethodDecl asyncMethod = treeMaker.MethodDef(
                treeMaker.Modifiers(Flags.PUBLIC),
                asyncName,
                returnType,
                List.nil(), params, List.nil(),
                treeMaker.Block(0, List.of(treeMaker.Return(cfCall))),
                null
        );

        classDecl.defs = classDecl.defs.append(asyncMethod);
    }

    private JCTree.JCExpression chainDots(String... elems) {
        JCTree.JCExpression e = treeMaker.Ident(names.fromString(elems[0]));
        for (int i = 1; i < elems.length; i++) e = treeMaker.Select(e, names.fromString(elems[i]));
        return e;
    }
}