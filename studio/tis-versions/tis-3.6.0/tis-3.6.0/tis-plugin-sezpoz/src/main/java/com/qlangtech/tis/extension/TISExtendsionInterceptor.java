/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.extension;

import net.java.sezpoz.impl.Indexer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * 通过编译时期拦截扩展插件接口，将一些插件的元信息写入到 class compiler output目录当中去
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-11-12 09:24
 **/
@SupportedAnnotationTypes("*")
@SupportedOptions("sezpoz.quiet")
public class TISExtendsionInterceptor extends AbstractProcessor {
    public static final String FILE_EXTENDPOINTS = "extendpoints.txt";

    public TISExtendsionInterceptor() {
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (env.processingOver()) {
            // TODO we should not write until processingOver`
            return false;
        }

        final Map<String, List<String>> extensionPoints = new HashMap<>();
        for (Element indexable : env.getElementsAnnotatedWith(TISExtension.class)) {
            Element enclose = indexable.getEnclosingElement();
            // System.out.println("--------------enclose:" + enclose + ",type:" + enclose.getKind());
            if (enclose.getKind() == ElementKind.PACKAGE) {
                // 类似LocalDataXJobSubmit 这样的直接是扩展类的也写入索引文件
                visitParent(extensionPoints, indexable, indexable.asType());
            } else {
                visitParent(extensionPoints, enclose, enclose.asType());
            }


        }

        if (extensionPoints.isEmpty()) {
            return false;
        }
        try {
            FileObject out = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                    "", Indexer.METAINF_ANNOTATIONS + FILE_EXTENDPOINTS
            );
           // System.out.println("create new res:" + out.getName());
            try (ObjectOutputStream o = new ObjectOutputStream(out.openOutputStream())) {
                o.writeObject(extensionPoints);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        for (Map.Entry<String, List<String>> entry : extensionPoints.entrySet()) {
//            System.out.println(entry.getKey() + "->" + entry.getValue().stream().collect(Collectors.joining(",")));
//        }


//        for (TypeElement te : annotations) {
//            //Element enclosingElement = te.getEnclosingElement();
//            System.out.println("enclosingElement:" + te);
//            for (Element e : env.getElementsAnnotatedWith(te)) {
//                System.out.println("=====print:" + e.toString());
//            }
//        }

//        for (Element indexable : roundEnv.getElementsAnnotatedWith(Indexable.class)) {
//            String error = verifyIndexable(indexable);
//            if (error != null) {
//                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error, indexable);
//            } else {
//                Retention retention = indexable.getAnnotation(Retention.class);
//                if (retention == null || retention.value() != RetentionPolicy.SOURCE) {
//                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "should be marked @Retention(RetentionPolicy.SOURCE)", indexable);
//                }
//            }
//        }
//        // map from indexable annotation names, to actual uses
//        Map<String, Map<String, SerAnnotatedElement>> output = new TreeMap<String,Map<String,SerAnnotatedElement>>();
//        Map<String, Collection<Element>> originatingElementsByAnn = new HashMap<String,Collection<Element>>();
//        scan(annotations, originatingElementsByAnn, roundEnv, output);
//        write(output, originatingElementsByAnn);
        return false;
    }

    private void visitParent(Map<String, List<String>> extensionPoints, final Element impl, TypeMirror childtm) {
        if (childtm.getKind() != TypeKind.DECLARED) {
            return;
        }
        Types typeUtils = this.processingEnv.getTypeUtils();
        //String extendPoint = childtm.toString();
        TypeVisitor typeVisitor = null;
        List<? extends TypeMirror> typeMirrors = typeUtils.directSupertypes(childtm);
        List<String> impls = null;
        for (TypeMirror tm : typeMirrors) {
//com.qlangtech.tis.extension.Describable<com.qlangtech.tis.plugin.ds.DataSourceFactory>,
            typeVisitor = new TypeVisitor(childtm);
            tm.accept(typeVisitor, null);
            if (typeVisitor.extendPointMatch) {
                impls = extensionPoints.get(typeVisitor.extendPoint);
                if (impls == null) {
                    impls = new ArrayList<>();
                    extensionPoints.put(typeVisitor.extendPoint, impls);
                }
                impls.add(String.valueOf(impl));
                return;
            }
            visitParent(extensionPoints, impl, tm);
        }
    }

    private static class TypeVisitor extends SimpleTypeVisitor8<Void, Void> {
        private boolean extendPointMatch;
        String extendPoint;

        final TypeMirror childtm;

        public TypeVisitor(TypeMirror childtm) {
            this.childtm = childtm;
        }

        @Override
        public Void visitDeclared(DeclaredType t, Void aVoid) {
            extendPointMatch = t.asElement().getSimpleName().contentEquals("Describable");
            if (!extendPointMatch) {
                TISExtensible extensible = t.asElement().getAnnotation(TISExtensible.class);
                extendPointMatch = extensible != null;
                if (extendPointMatch) {
                    extendPoint = t.toString();
                }
            } else {
                extendPoint = childtm.toString();
            }
//            if (extendPointMatch) {
//                System.out.println(t + "----------->extendPointMatch:" + extendPointMatch + "t.getTypeArguments() size:" + t.getTypeArguments().size());
//            }

            if (extendPointMatch) {
                for (TypeMirror p : t.getTypeArguments()) {
                    extendPoint = String.valueOf(p);

                }
            }

            return null;
        }

    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

}
