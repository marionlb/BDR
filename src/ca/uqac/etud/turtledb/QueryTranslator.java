package ca.uqac.etud.turtledb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import ca.uqac.dim.turtledb.Attribute;
import ca.uqac.dim.turtledb.Condition;
import ca.uqac.dim.turtledb.Equality;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.Literal;
import ca.uqac.dim.turtledb.LogicalOr;
import ca.uqac.dim.turtledb.NAryCondition;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Schema;
import ca.uqac.dim.turtledb.Selection;
import ca.uqac.dim.turtledb.Union;
import ca.uqac.dim.turtledb.Value;
import ca.uqac.dim.turtledb.VariableTable;

public class QueryTranslator {
	static String query;
	static Relation r;
	static HashMap<String, VariableTable> tables;
	// plus vraiment necessaire
	// static HashMap<String, Attribute> attributs;
	static ArrayList<Condition> conditions;
	static Schema schemaProj;

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
		// Initialisation des variables locales
		init(q);

		// parsage de la requ�te
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
	 * Remplit l'attribut <code>tables</code> de la liste des tables � joindre.
	 */
	private static void getTables() {
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
	private static void getAttributes() {
		// TODO : G�rer le joker *

		// On ne travaille que sur le fragment de la requ�te compris entre
		// SELECT et FROM
		int i_from = query.indexOf("FROM");
		String q1 = query.substring(0 + "SELECT ".length(), i_from);
		// On supprime les espaces pour plus de maniabilit�
		q1 = q1.replace(" ", "");

		// // On r�cup�re un tableau des noms des attributs sur lesquels il
		// faudra
		// // projeter (initialement s�par�s par des virgules)
		// String[] t_attr = q1.split(",");
		//
		// // On cr�e un objet Attribute pour tous ces �l�ments et on les
		// // stocke dans l'objet attributs
		// Attribute tmp;
		// for (String attribut : t_attr) {
		// tmp = new Attribute(attribut);
		// // On ajoute le nom simple dans tous les cas plutot que le nom
		// // compos� avec la table
		// attributs.put(tmp.getName(), tmp);
		// }

		schemaProj = new Schema(q1);
	}

	/**
	 * Remplit l'attribut <code>conditions</code> de la liste des conditions de
	 * jointure et de selection
	 */
	private static void getConditions() {
		// TODO : G�rer les paranth�ses

		// On ne s'interesse qu'� la partie de la requ�te apr�s WHERE
		// s'il y en a une
		int i_where = query.indexOf("WHERE");

		if (i_where == -1)
			return;

		String q3 = query.substring(i_where + "WHERE".length(), query.length());
		// On supprime les espaces pour plus de maniabilit�
		q3 = q3.replace(" ", "");

		// tableau des conditions globales et tableaux des sous-conditions et
		// des op�randes
		String[] t_cond = q3.split("AND"), tor, teg;

		/*
		 * On it�re sur la liste des conditions (On cr�era un ensemble de
		 * conditions plut�t qu'une seule condition NAire)
		 */
		for (int i = 0; i < t_cond.length; i++) {
			// cas OR
			tor = t_cond[i].split("OR");
			Condition[] tor_c = new Condition[tor.length];

			for (int j = 0; j < tor.length; j++) {
				// on n'a affaire qu'� des �galit�s
				teg = tor[j].split("=", 2);
				/*
				 * on v�rifie si les op�randes sont des floats si oui, on en
				 * cr�e une Value si non, c'est un attribut On cr�e ensuite les
				 * Literal puis la Condition. Peut-�tre pb si on passe autre
				 * chose que des floats/ints
				 */
				Literal l1 = null, l2 = null;
				try {
					Float.parseFloat(teg[0]);
					l1 = new Value(teg[0]);
				} catch (NumberFormatException e) {
					l1 = new Attribute(teg[0]);
				}
				try {
					Float.parseFloat(teg[1]);
					l2 = new Value(teg[1]);
				} catch (NumberFormatException e) {
					l2 = new Attribute(teg[1]);
				}
				tor_c[j] = new Equality(l1, l2);
			}

			// Cas o� il n'y a pas de OU : une seule condition
			if (tor.length == 1)
				conditions.add(tor_c[0]);
			else {
				// Cas du OU : on utilise LogicalOR
				NAryCondition naire = new LogicalOr();
				for (Condition condition : tor_c) {
					naire.addCondition(condition);
				}
				conditions.add(naire);
			}
		}

	}

	/**
	 * Cr�e un arbre basique de jointures de toutes les tables de
	 * <code>tables</code> � partir des conditions de jointures trouv�es dans
	 * <code>conditions</code>
	 */
	private static void jointures() {
		// TODO G�rer le cas o� les conditions de jointure ne sont pas dans le
		// bon ordre
		/*
		 * ie quand on veut joindre A-B puis (A-B)-C mais que les conditions de
		 * jointure existent entre A-C et B-C (ici, on cr�era un produit
		 * cartesien de A-B au lieu de cr�er (B-C)-A
		 */

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

					// On a v�rifie si la condition en cours est une condition
					// de jointure
					if (tab != null) {

						// La condition de jointure concerne-t'elle les bonnes
						// tables ?
						boolean cond = (tab[0].equals(next.getName())
								&& dejaJoin.contains(tab[1]) && !dejaJoin
								.contains(tab[0]))
								|| (tab[1].equals(next.getName())
										&& dejaJoin.contains(tab[0]) && !dejaJoin
										.contains(tab[1]));
						if (cond) {
							// on r�cup�re la table � joindre
							toJoin = tables.get(next.getName());
							// on l'exclue des tables � joindre plus tard
							dejaJoin.add(toJoin.getName());
							// on cr�e la nouvelle jointure � partir de sa
							// condition
							newJoin = new Join(c);
							// On retire la condition de jointure de la liste
							// pour ne pas la r�utiliser plus tard
							itc.remove();
							// on casse la boucle : on a trouv� et trait� la
							// condition de jointure
							break;
						}
					}
				}
			}
			// A ce point, si la table courante n'est pas dans la liste des
			// tables � exclure, c'est qu'elle n'a pas encore �t� trait�e : elle
			// n'a pas de condition de jointure. On g�re ici ce cas
			if (!dejaJoin.contains(next.getName())) {
				// Join sans condition
				newJoin = new Join();
				dejaJoin.add(next.getName());
			}
			// Tous les cas de jointures ont �t� vus, on rempli maintenant le
			// noeud de jointure
			newJoin.setLeft(r);
			newJoin.setRight(next);
			// La nouvelle jointure devient la racine de l'arbre
			r = newJoin;
		}

	}

	/**
	 * Rajoute les selections � l'arbre de jointures. Utilise les conditions
	 * restantes de l'attribut <code>conditions</code> (les conditions de
	 * jointure ont d�j� �t� utilis�es.)
	 */
	private static void selections() {
		Iterator<Condition> it = conditions.iterator();
		Condition c;
		Selection s;
		while (it.hasNext()) {
			c = it.next();
			s = new Selection(c, r);
			r = s;
		}
	}

	private static void selectionsOR() {
		Iterator<Condition> it = conditions.iterator();
		Condition c;
		Selection s;
		while (it.hasNext()) {
			c = it.next(); 
			//Si ce n'est pas un OR , on ajoute la sélection en haut de l'arbre
			if(!(c instanceof LogicalOr)) {
				s = new Selection(c, r);
				r = s;
			} 
			//sinon, on ajoute une union de sélections
			else {
				Union union = new Union();
				LogicalOr lor = (LogicalOr) c;
				Condition c_simple;
				for (Condition cond : lor.m_conditions) {
//					union.addOperand(new Selection(cond, r.c));
				}
			}
		}
	}
	/**
	 * Rajoute l'op�ration de projection � l'arbre pr�cedemment construit.
	 * Construit un sch�ma � partir des attributs dans la {@link HashMap}
	 * <code>attributs</code>, qui sert � la construction de la projection.
	 */
	private static void projections() {
		//TODO Tester cas *

		if(!schemaProj.elementAt(0).equals(new Attribute("*"))) {
			Projection p = new Projection(schemaProj, r);
			r = p;
		}
	}

	private static void init(String q) {
		r = null;
		query = q;
		tables = new LinkedHashMap<String, VariableTable>();
		conditions = new ArrayList<Condition>();
	}

	public static void main(String[] args) throws Exception {
		String path = "data/Queries/";
		ArrayList<String> liste = new ArrayList<String>();
		String req = "SELECT A.truc ,    Bidule.bus,attr FROM A,B, Bidule WHERE A.chose=3";
		String query = "SELECT A.truc ,    Bidule.bus,attr FROM A,B, Bidule WHERE A.chose=B.chose";

		liste.add(req);
		liste.add(query);
		try {
			liste.add(readFile(path + "q1.txt"));
			liste.add(readFile(path + "q2.txt"));
			liste.add(readFile(path + "q3.txt"));
			liste.add(readFile(path + "q4.txt"));
			liste.add(readFile(path + "q5.txt"));
		} catch (IOException e) {
			System.err.println("Probl�me de lecture de fichier.");
		}

		for (int i = 0; i < liste.size(); i++) {
			// try {
			// translate(liste.get(i));
			init(liste.get(i));

			// parsage de la requ�te
			getTables();
			getAttributes();
			getConditions();

			// Construction de l'arbre
			jointures();
			selections();
			projections();

			System.out.println(r);
			// if (r instanceof Join)
			// System.out.println(i + " : " + (Join) r);
			// else if (r instanceof VariableTable)
			// System.out.println(i + " : " + (VariableTable) r);
			// else
			// System.err.println(i + " : " + r.getClass());
			// System.out.println("Fini de traduire la "+i);
			// } catch (Exception e) {
			// System.out.println("Probl�me dans la traduction de la "+i);
			// e.printStackTrace();
			// }

		}
	}

	// Pris sur le net :
	// http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
	private static String readFile(String path) throws IOException {
		FileInputStream stream = new FileInputStream(new File(path));
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc
					.size());
			/* Instead of using default, pass in a decoder. */
			return Charset.defaultCharset().decode(bb).toString().replace(
					System.getProperty("line.separator"), "");
		} finally {
			stream.close();
		}
	}
}
