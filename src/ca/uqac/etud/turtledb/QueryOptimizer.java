package ca.uqac.etud.turtledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import ca.uqac.dim.turtledb.Attribute;
import ca.uqac.dim.turtledb.BinaryRelation;
import ca.uqac.dim.turtledb.Engine;
import ca.uqac.dim.turtledb.NAryRelation;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.QueryPlan;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Schema;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.TableParser;
import ca.uqac.dim.turtledb.UnaryRelation;
import ca.uqac.dim.turtledb.VariableTable;

public class QueryOptimizer {

	static HashMap<String, Float> varInterCouts;
	static HashMap<String, Integer> varInterTuples;
	/**
	 * Les sites composant la base de donn�es r�partie
	 */
	HashMap<String, Engine> listeSites;

	/**
	 * Le nom et la composition (i.e. expression d'alg�bre relationnelle) de
	 * chacun des fragments + La solution d'allocation des fragments sur chacun
	 * des sites (i.e. quel fragment est h�berg� sur quel site)
	 */
	List<VariableTable> listeFragments;
	List<Table> listeTables;

	/**
	 * Tableau des co�ts de stockage
	 */
	// float[] coutsStockage;
	static HashMap<String, Float> coutsStockage;
	/**
	 * Matrice des co�ts de communication
	 */
	// float[][] coutsComm;
	static Matrice coutsComm;

	/**
	 * Calcule le co�t d'un plan de requ�tes donn�, � partir des co�ts de
	 * stockage et de communication pr�d�finis.
	 * 
	 * @param qp
	 *            Le plan de requ�te qu'on analyse
	 * @return Le co�t total du plan de requ�tes.
	 */
	public static float getCost(QueryPlan qp) {
		if (coutsStockage == null || coutsComm == null)
			for (Engine site : BD.sites.values()) {
				QueryOptimizer.addDefaultCost(site.getName());
			}

		float res = 0;
		varInterCouts = new HashMap<String, Float>();
		for (Entry<String, Set<Relation>> entry : qp.entrySet()) {
			res += cost(entry.getKey(), entry.getValue());
		}
		return res;
	}

	public static float cost(String site, Set<Relation> set) {
		float res = 0;
		if (!BD.sites.containsKey(site))
			return -1;

		for (Iterator<Relation> iterator = set.iterator(); iterator.hasNext();) {
			Relation r = iterator.next();

			if (r instanceof VariableTable && !BD.isATable((VariableTable) r)) {
				VariableTable vt = (VariableTable) r;

				res += calcCost(site, vt.getRelation());

				if (vt.getSite()!=null && vt.getSite()!="") {
					String siteTransfert = vt.getSite();
					if (BD.sites.containsKey(siteTransfert)) {
						float coutTransfert = cost(siteTransfert, site);
						System.out.println(vt.getName() +" : " +vt.getRelation().nTuples +" tuples");
						coutTransfert *= vt.getRelation().nTuples;
						res += coutTransfert;
					}
				}

				varInterCouts.put(((VariableTable) r).getName(), res);
			} else
				res = calcCost(site, r);
		}
		System.out.println(site + " : " + res);
		return res;
	}

	private static float calcCost(String site, Relation r) {
		float res = 0;

		// cas R�sultat interm�diaire feuille : cout d�j� calcul�
		if (r instanceof VariableTable && !BD.isATable((VariableTable) r)) {
			VariableTable vt = (VariableTable) r;
			if (!varInterCouts.containsKey(vt.getName())) {
				return -3;
			}
			float cost = varInterCouts.get(vt.getName());
//			vt.nTuples = cost;
			// res=cost;
		}
		// cas Table : transfert des tuples
		else if (/* r instanceof Table || */r instanceof VariableTable
				&& BD.isATable((VariableTable) r)) {
			// cout = nb tuples d�plac�s * cout de deplacement
			// Table table = (r instanceof Table ? (Table) r :
			// BD.tables.get(((VariableTable)r).getName()));
			Table table = BD.getTable((VariableTable) r);
			if (BD.isHostedOn(table) == null)
				return -2;
			String[] siteNames = BD.isHostedOn(table).split("\n");
			float coutMin = Float.MAX_VALUE;
			for (String siteName : siteNames) {
				if (siteName.equals(site)) {
					coutMin = 0;
					break;
				} else if (cost(site, siteName) < coutMin) {
					coutMin = cost(site, siteName);
				}
			}
			r.nTuples = table.tupleCount();
			System.out.println(table.getName()+" : "+r.nTuples+" tuples.");
			res = coutMin * r.nTuples;

		} else if (r instanceof BinaryRelation) {
			res = calcCost(site, ((BinaryRelation) r).getLeft());
			res += calcCost(site, ((BinaryRelation) r).getRight());
			
			r.nTuples = Math.max(((BinaryRelation) r).getLeft().nTuples, ((BinaryRelation) r).getRight().nTuples);
	
		} else if (r instanceof NAryRelation) {
			for (Relation rel : ((NAryRelation) r).getRelations()) {
				res += calcCost(site, rel);
				r.nTuples = Math.max(r.nTuples, rel.nTuples);
			}
		} else if (r instanceof UnaryRelation) {
			res = calcCost(site, ((UnaryRelation) r).getRelation());
			r.nTuples = ((UnaryRelation) r).getRelation().nTuples;
		}
		r.cost = res;
		return res;
	}

