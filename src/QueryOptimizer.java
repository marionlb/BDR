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

	/**
	 * Les sites composant la base de données répartie
	 */
	HashMap<String, Engine> listeSites;

	/**
	 * Le nom et la composition (i.e. expression d'algèbre relationnelle) de
	 * chacun des fragments + La solution d'allocation des fragments sur chacun
	 * des sites (i.e. quel fragment est hébergé sur quel site)
	 */
	List<VariableTable> listeFragments;

	/**
	 * Tableau des coûts de stockage
	 */
	// float[] coutsStockage;
	HashMap<String, Float> coutsStockage;
	/**
	 * Matrice des coûts de communication
	 */
	// float[][] coutsComm;
	Matrice coutsComm;

	/**
	 * Calcule le coût d'un plan de requêtes donné, à partir des coûts de
	 * stockage et de communication prédéfinis.
	 * 
	 * @param qp
	 *            Le plan de requête qu'on analyse
	 * @return Le coût total du plan de requêtes.
	 */
	public float getCost(QueryPlan qp) {
		float res = 0;
		for (Iterator<Entry<String, Set<Relation>>> iterator = qp.entrySet()
				.iterator(); iterator.hasNext();) {
			Entry<String, Set<Relation>> type = iterator.next();
			String nomSite = type.getKey();
			Relation r = type.getValue().iterator().next();
			res += getCost(nomSite, r);
		}
		return res;
	}

	private float getCost(String site, Relation r) {
		if (r instanceof VariableTable) {
			VariableTable frag = (VariableTable) r;
			if (frag.getSite().equals(site)) {
				return 0;
			} else {
				String site2 = frag.getSite();
				float coutStock = coutsStockage.get(site);
				float coutComm = coutsComm.get(site, site2);
				int tuplesDeplaces = listeSites.get(site).getTuplesReceived();
				return tuplesDeplaces * (coutComm + coutStock);
			}
		}
		int souscout = 0;
		if(r instanceof BinaryRelation) {
			souscout+=getCost(site,((BinaryRelation)r).getLeft());
			souscout+=getCost(site,((BinaryRelation)r).getRight());
		} else if (r instanceof NAryRelation) {
			for (Relation rel : ((NAryRelation)r).getM_relations()) {
				souscout+=getCost(site, rel);
			}
		} else if (r instanceof UnaryRelation) {
			
		} else if (r instanceof Table) {
			System.err.println("On a une Table dans l'évaluation des couts");
		}
		return 0;
	}

	/**
	 * Calcule le meilleur plan de requêtes distribué possible pour une requête,
	 * en matière de coût
	 * 
	 * @param r
	 *            L'arbre d'algèbre relationnelle de la requête
	 * @return Le plan de requêtes optimisé
	 */
	public QueryPlan optimizeQuery(Relation r) {
		return null;
	}


	// public QueryOptimizer(List<String> listeSites,
	// List<VariableTable> listeFragments, float[] coutsStockage,
	// float[][] coutsComm) {
	// super();
	// this.listeSites = listeSites;
	// this.listeFragments = listeFragments;
	// this.coutsStockage = coutsStockage;
	// this.coutsComm = coutsComm;
	// }

	// //////////////////////////////////////////////
	// ////////GETTERS ++ SETTERS////////////////////
	// //////////////////////////////////////////////

	public List<VariableTable> getListeFragments() {
		return listeFragments;
	}

	public void setListeFragments(List<VariableTable> listeFragments) {
		this.listeFragments = listeFragments;
	}

	// public float[] getCoutsStockage() {
	// return coutsStockage;
	// }
	//
	// public void setCoutsStockage(float[] coutsStockage) {
	// this.coutsStockage = coutsStockage;
	// }
	//
	// public float[][] getCoutsComm() {
	// return coutsComm;
	// }
	//
	// public void setCoutsComm(float[][] coutsComm) {
	// this.coutsComm = coutsComm;
	// }

	public HashMap<String, Float> getCoutsStockage() {
		return coutsStockage;
	}

	public void setCoutsStockage(HashMap<String, Float> coutsStockage) {
		this.coutsStockage = coutsStockage;
	}

	public Matrice getCoutsComm() {
		return coutsComm;
	}

	public void setCoutsComm(Matrice coutsComm) {
		this.coutsComm = coutsComm;
	}

	static QueryOptimizer randomInit(int nbSites, int nbTables) {
		// init
		List<String> lnomSites = new ArrayList<String>();
		List<VariableTable> lfrag = new ArrayList<VariableTable>();
		float[] cStock = new float[nbSites];
		float[][] cComm = new float[nbSites][nbSites];

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

			// Création de la table
			Schema schema = new Schema();
			// entre 1 et 5 attributs
			nbAtt = 1 + (int) (Math.random() * 5);
			for (int j = 0; j < nbAtt; j++) {
				schema.add(new Attribute("" + car, "att" + j));
			}
			// on crée la table vide
			table = TableParser.parseFromCsv("" + car, "");
			/* dû modifié la visibilité de setSchema */
			table.setSchema(schema);

			// Création des fragments
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

	public static void main(String[] args) {
		randomInit(4, 5);
	}

	private class Matrice extends HashMap<String, Float> {
		public Matrice() {
			super();
		}

		public Matrice(String csv) {
			String[] tab = csv.split(",");
			for (String string : tab) {
				String[] trio = string.split("[ -]");
				this.add(trio[0], trio[1], Float.parseFloat(trio[2]));
			}
		}

		public void add(String site1, String site2, float cout) {
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
	}
}
