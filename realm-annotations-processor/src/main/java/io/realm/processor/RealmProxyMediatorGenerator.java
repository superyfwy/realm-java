
/*
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.processor;

import com.squareup.javawriter.JavaWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;

import io.realm.annotations.internal.RealmModule;

public class RealmProxyMediatorGenerator {
    private final String className;
    private ProcessingEnvironment processingEnvironment;
    private List<String> qualifiedModelClasses = new ArrayList<String>();
    private List<String> simpleModelClasses = new ArrayList<String>();
    private List<String> proxyClasses = new ArrayList<String>();

    private static final String REALM_PACKAGE_NAME = "io.realm";
    private static final String EXCEPTION_MSG = "\"Could not find the generated proxy class for \" + clazz + \". \" + RealmProxyMediator.APT_NOT_EXECUTED_MESSAGE";

    public RealmProxyMediatorGenerator(ProcessingEnvironment processingEnvironment, String className, Set<ClassMetaData> classesToValidate) {
        this.processingEnvironment = processingEnvironment;
        this.className = className;

        for (ClassMetaData metadata : classesToValidate) {
            String simpleName = metadata.getSimpleClassName();
            qualifiedModelClasses.add(metadata.getFullyQualifiedClassName());
            simpleModelClasses.add(simpleName);
            proxyClasses.add(getProxyClassName(simpleName));
        }
    }

    public void generate() throws IOException {
        String qualifiedGeneratedClassName = String.format("%s.%sMediator", REALM_PACKAGE_NAME, className);
        JavaFileObject sourceFile = processingEnvironment.getFiler().createSourceFile(qualifiedGeneratedClassName);
        JavaWriter writer = new JavaWriter(new BufferedWriter(sourceFile.openWriter()));
        writer.setIndent("    ");

        writer.emitPackage(REALM_PACKAGE_NAME);
        writer.emitEmptyLine();

        writer.emitImports(
                "android.util.JsonReader",
                "java.io.IOException",
                "java.util.ArrayList",
                "java.util.Collections",
                "java.util.List",
                "java.util.Map",
                "io.realm.exceptions.RealmException",
                "io.realm.internal.ImplicitTransaction",
                "io.realm.internal.RealmObjectProxy",
                "io.realm.internal.RealmProxyMediator",
                "io.realm.internal.Table",
                "org.json.JSONException",
                "org.json.JSONObject"
        );
        writer.emitImports(qualifiedModelClasses);

        writer.emitEmptyLine();

        writer.emitAnnotation(RealmModule.class);
        writer.beginType(
                qualifiedGeneratedClassName,        // full qualified name of the item to generate
                "class",                            // the type of the item
                Collections.<Modifier>emptySet(),   // modifiers to apply
                null,                               // class to extend
                "RealmProxyMediator");              // Interfaces to implement
        writer.emitEmptyLine();

        emitFields(writer);
        emitCreateTableMethod(writer);
        emitValidateTableMethod(writer);
        emitGetFieldNamesMethod(writer);
        emitGetTableNameMethod(writer);
        emitNewInstanceMethod(writer);
        emitGetClassModelList(writer);
        emitGetColumnIndices(writer);
        emitCopyToRealmMethod(writer);
        emitPopulateUsingJsonObject(writer);
        emitPopulateUsingJsonStream(writer);

        writer.endType();
        writer.close();
    }

    private void emitFields(JavaWriter writer) throws IOException {
        writer.emitField("List<Class<? extends RealmObject>>", "MODEL_CLASSES", EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL));
        writer.beginInitializer(true);
        writer.emitStatement("List<Class<? extends RealmObject>> modelClasses = new ArrayList<Class<? extends RealmObject>>()");
        for (String clazz : simpleModelClasses) {
            writer.emitStatement("modelClasses.add(%s.class)", clazz);
        }
        writer.emitStatement("MODEL_CLASSES = Collections.unmodifiableList(modelClasses)");
        writer.endInitializer();
        writer.emitEmptyLine();
    }

    private void emitCreateTableMethod(JavaWriter writer) throws IOException {
        writer.emitAnnotation("Override");
        writer.beginMethod(
                "Table",
                "createTable",
                EnumSet.of(Modifier.PUBLIC),
                "Class<? extends RealmObject>", "clazz", "ImplicitTransaction", "transaction"
        );
        emitMediatorSwitch(new ProxySwitchStatement() {
            @Override
            public void emitStatement(int i, JavaWriter writer) throws IOException {
                writer.emitStatement("return %s.initTable(transaction)", proxyClasses.get(i));
            }
        }, writer);
        writer.endMethod();
        writer.emitEmptyLine();
    }

    private void emitValidateTableMethod(JavaWriter writer) throws IOException {
        writer.emitAnnotation("Override");
        writer.beginMethod(
                "void",
                "validateTable",
                EnumSet.of(Modifier.PUBLIC),
                "Class<? extends RealmObject>", "clazz", "ImplicitTransaction", "transaction"
        );
        emitMediatorSwitch(new ProxySwitchStatement() {
            @Override
            public void emitStatement(int i, JavaWriter writer) throws IOException {
                writer.emitStatement("%s.validateTable(transaction)", proxyClasses.get(i));
            }
        }, writer);
        writer.endMethod();
        writer.emitEmptyLine();
    }

    private void emitGetFieldNamesMethod(JavaWriter writer) throws IOException {
        writer.emitAnnotation("Override");
        writer.beginMethod(
                "List<String>",
                "getFieldNames",
                EnumSet.of(Modifier.PUBLIC),
                "Class<? extends RealmObject>", "clazz"
        );
        emitMediatorSwitch(new ProxySwitchStatement() {
            @Override
            public void emitStatement(int i, JavaWriter writer) throws IOException {
                writer.emitStatement("return %s.getFieldNames()", proxyClasses.get(i));
            }
        }, writer);
        writer.endMethod();
        writer.emitEmptyLine();
    }

    private void emitGetTableNameMethod(JavaWriter writer) throws IOException {
        writer.emitAnnotation("Override");
        writer.beginMethod(
                "String",
                "getTableName",
                EnumSet.of(Modifier.PUBLIC),
                "Class<? extends RealmObject>", "clazz"
        );
        emitMediatorSwitch(new ProxySwitchStatement() {
            @Override
            public void emitStatement(int i, JavaWriter writer) throws IOException {
                writer.emitStatement("return %s.getTableName()", proxyClasses.get(i));
            }
        }, writer);
        writer.endMethod();
        writer.emitEmptyLine();
    }

    private void emitNewInstanceMethod(JavaWriter writer) throws IOException {
        writer.emitAnnotation("Override");
        writer.beginMethod(
                "<E extends RealmObject> E",
                "newInstance",
                EnumSet.of(Modifier.PUBLIC),
                "Class<E>", "clazz"
        );
        emitMediatorSwitch(new ProxySwitchStatement() {
            @Override
            public void emitStatement(int i, JavaWriter writer) throws IOException {
                writer.emitStatement("return (E) new %s()", proxyClasses.get(i));
            }
        }, writer);
        writer.endMethod();
        writer.emitEmptyLine();
    }

    private void emitGetClassModelList(JavaWriter writer) throws IOException {
        writer.emitAnnotation("Override");
        writer.beginMethod("List<Class<? extends RealmObject>>", "getModelClasses", EnumSet.of(Modifier.PUBLIC));
        writer.emitStatement("return MODEL_CLASSES");
        writer.endMethod();
        writer.emitEmptyLine();
    }

    private void emitGetColumnIndices(JavaWriter writer) throws IOException {
        writer.emitAnnotation("Override");
        writer.beginMethod(
                "Map<String, Long>",
                "getColumnIndices",
                EnumSet.of(Modifier.PUBLIC),
                "Class<? extends RealmObject>", "clazz"
        );
        emitMediatorSwitch(new ProxySwitchStatement() {
            @Override
            public void emitStatement(int i, JavaWriter writer) throws IOException {
                writer.emitStatement("return %s.getColumnIndices()", proxyClasses.get(i));
            }
        }, writer, false);
        writer.endMethod();
        writer.emitEmptyLine();
    }

    private void emitCopyToRealmMethod(JavaWriter writer) throws IOException {
        writer.emitAnnotation("Override");
        writer.beginMethod(
                "<E extends RealmObject> E",
                "copyOrUpdate",
                EnumSet.of(Modifier.PUBLIC),
                "Realm", "realm", "E", "obj", "boolean", "update", "Map<RealmObject, RealmObjectProxy>",  "cache"
        );

        writer.emitStatement("Class<E> clazz = (Class<E>) ((obj instanceof RealmObjectProxy) ? obj.getClass().getSuperclass() : obj.getClass())");
        writer.emitEmptyLine();
        emitMediatorSwitch(new ProxySwitchStatement() {
            @Override
            public void emitStatement(int i, JavaWriter writer) throws IOException {
                writer.emitStatement("return (E) %s.copyOrUpdate(realm, (%s) obj, update, cache)", proxyClasses.get(i), simpleModelClasses.get(i));
            }
        }, writer, false);
        writer.endMethod();
        writer.emitEmptyLine();
    }

    private void emitPopulateUsingJsonObject(JavaWriter writer) throws IOException {
        writer.emitAnnotation("Override");
        writer.beginMethod(
                "<E extends RealmObject> void",
                "populateUsingJsonObject",
                EnumSet.of(Modifier.PUBLIC),
                Arrays.asList("E", "obj", "JSONObject", "json"),
                Arrays.asList("JSONException")
        );
        writer.emitStatement("Class<E> clazz = (Class<E>) ((obj instanceof RealmObjectProxy) ? obj.getClass().getSuperclass() : obj.getClass())");
        emitMediatorSwitch(new ProxySwitchStatement() {
            @Override
            public void emitStatement(int i, JavaWriter writer) throws IOException {
                writer.emitStatement("%s.populateUsingJsonObject((%s) obj, json)", proxyClasses.get(i), simpleModelClasses.get(i));
            }
        }, writer);
        writer.endMethod();
        writer.emitEmptyLine();
    }

    private void emitPopulateUsingJsonStream(JavaWriter writer) throws IOException {
        writer.emitAnnotation("Override");
        writer.beginMethod(
                "<E extends RealmObject> void",
                "populateUsingJsonStream",
                EnumSet.of(Modifier.PUBLIC),
                Arrays.asList("E", "obj", "JsonReader", "reader"),
                Arrays.asList("IOException")
        );
        writer.emitStatement("Class<E> clazz = (Class<E>) ((obj instanceof RealmObjectProxy) ? obj.getClass().getSuperclass() : obj.getClass())");
        emitMediatorSwitch(new ProxySwitchStatement() {
            @Override
            public void emitStatement(int i, JavaWriter writer) throws IOException {
                writer.emitStatement("%s.populateUsingJsonStream((%s) obj, reader)", proxyClasses.get(i), simpleModelClasses.get(i));
            }
        }, writer);
        writer.endMethod();
        writer.emitEmptyLine();
    }

    // Emits the control flow for selecting the appropriate proxy class based on the model class
    // Currently it is just if..else, which is inefficient for large amounts amounts of model classes.
    // Consider switching to HashMap or similar.
    private void emitMediatorSwitch(ProxySwitchStatement statement, JavaWriter writer) throws IOException {
        emitMediatorSwitch(statement, writer, true);
    }

    private void emitMediatorSwitch(ProxySwitchStatement statement, JavaWriter writer, boolean nullPointerCheck) throws IOException {
        if (nullPointerCheck) {
            writer.emitStatement("if (clazz == null) throw new NullPointerException(\"A class extending RealmObject must be provided\")");
            writer.emitEmptyLine();
        }
        writer.emitStatement("String classQualifiedName = clazz.getName()");
        if (simpleModelClasses.size() == 0) {
            writer.emitStatement("throw new RealmException(%s)", EXCEPTION_MSG);
        } else {
            writer.beginControlFlow("if (classQualifiedName.equals(%s.class.getName()))", simpleModelClasses.get(0));
            statement.emitStatement(0, writer);
            for (int i = 1; i < simpleModelClasses.size(); i++) {
                writer.nextControlFlow("else if (classQualifiedName.equals(%s.class.getName()))", simpleModelClasses.get(i));
                statement.emitStatement(i, writer);
            }
            writer.nextControlFlow("else");
            writer.emitStatement("throw new RealmException(%s)", EXCEPTION_MSG);
            writer.endControlFlow();
        }
    }

    private String getProxyClassName(String clazz) {
        return clazz + Constants.PROXY_SUFFIX;
    }

    private interface ProxySwitchStatement {
        public void emitStatement(int i, JavaWriter writer) throws IOException;
    }
}