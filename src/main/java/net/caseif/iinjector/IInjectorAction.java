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

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class IInjectorAction implements Action<Task> {

    private Map<String, Set<String>> injections = new HashMap<>();

    @Override
    public void execute(Task baseTask) {
        final IInjectorTask task = (IInjectorTask)baseTask;
        constructInjectionMap(task);
        try (
                FileInputStream jarIn = new FileInputStream(task.getInputJar());
                ZipInputStream zipIn = new ZipInputStream(jarIn);
                FileOutputStream jarOut = new FileOutputStream(task.getOutputJar());
                ZipOutputStream zipOut = new ZipOutputStream(jarOut)
        ) {
            int applied = 0;
            ZipEntry zipEntry;
            while ((zipEntry = zipIn.getNextEntry()) != null) {
                ZipEntry zipEntryOut = new ZipEntry(zipEntry);
                if (zipEntry.getName().endsWith(".class") && !zipEntry.isDirectory()
                        && injections.containsKey(zipEntry.getName().replace(".class", ""))) {
                    byte[] data = transform(zipIn, zipEntry.getName().replace(".class", ""));
                    zipEntryOut.setSize(data.length);
                    zipEntryOut.setCompressedSize(-1);
                    zipOut.putNextEntry(zipEntryOut);
                    zipOut.write(data);
                    applied++;
                } else {
                    zipOut.putNextEntry(zipEntryOut);
                    transfer(zipIn, zipOut);
                }
            }
            task.getProject().getLogger().info("Successfully applied " + applied + " injections");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void constructInjectionMap(IInjectorTask task) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(task.getConfig()));
            String line;
            try {
                int i = 1;
                while ((line = reader.readLine()) != null) {
                    String[] arr = line.split(" ");
                    if (arr.length < 2) {
                        task.getProject().getLogger().warn("Invalid injection defintion on line " + i
                                + " of config file");
                        continue;
                    }
                    String clazz = arr[0];
                    HashSet<String> interfaces = new HashSet<>();
                    interfaces.addAll(Arrays.asList(arr).subList(1, arr.length));
                    injections.put(clazz, interfaces);
                    i++;
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Invalid config file", ex);
        }
    }

    public Map<String, Set<String>> getInjections() {
        return injections;
    }

    public byte[] transform(InputStream input, String className) throws IOException {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new IInjectorClassVisitor(writer, injections.get(className));
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private void transfer(InputStream in, OutputStream out) throws IOException {
        int n;
        byte[] buffer = new byte[4096];
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
    }

}
