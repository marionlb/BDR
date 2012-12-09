package ca.uqac.dim.turtledb.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * Utility class for file reading and writing.
 * @author sylvain
 *
 */
public class FileReadWrite
{
	/**
	 * Default file encoding
	 */
	public static String DEFAULT_ENCODING = "utf-8";
	
	/**
	 * Returns the content of a file into a string
	 * @param filename The file name to read from
	 * @param encoding The file encoding (optional, default "utf-8")
	 * @return The contents of the file
	 * @throws IOException If reading the file was impossible
	 */
	public static String getFileContents(String filename, String encoding) throws IOException
	{
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		Scanner scanner = new Scanner(new FileInputStream(filename), encoding);
		try {
			while (scanner.hasNextLine()){
				text.append(scanner.nextLine() + NL);
			}
		}
		finally{
			scanner.close();
		}
		return text.toString();
	}
	
	public static String getFileContents(String filename) throws IOException
	{
		return getFileContents(filename, DEFAULT_ENCODING);
	}
	
	// Pris sur le net :
	// http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
	public static String readFile(String path) throws IOException
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
