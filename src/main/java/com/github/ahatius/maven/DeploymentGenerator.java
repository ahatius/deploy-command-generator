package com.github.ahatius.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class DeploymentGenerator {
  public DeploymentGenerator(String[] args) throws ParserConfigurationException, IOException, SAXException {
    // Parse arguments and validate them
    Arguments arguments = new Arguments();
    JCommander cli = new JCommander(arguments);

    try {
      cli.parse(args);
    } catch (ParameterException e) {
      // Display help and exit
      printHelp(cli, e.getMessage());
      System.exit(1);
      return;
    }

    Collection<File> files = FileUtils.listFiles(arguments.dir, new RegexFileFilter("^(.*?)\\.pom"),
        DirectoryFileFilter.DIRECTORY);

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

    BufferedWriter writer = new BufferedWriter(new FileWriter(arguments.output));
    try {
      for (File file : files) {
        Document document = documentBuilder.parse(file);

        String groupId = document.getElementsByTagName("groupId").item(0).getTextContent();
        String artifactId = document.getElementsByTagName("artifactId").item(0).getTextContent();
        String version = document.getElementsByTagName("version").item(0).getTextContent();

        File artifact = new File(file.getParent(), artifactId + "-" + version + ".jar");
        File sources = new File(file.getParent(), artifactId + "-" + version + "-sources.jar");
        File javadoc = new File(file.getParent(), artifactId + "-" + version + "-javadoc.jar");

        if(!artifact.isFile()) {
          continue;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("mvn deploy:deploy-file -DgeneratePom=false ");
        sb.append("-Durl=");

        if(!version.toLowerCase().endsWith("-snapshot")) {
          sb.append(arguments.releaseUrl);
        } else {
          sb.append(arguments.snapshotUrl);
        }

        sb.append(" ");
        sb.append("-DgroupId=");
        sb.append(groupId);
        sb.append(" ");
        sb.append("-DartifactId=");
        sb.append(artifactId);
        sb.append(" ");
        sb.append("-Dversion=");
        sb.append(version);
        sb.append(" ");
        sb.append("-DpomFile=");
        sb.append(file.getAbsolutePath());
        sb.append(" ");
        sb.append("-Dfile=");
        sb.append(artifact.getAbsolutePath());
        sb.append(" ");
        sb.append("-DrepositoryId=");
        sb.append(arguments.repositoryId);

        if (sources.exists()) {
          sb.append(" ");
          sb.append("-Dsources=");
          sb.append(sources.getAbsolutePath());
        }

        if (javadoc.exists()) {
          sb.append(" ");
          sb.append("-Djavadoc=");
          sb.append(javadoc.getAbsolutePath());
        }

        writer.write(sb.toString());
        writer.newLine();
      }
    } finally {
      writer.close();
    }
  }

  /**
   * Main method
   *
   * @param args Commandline arguments
   */
  public static void main(String[] args)
      throws IOException, SAXException, ParserConfigurationException {
    new DeploymentGenerator(args);
  }

  /**
   * Prints the help text including a message what went wrong
   *
   * @param cli The JCommander object that will be used to generate the help
   * @param message Additional information that shows why the help is shown
   */
  private static void printHelp(JCommander cli, String message) {
    System.out.println();
    System.out.println(message);
    System.out.println();
    cli.usage();
  }

  /**
   * Contains the allowed and required arguments for this application
   */
  private static class Arguments {
    @Parameter(names = {"-d"}, description = "The directory to scan for pom files", required = true)
    private File dir;

    @Parameter(names = {"-r"},
        description = "The target repository id that is configured in the settings.xml",
        required = true)
    private String repositoryId;

    @Parameter(names = {"-u"}, description = "The target deployment URL for releases",
        required = true)
    private String releaseUrl;

    @Parameter(names = {"-s"}, description = "The target deployment URL for snapshots",
            required = true)
    private String snapshotUrl;

    @Parameter(names = {"-o"},
        description = "The shell script that should be generated by this application",
        required = true)
    private File output;
  }
}
