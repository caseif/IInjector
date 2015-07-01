/*
 * New BSD License (BSD-new)
 *
 * Copyright (c) 2015 Maxim Roncacé
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the name of the copyright holder nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.caseif.iinjector;

import org.gradle.api.Task;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.util.Set;

/**
 * The main "iinject" task for the plugin.
 */
public class IInjectorTask extends AbstractTask {

    private File config;
    @InputFile @Optional private File inputJar;
    private AbstractArchiveTask inputTask;
    private String outputJarName;
    private String classifier = "-iinjected";

    public IInjectorTask() {
        doLast(new IInjectorAction());
    }

    @InputFile
    public File getConfig() {
        return config;
    }

    public void config(File configFile) {
        this.config = configFile;
    }

    public File getInputJar() {
        File input = inputTask != null ? inputTask.getArchivePath() : inputJar;
        if (input == null) {
            throw new IllegalArgumentException("Either inputTask or inputJar must be supplied!");
        }
        return input;
    }

    public void inputJar(File inputFile) {
        this.inputJar = inputFile;
    }

    public Task getInputTask() {
        return inputTask;
    }

    public void inputTask(AbstractArchiveTask task) {
        this.inputTask = task;
    }

    public String getOutputJarName() {
        return outputJarName;
    }

    public void outputJarName(String output) {
        this.outputJarName = output;
    }

    public String getClassifier() {
        return this.classifier;
    }

    public void classifier(String classifier) {
        this.classifier = classifier;
    }

    File getOutputJar() {
        String realClassifier = (!getClassifier().isEmpty() ? "-" : "") + getClassifier();
        AbstractArchiveTask archiveTask;
        if (inputTask != null) {
            archiveTask = inputTask;
        } else {
            Set<Task> tasks = getProject().getTasksByName("jar", false);
            archiveTask = tasks.size() > 0 ? (AbstractArchiveTask)tasks.toArray()[0] : null;
        }
        return new File(getInputJar().getParent(), outputJarName != null
                ? outputJarName + (!outputJarName.endsWith(".jar") ? ".jar" : "")
                : (archiveTask != null
                        ? archiveTask.getArchiveName().replace("-" + archiveTask.getClassifier(), "") + realClassifier
                        : getInputJar().getName().replace(".jar", "" + realClassifier + ".jar"))
        );
    }

}
