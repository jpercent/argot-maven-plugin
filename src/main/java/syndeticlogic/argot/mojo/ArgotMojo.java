/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package syndeticlogic.argot.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import syndeticlogic.argot.parser.ParseArgot;

/**
 * Compile an Argot accessor or schema file.
 *
 * @goal compile
 * @phase generate-sources
 */
public class ArgotMojo extends AbstractMojo {

    public static final String IDL_EXTENSION = ".argot";

    /**
     * @parameter expression="${sourceDirectory}" default-value="${basedir}/src/main/argot"
     */
    protected File sourceDirectory;

    /**
     * @parameter expression="${outputDirectory}" default-value="${project.build.directory}/generated-sources/argot"
     */
    protected File outputDirectory;

    /**
     * A set of Ant-like inclusion patterns used to select files from
     * the source directory for processing. By default, the pattern
     * <code>**\/*.argot</code> is used to select grammar files.
     *
     * @parameter
     */
    private final String[] includes = new String[] { "**/*" + IDL_EXTENSION };

    /**
     * A set of Ant-like exclusion patterns used to prevent certain
     * files from being processed. By default, this set is empty such
     * that no files are excluded.
     *
     * @parameter
     */
    private final String[] excludes = new String[0];

    /**
     * The current Maven project.
     *
     * @parameter default-value="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;
    
    private final FileSetManager fileSetManager = new FileSetManager();

    public void execute() throws MojoExecutionException {
        if (!sourceDirectory.isDirectory()) {
            this.getLog().warn("Missing source directory " + sourceDirectory);
            // Some prefer to throw an exception if there's not avro directory, but
            // I think it's fine not to have a directory since in a multi-module project
            // some subprojects would have a src/main/avro and some won't
            return;
        }

        FileSet fs = new FileSet();
        fs.setDirectory(sourceDirectory.getAbsolutePath());
        fs.setFollowSymlinks(true);

        for (String include : includes) {
            fs.addInclude(include);
        }
        for (String exclude : excludes) {
            fs.addExclude(exclude);
        }

        String[] includedFiles = fileSetManager.getIncludedFiles(fs);

        this.getLog().info("Found " + includedFiles.length + " files");

        // Directory for genavro tmp files, only create if needed
        File tmpOutDir = null;

        for (String filename : includedFiles) {
            this.getLog().info("Processing " + filename);
            try {
                File srcFile = new File(sourceDirectory, filename);
                if (srcFile.getAbsolutePath().endsWith(IDL_EXTENSION)) {
                    if (tmpOutDir == null) {
                        tmpOutDir = File.createTempFile("argot", null);
                        tmpOutDir.delete();
                        tmpOutDir.mkdir();
                    }

                    File outFile = new File(tmpOutDir,
                            getNameWithoutExtension(filename) +
                            IDL_EXTENSION);
                    InputStream parseIn = new FileInputStream(srcFile);
                    PrintStream parseOut = new PrintStream(
                            new FileOutputStream(outFile));
                    FileReader reader = new FileReader(srcFile);
                    ParseArgot.parse(reader);
                    
                    
              /*      
                    
                    
                    GenAvro parser = new GenAvro(parseIn);
                    Protocol p = parser.CompilationUnit();
                    parseOut.print(p.toString(true));
                    parseOut.flush();
                    parseOut.close();
                    parseIn.close();
                    srcFile = outFile;
                    filename = srcFile.getName();
                    */
                }
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Error compiling file " + filename
                        + " to " + outputDirectory, e);
            } 
        }

        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

        // cleanup
        if (tmpOutDir != null) {
            deleteDir(tmpOutDir);
        }
    }

    /**
     * Gets a file name from the string without the file extension.
     * For example: x.argot => x.
     */
    private String getNameWithoutExtension(String filename) {
      if (filename == null) {
        return null;
      }
      int dot = filename.lastIndexOf('.');
      return filename.substring(0, dot);
    }

    /**
     * Deletes all files and subdirectories under dir. If a deletion fails,
     * the method stops attempting to delete and returns false.
     **/
    public static boolean deleteDir(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

}
