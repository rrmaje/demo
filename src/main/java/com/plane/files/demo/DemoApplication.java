package com.plane.files.demo;

import java.io.Console;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(DemoApplication.class, args);

		Console console = System.console();

		if (args.length !=1 && args.length != 4)   {
			System.err.println("\nUsage: java -Dfile.encoding=\"UTF-8\" -jar path_to_jar kb_directory <instance> <user> <pass>\n");
			System.exit(-1);
		}

		String instance, user, pass;
		if (args.length == 4) {
			instance = args[1];
			user = args[2];
			pass = args[3];
		} else {

			instance = console.readLine("sn instance:[%s]", KbKnowledgeAPI.DEFAULT_INSTANCE);
			if (instance.length() == 0) {
				instance = KbKnowledgeAPI.DEFAULT_INSTANCE;
			}
			user = console.readLine("user:");
			char[] pwd = console.readPassword("pass:");
			pass = String.valueOf(pwd);
		}

		final KbKnowledgeAPI demo = new KbKnowledgeAPI(user, pass, instance, Paths.get(args[0]));

		demo.createDefaultHttpClient();

		demo.createResourceReferences();

	}

}
