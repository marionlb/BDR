import java.util.HashMap;
import java.util.HashSet;

import ca.uqac.dim.turtledb.Attribute;
import ca.uqac.dim.turtledb.Condition;
import ca.uqac.dim.turtledb.Equality;
import ca.uqac.dim.turtledb.Literal;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Value;
import ca.uqac.dim.turtledb.VariableTable;

public class QueryTranslator {
	static String query;
	static Relation r;
	static HashMap<String, VariableTable> tables;
	static HashMap<String, Attribute> attributs;
	static HashSet<Condition> conditions;

	/**
	 * Remplit l'attribut <code>tables</code> de la liste des tables � joindre.
	 */
	static void getTables() {
		// On ne travaille que sur le fragment de la requ�te compris entre
		// FROM et WHERE
		int i_from = query.indexOf("FROM");
		int i_where = query.indexOf("WHERE");

		String q2;
		if (i_where != -1) {
			q2 = query.substring(i_from + "FROM ".length(), i_where);
		} else {
			q2 = query.substring(i_from + "FROM ".length(), query.length());
		}
		// On supprime les espaces pour plus de maniabilit�
		q2 = q2.replace(" ", "");

		// On r�cup�re un tableau des noms de tables (initialement s�par�s par
		// des virgules)
		String[] t_tables = q2.split(",");

		// On cr�e un objet VariableTable pour toutes ces tables et on les
		// stocke dans l'attribut tables (index�s par leur nom de table)
		VariableTable tmp;
		for (String table : t_tables) {
			tmp = new VariableTable(table);
			tables.put(table, tmp);
		}
	}

	/**
	 * Remplit l'attribut <code>attributs</code> de la liste des attributs sur
	 * lesquels projeter
	 */
	static void getAttributes() {
		// On ne travaille que sur le fragment de la requ�te compris entre
		// SELECT et FROM
		int i_from = query.indexOf("FROM");
		String q1 = query.substring(0 + "SELECT ".length(), i_from);
		// On supprime les espaces pour plus de maniabilit�
		q1 = q1.replace(" ", "");

		// On r�cup�re un tableau des noms des attributs sur lesquels il faudra
		// projeter (initialement s�par�s par des virgules)
		String[] t_attr = q1.split(",");

		// On cr�e un objet Attribute pour tous ces �l�ments et on les
		// stocke dans l'objet attributs
		Attribute tmp;
		for (String attribut : t_attr) {
			tmp = new Attribute(attribut);
			// On ajoute le nom simple dans tous les cas plutot que le nom
			// compos� avec la table
			attributs.put(tmp.getName(), tmp);
		}
	}

	/**
	 * Remplit l'attribut <code>conditions</code> de la liste des conditions de
	 * jointure et de selection
	 */
	static void getConditions() {
		// On ne s'interesse qu'� la partie de la requ�te apr�s WHERE
		// s'il y en a une
		int i_where = query.indexOf("WHERE");

		if (i_where == -1)
			return;

		String q3 = query.substring(i_where + "WHERE".length(), query.length());
		// On supprime les espaces pour plus de maniabilit�
		q3 = q3.replace(" ", "");

		// tableau des conditions
		String[] t_cond = q3.split("AND");
		// tableau � double entr�e des conditions s�par�s selon leurs op�randes
		String[][] t_t_cond = new String[t_cond.length][2];

		/*
		 * On it�re sur la liste des conditions (On cr�era un ensemble de
		 * conditions plut�t qu'une seule condition NAire)
		 */
		for (int i = 0; i < t_t_cond.length; i++) {
			// on n'a affaire qu'� des �galit�s
			t_t_cond[i] = t_cond[i].split("=", 2);

			/*
			 * on v�rifie si les op�randes sont des floats si oui, on en cr�e
			 * une Value si non, c'est un attribut On cr�e ensuite les Literal
			 * puis la Condition **** peut-�tre pb si on passe autre chose que
			 * des floats/ints
			 */
			Literal l1 = null, l2 = null;
			try {
				Float.parseFloat(t_t_cond[i][0]);
				l1 = new Value(t_t_cond[i][0]);
			} catch (NumberFormatException e) {
				l1 = new Attribute(t_t_cond[i][0]);
			}
			try {
				Float.parseFloat(t_t_cond[i][1]);
				l1 = new Value(t_t_cond[i][1]);
			} catch (NumberFormatException e) {
				l2 = new Attribute(t_t_cond[i][1]);
			}
			// La condition peut �tre cr��e
			conditions.add(new Equality(l1, l2));
		}

	}

	/**
	 * Retourne un arbre d'op�rations de type {@link Relation} � partir d'une
	 * requ�te SQL syntaxiquement et s�mantiquement correcte. Les feuilles de
	 * cet arbre (qui correspondent aux fragments) sont des
	 * {@link VariableTable}
	 * 
	 * @param q
	 *            La requ�te SQL
	 * @return Un objet {@link Relation} si la requ�te est correcte, null sinon
	 */
	public static Relation translate(String q) {
		init(q);

		// TODO

		return null;
	}

	private static void init(String q) {
		query = q;
		tables = new HashMap<String, VariableTable>();
		attributs = new HashMap<String, Attribute>();
		conditions = new HashSet<Condition>();
	}

	public static void main(String[] args) {
		// String regex_attr = "([a-zA-Z]+\\.?[a-zA-Z][a-zA-Z0-9]*)";
		// String regex_select = "SELECT\\s" + regex_attr + "\\s*(,\\s*"
		// + regex_attr + ")*";
		// String regex_from = "FROM\\s+([^ ,]+)(?:\\s*,\\s*([^ ,]+))*\\s+";
		String req = "SELECT A.truc ,    Bidule.bus,attr FROM A,B, Bidule WHERE A.chose=3";
		// Pattern p = Pattern.compile(regex_select);
		//
		// translate(req);
		init(req);
		getConditions();
		getTables();
		getAttributes();
		System.out.println();
	}

}
