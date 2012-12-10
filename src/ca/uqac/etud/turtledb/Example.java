package ca.uqac.etud.turtledb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.uqac.dim.turtledb.Engine;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.XmlQueryParser;
import ca.uqac.dim.turtledb.util.FileReadWrite;

public class Example {

	private static final String path = "data/Queries/";
	static List<String> queries;

	public static void main(String[] args) {
		importer();
		BD.affiche();	

	}

	public static void importer() {
		//TABLES !
		Relation r_Astronaut = null, r_Mission = null, r_Crew = null, r_Rocket = null;
		try {
			r_Astronaut = XmlQueryParser.parse(FileReadWrite
					.getFileContents("data/Space/Astronaut.xml"));
			r_Mission = XmlQueryParser.parse(FileReadWrite
					.getFileContents("data/Space/Mission.xml"));
			r_Crew = XmlQueryParser.parse(FileReadWrite
					.getFileContents("data/Space/Crew.xml"));
			r_Rocket = XmlQueryParser.parse(FileReadWrite
					.getFileContents("data/Space/Rocket.xml"));
		} catch (FileNotFoundException e) {
			System.err.println("File not found");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Error reading files");
			System.exit(1);
		} catch (XmlQueryParser.ParseException e) {
			System.err.println("Error parsing XML files");
			System.exit(1);
		}
		if (r_Astronaut == null || r_Mission == null || r_Crew == null
				|| r_Rocket == null) {
			System.err.println("Error reading Space database");
			System.exit(1);
		}

		//SITES !
		Engine site_1 = new Engine("Site 1");
		Engine site_2 = new Engine("Site 2");
		Engine site_3 = new Engine("Site 3");
		
		site_1.putRelation("Astronaut", r_Astronaut);
		site_2.putRelation("Crew",r_Crew);
		site_2.putRelation("Rocket", r_Rocket);
		site_3.putRelation("Mission", r_Mission);
		//table répliquée
		site_1.putRelation("Rocket", r_Rocket);
		
		//QUERIES !!
		queries = new ArrayList<String>();

		try {
			for (int i = 1; i <= 3; i++) {
				queries.add(FileReadWrite.readFile(path + "S" + i + ".txt"));
			}

		} catch (IOException e) {
			System.err.println("Problème de lecture des requêtes.");
		}

	}

}