	/**
	 * Calcule le meilleur plan de requ�tes distribu� possible pour une requ�te,
	 * en mati�re de co�t
	 * 
	 * @param r
	 *            L'arbre d'alg�bre relationnelle de la requ�te
	 * @return Le plan de requ�tes optimis�
	 */
	public QueryPlan optimizeQuery(Relation r) {
		return null;
	}

	static QueryOptimizer randomInit(int nbSites, int nbTables) {
		// init
		List<String> lnomSites = new ArrayList<String>();
		List<VariableTable> lfrag = new ArrayList<VariableTable>();
		// init des sites
		for (int i = 0; i < nbSites; i++) {
			lnomSites.add("Site" + i);
		}

		// init des fragments
		int nbfrag, site, nbAtt;
		char car;
		VariableTable fragment;
		Table table;
		for (int i = 0; i < nbTables; i++) {
			car = (char) ('A' + i);

			// Cr�ation de la table
			Schema schema = new Schema();
			// entre 1 et 5 attributs
			nbAtt = 1 + (int) (Math.random() * 5);
			for (int j = 0; j < nbAtt; j++) {
				schema.add(new Attribute("" + car, "att" + j));
			}
			// on cr�e la table vide
			table = TableParser.parseFromCsv("" + car, "");
			/* d� modifi� la visibilit� de setSchema */
			table.setSchema(schema);

			// Cr�ation des fragments
			// entre 1 et 4 fragments
			nbfrag = 1 + (int) (Math.random() * 4);

			for (int j = 0; j < nbfrag; j++) {
				site = (int) (Math.random() * nbSites);
				fragment = new VariableTable("" + car + j, lnomSites.get(site));
				// la projection n'est pas exacte, mais c'est le principe qui
				// compte :)
				fragment.setRelation(new Projection(schema, table));
				lfrag.add(fragment);
			}
		}

		// return new QueryOptimizer(lnomSites, lfrag, cStock, cComm);
		return null;
	}

	public static float cost(String siteDest, String s2) {
		return coutsStockage.get(siteDest) + coutsComm.get(siteDest, s2);
	}

	public static void addDefaultCost(String site1) {
		if (coutsStockage == null)
			coutsStockage = new HashMap<String, Float>();
		if (coutsComm == null)
			coutsComm = new Matrice();

		// cout stockage
		coutsStockage.put(site1, (float) 0.01);
		// cout comm
		for (Engine site : BD.sites.values()) {
			if (!site.getName().equals(site1))
				coutsComm.set(site1, site.getName(), 1);
		}
	}
}

class Matrice extends HashMap<String, Float> {

	public Matrice() {
		super();
		// for (Engine site : BD.sites.values()) {
		// QueryOptimizer.addDefaultCost(site.getName());
		// }
	}

	private void add(String site1, String site2, float cout) {
		this.put(site1 + "-" + site2, cout);
	}

	public float get(String site1, String site2) {
		if (this.containsKey(site1 + "-" + site2)) {
			return this.get(site1 + "-" + site2);
		} else if (this.containsKey(site2 + "-" + site1)) {
			return this.get(site2 + "-" + site1);
		}
		return -1;
	}

	public void set(String site1, String site2, float cout) {
		if (!this.containsKey(site1 + "-" + site2)
				&& !this.containsKey(site2 + "-" + site1)) {
			this.add(site1, site2, cout);
		} else {
			if (this.containsKey(site1)) {
				this.remove(site1);
			} else {
				this.remove(site2);
			}
			this.add(site1, site2, cout);
		}
	}

}