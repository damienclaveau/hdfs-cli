// Copyright (c) 2012 P. Taylor Goetz (ptgoetz@gmail.com)

package com.instanceone.hdfs.shell.command;

import java.io.IOException;
import java.net.URI;
import java.io.File;

import com.instanceone.stemshell.command.AbstractCommand;
import com.instanceone.stemshell.Environment;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.apache.commons.cli.CommandLine;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;

public class HdfsConnect extends AbstractCommand {
    
    public static final String HADOOP_CONF_DIR = "HADOOP_CONF_DIR";
    private static final String[] HADOOP_CONF_FILES = {"core-site.xml", "hdfs-site.xml"};
    
    public HdfsConnect(String name) {
        super(name);
        Completer completer = new StringsCompleter("hdfs://localhost:8020/", "hdfs://hdfshost:8020/");
        this.completer = completer;
    }

    public void execute(Environment env, CommandLine cmd, ConsoleReader reader) {
        try {
            if(cmd.getArgs().length > 0){
                Configuration config = new Configuration(false);
                
                String hadoopConfDirProp = System.getenv().get(HADOOP_CONF_DIR);
                File hadoopConfDir = new File(hadoopConfDirProp).getAbsoluteFile();
                for (String file : HADOOP_CONF_FILES) {
                  File f = new File(hadoopConfDir, file);
                  if (f.exists()) {
                    config.addResource(new Path(f.getAbsolutePath()));
                  }
                }

                if (env.getProperty(HdfsKrb.USE_KERBEROS) != null && env.getProperty(HdfsKrb.USE_KERBEROS) == "true") {
                    config.set(HdfsKrb.HADOOP_AUTHENTICATION, HdfsKrb.KERBEROS);
                    config.set(HdfsKrb.HADOOP_AUTHORIZATION, "true");
                    config.set(HdfsKrb.HADOOP_KERBEROS_NN_PRINCIPAL, env.getProperty(HdfsKrb.HADOOP_KERBEROS_NN_PRINCIPAL));
                    UserGroupInformation.setConfiguration(config);
                }

                FileSystem hdfs = FileSystem.get(URI.create(cmd.getArgs()[0]),
                                config);
                env.setValue(HdfsCommand.CFG,config);
                env.setValue(HdfsCommand.HDFS, hdfs);
                // set working dir to root
                hdfs.setWorkingDirectory(hdfs.makeQualified(new Path("/")));
                
                FileSystem local = FileSystem.getLocal(new Configuration());
                env.setValue(HdfsCommand.LOCAL_FS, local);
                env.setProperty(HdfsCommand.HDFS_URL, hdfs.getUri().toString());

                FSUtil.prompt(env);

                log(cmd, "Connected: " + hdfs.getUri());
                logv(cmd, "HDFS CWD: " + hdfs.getWorkingDirectory());
                logv(cmd, "Local CWD: " + local.getWorkingDirectory());

            }     
        }
        catch (IOException e) {
            log(cmd, e.getMessage());
        }
    }

    @Override
    public Completer getCompleter() {
        return this.completer;
    }


}
