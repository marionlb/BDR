import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import ca.uqac.dim.turtledb.Attribute;
import ca.uqac.dim.turtledb.Condition;
import ca.uqac.dim.turtledb.Equality;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.Literal;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Schema;
import ca.uqac.dim.turtledb.Value;
import ca.uqac.dim.turtledb.VariableTable;

public class QueryTranslator {
	static String query;
	static Relation r;
	static HashMap<String, VariableTable> tables;
	//plus vraiment necessaire
//	static HashMap<String, Attribute> attributs;
	static ArrayList<Condition> conditions;
	static Schema schemaProj;
	/**
	 * Retourne un arbre d'opérations de type {@link Relation} à  partir d'une
	 * requète SQL syntaxiquement et sémantiquement correcte. Les feuilles de
	 * cet arbre (qui correspondent aux fragments) sont des
	 * {@link VariableTable}
	 * 
	 * @param q
	 *            La requète SQL
	 * @return Un objet {@link Relation} si la requête est correcte, null sinon
	 */
	public static Relation translate(String q) {
		// Initialisation des variables locales
		init(q);

		// parsage de la requête
		getTables();
		getAttributes();
		getConditions();

		// Construction de l'arbre
		jointures();
		selections();
		projections();

		return r;
	}

	/**
	 * Remplit l'attribut <code>tables</code> de la liste des tables à joindre.
	 */
	private static void getTables() {
		// On ne travaille que sur le fragment de la requête compris entre
		// FROM et WHERE
		int i_from = query.indexOf("FROM");
		int i_where = query.indexOf("WHERE");

		String q2;
		if (i_where != -1) {
			q2 = query.substring(i_from + "FROM ".length(), i_where);
		} else {
			q2 = query.substring(i_from + "FROM ".length(), query.length());
		}
		// On supprime les espaces pour plus de maniabilité
		q2 = q2.replace(" ", "");

		// On récupère un tableau des noms de tables (initialement séparés par
		// des virgules)
		String[] t_tables = q2.split(",");

		// On crée un objet VariableTable pour toutes ces tables et on les
		// stocke dans l'attribut tables (indexés par leur nom de table)
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
	private static void getAttributes() {
		// On ne travaille que sur le fragment de la requête compris entre
		// SELECT et FROM
		int i_from = query.indexOf("FROM");
		String q1 = query.substring(0 + "SELECT ".length(), i_from);
		// On supprime les espaces pour plus de maniabilité
		q1 = q1.replace(" ", "");

//		// On récupère un tableau des noms des attributs sur lesquels il faudra
//		// projeter (initialement séparés par des virgules)
//		String[] t_attr = q1.split(",");
//		
//		// On crée un objet Attribute pour tous ces éléments et on les
//		// stocke dans l'objet attributs
//		Attribute tmp;
//		for (String attribut : t_attr) {
//			tmp = new Attribute(attribut);
//			// On ajoute le nom simple dans tous les cas plutot que le nom
//			// composé avec la table
//			attributs.put(tmp.getName(), tmp);
//		}

		schemaProj = new Schema(q1);
	}

	/**
	 * Remplit l'attribut <code>conditions</code> de la liste des conditions de
	 * jointure et de selection
	 */
	private static void getConditions() {
		// On ne s'interesse qu'à la partie de la requête après WHERE
		// s'il y en a une
		int i_where = query.indexOf("WHERE");

		if (i_where == -1)
			return;

		String q3 = query.substring(i_where + "WHERE".length(), query.length());
		// On supprime les espaces pour plus de maniabilité
		q3 = q3.replace(" ", "");

		// tableau des conditions
		String[] t_cond = q3.split("AND");
		// tableau à double entrée des conditions séparés selon leurs opérandes
		String[][] t_t_cond = new String[t_cond.length][2];

		/*
		 * On itère sur la liste des conditions (On créera un ensemble de
		 * conditions plutôt qu'une seule condition NAire)
		 */
		for (int i = 0; i < t_t_cond.length; i++) {
			// on n'a affaire qu'à des égalités
			t_t_cond[i] = t_cond[i].split("=", 2);

			/*
			 * on vérifie si les opérandes sont des floats si oui, on en crée
			 * une Value si non, c'est un attribut On crée ensuite les Literal
			 * puis la Condition **** peut-être pb si on passe autre chose que
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
				l2 = new Value(t_t_cond[i][1]);
			} catch (NumberFormatException e) {
				l2 = new Attribute(t_t_cond[i][1]);
			}
			// La condition peut être créée
			conditions.add(new Equality(l1, l2));
		}

	}

	/**
	 * Crée un arbre basique de jointures de toutes les tables de
	 * <code>tables</code> à partir des conditions de jointures trouvées dans
	 * <code>conditions</code>
	 */
	private static void jointures() {
		Iterator<Entry<String, VariableTable>> it = tables.entrySet()
				.iterator();
		ArrayList<String> dejaJoin = new ArrayList<String>();

		VariableTable first, next;
		first = it.next().getValue();
		dejaJoin.add(first.getName());
		r = first;

		while (it.hasNext()) {
			next = it.next().getValue();
			// Il faut faire une jointure avec la prochaine table
			// on cherche donc la condition de jointure
			Iterator<Condition> itc = conditions.iterator();
			Condition c = null;
			Equality e;
			VariableTable toJoin;
			Join newJoin = null;
			while (itc.hasNext()) {
				c = itc.next();
				if (c instanceof Equality) {
					e = (Equality) c;
					String[] tab = e.joinTables();

					// On a vérifie si la condition en cours est une condition
					// de
					// jointure
					if (tab != null) {
						// vérifier si on a pas de circularité enre r et newJoin

						// La condition de jointure concerne-t'elle les bonnes
						// tables ?
						boolean cond = (tab[0].equals(next.getName())
								&& dejaJoin.contains(tab[1]) && !dejaJoin
									.contains(tab[0]))
								|| (tab[1].equals(next.getName())
										&& dejaJoin.contains(tab[0]) && !dejaJoin
											.contains(tab[1]));
						if (cond) {
							// on récupère la table à joindre
							toJoin = tables.get(next.getName());
							// on l'exclue des tables à joindre plus tard
							dejaJoin.add(toJoin.getName());
							// on crée la nouvelle jointure à partir de sa
							// condition
							newJoin = new Join(c);
							// On retire la condition de jointure de la liste
							// pour ne pas la réutiliser plus tard
							itc.remove();
							// on casse la boucle : on a trouvé et traité la
							// condition de jointure
							break;
						}
					}
				}
			}
			// A ce point, si la table courante n'est pas dans la liste des
			// tables à exclure, c'est qu'elle n'a pas encore été traitée : elle
			// n'a pas de condition de jointure. On gère ici ce cas
			if (!dejaJoin.contains(next.getName())) {
				// Join sans condition
				newJoin = new Join();
				dejaJoin.add(next.getName());
			}
			// Tous les cas de jointures ont été vus, on rempli maintenant le
			// noeud de jointure
			newJoin.setLeft(r);
			newJoin.setRight(next);
			// La nouvelle jointure devient la racine de l'arbre
			r = newJoin;
		}

	}

	/**
	 * Rajoute les selections à l'arbre de jointures. Utilise les conditions
	 * restantes de l'attribut <code>conditions</code> (les conditions de
	 * jointure ont déjà été utilisées.)
	 */
	private static void selections() {
		// TODO
	}

	/**
	 * Rajoute l'opération de projection à l'arbre précedemment construit.
	 * Construit un schéma à partir des attributs dans la {@link HashMap}
	 * <code>attributs</code>, qui sert à la construction de la projection.
	 */
	private static void projections() {
		//////////////A TESTER//////////////
//		String s="";
//		for (Entry<String,Attribute> entry : attributs.entrySet()) {
//			s+=entry.getKey();
//		}
//		Schema schema = new Schema(s);
		Projection p = new Projection(schemaProj, r);
		r=p;
	}

	private static void init(String q) {
		r = null;
		query = q;
		tables = new HashMap<String, VariableTable>();
		conditions = new ArrayList<Condition>();
	}

	public static void main(String[] args) {
		// String regex_attr = "([a-zA-Z]+\\.?[a-zA-Z][a-zA-Z0-9]*)";
		// String regex_select = "SELECT\\s" + regex_attr + "\\s*(,\\s*"
		// + regex_attr + ")*";
		// String regex_from = "FROM\\s+([^ ,]+)(?:\\s*,\\s*([^ ,]+))*\\s+";
		String req = "SELECT A.truc ,    Bidule.bus,attr FROM A,B, Bidule WHERE A.chose=3";
		String query = "SELECT A.truc ,    Bidule.bus,attr FROM A,B, Bidule WHERE A.chose=B.chose";
		// Pattern p = Pattern.compile(regex_select);
		//
		// translate(req);
		init(query);
		getConditions();
		getTables();
		getAttributes();
		// System.out.println(conditions.size());
		// System.out.println(((Equality)conditions.get(0)).toString());
		jointures();
		System.out.println(r.getClass());
		if (r instanceof Join) {
			Join j = (Join) r;
			System.out.println(j);
		}
		for (Attribute att : schemaProj) {
			System.out.println(att);
		}
	
	}

}
