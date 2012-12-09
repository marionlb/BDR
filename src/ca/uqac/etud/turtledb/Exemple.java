/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import ca.uqac.dim.turtledb.Relation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 *
 * @author fx
 */
public class Exemple
{
	public static void main(String[] args) throws Exception
	{
		String path = "data/Queries/";
		ArrayList<String> liste = new ArrayList<String>();
		String req = "SELECT A.truc ,    Bidule.bus,attr FROM A,B, Bidule WHERE A.chose=3";
		String query = "SELECT A.truc ,    Bidule.bus,attr FROM A,B, Bidule WHERE A.chose=B.chose";

		liste.add(req);
		liste.add(query);
		try
		{
			liste.add(readFile(path + "q1.txt"));
			liste.add(readFile(path + "q2.txt"));
			liste.add(readFile(path + "q3.txt"));
			liste.add(readFile(path + "q4.txt"));
			liste.add(readFile(path + "q5.txt"));
		} catch (IOException e) {
			System.err.println("Problème de lecture de fichier.");
		}

		for (int i = 0; i < liste.size(); i++)
		{
			Relation r = QueryTranslator.translate(liste.get(i));
			System.out.println(r);

		}
	}
	// Pris sur le net :
	// http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
	private static String readFile(String path) throws IOException
	{
		FileInputStream stream = new FileInputStream(new File(path));
		try
		{
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc
					.size());
			/* Instead of using default, pass in a decoder. */
			return Charset.defaultCharset().decode(bb).toString().replace(
					System.getProperty("line.separator"), "");
		} finally
		{
			stream.close();
		}
	}
	
}
