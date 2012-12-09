package ca.uqac.etud.turtledb;

//~--- non-JDK imports --------------------------------------------------------
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
	private static String query;
	private static Relation r;
	private static HashMap<String, VariableTable> tables;

	// plus vraiment necessaire
	// static HashMap<String, Attribute> attributs;
	private static ArrayList<Condition> conditions;
	private static Schema schemaProj;

	/**
	 * Retourne un arbre d'opérations de type {@link Relation} à partir d'une
	 * requête SQL syntaxiquement et sémantiquement correcte. Les feuilles de
	 * cet arbre (qui correspondent aux fragments) sont des
	 * {@link VariableTable}
	 * 
	 * @param q
	 *            La requête SQL
	 * @return Un objet {@link Relation} si la requête est correcte, null sinon
	 */
	public static Relation translate(String q)
	{
		// Initialisation des variables locales
		init(q);

		// parsage de la requête
		getTables();
		getAttributes();
		getConditions();

		// Construction de l'arbre
		jointures();
		selectionsOR();
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
		if (i_where != -1)
		{
			q2 = query.substring(i_from + "FROM ".length(), i_where);
		} else
		{
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
		for (String table : t_tables)
		{
			tmp = new VariableTable(table);
			tables.put(table, tmp);
		}
	}

	/**
	 * Remplit l'attribut
	 * <code>attributs</code> de la liste des attributs sur lesquels projeter
	 */
	private static void getAttributes() {		
		// On ne travaille que sur le fragment de la requête compris entre
		// SELECT et FROM
		int i_from = query.indexOf("FROM");
		String q1 = query.substring(0 + "SELECT ".length(), i_from);
		// On supprime les espaces pour plus de maniabilité
		q1 = q1.replace(" ", "");

		schemaProj = new Schema(q1);
	}

	/**
	 * Remplit l'attribut
	 * <code>conditions</code> de la liste des conditions de jointure et de
	 * selection
	 */
	private static void getConditions() {
		// TODO : Gérer les paranthèses

		// On ne s'interesse qu'à la partie de la requête après WHERE
		// s'il y en a une
		int i_where = query.indexOf("WHERE");

		if (i_where == -1)
		{
			return;
		}

		String q3 = query.substring(i_where + "WHERE".length(), query.length());
		// On supprime les espaces pour plus de maniabilité
		q3 = q3.replace(" ", "");

		// tableau des conditions globales et tableaux des sous-conditions et
		// des opérandes
		String[] t_cond = q3.split("AND"), tor, teg;

		/*
		 * On itère sur la liste des conditions (On créera un ensemble de
		 * conditions plutôt qu'une seule condition NAire)
		 */
		for (int i = 0; i < t_cond.length; i++)
		{
			// cas OR
			tor = t_cond[i].split("OR");
			Condition[] tor_c = new Condition[tor.length];

			for (int j = 0; j < tor.length; j++) {
				// on n'a affaire qu'à des égalités
				teg = tor[j].split("=", 2);
				/*
				 * on vérifie si les opérandes sont des floats si oui, on en
				 * crée une Value si non, c'est un attribut On crée ensuite les
				 * Literal puis la Condition. Peut-être pb si on passe autre
				 * chose que des floats/ints
				 */
				Literal l1 = null, l2 = null;
				try
				{
					Float.parseFloat(teg[0]);
					l1 = new Value(teg[0]);
				} catch (NumberFormatException e)
				{
					l1 = new Attribute(teg[0]);
				}
				try
				{
					Float.parseFloat(teg[1]);
					l2 = new Value(teg[1]);
				} catch (NumberFormatException e)
				{
					l2 = new Attribute(teg[1]);
				}
				tor_c[j] = new Equality(l1, l2);
			}

			// Cas où il n'y a pas de OU : une seule condition
			if (tor.length == 1)
			{
				conditions.add(tor_c[0]);
			} else
			{
				// Cas du OU : on utilise LogicalOR
				NAryCondition naire = new LogicalOr();
				for (Condition condition : tor_c)
				{
					naire.addCondition(condition);
				}
				conditions.add(naire);
			}
		}

	}

	/**
	 * crée un arbre basique de jointures de toutes les tables de
	 * <code>tables</code> à partir des conditions de jointures trouvées dans
	 * <code>conditions</code>
	 */
	private static void jointures() {
		// TODO Gérer le cas où les conditions de jointure ne sont pas dans le
		// bon ordre
		/*
		 * ie quand on veut joindre A-B puis (A-B)-C mais que les conditions de
		 * jointure existent entre A-C et B-C (ici, on créera un produit
		 * cartesien de A-B au lieu de créer (B-C)-A
		 */

		Iterator<Entry<String, VariableTable>> it = tables.entrySet()
				.iterator();
		ArrayList<String> dejaJoin = new ArrayList<String>();

		VariableTable first, next;
		first = it.next().getValue();
		dejaJoin.add(first.getName());
		r = first;

		while (it.hasNext())
		{
			next = it.next().getValue();
			// Il faut faire une jointure avec la prochaine table
			// on cherche donc la condition de jointure
			Iterator<Condition> itc = conditions.iterator();
			Condition c = null;
			Equality e;
			VariableTable toJoin;
			Join newJoin = null;
			while (itc.hasNext())
			{
				c = itc.next();
				if (c instanceof Equality)
				{
					e = (Equality) c;
					String[] tab = e.joinTables();

					// On a vérifie si la condition en cours est une condition
					// de jointure
					if (tab != null)
					{

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
	private static void selections()
	{
		Iterator<Condition> it = conditions.iterator();
		Condition c;
		Selection s;
		while (it.hasNext())
		{
			c = it.next();
			s = new Selection(c, r);
			r = s;
		}
	}

	private static void selectionsOR()
	{
		Iterator<Condition> it = conditions.iterator();
		Condition c;
		Selection s;
		while (it.hasNext()) {
			c = it.next();
			// Si ce n'est pas un OR , on ajoute la sélection en haut de l'arbre
			if (!(c instanceof LogicalOr)) {
				s = new Selection(c, r);
				r = s;
			}
			// sinon, on ajoute une union de sélections
			else {
				Union union = new Union();
				LogicalOr lor = (LogicalOr) c;
				for (Condition cond : lor.m_conditions) {
					union.addOperand(new Selection(cond, (Relation) r.clone()));
				}
				r = union;
			}
		}
	}

	/**
	 * Rajoute l'opération de projection à l'arbre précedemment construit.
	 * Construit un schéma à partir des attributs dans la {@link HashMap}
	 * <code>attributs</code>, qui sert à la construction de la projection.
	 */
	private static void projections() {
		// TODO Tester cas *

		if (!schemaProj.elementAt(0).equals(new Attribute("*"))) {
			Projection p = new Projection(schemaProj, r);
			r = p;
		}
	}

	private static void init(String q)
	{
		r = null;
		query = q;
		tables = new LinkedHashMap<String, VariableTable>();
		conditions = new ArrayList<Condition>();
	}


}
