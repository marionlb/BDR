import java.util.regex.Pattern;

import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.VariableTable;


public class QueryTranslator {

	/**
	 * Retourne un arbre d'opérations de type {@link Relation} à  partir d'une requète SQL 
	 * syntaxiquement et sémantiquement correcte. 
	 * Les feuilles de cet arbre (qui correspondent aux fragments) sont 
	 * des {@link VariableTable}
	 * @param q La requète SQL
	 * @return Un objet {@link Relation} si la requête est correcte, null sinon
	 */
	public static Relation translate(String q) {				
		try {
			/*
			 * On sépare trois chaines de caractères correspondant à  :
			 * -la ligne SELECT avec les attributs de la projection
			 * -la ligne FROM avec les tables à joindre
			 * -la/les ligne(s) WHERE avec les conditions de selection 
			 */
			int i_from = q.indexOf("FROM");
			int i_where = q.indexOf("WHERE");
			String q1,q2,q3;
			
			q1=q.substring(0+"SELECT ".length(), i_from);
			if(i_where!=-1) {
				q2=q.substring(i_from+"FROM ".length(), i_where);
				q3=q.substring(i_where+"WHERE".length(), q.length());
			} else {
				q2=q.substring(i_from+"FROM ".length(), q.length());
				q3="";
			}
			//on enlève les espaces qui ne comptent pas
			q1=q1.replace(" ", "");
			q2=q2.replace(" ", "");
			q3=q3.replace(" ", "");
			
			//on sélectionne les informations qui nous interessent
			String[] t_attr = q1.split(",");
			String[] t_tables = q2.split(",");
			String[] t_cond = q3.split("AND");
			
//			for (String string : t_cond) {
//				System.out.print(string);
//				System.out.println("  "+string.length());
//			}
			
			Table[] t_t = new Table[t_tables.length];
			for (int i=0; i<t_tables.length; i++) {
				t_t[i]=new Table(null);
				t_t[i].setName(t_tables[i]);
			}
			
			Join jointure = new Join(), tmp=jointure;
			for (int i = 0; i < t_t.length-1; i++) {
				tmp.setLeft(t_t[i]);
				Join tmp2 = new Join();
				tmp.setRight(tmp2);
				tmp=tmp2;
			}
			tmp.setLeft(t_t[t_t.length-2]);
			tmp.setRight(t_t[t_t.length-1]);
			
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static void main(String[] args) {
		String regex_attr="([a-zA-Z]+\\.?[a-zA-Z][a-zA-Z0-9]*)";
		String regex_select="SELECT\\s"+regex_attr+"\\s*(,\\s*"+regex_attr+")*";
		String regex_from="FROM\\s+([^ ,]+)(?:\\s*,\\s*([^ ,]+))*\\s+";
		String req = "SELECT A.truc ,    Bidule.bus,attr FROM A,B, Bidule WHERE A.chose=3";
		Pattern p = Pattern.compile(regex_select);

		translate(req);

	}

}
